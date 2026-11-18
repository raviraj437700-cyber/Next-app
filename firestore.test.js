const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_uid_1";
const ADMIN_EMAIL = "raviraj43770@gmail.com";
const STUDENT_A_UID = "student_a_123";
const STUDENT_B_UID = "student_b_456";
const SPOOF_UID = "spoof_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

function spoofAdminContext() {
  return testEnv.authenticatedContext(SPOOF_UID, {
    email: ADMIN_EMAIL,
    email_verified: false,
  });
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

test("Unauthenticated user: cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("subjects").where("published", "==", true).get());
  await assertFails(unauth.collection("leaderboard").where("visibility", "==", "public").get());
  await assertFails(unauth.collection("users").doc(STUDENT_A_UID).get());
  await assertFails(unauth.collection("attempts").where("userId", "==", STUDENT_A_UID).get());
  await assertFails(
    unauth.collection("subjects").doc("sub_1").set({
      name: "Maths",
      description: "Class 10",
      boardClass: "Class 10",
      iconKey: "math",
      orderIndex: 1,
      published: true,
      createdBy: STUDENT_A_UID,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Email Spoofing Test: unverified admin email cannot access admin features", async () => {
  const spoofDb = spoofAdminContext().firestore();
  await assertFails(
    spoofDb.collection("subjects").doc("sub_maths").set({
      name: "Mathematics",
      description: "Bihar Board Maths",
      boardClass: "Class 10",
      iconKey: "math",
      orderIndex: 1,
      published: true,
      createdBy: SPOOF_UID,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(spoofDb.collection("users").get());
});

test("User Profile & PII Isolation + Leaderboard Split Collection", async () => {
  const studentADb = testEnv.authenticatedContext(STUDENT_A_UID, { email: "a@student.com", email_verified: true }).firestore();
  const studentBDb = testEnv.authenticatedContext(STUDENT_B_UID, { email: "b@student.com", email_verified: true }).firestore();
  const adminDb = adminContext().firestore();
  const now = new Date(Date.now() - 2000);

  // Regression check: reading own non-existent /users and /leaderboard docs must succeed before creation
  await assertSucceeds(studentADb.collection("users").doc(STUDENT_A_UID).get());
  await assertSucceeds(studentADb.collection("leaderboard").doc(STUDENT_A_UID).get());
  await assertFails(studentBDb.collection("leaderboard").doc(STUDENT_A_UID).get());

  // Student A creates own profile (mirrors ensureUserProfileExists payload with empty schoolName)
  await assertSucceeds(
    studentADb.collection("users").doc(STUDENT_A_UID).set({
      userId: STUDENT_A_UID,
      displayName: "Aarav Kumar",
      email: "a@student.com",
      boardClass: "Class 10",
      schoolName: "",
      totalScore: 0,
      testsTaken: 0,
      averagePercent: 0.0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Student A creates public leaderboard entry
  await assertSucceeds(
    studentADb.collection("leaderboard").doc(STUDENT_A_UID).set({
      userId: STUDENT_A_UID,
      displayName: "Aarav Kumar",
      boardClass: "Class 10",
      visibility: "public",
      totalScore: 0,
      testsTaken: 0,
      averagePercent: 0.0,
      updatedAt: now,
    })
  );

  // Student B cannot read Student A's private profile (PII email) or list /users
  await assertFails(studentBDb.collection("users").doc(STUDENT_A_UID).get());
  await assertFails(studentBDb.collection("users").get());

  // Student B CAN query public leaderboard with visibility == 'public'
  await assertSucceeds(studentBDb.collection("leaderboard").where("visibility", "==", "public").get());
  // Unfiltered leaderboard list query fails
  await assertFails(studentBDb.collection("leaderboard").get());

  // Admin CAN list all users for performance dashboard
  await assertSucceeds(adminDb.collection("users").get());

  // Ghost field & future timestamp rejected
  await assertFails(
    studentADb.collection("users").doc(STUDENT_A_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    studentADb.collection("users").doc(STUDENT_A_UID).update({
      totalScore: 10,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Curriculum Folders (Subject -> Chapter -> Quiz): Admin full control, Student published-only reads", async () => {
  const adminDb = adminContext().firestore();
  const studentDb = testEnv.authenticatedContext(STUDENT_A_UID).firestore();
  const now = new Date(Date.now() - 5000);

  // Admin creates Subject
  await assertSucceeds(
    adminDb.collection("subjects").doc("sub_maths").set({
      name: "Mathematics",
      description: "BSEB Class 10 Objective Sets",
      boardClass: "Class 10",
      iconKey: "calculate",
      orderIndex: 1,
      published: true,
      createdBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Student cannot create or modify Subject
  await assertFails(
    studentDb.collection("subjects").doc("sub_hack").set({
      name: "Hacked",
      description: "Hack",
      boardClass: "Class 10",
      iconKey: "book",
      orderIndex: 2,
      published: true,
      createdBy: STUDENT_A_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin creates published Chapter inside Subject
  await assertSucceeds(
    adminDb.collection("chapters").doc("chap_real_nums").set({
      subjectId: "sub_maths",
      title: "Real Numbers (वास्तविक संख्याएँ)",
      summary: "Euclid division lemma, HCF & LCM",
      chapterNumber: 1,
      published: true,
      createdBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Orphaned chapter referencing non-existent subject fails
  await assertFails(
    adminDb.collection("chapters").doc("chap_orphan").set({
      subjectId: "non_existent_sub",
      title: "Orphan",
      summary: "Test",
      chapterNumber: 1,
      published: true,
      createdBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin creates AI PDF Quiz inside Chapter
  await assertSucceeds(
    adminDb.collection("quizzes").doc("quiz_1").set({
      subjectId: "sub_maths",
      chapterId: "chap_real_nums",
      title: "Chapter 1 AI Practice Set",
      durationMinutes: 15,
      sourceType: "ai_pdf",
      published: true,
      questions: [
        {
          question: "HCF of 135 and 225 is:",
          options: ["15", "45", "35", "55"],
          correctIndex: 1,
          explanation: "By Euclid's division algorithm, HCF(135, 225) = 45.",
        },
      ],
      createdBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Student filtered queries succeed
  await assertSucceeds(studentDb.collection("subjects").where("published", "==", true).get());
  await assertSucceeds(
    studentDb.collection("chapters").where("subjectId", "==", "sub_maths").where("published", "==", true).get()
  );
  await assertSucceeds(
    studentDb.collection("quizzes").where("chapterId", "==", "chap_real_nums").where("published", "==", true).get()
  );

  // Student unfiltered query fails
  await assertFails(studentDb.collection("subjects").get());
});

test("Test Attempts, Direct Admin-Student Chat & Targeted Announcements", async () => {
  const adminDb = adminContext().firestore();
  const studentADb = testEnv.authenticatedContext(STUDENT_A_UID).firestore();
  const studentBDb = testEnv.authenticatedContext(STUDENT_B_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Student A records attempt
  await assertSucceeds(
    studentADb.collection("attempts").doc("att_1").set({
      userId: STUDENT_A_UID,
      userName: "Aarav Kumar",
      quizId: "quiz_1",
      chapterId: "chap_real_nums",
      subjectId: "sub_maths",
      quizTitle: "Chapter 1 AI Practice Set",
      subjectName: "Mathematics",
      totalQuestions: 3,
      correctCount: 2,
      wrongCount: 1,
      percentage: 66.5,
      answers: [1],
      createdAt: now,
    })
  );

  // Student B cannot read Student A's attempt
  await assertFails(studentBDb.collection("attempts").doc("att_1").get());
  await assertFails(studentBDb.collection("attempts").where("userId", "==", STUDENT_A_UID).get());
  await assertSucceeds(studentADb.collection("attempts").where("userId", "==", STUDENT_A_UID).get());

  // 1-to-1 Chat: Student A opens chat thread and sends message
  await assertSucceeds(
    studentADb.collection("chats").doc(STUDENT_A_UID).set({
      studentId: STUDENT_A_UID,
      studentName: "Aarav Kumar",
      lastMessage: "Sir, Q3 me doubt hai",
      unreadByAdmin: 1,
      unreadByStudent: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    studentADb.collection("chats").doc(STUDENT_A_UID).collection("messages").doc("msg_1").set({
      senderId: STUDENT_A_UID,
      senderRole: "student",
      senderName: "Aarav Kumar",
      text: "Sir, Q3 me doubt hai",
      createdAt: now,
    })
  );

  // Student A cannot spoof senderRole: "admin"
  await assertFails(
    studentADb.collection("chats").doc(STUDENT_A_UID).collection("messages").doc("msg_spoof").set({
      senderId: STUDENT_A_UID,
      senderRole: "admin",
      senderName: "Fake Admin",
      text: "Hello",
      createdAt: now,
    })
  );

  // Student B cannot read Student A's chat messages
  await assertFails(studentBDb.collection("chats").doc(STUDENT_A_UID).collection("messages").get());

  // Admin can reply in Student A's chat
  await assertSucceeds(
    adminDb.collection("chats").doc(STUDENT_A_UID).collection("messages").doc("msg_2").set({
      senderId: ADMIN_UID,
      senderRole: "admin",
      senderName: "Admin",
      text: "Euclid lemma lagao beta!",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Announcements: Admin sends broadcast & targeted message
  await assertSucceeds(
    adminDb.collection("announcements").doc("ann_all").set({
      title: "BSEB Model Paper Live",
      message: "Maths Chapter 1-3 sets uploaded!",
      targetUserId: "ALL",
      targetUserName: "All Students",
      priority: "urgent",
      createdBy: ADMIN_UID,
      createdAt: now,
    })
  );
  await assertSucceeds(
    adminDb.collection("announcements").doc("ann_a").set({
      title: "Well Done Aarav",
      message: "Keep up the 100% streak!",
      targetUserId: STUDENT_A_UID,
      targetUserName: "Aarav Kumar",
      priority: "normal",
      createdBy: ADMIN_UID,
      createdAt: now,
    })
  );

  // Student B can read ALL broadcast, cannot read Student A's personal announcement
  await assertSucceeds(studentBDb.collection("announcements").where("targetUserId", "==", "ALL").get());
  await assertSucceeds(studentBDb.collection("announcements").where("targetUserId", "==", STUDENT_B_UID).get());
  await assertFails(studentBDb.collection("announcements").doc("ann_a").get());
  await assertFails(studentBDb.collection("announcements").where("targetUserId", "==", STUDENT_A_UID).get());
});

test("Study Groups: create, join, leave, and member-only group chat", async () => {
  const studentADb = testEnv.authenticatedContext(STUDENT_A_UID).firestore();
  const studentBDb = testEnv.authenticatedContext(STUDENT_B_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Student A creates a public study group
  await assertSucceeds(
    studentADb.collection("study_groups").doc("grp_maths").set({
      name: "BSEB Maths Toppers",
      description: "Chapter 1 Real Numbers doubt solving",
      boardClass: "Class 10",
      subjectTag: "Maths",
      inviteCode: "MATH10",
      isPublic: true,
      createdBy: STUDENT_A_UID,
      creatorName: "Aarav Kumar",
      memberIds: [STUDENT_A_UID],
      memberCount: 1,
      lastMessage: "Group created!",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Student B can discover public study groups
  await assertSucceeds(studentBDb.collection("study_groups").where("isPublic", "==", true).get());

  // Non-member Student B CANNOT read or post messages before joining
  await assertFails(studentBDb.collection("study_groups").doc("grp_maths").collection("messages").get());
  await assertFails(
    studentBDb.collection("study_groups").doc("grp_maths").collection("messages").doc("gm_fail").set({
      senderId: STUDENT_B_UID,
      senderName: "Riya",
      text: "Intruder message",
      createdAt: now,
    })
  );

  // Student B joins the group (adds only own UID & increments memberCount)
  await assertSucceeds(
    studentBDb.collection("study_groups").doc("grp_maths").update({
      memberIds: [STUDENT_A_UID, STUDENT_B_UID],
      memberCount: 2,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Now Student B CAN read and send messages in the group
  await assertSucceeds(studentBDb.collection("study_groups").doc("grp_maths").collection("messages").get());
  await assertSucceeds(
    studentBDb.collection("study_groups").doc("grp_maths").collection("messages").doc("gm_1").set({
      senderId: STUDENT_B_UID,
      senderName: "Riya",
      text: "Ch 1 HCF question 4 solve karein!",
      createdAt: new Date(Date.now() - 500),
    })
  );

  // Student B CANNOT delete Student A's group, while Student A can
  await assertFails(studentBDb.collection("study_groups").doc("grp_maths").delete());
  await assertSucceeds(studentADb.collection("study_groups").doc("grp_maths").delete());
});
