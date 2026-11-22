const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_gate_ec_123";
const BOB_UID = "bob_gate_ec_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, createdAt, updatedAt) {
  return {
    userId: uid,
    displayName: "Aarav EC Candidate",
    dailyGoal: 10,
    currentStreak: 3,
    longestStreak: 7,
    lastActiveDate: "2026-11-21",
    lastSectionId: "analog_circuits",
    remainingTopicDeck: ["em_linear_algebra", "dc_fsm", "cm_digital_mod"],
    totalAttempted: 24,
    totalCorrect: 18,
    netGateScore: 25.33,
    unlockMonitorEnabled: true,
    unlockCooldownMinutes: 0,
    darkExamTheme: false,
    createdAt,
    updatedAt,
  };
}

function validSavedQuestionPayload(uid, createdAt, updatedAt) {
  return {
    userId: uid,
    sectionId: "analog_circuits",
    subtopicId: "ac_opamp",
    questionText: "An ideal op-amp inverting amplifier has R1 = 10 kOhm and Rf = 100 kOhm. Find voltage gain.",
    options: ["-10", "+10", "-11", "+11"],
    correctIndex: 0,
    marks: 1,
    negativeMarks: 0.33,
    difficulty: "Easy",
    explanation: "For an ideal inverting amplifier, Av = -Rf/R1 = -100k/10k = -10.",
    keyFormula: "Av = -Rf / R1",
    source: "AI",
    createdAt,
    updatedAt,
  };
}

function validAttemptPayload(uid, createdAt) {
  return {
    userId: uid,
    questionId: "q_seed_01",
    sectionId: "analog_circuits",
    subtopicId: "ac_opamp",
    selectedIndex: 0,
    correctAnswer: true,
    scoreDelta: 1.0,
    timeSpentSec: 42,
    answeredVia: "NOTIFICATION",
    dateKey: "2026-11-21",
    createdAt,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any user profile, saved questions, or attempts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, now, now))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("savedQuestions").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("attempts").get()
  );
});

test("Authenticated owner can create, read, and partially update their UserStudyProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created, created))
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Partial update mirroring Kotlin repository update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      currentStreak: 4,
      totalAttempted: 25,
      totalCorrect: 19,
      netGateScore: 27.33,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject mutating immutable createdAt
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject shadow/ghost field injection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp tampering
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      dailyGoal: 15,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Authenticated owner can CRUD savedQuestions and list empty/populated subcollection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Listing empty subcollection succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("savedQuestions").get()
  );

  // Create valid saved question (no @DocumentId `id` field)
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("savedQuestions")
      .doc("q_ai_101")
      .set(validSavedQuestionPayload(ALICE_UID, created, created))
  );

  // Reject payload with extra `id` field
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("savedQuestions")
      .doc("q_ai_102")
      .set({
        id: "q_ai_102",
        ...validSavedQuestionPayload(ALICE_UID, created, created),
      })
  );

  // Reject invalid options array length (must be 4)
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("savedQuestions")
      .doc("q_ai_103")
      .set({
        ...validSavedQuestionPayload(ALICE_UID, created, created),
        options: ["Only two", "Options"],
      })
  );

  // Owner can list and delete
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("savedQuestions").get()
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("savedQuestions")
      .doc("q_ai_101")
      .delete()
  );
});

test("Authenticated owner can create and list immutable QuestionAttempts", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("attempts")
      .doc("att_001")
      .set(validAttemptPayload(ALICE_UID, created))
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("attempts").get()
  );

  // Attempts are immutable once logged
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("attempts")
      .doc("att_001")
      .update({ correctAnswer: false })
  );
});

test("Cross-user isolation prevents Bob from reading or modifying Alice's data", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb
      .collection("users")
      .doc(ALICE_UID)
      .set(validProfilePayload(ALICE_UID, created, created));
    await adminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("savedQuestions")
      .doc("q_alice_1")
      .set(validSavedQuestionPayload(ALICE_UID, created, created));
    await adminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("attempts")
      .doc("att_alice_1")
      .set(validAttemptPayload(ALICE_UID, created));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({ currentStreak: 999, updatedAt: new Date(Date.now() - 500) })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("savedQuestions").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("savedQuestions")
      .doc("q_alice_1")
      .delete()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("attempts").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("attempts")
      .doc("att_bob_inject")
      .set(validAttemptPayload(ALICE_UID, created))
  );
});
