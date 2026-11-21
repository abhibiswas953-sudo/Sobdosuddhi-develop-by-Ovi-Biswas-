const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_writer_01";
const BOB_UID = "bob_writer_02";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validManuscriptPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    title: "শরতের কবিতা",
    content: "আকাশে সাদা মেঘের ভেলা ভেসে যায়।",
    originalContent: "আকাশে সাদা মেঘের ভেলা ভেসে যায়।",
    category: "POETRY",
    writingStyle: "POETIC",
    preferredMeter: "MATRABRITTO",
    expectedMatra: 14,
    wordCount: 7,
    charCount: 33,
    paragraphCount: 1,
    pinned: false,
    createdAt: now,
    updatedAt: now,
    ...overrides,
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

test("Unauthenticated users cannot read or write manuscripts or dictionary", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("manuscripts").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("manuscripts")
      .doc("ms_1")
      .set(validManuscriptPayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("dictionary").get()
  );
});

test("Owner can create, list, partial update, and delete manuscript", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("manuscripts")
    .doc("ms_001");

  await assertSucceeds(ref.set(validManuscriptPayload(ALICE_UID)));

  // List matches Kotlin repository path users/{uid}/manuscripts
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("manuscripts").get()
  );

  // Partial update preserves immutable userId & createdAt
  await assertSucceeds(
    ref.update({
      title: "শরতের নতুন কবিতা",
      content: "আকাশে সাদা মেঘের ভেলা ভেসে যায় দূরে।",
      wordCount: 8,
      charCount: 38,
      pinned: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    ref.update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Reject shadow/ghost field injection
  await assertFails(
    ref.update({
      isVerified: true,
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Reject future timestamp spoofing
  await assertFails(
    ref.update({
      title: "Future edit",
      updatedAt: new Date(Date.now() + 120000),
    })
  );

  await assertSucceeds(ref.delete());
});

test("Reject invalid manuscript payloads (extra @DocumentId id field, invalid enum, empty title)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const col = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("manuscripts");

  await assertFails(
    col.doc("ms_extra_id").set({
      id: "ms_extra_id",
      ...validManuscriptPayload(ALICE_UID),
    })
  );

  await assertFails(
    col.doc("ms_bad_cat").set(validManuscriptPayload(ALICE_UID, { category: "INVALID" }))
  );

  await assertFails(
    col.doc("ms_empty_title").set(validManuscriptPayload(ALICE_UID, { title: "" }))
  );
});

test("Cross-user isolation blocks Bob from reading or modifying Alice's manuscripts", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("manuscripts")
      .doc("ms_private")
      .set(validManuscriptPayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const target = bobDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("manuscripts")
    .doc("ms_private");

  await assertFails(target.get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("manuscripts").get()
  );
  await assertFails(
    target.update({ title: "Hijacked", updatedAt: new Date(Date.now() - 500) })
  );
  await assertFails(target.delete());
});

test("Chapters and revisions require existing parent manuscript and obey immutability", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Orphan chapter without parent manuscript must fail
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("manuscripts")
      .doc("missing_ms")
      .collection("chapters")
      .doc("ch_1")
      .set({
        userId: ALICE_UID,
        manuscriptId: "missing_ms",
        title: "প্রথম অধ্যায়",
        content: "গল্পের শুরু।",
        orderIndex: 0,
        wordCount: 2,
        createdAt: now,
        updatedAt: now,
      })
  );

  // Create parent manuscript first
  const msRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("manuscripts")
    .doc("ms_novel_1");
  await assertSucceeds(
    msRef.set(validManuscriptPayload(ALICE_UID, { category: "NOVEL" }))
  );

  // Create chapter under existing parent manuscript
  const chRef = msRef.collection("chapters").doc("ch_1");
  await assertSucceeds(
    chRef.set({
      userId: ALICE_UID,
      manuscriptId: "ms_novel_1",
      title: "প্রথম অধ্যায়",
      content: "নদীর তীরে সন্ধ্যা নামল।",
      orderIndex: 0,
      wordCount: 4,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update on chapter succeeds
  await assertSucceeds(
    chRef.update({
      content: "নদীর তীরে গভীর সন্ধ্যা নামল।",
      wordCount: 5,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Create revision snapshot succeeds, but updating revision fails (immutable)
  const revRef = msRef.collection("revisions").doc("rev_1");
  await assertSucceeds(
    revRef.set({
      userId: ALICE_UID,
      manuscriptId: "ms_novel_1",
      title: "শরতের কবিতা",
      content: "আগের খসড়া।",
      summary: "স্বয়ংক্রিয় সংরক্ষণ",
      createdAt: now,
    })
  );
  await assertFails(revRef.update({ summary: "Tampered" }));
});

test("Custom Bengali dictionary CRUD enforces ownership and schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  const wordRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("dictionary")
    .doc("word_1");

  await assertSucceeds(
    wordRef.set({
      userId: ALICE_UID,
      word: "জোছনাস্নাত",
      category: "POETIC",
      note: "কবিতার নিজস্ব প্রয়োগ",
      createdAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("dictionary").get()
  );

  await assertSucceeds(wordRef.update({ note: "হালনাগাদ নোট" }));
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("dictionary")
      .doc("word_1")
      .get()
  );
  await assertSucceeds(wordRef.delete());
});
