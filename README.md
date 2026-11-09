**COMP3040 Mobile Device Programming — CW1 Part 1: Requirements and Analysis**

| Group member | Student ID | Main responsibility |
|---|---|---|
| Moiz Israr Malik | 20705922 | Requirements and user stories: R1–R6 |
| Tham Wing Lok | 20611657 | Technical design and diagrams |
| Wong Cheuk Kei | 20602511 | UI/UX design and wireframes |


## 1. Project overview

s-CPD is a native Android app for University of Nottingham Malaysia students and club committees. Students discover and book events, check in with a rotating QR code, and track verified attendance. Events that need a later checkpoint show the two-scan requirement before booking. Committees submit events and run check-in. The s-CPD office reviews submissions and corrects attendance in the backend. The app adds two original features: personalised event recommendations and weekly attendance streaks.

The report is split into short pages so each one can be read on its own. Every page keeps the README's section numbers and headings.

| Section | Page | What it covers |
|:---:|---|---|
| **2** | [Requirements analysis](docs/2-requirements-analysis.md) | Functional and non-functional requirements, user stories, role permissions and original features |
| **3** | [Challenges, risks, and assumptions](docs/3-challenges-risks-assumptions.md) | Attendance, booking and access risks, and the assumptions behind the design |
| **4** | [Architecture](docs/4-1-architecture.md) | MVVM app structure, Firebase backend, roles and sign-in |
| | [Screen flows](docs/4-2-screen-flows.md) | Student and Committee navigation |
| | [Status terms and data model](docs/4-3-status-and-data-model.md) | Status definitions, lifecycles and the entity-relationship diagram |
| | [Attendance security and offline sync](docs/4-4-attendance-security-and-sync.md) | Arrival and checkpoint verification, saved QR scans and attendance outcomes |
| **5** | [Student wireframes](docs/5-1-student-wireframes.md) | Entry, discovery, booking, My Events, check-in, progress and profile |
| | [Committee wireframes](docs/5-2-committee-wireframes.md) | Entry, dashboard, submissions, events, check-in, attendance and account |


## 2. Requirements analysis

The app meets six requirements: event review (R1), discovery and booking (R2), check-in (R3), offline use (R4), points and progress (R5) and access (R6). Twenty-two user stories cover all three actors, including full events, attendance without a connection and attempts to cheat. [Read Section 2](docs/2-requirements-analysis.md).


## 3. Challenges, risks, and assumptions

The main risks are proxy check-in, relayed codes, late or altered offline scans, overbooking and cross-club access. Each risk has a response and the limitation that remains. [Read Section 3](docs/3-challenges-risks-assumptions.md).


## 4. Technical design

The Kotlin app uses MVVM with repositories, Room for offline data and Firebase for shared data and trusted operations. Section 4 is split into four pages: [architecture](docs/4-1-architecture.md), [screen flows](docs/4-2-screen-flows.md), [status terms and data model](docs/4-3-status-and-data-model.md), and [attendance security and offline sync](docs/4-4-attendance-security-and-sync.md).


## 5. UI design and wireframes

The wireframes are exported from the [Figma file](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=0-1) and cover every state in the Student and Committee journeys. [Student wireframes](docs/5-1-student-wireframes.md) · [Committee wireframes](docs/5-2-committee-wireframes.md).
