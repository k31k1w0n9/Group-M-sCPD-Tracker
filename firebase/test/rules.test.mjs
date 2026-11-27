// Rules tests. Run with `npm test`: it starts the Firestore emulator, runs these, and stops it.
import { readFileSync } from 'node:fs';
import { after, before, beforeEach, describe, it } from 'node:test';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import {
  Timestamp, arrayRemove, arrayUnion, collection, doc, getDoc, getDocs, increment, orderBy, query, serverTimestamp, setDoc,
  updateDoc, where, writeBatch,
} from 'firebase/firestore';
import { backupCode, qrSig, windowAt } from './codes.mjs';

const SECRET = 'a'.repeat(16) + 'test-session-secret-0123456789';
const HOUR = 3_600_000;
const ts = (ms) => Timestamp.fromMillis(ms);

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-scpd-rules',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});
after(async () => env?.cleanup());

const as = (uid) => env.authenticatedContext(uid).firestore();

/** Writes the starting data with the rules switched off. */
async function seed(extra = async () => {}) {
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const now = Date.now();
    await setDoc(doc(db, 'admins/office'), { name: 's-CPD office' });
    await setDoc(doc(db, 'clubs/cs'), { name: 'CS Society' });
    await setDoc(doc(db, 'clubs/cs/members/com'), { role: 'committee' });
    await setDoc(doc(db, 'clubs/cs/members/scan'), { role: 'scanner' });
    await setDoc(doc(db, 'users/alice'), { displayName: 'Alice', studentId: '2001', interests: [], hasChosenInterests: false, deviceId: 'phone-a', deviceBoundAt: ts(now - 48 * HOUR) });
    await setDoc(doc(db, 'users/bob'), { displayName: 'Bob', studentId: '2002', interests: [], hasChosenInterests: false, deviceId: 'phone-b', deviceBoundAt: ts(now - 48 * HOUR) });
    await setDoc(doc(db, 'users/com'), { displayName: 'Com', studentId: '2003', interests: [], hasChosenInterests: false });
    const base = {
      clubId: 'cs', clubName: 'CS Society', description: 'x', venue: 'F1A15', tags: ['Tech'],
      attendanceMode: 'ArrivalOnly', pointsRequested: true, featured: false, createdBy: 'com', waitlist: [],
      bookedCount: 0, capacity: 2, reviewComment: null, cancelReason: null, checkpointOpen: false,
      checkInOpenedAt: null, checkInClosedAt: null, checkpointOpenedAt: null, openedBy: null, submittedAt: null,
    };
    await setDoc(doc(db, 'events/future'), { ...base, title: 'Future talk', reviewStatus: 'Approved', state: 'BookingOpen', start: ts(now + 72 * HOUR), end: ts(now + 74 * HOUR) });
    await setDoc(doc(db, 'events/draft'), { ...base, title: 'Draft talk', reviewStatus: 'Draft', state: 'BookingOpen', start: ts(now + 72 * HOUR), end: ts(now + 74 * HOUR) });
    await setDoc(doc(db, 'events/live'), {
      ...base, title: 'Live workshop', reviewStatus: 'Approved', state: 'CheckInOpen', bookedCount: 2,
      start: ts(now - HOUR / 2), end: ts(now + HOUR), checkInOpenedAt: ts(now - 10 * 60_000), openedBy: 'com',
    });
    await setDoc(doc(db, 'events/closed'), {
      ...base, title: 'Closed workshop', reviewStatus: 'Approved', state: 'CheckInClosed', bookedCount: 1,
      start: ts(now - 5 * HOUR), end: ts(now - 4 * HOUR), checkInOpenedAt: ts(now - 5 * HOUR), checkInClosedAt: ts(now - 3 * HOUR),
    });
    await setDoc(doc(db, 'events/justclosed'), {
      ...base, title: 'Just closed', reviewStatus: 'Approved', state: 'CheckInClosed', bookedCount: 1,
      start: ts(now - 2 * HOUR), end: ts(now - HOUR), checkInOpenedAt: ts(now - 2 * HOUR), checkInClosedAt: ts(now - 30 * 60_000),
    });
    for (const id of ['live', 'closed', 'justclosed']) await setDoc(doc(db, `eventSecrets/${id}`), { secret: SECRET, createdBy: 'com' });
    const booked = (eventId, user) => setDoc(doc(db, `bookings/${eventId}_${user}`), {
      eventId, userId: user, clubId: 'cs', status: 'booked', createdAt: ts(now - 24 * HOUR), name: user, studentId: '1', requestId: 'r',
    });
    await booked('live', 'alice');
    await booked('live', 'bob');
    await booked('closed', 'alice');
    await booked('justclosed', 'alice');
    await extra(db, now);
  });
}

beforeEach(async () => {
  await env.clearFirestore();
  await seed();
});

// ---- Events ------------------------------------------------------------------------------------

describe('events', () => {
  const draft = (overrides = {}) => ({
    clubId: 'cs', clubName: 'CS Society', title: 'New talk', description: 'About things', venue: 'F1A15',
    capacity: 40, start: ts(Date.now() + 96 * HOUR), end: ts(Date.now() + 98 * HOUR), tags: ['Tech'],
    attendanceMode: 'ArrivalOnly', pointsRequested: true, reviewStatus: 'Draft', state: 'BookingOpen',
    bookedCount: 0, waitlist: [], featured: false, createdBy: 'com', ...overrides,
  });

  it('a committee member creates a draft for their own club', async () => {
    await assertSucceeds(setDoc(doc(as('com'), 'events/new1'), draft()));
  });

  it('a student cannot create an event', async () => {
    await assertFails(setDoc(doc(as('alice'), 'events/new2'), draft({ createdBy: 'alice' })));
  });

  it('a committee member cannot create an already approved event', async () => {
    await assertFails(setDoc(doc(as('com'), 'events/new3'), draft({ reviewStatus: 'Approved' })));
  });

  it('a draft can be submitted when it starts more than 2 days ahead', async () => {
    await assertSucceeds(updateDoc(doc(as('com'), 'events/draft'), { reviewStatus: 'PendingReview', submittedAt: serverTimestamp() }));
  });

  it('a draft starting in under 2 days cannot be submitted', async () => {
    await assertFails(updateDoc(doc(as('com'), 'events/draft'), {
      reviewStatus: 'PendingReview', submittedAt: serverTimestamp(), start: ts(Date.now() + 24 * HOUR), end: ts(Date.now() + 26 * HOUR),
    }));
  });

  it('the committee cannot approve its own event', async () => {
    await assertFails(updateDoc(doc(as('com'), 'events/draft'), { reviewStatus: 'Approved' }));
  });

  it('the office can approve an event', async () => {
    await assertSucceeds(updateDoc(doc(as('office'), 'events/draft'), { reviewStatus: 'Approved' }));
  });

  it('approved details are locked for the committee', async () => {
    await assertFails(updateDoc(doc(as('com'), 'events/future'), { title: 'Changed title' }));
  });

  it('students see approved events but not drafts', async () => {
    await assertSucceeds(getDoc(doc(as('alice'), 'events/future')));
    await assertFails(getDoc(doc(as('alice'), 'events/draft')));
  });

  it('opening check-in needs the session secret in the same batch', async () => {
    const db = as('com');
    await assertFails(updateDoc(doc(db, 'events/future'), {
      state: 'CheckInOpen', checkInOpenedAt: serverTimestamp(), checkInClosedAt: null, openedBy: 'com',
    }));
    const batch = writeBatch(db);
    batch.set(doc(db, 'eventSecrets/future'), { secret: SECRET, createdBy: 'com' });
    batch.update(doc(db, 'events/future'), { state: 'CheckInOpen', checkInOpenedAt: serverTimestamp(), checkInClosedAt: null, openedBy: 'com' });
    await assertSucceeds(batch.commit());
  });

  it('students cannot read the session secret', async () => {
    await assertFails(getDoc(doc(as('alice'), 'eventSecrets/live')));
    await assertSucceeds(getDoc(doc(as('com'), 'eventSecrets/live')));
  });

  it('a scanner runs check-in but cannot create, edit or cancel events', async () => {
    const db = as('scan');
    await assertFails(setDoc(doc(db, 'events/new4'), draft({ createdBy: 'scan' })));
    await assertFails(updateDoc(doc(db, 'events/draft'), { title: 'Renamed by the scanner' }));
    await assertFails(updateDoc(doc(db, 'events/future'), { state: 'Cancelled', cancelReason: 'Not my call', checkpointOpen: false }));
    const batch = writeBatch(db);
    batch.set(doc(db, 'eventSecrets/future'), { secret: SECRET, createdBy: 'scan' });
    batch.update(doc(db, 'events/future'), { state: 'CheckInOpen', checkInOpenedAt: serverTimestamp(), checkInClosedAt: null, openedBy: 'scan' });
    await assertSucceeds(batch.commit());
    await assertSucceeds(updateDoc(doc(db, 'events/future'), { state: 'CheckInClosed', checkpointOpen: false, checkInClosedAt: serverTimestamp() }));
  });
});

// ---- Bookings ----------------------------------------------------------------------------------

describe('bookings', () => {
  const booking = (eventId, user, status = 'booked') => ({
    eventId, userId: user, clubId: 'cs', status, createdAt: serverTimestamp(),
    name: user === 'alice' ? 'Alice' : 'Bob', studentId: user === 'alice' ? '2001' : '2002', requestId: 'req-1',
  });

  async function book(user, eventId = 'future') {
    const db = as(user);
    const batch = writeBatch(db);
    batch.set(doc(db, `bookings/${eventId}_${user}`), booking(eventId, user));
    batch.update(doc(db, `events/${eventId}`), { bookedCount: increment(1) });
    return batch.commit();
  }

  it('a student books a place and the count goes up in the same batch', async () => {
    await assertSucceeds(book('alice'));
  });

  it('a booking without the count update is refused', async () => {
    await assertFails(setDoc(doc(as('alice'), 'bookings/future_alice'), booking('future', 'alice')));
  });

  it('a student cannot book for someone else', async () => {
    const db = as('alice');
    const batch = writeBatch(db);
    batch.set(doc(db, 'bookings/future_bob'), booking('future', 'bob'));
    batch.update(doc(db, 'events/future'), { bookedCount: increment(1) });
    await assertFails(batch.commit());
  });

  it('a full event cannot be booked, but the student can join the waitlist', async () => {
    await env.withSecurityRulesDisabled((ctx) => updateDoc(doc(ctx.firestore(), 'events/future'), { bookedCount: 2 }));
    await assertFails(book('alice'));
    const db = as('alice');
    const batch = writeBatch(db);
    batch.set(doc(db, 'bookings/future_alice'), booking('future', 'alice', 'waitlisted'));
    batch.update(doc(db, 'events/future'), { waitlist: arrayUnion('alice') });
    await assertSucceeds(batch.commit());
  });

  it('a student cannot join the waitlist of an event with places left', async () => {
    const db = as('alice');
    const batch = writeBatch(db);
    batch.set(doc(db, 'bookings/future_alice'), booking('future', 'alice', 'waitlisted'));
    batch.update(doc(db, 'events/future'), { waitlist: arrayUnion('alice') });
    await assertFails(batch.commit());
  });

  it('a draft event cannot be booked', async () => {
    await assertFails(book('alice', 'draft'));
  });

  it('cancelling gives the place to the first student on the waitlist', async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await updateDoc(doc(db, 'events/future'), { bookedCount: 2, waitlist: ['bob', 'carol'] });
      await setDoc(doc(db, 'bookings/future_alice'), { ...booking('future', 'alice'), createdAt: ts(Date.now()) });
      await setDoc(doc(db, 'bookings/future_bob'), { ...booking('future', 'bob', 'waitlisted'), createdAt: ts(Date.now()) });
      await setDoc(doc(db, 'bookings/future_carol'), { ...booking('future', 'carol', 'waitlisted'), userId: 'carol', createdAt: ts(Date.now()) });
    });
    const db = as('alice');
    // Giving the place to the second student in the queue is refused.
    const wrong = writeBatch(db);
    wrong.update(doc(db, 'bookings/future_alice'), { status: 'cancelled' });
    wrong.update(doc(db, 'bookings/future_carol'), { status: 'booked' });
    wrong.update(doc(db, 'events/future'), { waitlist: arrayRemove('carol') });
    await assertFails(wrong.commit());

    const right = writeBatch(db);
    right.update(doc(db, 'bookings/future_alice'), { status: 'cancelled' });
    right.update(doc(db, 'bookings/future_bob'), { status: 'booked' });
    right.update(doc(db, 'events/future'), { waitlist: arrayRemove('bob') });
    await assertSucceeds(right.commit());
  });

  it('a booking cannot be cancelled within 2 hours of the start', async () => {
    const db = as('alice');
    const batch = writeBatch(db);
    batch.update(doc(db, 'bookings/live_alice'), { status: 'cancelled' });
    batch.update(doc(db, 'events/live'), { bookedCount: increment(-1) });
    await assertFails(batch.commit());
  });

  it('a student cannot read another student\'s booking, but the organising club can', async () => {
    await assertFails(getDoc(doc(as('bob'), 'bookings/live_alice')));
    await assertSucceeds(getDoc(doc(as('com'), 'bookings/live_alice')));
  });
});

// ---- Attendance --------------------------------------------------------------------------------

describe('attendance', () => {
  const qrStage = (eventId, window, { offline = false, sig } = {}) => ({
    method: 'qr', window, sig: sig ?? qrSig(SECRET, eventId, 'arrival', window), offline,
    receivedAt: serverTimestamp(), requestId: 'req-' + window,
  });

  async function claim(user, eventId, stage, { device, flagged = null } = {}) {
    const db = as(user);
    const deviceId = device ?? (user === 'alice' ? 'phone-a' : 'phone-b');
    const batch = writeBatch(db);
    const use = await getDoc(doc(db, `deviceUse/${eventId}_${deviceId}`));
    if (!use.exists()) batch.set(doc(db, `deviceUse/${eventId}_${deviceId}`), { eventId, deviceId, uid: user, at: serverTimestamp() });
    batch.set(doc(db, `attendance/${eventId}_${user}`), { eventId, userId: user, clubId: 'cs', deviceId, arrival: stage, flagged });
    return batch.commit();
  }

  it('the live QR code is accepted', async () => {
    await assertSucceeds(claim('alice', 'live', qrStage('live', windowAt(Date.now()))));
  });

  it('an altered QR code is refused', async () => {
    await assertFails(claim('alice', 'live', qrStage('live', windowAt(Date.now()), { sig: 'f'.repeat(64) })));
  });

  it('an old QR code (a screenshot from earlier) is refused', async () => {
    const old = windowAt(Date.now()) - 4;
    await assertFails(claim('alice', 'live', qrStage('live', old)));
  });

  it('a QR code for another event is refused', async () => {
    const w = windowAt(Date.now());
    await assertFails(claim('alice', 'live', { ...qrStage('live', w), sig: qrSig(SECRET, 'closed', 'arrival', w) }));
  });

  it('a scan saved offline uploads within 2 hours of check-in closing', async () => {
    const issued = windowAt(Date.now() - 60 * 60_000); // during the session, an hour ago
    await assertSucceeds(claim('alice', 'justclosed', qrStage('justclosed', issued, { offline: true })));
  });

  it('a scan saved offline is refused more than 2 hours after check-in closed', async () => {
    const issued = windowAt(Date.now() - 4 * HOUR);
    await assertFails(claim('alice', 'closed', qrStage('closed', issued, { offline: true })));
  });

  it('a student without a booking cannot check in', async () => {
    await env.withSecurityRulesDisabled((ctx) => updateDoc(doc(ctx.firestore(), 'bookings/live_alice'), { status: 'cancelled' }));
    await assertFails(claim('alice', 'live', qrStage('live', windowAt(Date.now()))));
  });

  it('a phone that is not bound to the account cannot make a clean claim', async () => {
    await assertFails(claim('alice', 'live', qrStage('live', windowAt(Date.now())), { device: 'someone-elses-phone' }));
  });

  it('a second account on the same phone is only saved as Under review, and flags the first account', async () => {
    const w = windowAt(Date.now());
    await assertSucceeds(claim('alice', 'live', qrStage('live', w)));
    // Bob signs in on Alice's phone and tries a clean claim: refused.
    await assertFails(claim('bob', 'live', qrStage('live', w), { device: 'phone-a' }));
    // Saved as under review, and Alice's record is flagged in the same batch.
    const db = as('bob');
    const batch = writeBatch(db);
    const flag = 'Same phone used by another account';
    batch.set(doc(db, 'attendance/live_bob'), { eventId: 'live', userId: 'bob', clubId: 'cs', deviceId: 'phone-a', arrival: qrStage('live', w), flagged: flag });
    batch.update(doc(db, 'attendance/live_alice'), { flagged: flag });
    await assertSucceeds(batch.commit());
  });

  it('a student cannot remove a flag', async () => {
    await env.withSecurityRulesDisabled((ctx) => setDoc(doc(ctx.firestore(), 'attendance/live_alice'), {
      eventId: 'live', userId: 'alice', clubId: 'cs', deviceId: 'phone-a', arrival: null, flagged: 'Same phone used by another account',
    }));
    const db = as('alice');
    await assertFails(updateDoc(doc(db, 'attendance/live_alice'), { arrival: qrStage('live', windowAt(Date.now())), flagged: null }));
  });

  it('a backup code counts only after the attempt is registered, and only 5 tries are allowed', async () => {
    const w = windowAt(Date.now());
    const code = backupCode(SECRET, 'live', 'arrival', w);
    const stage = { method: 'code', window: w, code, offline: false, receivedAt: serverTimestamp(), requestId: 'req-code' };
    // Without registering the attempt first: refused.
    await assertFails(claim('alice', 'live', stage));
    const db = as('alice');
    for (let i = 1; i <= 5; i++) {
      const wrong = String((Number(code) + i) % 1_000_000).padStart(6, '0');
      const ref = doc(db, 'codeAttempts/live_alice');
      await assertSucceeds(i === 1
        ? setDoc(ref, { eventId: 'live', count: 1, lastCode: wrong, at: serverTimestamp() })
        : updateDoc(ref, { count: i, lastCode: wrong, at: serverTimestamp() }));
    }
    // The 6th try is refused, so even the right code can't be registered now.
    await assertFails(updateDoc(doc(db, 'codeAttempts/live_alice'), { count: 6, lastCode: code, at: serverTimestamp() }));
  });

  it('the right backup code is accepted after registering the attempt', async () => {
    const w = windowAt(Date.now());
    const code = backupCode(SECRET, 'live', 'arrival', w);
    const db = as('alice');
    await assertSucceeds(setDoc(doc(db, 'codeAttempts/live_alice'), { eventId: 'live', count: 1, lastCode: code, at: serverTimestamp() }));
    await assertSucceeds(claim('alice', 'live', { method: 'code', window: w, code, offline: false, receivedAt: serverTimestamp(), requestId: 'req-code' }));
  });

  it('a verified stage cannot be overwritten', async () => {
    const w = windowAt(Date.now());
    await assertSucceeds(claim('alice', 'live', qrStage('live', w)));
    await assertFails(updateDoc(doc(as('alice'), 'attendance/live_alice'), { arrival: qrStage('live', w - 1) }));
  });
});

// ---- Points ------------------------------------------------------------------------------------

describe('points ledger', () => {
  const award = (eventId, user, title) => ({
    userId: user, eventId, eventTitle: title, eventDate: '2026-11-26', points: 3, reason: 'Verified attendance', createdAt: serverTimestamp(),
  });

  async function verifiedArrival(user = 'alice', flagged = null) {
    await env.withSecurityRulesDisabled((ctx) => setDoc(doc(ctx.firestore(), `attendance/live_${user}`), {
      eventId: 'live', userId: user, clubId: 'cs', deviceId: 'phone-a', flagged,
      arrival: { method: 'qr', window: 1, sig: 'x', offline: false, receivedAt: ts(Date.now()), requestId: 'r' },
    }));
  }

  it('a student writes their own +3 once for a verified record', async () => {
    await verifiedArrival();
    await assertSucceeds(setDoc(doc(as('alice'), 'ledger/live_alice_award'), award('live', 'alice', 'Live workshop')));
    // The fixed ID means a second award is an update, and the ledger is append-only.
    await assertFails(setDoc(doc(as('alice'), 'ledger/live_alice_award'), award('live', 'alice', 'Live workshop')));
  });

  it('no award without a verified record', async () => {
    await assertFails(setDoc(doc(as('alice'), 'ledger/live_alice_award'), award('live', 'alice', 'Live workshop')));
  });

  it('no award while the record is under review', async () => {
    await verifiedArrival('alice', 'Same phone used by another account');
    await assertFails(setDoc(doc(as('alice'), 'ledger/live_alice_award'), award('live', 'alice', 'Live workshop')));
  });

  it('a student cannot award themselves more than 3 points', async () => {
    await verifiedArrival();
    await assertFails(setDoc(doc(as('alice'), 'ledger/live_alice_award'), { ...award('live', 'alice', 'Live workshop'), points: 30 }));
  });

  it('own-club attendance earns no points', async () => {
    await env.withSecurityRulesDisabled((ctx) => setDoc(doc(ctx.firestore(), 'clubs/cs/members/alice'), { role: 'scanner' }));
    await verifiedArrival();
    await assertFails(setDoc(doc(as('alice'), 'ledger/live_alice_award'), award('live', 'alice', 'Live workshop')));
  });
});

// ---- Users -------------------------------------------------------------------------------------

describe('users', () => {
  it('a student can save interests but not change their student ID', async () => {
    const db = as('alice');
    await assertSucceeds(updateDoc(doc(db, 'users/alice'), { interests: ['Tech'], hasChosenInterests: true }));
    await assertFails(updateDoc(doc(db, 'users/alice'), { studentId: '9999' }));
  });

  it('switching phones waits for the 24-hour cooldown', async () => {
    const db = as('alice');
    await assertSucceeds(updateDoc(doc(db, 'users/alice'), { deviceId: 'phone-new', deviceBoundAt: serverTimestamp() }));
    await assertFails(updateDoc(doc(db, 'users/alice'), { deviceId: 'phone-newer', deviceBoundAt: serverTimestamp() }));
  });

  it('students cannot read other students', async () => {
    await assertFails(getDoc(doc(as('alice'), 'users/bob')));
  });
});

// ---- Queries the app runs ----------------------------------------------------------------------

describe('app queries', () => {
  it('students list approved events, not every event', async () => {
    const db = as('alice');
    await assertSucceeds(getDocs(query(collection(db, 'events'), where('reviewStatus', '==', 'Approved'), orderBy('start'))));
    await assertFails(getDocs(collection(db, 'events')));
  });

  it('the committee lists every event of its own club, but not another club', async () => {
    const db = as('com');
    await assertSucceeds(getDocs(query(collection(db, 'events'), where('clubId', '==', 'cs'))));
    await assertFails(getDocs(query(collection(db, 'events'), where('clubId', '==', 'arts'))));
  });

  it('students list only their own bookings, attendance and ledger', async () => {
    const db = as('alice');
    for (const name of ['bookings', 'attendance', 'ledger']) {
      await assertSucceeds(getDocs(query(collection(db, name), where('userId', '==', 'alice'))));
      await assertFails(getDocs(query(collection(db, name), where('userId', '==', 'bob'))));
    }
  });

  it('the committee lists bookings and attendance for its own event', async () => {
    const db = as('com');
    await assertSucceeds(getDocs(query(collection(db, 'bookings'), where('eventId', '==', 'live'), where('clubId', '==', 'cs'))));
    await assertSucceeds(getDocs(query(collection(db, 'attendance'), where('eventId', '==', 'live'), where('clubId', '==', 'cs'))));
    await assertFails(getDocs(query(collection(db, 'bookings'), where('eventId', '==', 'live'))));
  });

  it("a student can read their own missing booking or record by ID, but not someone else's", async () => {
    const db = as('alice');
    await assertSucceeds(getDoc(doc(db, 'bookings/future_alice')));
    await assertSucceeds(getDoc(doc(db, 'attendance/future_alice')));
    await assertFails(getDoc(doc(db, 'attendance/live_bob')));
    await assertFails(getDoc(doc(db, 'bookings/future_bob')));
  });
});
