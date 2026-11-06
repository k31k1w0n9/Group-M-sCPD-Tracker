# 4. Attendance security and offline sync

## Attendance verification

Committee **Scan** requests a QR and a six-digit backup code from the backend. Both rotate every 30 seconds while the committee device is online. The QR carries the event ID, check-in session ID, attendance stage, token ID, issue time and expiry. A server-generated authentication tag protects those fields. The backend keeps the key used to generate the tag.

For events that require a checkpoint, students scan on arrival and again during a window the organiser opens and announces in the final third of the event. Booking review explains both scans before students confirm. The backend issues separate codes for arrival and checkpoint. The wireframes show a five-minute checkpoint as an example. The event eligibility rule and window duration still need team agreement.

The server checks the account, its booking, the approved event, the expected stage and the active code window. Online submissions use server receipt time. Manual entry requires internet, applies to the active stage and is rate-limited by account and session. The backend stores one evidence record per required stage and one attendance outcome per student and event. A student can retry with a fresh, valid code while the stage is open. The error affects the attempt and preserves any previously verified evidence. Retrying the same request returns its recorded result. A separate duplicate attempt cannot overwrite a verified result. Once all stages pass or the office verifies attendance through an authorised decision, award 3 points and update the streak only for an eligible event. Record the outcome and award together to prevent a second award.

| Attempt | Defence | Remaining weakness |
|---|---|---|
| Proxy check-in | Tie the claim to the authenticated account and booking. | Sharing a login or handing over an unlocked phone can still work. |
| Check-in from home | Rotate codes and validate the event, session and stage. A checkpoint adds evidence at a second moment. | Someone at the event can relay a live code at either stage. |
| Late or altered offline scan | Check the token tag, issuance window and upload deadline. | A copied authentic token could still be used in an offline claim. |
| Duplicate claim | Deduplicate by request ID, keep one evidence record per stage and cap the award at one per student and event. | Someone else can still try to submit the first claim with access to the account. |

**DG-08 Attendance verification sequence (R3, R5)**

```mermaid
sequenceDiagram
  participant C as Committee app
  participant S as Student app
  participant F as Cloud Function
  participant DB as Firestore
  C->>F: Request current code for the active stage
  F-->>C: Authenticated QR and six-digit backup code
  C-->>S: Display code at the event
  S->>F: Submit QR or backup code with request ID
  F->>F: Verify account, booking, session, stage and server receipt time
  alt Stage accepted
    F->>DB: Save verified stage evidence
    opt All required stages verified
      F->>DB: Verify attendance and award 3 points once if eligible
    end
    F-->>S: Return stage and attendance status
  else Invalid code, expired code or rejected claim
    F-->>S: Return reason and preserve verified evidence
  end
```

## Offline sync

With a cached booking, the student app can capture the arrival or checkpoint QR offline. Room saves the full token, account and event IDs, stage, request ID and optional device capture time. The student sees **Pending sync**. WorkManager submits the record on reconnection. The server checks the token tag, the stage’s code window and the upload deadline using server receipt time. Device capture time is diagnostic. The office can review flagged claims before the server finalises attendance. The token confirms when the server issued the code. Live code relay and shared access to an unlocked phone remain possible.

Use a configurable sync grace period after check-in closes. The wireframes use 24 hours as an example. While uploads are accepted, the committee shows received stages and **Awaiting sync** for outstanding evidence. At the deadline, either verified stage with the other required stage missing becomes **Incomplete attendance**. Use **No-show** when the server received no claim by the deadline. A received claim **Under review** stays unresolved until the office decides. When the organiser never opened a required checkpoint, send verified arrivals to office review for an authorised decision. The office records its decision and reason before attendance is verified or rejected.

Save a definitive server response locally before clearing the raw token. After a timeout or lost acknowledgement, retry or query with the same request ID. If the server confirms the deadline passed without accepting the claim, mark the queue item **Sync deadline passed**, clear the expired token and retain the request ID for reconciliation. This local queue outcome preserves recorded server decisions. Show a late or rejected upload’s reason and provide the organiser contact for review. An authorised correction reverses an existing award once and recalculates progress and streaks.

**DG-09 Offline sync sequence (R4, R5)**

```mermaid
sequenceDiagram
  participant S as Student app
  participant R as Room
  participant W as WorkManager
  participant F as Cloud Function
  S->>R: Save full QR token and request ID
  S->>S: Show Pending sync
  W->>R: Read queue after reconnecting
  W->>F: Upload saved claim
  F->>F: Verify token, booking, session, stage, deadline and duplicates
  alt Definitive response received
    F-->>W: Return stored result, review status or confirmed expiry
    W->>R: Persist the response and clear the raw token
  else Connection fails or response is uncertain
    W->>R: Keep the token and request ID for retry or reconciliation
  end
```

See the [arrival and checkpoint wireframes](ui-and-wireframes.md#55-attendance-and-check-in--r3-r4) for the Student and Committee views.

[Back to project overview](../README.md)
