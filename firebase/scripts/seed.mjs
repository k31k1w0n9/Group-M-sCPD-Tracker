// Loads the demo world into the local Firebase emulators: the same accounts, clubs and events as the app's
// in-memory demo (app/.../data/fake/FakeData.kt). Dates are relative to now, so there is always an event happening.
//
//   npm run emulators            (in one terminal)
//   npm run seed                 (in another)
//
// Real (free Spark) project, for the build the markers use. Needs a service-account key file from
// Firebase console > Project settings > Service accounts (keep it out of git):
//
//   GOOGLE_APPLICATION_CREDENTIALS=path/to/key.json node scripts/seed.mjs --live --project <project-id>
//
// --live never deletes anything unless you also pass --reset (which empties the whole project first).
// Team rule: only Keiki runs --live --reset, once, before a release. It also needs --confirm-wipe <project-id>,
// so it can't happen by accident or from a copied command.
import { randomBytes } from 'node:crypto';
import { applicationDefault, initializeApp } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { Timestamp, getFirestore } from 'firebase-admin/firestore';

const args = process.argv.slice(2);
const live = args.includes('--live');
const reset = !live || args.includes('--reset');
const projectId = live ? args[args.indexOf('--project') + 1] : (process.env.GCLOUD_PROJECT ?? 'demo-scpd');
if (live) {
  if (!args.includes('--project') || !projectId || projectId.startsWith('--')) throw new Error('Use --live --project <project-id>.');
  if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) throw new Error('Set GOOGLE_APPLICATION_CREDENTIALS to the service-account key file.');
  if (reset && args[args.indexOf('--confirm-wipe') + 1] !== projectId) {
    throw new Error(`--reset wipes the whole live project "${projectId}". Only Keiki runs it, before a release. ` +
      `To go ahead, add: --confirm-wipe ${projectId}`);
  }
  delete process.env.FIRESTORE_EMULATOR_HOST;
  delete process.env.FIREBASE_AUTH_EMULATOR_HOST;
} else {
  process.env.FIRESTORE_EMULATOR_HOST ??= '127.0.0.1:8080';
  process.env.FIREBASE_AUTH_EMULATOR_HOST ??= '127.0.0.1:9099';
  if (!projectId.startsWith('demo-')) throw new Error('Without --live the seed only runs against a demo- project on the emulators.');
}

initializeApp(live ? { projectId, credential: applicationDefault() } : { projectId });
const auth = getAuth();
const db = getFirestore();

export const PASSWORD = 'scpd-demo';
const ZONE_OFFSET_MS = 8 * 3_600_000; // Malaysia time, UTC+8, no daylight saving
const MIN = 60_000;
const HOUR = 60 * MIN;
const DAY = 24 * HOUR;
const now = Date.now();
const ts = (ms) => Timestamp.fromMillis(ms);

// Local midnight today in Malaysia, and the Monday of this week.
const today = Math.floor((now + ZONE_OFFSET_MS) / DAY) * DAY - ZONE_OFFSET_MS;
const weekday = (new Date(today + ZONE_OFFSET_MS).getUTCDay() + 6) % 7; // Monday = 0
const thisWeek = today - weekday * DAY;
const at = (days, hhmm) => { const [h, m] = hhmm.split(':').map(Number); return today + days * DAY + h * HOUR + m * MIN; };
const weeksAgo = (weeks, dayOffset, hhmm) => { const [h, m] = hhmm.split(':').map(Number); return thisWeek - weeks * 7 * DAY + dayOffset * DAY + h * HOUR + m * MIN; };

const clubs = {
  'cs-society': { name: 'Computer Science Society', contact: 'cssociety@nottingham.edu.my' },
  'arts-club': { name: 'Arts Club', contact: 'artsclub@nottingham.edu.my' },
  rotaract: { name: 'Rotaract Club of the University of Nottingham Malaysia', contact: 'rotaract@nottingham.edu.my' },
  sports: { name: 'Sports Council', contact: 'sports@nottingham.edu.my' },
};

const classmates = ['Aiden Tan', 'Bella Ong', 'Chen Wei', 'Divya Nair', 'Ethan Lau', 'Farah Aziz', 'Grace Lim', 'Hafiz Ismail',
  'Ivy Chong', 'Jason Wong', 'Kavya Rao', 'Liam Ng', 'Mei Ling', 'Nadia Hassan', 'Omar Rahim', 'Priya Menon', 'Qistina Zain',
  'Ryan Teo', 'Sofia Yap', 'Tariq Ali'];

const people = [
  { uid: 'u-aisha', displayName: 'Aisha Rahman', email: 'student1@scpd.example', studentId: '20612345', interests: ['Tech', 'Career'], hasChosenInterests: true },
  { uid: 'u-daniel', displayName: 'Daniel Lim', email: 'student2@scpd.example', studentId: '20654321', interests: ['Sports', 'Volunteering'], hasChosenInterests: false },
  { uid: 'u-cs', displayName: 'Computer Science Society', email: 'committee.tech@scpd.example', studentId: '20692001', clubIds: ['cs-society'], interests: ['Tech', 'Career'], hasChosenInterests: true },
  // Several personal accounts for one club (owner, committee, scanner): no shared club login.
  { uid: 'u-cs-com', displayName: 'Lee Wei Jie (CS Society committee)', email: 'committee2.tech@scpd.example', studentId: '20692003', clubIds: ['cs-society'], interests: ['Tech'], hasChosenInterests: true },
  { uid: 'u-cs-scan', displayName: 'Nur Aina (CS Society scanner)', email: 'scanner.tech@scpd.example', studentId: '20692004', clubIds: ['cs-society'], interests: ['Tech'], hasChosenInterests: true },
  { uid: 'u-arts', displayName: 'Arts Club', email: 'committee.arts@scpd.example', studentId: '20692002', clubIds: ['arts-club'], interests: ['Arts'], hasChosenInterests: true },
  { uid: 'u-office', displayName: 's-CPD Office', email: 'office@scpd.example', studentId: null, admin: true },
  ...classmates.map((name, i) => ({ uid: `u-c${i}`, displayName: name, email: `c${i}@scpd.example`, studentId: `2060${1000 + i}` })),
];

const liveStart = Math.floor((now - 30 * MIN) / MIN) * MIN;
const justification = 'Open to all students and builds a transferable skill.';
function event(id, club, title, start, hours, venue, capacity, booked, tags, description, o = {}) {
  const end = start + hours * HOUR;
  const approved = (o.review ?? 'Approved') === 'Approved';
  const past = end <= now && approved && o.state !== 'Cancelled';
  const state = past ? 'CheckInClosed' : (o.state ?? 'BookingOpen');
  const hadSession = state === 'CheckInClosed' || state === 'CheckInOpen';
  return {
    id, data: {
      clubId: club, clubName: clubs[club].name, title, description, start: ts(start), end: ts(end), venue, capacity,
      bookedCount: booked, waitlist: [], tags, reviewStatus: o.review ?? 'Approved', reviewComment: o.comment ?? null,
      state, attendanceMode: o.mode ?? 'ArrivalOnly', pointsRequested: true, pointsJustification: justification,
      organiserContact: clubs[club].contact, poster: o.poster ?? 'Teal', featured: o.featured ?? false,
      cancelReason: o.cancelReason ?? null, checkpointOpen: false, createdBy: club === 'arts-club' ? 'u-arts' : 'u-cs',
      submittedAt: ts(start - 10 * DAY), openedBy: hadSession ? 'u-cs' : null,
      checkInOpenedAt: hadSession ? ts(start) : null, checkInClosedAt: state === 'CheckInClosed' ? ts(end) : null,
      checkpointOpenedAt: hadSession && o.mode === 'ArrivalAndCheckpoint' && state === 'CheckInClosed' ? ts(end - 15 * MIN) : null,
    },
  };
}

const events = [
  event('leadership-workshop', 'cs-society', 'Leadership Workshop', liveStart, 2, 'DEWAN 2', 30, 22, ['Leadership', 'Career'],
    'Practise running a team meeting, giving feedback and leading a small project, with student leaders from five societies.',
    { state: 'CheckInOpen', mode: 'ArrivalAndCheckpoint', poster: 'Gold' }),
  event('build-with-ai', 'cs-society', 'Build with AI', at(3, '14:00'), 2, 'F1A02', 40, 28, ['Tech', 'Career'],
    'A hands-on workshop: build a small study helper with an AI API, then talk through what it gets wrong and why.',
    { mode: 'ArrivalAndCheckpoint', poster: 'PhotoFeatured', featured: true }),
  event('python-next-steps', 'cs-society', 'Python: Next Steps', at(5, '15:00'), 2, 'TCR1', 35, 19, ['Tech'],
    'For students who finished Intro to Python: files, testing and a small data project you can put on your CV.'),
  event('tech-talk-android', 'cs-society', 'Tech Talk: Android in Industry', at(5, '16:00'), 2, 'F2A04', 60, 30, ['Tech', 'Career'],
    'Two alumni explain how they ship Android apps at work, and what they look for when hiring interns.', { poster: 'PhotoAi' }),
  event('garden-volunteering', 'rotaract', 'Semenyih Community Garden Volunteering Morning', at(6, '08:30'), 4,
    'Community garden, bus from Main Gate', 25, 25, ['Volunteering'],
    'Plant, weed and build raised beds with the Semenyih community. Transport, gloves and breakfast provided.', { poster: 'Gold' }),
  event('open-mic', 'arts-club', 'Open Mic Night', at(4, '19:30'), 2, 'Café Pulse', 50, 12, ['Arts'],
    'Sing, read or play something for five minutes. First-timers welcome; sign up for a slot at the door.', { poster: 'Gold' }),
  event('five-a-side', 'sports', 'Five-a-side Futsal Cup', at(8, '17:00'), 3, 'Sports Complex', 48, 20, ['Sports'],
    'Teams of five, mixed levels. Bring a friend or join a team on the night.'),
  event('career-fair', 'cs-society', 'Tech Career Fair', at(7, '10:00'), 4, 'Great Hall', 200, 0, ['Career'],
    'Meet recruiters from twelve companies hiring interns and graduates.',
    { state: 'Cancelled', cancelReason: 'The venue is closed for maintenance. We will announce a new date soon.' }),
  event('prototype-playground', 'cs-society', 'Prototype Playground', at(9, '16:00'), 2, 'F2A04', 30, 0, ['Tech'],
    'Bring an idea and leave with a clickable prototype. Mentors from the Design Society help each team.', { review: 'PendingReview' }),
  event('campus-hack-night', 'cs-society', 'Campus Hack Night', at(12, '18:00'), 3, 'F4C08', 60, 0, ['Tech', 'Leadership'],
    "An evening hackathon on campus problems, judged by staff from Estates and the Students' Association.",
    { review: 'ChangesRequested', comment: 'Venue approval needed. Upload the booking confirmation and resubmit.' }),
  event('ai-ethics', 'cs-society', 'AI Ethics Roundtable', at(14, '13:00'), 2, 'Library Seminar Room', 25, 0, ['Tech', 'Leadership'],
    'A guided discussion on fairness and privacy in campus AI tools.', { review: 'Draft' }),
  event('crypto-meetup', 'cs-society', 'Crypto Trading Meetup', at(10, '20:00'), 2, 'F1A02', 40, 0, ['Career'],
    'Share trading tips.', { review: 'Rejected', comment: 'Not eligible for s-CPD points: no learning outcome.' }),
  event('intro-python', 'cs-society', 'Intro to Python', weeksAgo(1, 2, '15:00'), 2, 'TCR1', 35, 30, ['Tech'], 'Your first Python programs.'),
  event('design-sprint', 'arts-club', 'Poster Design Sprint', weeksAgo(1, 4, '14:00'), 2, 'Arts Studio', 20, 18, ['Arts'],
    'Design a poster for a real campus event in two hours.', { mode: 'ArrivalAndCheckpoint', poster: 'Gold' }),
  event('volunteer-day', 'rotaract', 'Volunteer Day', weeksAgo(2, 5, '09:00'), 4, 'Main Gate', 40, 35, ['Volunteering'], 'Clean-up at the lake.', { poster: 'Gold' }),
  event('career-talk', 'cs-society', 'Careers in Tech Talk', weeksAgo(3, 3, '14:00'), 2, 'F1A02', 80, 61, ['Career', 'Tech'],
    'Graduates talk about their first year at work.', { mode: 'ArrivalAndCheckpoint' }),
  event('debate-night', 'cs-society', 'Debate Night', weeksAgo(3, 1, '19:00'), 2, 'DEWAN 1', 50, 40, ['Leadership'], 'Friendly debate on tech policy.'),
  event('sports-day', 'sports', 'Sports Day', weeksAgo(4, 5, '08:00'), 6, 'Field', 100, 90, ['Sports'], 'Relay, tug of war and more.'),
];
const eventById = Object.fromEntries(events.map((e) => [e.id, e.data]));

/** Empties both emulators first, so check-ins and bookings from an earlier run can't leak into this one. */
async function wipe() {
  // Plain batched deletes: the Admin SDK's bulk writer (recursiveDelete) is unreliable on the emulator.
  async function clear(collection) {
    const snap = await collection.get();
    for (const docSnap of snap.docs) {
      for (const sub of await docSnap.ref.listCollections()) await clear(sub);
    }
    for (let i = 0; i < snap.docs.length; i += 400) {
      const b = db.batch();
      snap.docs.slice(i, i + 400).forEach((d) => b.delete(d.ref));
      await b.commit();
    }
  }
  for (const collection of await db.listCollections()) await clear(collection);
  if (live) {
    // On a real project only the demo accounts are removed, never anyone else's.
    await auth.deleteUsers(people.map((p) => p.uid));
    return;
  }
  const url = `http://${process.env.FIREBASE_AUTH_EMULATOR_HOST}/emulator/v1/projects/${projectId}/accounts`;
  const res = await fetch(url, { method: 'DELETE' });
  if (!res.ok) throw new Error(`Could not clear the Auth emulator: ${res.status}`);
}

async function main() {
  if (reset) await wipe();
  // Accounts (Auth) and profiles (Firestore).
  for (const p of people) {

    // Create, or reset an existing demo account (on a live project without --reset).
    const account = { email: p.email, password: PASSWORD, displayName: p.displayName };
    await auth.updateUser(p.uid, account).catch(() => auth.createUser({ uid: p.uid, ...account }));
  }
  const batch = () => db.batch();
  let b = batch();
  for (const p of people) {
    b.set(db.doc(`users/${p.uid}`), {
      displayName: p.displayName, email: p.email, studentId: p.studentId, clubIds: p.clubIds ?? [],
      interests: p.interests ?? [], hasChosenInterests: p.hasChosenInterests ?? false,
      deviceId: null, deviceBoundAt: null,
    });
    if (p.admin) b.set(db.doc(`admins/${p.uid}`), { name: p.displayName });
  }
  for (const [id, club] of Object.entries(clubs)) b.set(db.doc(`clubs/${id}`), club);
  b.set(db.doc('clubs/cs-society/members/u-cs'), { role: 'owner', addedBy: 'u-office' });
  b.set(db.doc('clubs/cs-society/members/u-cs-com'), { role: 'committee', addedBy: 'u-office' });
  b.set(db.doc('clubs/cs-society/members/u-cs-scan'), { role: 'scanner', addedBy: 'u-office' });
  b.set(db.doc('clubs/arts-club/members/u-arts'), { role: 'owner', addedBy: 'u-office' });
  for (const e of events) b.set(db.doc(`events/${e.id}`), e.data);
  // The live event already has a check-in session, so the committee phone can show codes at once.
  b.set(db.doc('eventSecrets/leadership-workshop'), { secret: randomBytes(32).toString('hex'), createdBy: 'u-cs' });
  await b.commit();

  // Bookings.
  b = batch();
  const booking = (uid, eventId, status = 'booked') => {
    const p = people.find((x) => x.uid === uid);
    b.set(db.doc(`bookings/${eventId}_${uid}`), {
      eventId, userId: uid, clubId: eventById[eventId].clubId, status, createdAt: ts(now - 5 * DAY),
      name: p.displayName, studentId: p.studentId, requestId: `seed-${eventId}-${uid}`,
    });
  };
  ['leadership-workshop', 'python-next-steps', 'intro-python', 'design-sprint', 'volunteer-day', 'career-talk', 'debate-night', 'career-fair']
    .forEach((e) => booking('u-aisha', e));
  booking('u-aisha', 'sports-day', 'no_show');
  booking('u-daniel', 'leadership-workshop');
  booking('u-daniel', 'garden-volunteering');
  classmates.forEach((_, i) => booking(`u-c${i}`, 'leadership-workshop'));
  classmates.slice(0, 18).forEach((_, i) => booking(`u-c${i}`, 'python-next-steps'));
  await b.commit();

  // Past attendance and the points ledger (the office writes these, so the rules are bypassed here).
  b = batch();
  const stage = (eventId, when) => ({ method: 'qr', window: Math.floor(when / 30_000), sig: 'seed', offline: false, receivedAt: ts(when), requestId: `seed-${eventId}` });
  const record = (uid, eventId, { checkpoint = false, flagged = null, rejectReason = null } = {}) => {
    const start = eventById[eventId].start.toMillis();
    b.set(db.doc(`attendance/${eventId}_${uid}`), {
      eventId, userId: uid, clubId: eventById[eventId].clubId, deviceId: `seed-device-${uid}`,
      arrival: stage(eventId, start + 5 * MIN), ...(checkpoint ? { checkpoint: stage(eventId, start + 100 * MIN) } : {}),
      flagged, ...(rejectReason ? { rejectReason } : {}),
    });
  };
  record('u-aisha', 'intro-python');
  record('u-aisha', 'design-sprint');
  record('u-aisha', 'volunteer-day');
  record('u-aisha', 'career-talk', { checkpoint: true });
  record('u-aisha', 'debate-night', { rejectReason: 'Office review: this phone checked in two accounts.' });
  for (let i = 0; i < 13; i++) record(`u-c${i}`, 'leadership-workshop');
  record('u-c13', 'leadership-workshop', { flagged: 'Same phone used by another account' });

  const ledger = (eventId, points, reason, suffix) => {
    const e = eventById[eventId];
    const date = new Date(e.start.toMillis() + ZONE_OFFSET_MS).toISOString().slice(0, 10);
    b.set(db.doc(`ledger/${eventId}_u-aisha_${suffix}`), {
      userId: 'u-aisha', eventId, eventTitle: e.title, eventDate: date, points, reason, createdAt: ts(e.start.toMillis() + 3 * HOUR),
    });
  };
  ledger('intro-python', 3, 'Verified attendance', 'award');
  ledger('volunteer-day', 3, 'Verified attendance', 'award');
  ledger('career-talk', 3, 'Verified attendance', 'award');
  ledger('debate-night', 3, 'Verified attendance', 'award');
  ledger('debate-night', -3, 'Office review: this phone checked in two accounts.', 'reversal');
  await b.commit();

  console.log(`Seeded ${people.length} accounts, ${events.length} events into ${projectId}${live ? ' (LIVE)' : ''}. Password for every account: ${PASSWORD}`);
}

// The Firestore emulator sometimes fails a write with UNKNOWN right after an app that was listening disappears
// (for example when a test uninstalls it). Starting again from the wipe fixes it.
async function seedWithRetry(attempts = 3) {
  for (let i = 1; ; i++) {
    try { return await main(); } catch (e) {
      if (i >= attempts) throw e;
      console.warn(`Seed attempt ${i} failed (${e.code ?? e.message}), trying again`);
    }
  }
}

seedWithRetry().then(() => process.exit(0), (e) => { console.error(e); process.exit(1); });
