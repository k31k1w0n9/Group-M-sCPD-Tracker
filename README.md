**COMP3040 Mobile Device Programming — CW1 Part 1: Requirements and Analysis**

| Group member | Main responsibility |
|---|---|
| [Name] | Requirements and user stories: R1–R6 |
| [Name] | Technical design and diagrams |
| [Name] | UI/UX design and wireframes |

> **Target:** approximately 1,500 words, excluding UI components and diagrams.

## 1. Project overview

[Describe the problem, the app’s purpose, and its scope.]

### Actors

- **Student:** [Main actions]
- **Club representative:** [Main actions]
- **Admin / s-CPD office:** [Main actions and whether handled in the backend]

## 2. Requirements analysis

### Functional requirements

| ID | Requirement | CW requirement |
|---|---|---|
| FR-01 | [System behaviour] | R1 |
| FR-02 | [System behaviour] | R2 |
| FR-03 | [System behaviour] | R3 |
| FR-04 | [System behaviour] | R4 |
| FR-05 | [System behaviour] | R5 |
| FR-06 | [System behaviour] | R6 |

### Non-functional requirements

| ID | Quality | Requirement / target |
|---|---|---|
| NFR-01 | Security | [How roles and permissions are protected] |
| NFR-02 | Usability | [How the app stays clear and easy to use] |
| NFR-03 | Reliability | [How errors or offline use are handled] |
| NFR-04 | Performance | [Measurable target, if agreed] |

### User stories and acceptance criteria

[Include Student, Club, and Admin stories covering R1–R6. Mark each Must-have or Nice-to-have, and include exceptional and cheating cases.]

| ID | Actor | Priority / type | User story | Acceptance criteria |
|---|---|---|---|---|
| S-R1-01 | [Actor] | [Must / normal] | [As a…, I want…, so that…] | Given…, when…, then… |
| S-R2-01 | [Actor] | [Must / exceptional] | [Story] | Given…, when…, then… |
| S-R3-01 | [Actor] | [Must / cheating] | [Story] | Given…, when…, then… |
| S-R4-01 | [Actor] | [Must / exceptional] | [Story] | Given…, when…, then… |
| S-R5-01 | [Actor] | [Must / normal] | [Story] | Given…, when…, then… |
| S-R6-01 | [Actor] | [Must / normal] | [Story] | Given…, when…, then… |

### Proposed feature: Interest Passport

**Feature:** [Explain interest tags, event recommendations, and progress stamps.]

**Value:** [How it helps students find relevant events or encourages participation.]

**Scope:** [What is included in this version; note any future extensions.]

## 3. Challenges, risks, and assumptions

### Challenges and risks

| Risk / challenge | Response | Remaining limitation |
|---|---|---|
| [Example: checking in for a friend] | [Defence] | [Weakness] |
| [Example: poor connection at an event] | [Offline handling] | [Limitation] |
| [Example: incorrect role access] | [Permission checks] | [Limitation] |

### Assumptions

- [Assumption about accounts, roles, or Club membership]
- [Assumption about devices, connectivity, or event information]

## 4. Technical design

### Architecture

[Describe the app, authentication, backend, local storage, and sync.]

![Architecture diagram](docs/diagrams/architecture.png)

### Data model

[Describe the main entities and relationships. Use stable IDs such as `userId`, `eventId`, and `bookingId`.]

![Data model / ERD](docs/diagrams/data-model.png)

### Event approval workflow

[Explain the event states and how approved events are locked against editing.]

![Event lifecycle diagram](docs/diagrams/event-lifecycle.png)

### Attendance security

[Explain how attendance is validated and how the design handles late, remote, duplicate, or friend check-ins.]

| Cheating attempt | Defence | Remaining weakness |
|---|---|---|
| [Attempt] | [Defence] | [Weakness] |

![Attendance flow](docs/diagrams/attendance-flow.png)

### Offline sync

[Explain what is saved on the device, how pending records sync, and when attendance is verified.]

![Offline sync diagram](docs/diagrams/offline-sync.png)

## 5. UI/UX design

[Add low-fidelity wireframes for all app screens and important states, including full, rejected, pending, offline, and error states.]

### Student screens

[Browse, event details, booking result, bookings, check-in, points, and Interest Passport.]

![Student wireframes](docs/wireframes/student-screens.png)

### Club screens

[Dashboard, create/edit draft, approval status, and attendance setup.]

![Club wireframes](docs/wireframes/club-screens.png)

[If Admin review is handled in the backend, explain that no Admin Android screen is required.]

## 6. References

- [Coursework description sheet]
- [Other sources, assets, or tools used]
