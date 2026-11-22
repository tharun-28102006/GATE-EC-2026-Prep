# GATE EC 2026 Prep — Implementation Plan

An authentic GATE CBT-styled Android preparation app for **GATE 2026 Electronics and Communication Engineering (EC)** featuring AI-generated syllabus questions with detailed explanations, a zero-repetition topic rotation engine, instant lock-screen quiz notifications triggered on every phone unlock, lightweight Room offline review, and secure cross-device cloud sync via Firebase.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The decisions below reflect your confirmed preferences from our setup questions, along with recommended defaults for technical details. Review and click **Proceed** to begin building, or let me know if you'd like any adjustments.

- **Confirmed — GATE 2026 Stream**: **Electronics and Communication Engineering (EC)** covering all 8 official EC technical sections (Engineering Mathematics, Networks/Signals & Systems, Electronic Devices, Analog Circuits, Digital Circuits, Control Systems, Communications, Electromagnetics) plus General Aptitude (36 granular subtopics).
- **Confirmed — Question Sourcing**: **AI-generated questions available both online and offline** via Firebase AI Logic (`gemini-3.8-flash` structured JSON) combined with a pre-cached rolling offline question pool and starter seed bank so you can practice seamlessly with or without internet.
- **Confirmed — Notification Trigger**: **Every phone unlock** (`ACTION_USER_PRESENT`) via a lightweight, low-memory background unlock monitor that posts an unobtrusive lock-screen interactive notification where you can answer (`A`/`B`/`C`/`D`) directly without opening the app.
- **Confirmed — Visual Style**: **Official GATE CBT Exam Style** — high-legibility institutional exam sheet aesthetic with navy exam header, marks/negative-marking pill (`+1 / -0.33` or `+2 / -0.67`), GATE status palette colors (Answered Green, Incorrect Red, Marked for Review Purple, Unvisited Slate), and crisp monospace formula typography.
- **Recommended Default — Cloud Sync & Security**: **Google Sign-In (Jetpack Credential Manager) + Firebase Firestore** to securely sync saved questions, streaks, attempt history, and topic rotation state across all your devices, backed by per-user zero-trust Firestore security rules. *(During build, you will be prompted once to approve Firebase provisioning for Firestore and AI Logic.)*

---

## 1. Overview & Core Concept

- **What It Does**: Transforms every phone unlock and dedicated study session into high-yield GATE 2026 EC micro-practice. Each question is drawn from a different syllabus topic than the previous ones, graded immediately with official GATE marking rules and a step-by-step derivation, saved for anytime offline review, and synced automatically to your cloud account.
- **Target Audience / Persona**: Aspirants preparing for the GATE 2026 EC exam who want disciplined daily consistency, rapid conceptual revision across all 9 syllabus sections, and zero-friction recall during everyday phone usage.
- **Key Value**: Eliminates topic clustering and repeated questions, keeps RAM/battery footprint minimal through local SQLite caching, and bridges quick lock-screen drills with deep analytical progress tracking.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **First Launch & Cloud Sign-In**:
   - Clean GATE CBT-branded sign-in gate with a one-tap **Sign in with Google** button so progress, bookmarks, and streaks sync securely across devices.
   - Immediately upon sign-in, the app restores cloud progress (if returning on a new device) and loads the active exam workspace with zero empty-screen delay using the offline starter bank while quietly pre-fetching fresh AI questions in the background.
2. **GATE Exam Practice Desk (Primary Tab)**:
   - Displays an authentic GATE CBT exam bar: `GATE 2026 • EC`, active syllabus section & subtopic tag, question difficulty, marks badge (`+1 / -0.33` or `+2 / -0.67`), and a live per-question stopwatch.
   - User selects option `A`, `B`, `C`, or `D`, can tap **Clear Response**, **Save Offline / Mark for Review**, or **Submit Answer**.
   - Immediately after submission, the **Detailed Solution Panel** expands in place with the verdict banner, net GATE score impact, concise final answer, step-by-step engineering derivation, highlighted key formula block, and why common distractors are wrong.
   - Tapping **Next Question** invokes the non-repeating randomized topic engine to pull a question from a strictly different syllabus section and subtopic.
   - Tapping **Topic Palette** opens a GATE CBT-style color-coded section/topic grid showing coverage across all 9 EC sections.
3. **Unlock-Triggered Lock-Screen Quiz Flow**:
   - User enables the **Unlock Quiz Monitor** toggle in the Notifications / Settings sheet (requests `POST_NOTIFICATIONS` on Android 13+).
   - Every time the phone is unlocked (`ACTION_USER_PRESENT`), the lightweight monitor draws the next unseen non-repeating question from the local Room queue (instant, works offline) and posts an unobtrusive `VISIBILITY_PUBLIC` notification visible on the lock screen and shade.
   - User can reply directly from the notification using inline quick-answer (`A`, `B`, `C`, or `D`) or tap **Save Offline** / **Open Full Question**.
   - Submitting an answer from the notification immediately grades it, logs the attempt to Room & Firestore, updates the daily streak, and refreshes the notification in place with the verdict and concise explanation (tapping opens the full step-by-step derivation in the app).
   - Includes a **"Send Test Unlock Quiz Now"** button and optional anti-fatigue cooldown (`Every unlock`, `5 min`, `15 min`, `30 min`) so the user stays in full control.
4. **Offline Saved Bank & Review Tab**:
   - Dedicated offline-first library of bookmarked and previously attempted questions, filterable by GATE EC section, status (Saved / Incorrect / Mastered), and instant local search.
   - Works 100% offline with full step-by-step explanations and a **"Retry Offline"** mode to re-test weak concepts anytime.
   - Shows offline AI question buffer status (e.g., `12 fresh questions cached offline`) with a manual **"Refill Offline AI Pool"** button when connected.
5. **Progress & Consistency Tracker Tab**:
   - Tracks daily consistency with a **Current & Longest Streak** badge, **30-Day Consistency Heatmap Grid**, and daily goal progress ring.
   - Displays **Performance Trends** via a clean custom Jetpack Compose accuracy & GATE-score trend chart over recent sessions.
   - Shows **Syllabus Mastery Breakdown** across all 9 GATE 2026 EC sections with accuracy bars and weak-topic callouts.

### Visual Identity & Theme

- **Aesthetic Direction**: **Authentic GATE CBT Examination Desk** — crisp, distraction-free, structured institutional engineering aesthetic modeled on the official IIT GATE Computer-Based Test portal, refined with modern Material 3 polish and edge-to-edge responsiveness.
- **Color Palette & Mood**:
  - *Exam Light Posture (Default — Official CBT Paper)*:
    - Primary Institutional Navy: `#1A3A5C` (top exam header & primary authority elements)
    - Primary Interactive Blueprint Blue: `#1D4ED8` (selected option border, primary action buttons)
    - Canvas Background: `#F4F6F9` (clean cool exam desk gray-white)
    - Question Sheet Surface: `#FFFFFF` with crisp `1dp` `#CBD5E1` structural borders
    - GATE Status Accents: Answered/Correct Emerald `#15803D`, Incorrect/Negative Crimson `#DC2626`, Marked for Review / Saved Purple `#6D28D9`, Warning/Streak Amber `#D97706`
    - Primary Ink: `#0F172A`, Secondary Metadata Ink: `#475569`
  - *Night Exam Dark Posture (Optional Toggle)*: Deep Slate `#0B1220` background, `#131C2E` question sheet cards, crisp `#E2E8F0` ink, muted exam semantic accents for low-glare late-night revision.
- **Typography & Hierarchy**:
  - Bundled locally via `font-util` (zero network font dependency):
    - **UI & Question Body**: `IBM Plex Sans` — engineered for high readability in technical prose and dense examination statements.
    - **Equations, Marks, Timers & Circuit Values**: `IBM Plex Mono` — tabular figures for stopwatch, marks (`+2 / -0.67`), formulas (`H(s) = 1 / (s + a)`), and option badges (`[A]`, `[B]`, `[C]`, `[D]`).
- **Component Styling & Layout**:
  - Structured card elevation via hairline borders (`1dp`) rather than heavy drop shadows to maintain exam-sheet clarity and minimize GPU overdraw.
  - Persistent bottom `NavigationBar` on compact screens (`Practice`, `Saved`, `Progress`, `Unlock & Sync`) adapting to a `NavigationRail` on expanded tablet/foldable layouts, strictly respecting `WindowInsets.safeDrawing` and `WindowInsets.navigationBars`.
  - Minimum `48dp` touch targets on all radio options, palette tiles, and action buttons.

### Interactive Feedback & Motion

- Snappy, restrained motion (< 250ms `AnimatedVisibility` and `spring()` transitions) suited for serious exam prep: crisp radio selection highlight, smooth accordion expansion for the post-answer Detailed Explanation card, subtle animated score delta pill (`+2.00` / `-0.67`), clear loading skeleton during live AI generation, and actionable error/offline banners with 1-tap retry.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1 — Non-Repeating Topic Rotation Algorithm (Deck / Shuffle-Bag + Section Cooldown)**
  - *Chosen Approach*: Maintain a persistent shuffled queue of all 36 GATE 2026 EC subtopics across the 9 syllabus sections with a strict constraint that consecutive questions never share the same **subtopic** or **parent section**. Previously answered question hashes/IDs are tracked so no question is ever served twice. Once a full 36-topic cycle completes, the deck reshuffles with boundary checks so the new cycle's first topic never matches the previous cycle's last section.
  - *Why*: Pure `Random.nextInt()` frequently repeats topics back-to-back due to the birthday paradox. A section-aware shuffle-bag mathematically guarantees balanced coverage of the entire GATE 2026 EC syllabus with zero consecutive repetition.
  - *Alternatives Considered*: Simple random selection (rejected because it violates the no-topic-repetition requirement).

- **Decision 2 — Hybrid Offline-First Room Cache + Firebase Firestore Cloud Sync**
  - *Chosen Approach*: Use **Room (SQLite)** as the ultra-low-memory local source of truth for the pre-fetched AI question buffer, saved offline questions, and pending attempt logs, synchronized bi-directionally with **Firebase Firestore** (scoped strictly under `/users/{uid}/...`) when online.
  - *Why*: Delivers instant zero-latency lock-screen notifications even in airplane mode, keeps RAM usage tiny by streaming only paginated/active rows from SQLite, and ensures seamless multi-device cloud backup and restoration.
  - *Alternatives Considered*: Firestore-only without Room (slower cold-start in lock-screen BroadcastReceivers and less control over pre-cached offline AI question buffer size).

- **Decision 3 — Firebase AI Logic (`gemini-3.8-flash`) with Structured JSON Schema**
  - *Chosen Approach*: Generate fresh GATE 2026 EC questions on-demand and in small background buffer refills (3–5 questions per refill) using `Firebase.ai.generativeModel("gemini-3.8-flash")` with strict `responseSchema` (`Schema.obj`) enforcing exact option arrays, marking scheme, syllabus section/subtopic, correct index, formula summary, and step-by-step explanation. Paired with a curated 18-question offline starter seed (2 per section) so first-run and offline sessions work immediately.
  - *Why*: Eliminates hardcoded question limits while keeping generation fast, structured, and accurate to GATE 2026 EC depth, requiring no client-side API key (secured via Firebase App Check).
  - *Alternatives Considered*: Static local question bank only (rejected because the user explicitly chose AI-generated questions usable online or offline).

- **Decision 4 — Unlock-Triggered Notification Architecture (Android 14–16 Compliant & Low Memory)**
  - *Chosen Approach*: Since Android 8+ prohibits static manifest registration of `ACTION_USER_PRESENT`, run a lean, user-toggleable foreground service (`foregroundServiceType="specialUse"`, minimal low-priority silent indicator) that holds only a dynamic `BroadcastReceiver` for `Intent.ACTION_USER_PRESENT` and queries Room for one pre-cached question (~few KB RAM, zero continuous polling or CPU wake loops). Also support direct lock-screen answering via `NotificationCompat.Action` + `RemoteInput` inline quick-reply.
  - *Why*: Truly responds immediately on every phone unlock without polling sensors or UsageStats, respects Google Play foreground service requirements, and uses negligible memory and battery.
  - *Alternatives Considered*: Periodic `WorkManager` only (cannot trigger precisely on phone unlock; kept only as an optional fallback if the user disables the persistent unlock monitor).

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```text
┌────────────────────────────────────────────────────────────────────────────┐
│                             Android UI Layer                               │
│  ┌────────────────┐  ┌───────────────┐  ┌──────────────┐  ┌─────────────┐  │
│  │ Practice Desk  │  │ Saved Offline │  │   Progress   │  │ Unlock Quiz │  │
│  │ (GATE CBT UI + │  │ Bank & Retry  │  │  Streaks &   │  │ & Cloud     │  │
│  │  Topic Palette)│  │   Library     │  │ Trend Charts │  │  Settings   │  │
│  └───────┬────────┘  └───────┬───────┘  └──────┬───────┘  └──────┬──────┘  │
│          └───────────────────┴─────────────────┴─────────────────┘         │
│                                      │                                     │
│                          ┌───────────▼───────────┐                         │
│                          │   Auth Gate (Google   │                         │
│                          │  Credential Manager)  │                         │
│                          └───────────┬───────────┘                         │
└──────────────────────────────────────┼─────────────────────────────────────┘
                                       │ StateFlow / Events
┌──────────────────────────────────────▼─────────────────────────────────────┐
│                       Domain & ViewModel Layer                             │
│  ┌──────────────────────┐  ┌───────────────────────┐  ┌─────────────────┐  │
│  │  PracticeViewModel   │  │  ProgressViewModel    │  │ SavedViewModel  │  │
│  └──────────┬───────────┘  └───────────┬───────────┘  └────────┬────────┘  │
│             └──────────────┬───────────┴───────────────────────┘           │
│                            ▼                                               │
│         ┌─────────────────────────────────────────────────────┐            │
│         │  TopicRotationEngine (Non-Repeating Section/Topic   │            │
│         │  Shuffle-Bag + Seen Question Deduplication)         │            │
│         └──────────────────────────┬──────────────────────────┘            │
└────────────────────────────────────┼───────────────────────────────────────┘
                                     │
┌────────────────────────────────────▼───────────────────────────────────────┐
│                        Unified Repository & Services                       │
│  ┌──────────────────────┐  ┌───────────────────────┐  ┌─────────────────┐  │
│  │  Room Local Cache    │◄─┤   GateSyncRepository  ├─►│ Firebase Cloud  │  │
│  │  • Cached Questions  │  │  (Offline-First Sync) │  │ • Firestore DB  │  │
│  │  • Saved Bookmarks   │  └───────────┬───────────┘  │   /users/{uid}  │  │
│  │  • Attempt History   │              │              │ • Firebase Auth │  │
│  │  • Rotation Cursor   │              ▼              └─────────────────┘  │
│  └──────────▲───────────┘  ┌───────────────────────┐                       │
│             │              │  Firebase AI Logic    │                       │
│  ┌──────────┴───────────┐  │  (gemini-3.8-flash +  │                       │
│  │ UnlockMonitorService │  │   App Check + JSON    │                       │
│  │ • ACTION_USER_PRESENT│  │   Response Schema)    │                       │
│  │ • Lock-Screen Notif  │  └───────────────────────┘                       │
│  │ • Inline Answer Rx   │                                                  │
│  └──────────────────────┘                                                  │
└────────────────────────────────────────────────────────────────────────────┘
```

### Data Model & State

- **Syllabus Taxonomy (`GateEcSyllabus`)**: Immutable in-memory catalog of all 9 GATE 2026 EC sections and 36 subtopics (lightweight static constants, near-zero memory footprint).
- **Question Entity (`GateQuestion`)**: `id`, `sectionId`, `subtopicId`, `questionText`, `options` (4 strings), `correctIndex` (`0..3`), `marks` (`1` or `2`), `negativeMarks` (`0.33` or `0.67`), `difficulty` (`Easy`, `Medium`, `Hard`), `explanation`, `keyFormula`, `source` (`AI` or `SEED`), `savedOffline`, `isAttempted`, `createdAt`.
- **Attempt Record (`QuestionAttempt`)**: `id`, `questionId`, `sectionId`, `subtopicId`, `selectedIndex`, `isCorrect`, `scoreDelta`, `timeSpentSec`, `answeredVia` (`IN_APP` or `NOTIFICATION`), `dateKey` (`YYYY-MM-DD`), `attemptedAt`.
- **User Progress & Rotation State (`UserStudyProfile`)**: `currentStreak`, `longestStreak`, `lastActiveDate`, `dailyGoal`, `remainingTopicDeck` (ordered list of unvisited subtopic IDs in current cycle), `lastSectionId`, `unlockMonitorEnabled`, `unlockCooldownMinutes`, `darkExamTheme`.
- **Firestore Cloud Structure** (per-user strict ownership rules):
  - `/users/{userId}` — `UserStudyProfile` sync document (streaks, daily goal, topic deck state, settings).
  - `/users/{userId}/savedQuestions/{questionId}` — Full saved question payload + explanation for cross-device offline review.
  - `/users/{userId}/attempts/{attemptId}` — Attempt telemetry for multi-device trend charts and accuracy calculation.

### Interactive Component & State Mapping

- **Auth Gate**: Silent `GetGoogleIdOption` auto-sign-in attempt on launch with visible **Sign in with Google** (`GetSignInWithGoogleOption`) button fallback; mounts authenticated app scoped by `key(uid)` and tears down listeners cleanly on sign-out.
- **Practice Desk (`PracticeViewModel`)**:
  - Option selection updates local selection state without committing; **Clear Response** resets selection; **Submit Answer** computes GATE net marks, saves attempt to Room, triggers non-blocking Firestore upload, increments daily streak, and reveals the **Detailed Explanation Panel**.
  - **Save Offline** toggles bookmark state in Room and syncs/deletes from `/users/{uid}/savedQuestions/{id}`.
  - **Next Question** pulls the next subtopic from `TopicRotationEngine`; serves immediately from the Room offline buffer if available, or invokes `Firebase.ai` (`withTimeout(60_000L)`) with explicit loading/error recovery UI if the buffer for that topic is empty. Whenever online and buffer drops below threshold, replenishes silently in the background.
- **Unlock Quiz Notification Pipeline**:
  - `UnlockMonitorService` listens for `Intent.ACTION_USER_PRESENT`, checks cooldown preference, fetches the next non-repeating cached question from Room, and posts an interactive `VISIBILITY_PUBLIC` notification with inline `RemoteInput` quick-answer (`A`/`B`/`C`/`D`) and action buttons (`Save`, `Open`).
  - `NotificationAnswerReceiver` parses the user's answer (`A`/`B`/`C`/`D`), grades it immediately, records the attempt in Room + schedules cloud sync, and updates the notification in-place with the verdict + concise explanation + tap-to-open deep link to the full derivation.
- **Saved Offline Tab (`SavedViewModel`)**: Reactive `Flow` from Room filtered by section/search/status; works offline identically to online; supports inline explanation expansion, remove bookmark, and interactive **Retry Quiz** modal.
- **Progress Tracker Tab (`ProgressViewModel`)**: Reactive aggregation of attempts by `dateKey` (for 30-day consistency heatmap & streak) and by `sectionId` (for accuracy percentage & net GATE score trend chart).

### Key Sequences & Integrations

- **App Check & AI Logic**: `configureAppCheck(this, intent)` runs in `MainActivity.onCreate()` before Compose UI or ViewModels initialize; `Firebase.ai` generative model is initialized lazily (`by lazy`) with `gemini-3.8-flash` and strict `Schema.obj` JSON output.
- **Named Firestore Database**: All Firestore reads/writes resolve via `FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))` with verified `firestore.rules` tested locally against the Firestore emulator before deployment.
- **Low-Memory Engineering**: No large bitmaps or heavy third-party charting libraries — crisp vector iconography, lightweight custom Compose `Canvas` charts, Room query limits/pagination, and an ultra-lean unlock broadcast listener without polling loops.
