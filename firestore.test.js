const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds
} = require("@firebase/rules-unit-testing");
const {
  collection,
  deleteDoc,
  doc,
  getDoc,
  setDoc,
  updateDoc
} = require("firebase/firestore");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
let testEnv;

const userDoc = (db, userId) => doc(db, "users", userId);
const expenseDoc = (db, userId, expenseId) =>
  doc(db, "users", userId, "expenses", expenseId);
const savingsGoalDoc = (db, userId, goalId) =>
  doc(db, "users", userId, "savingsGoals", goalId);

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085
    }
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("unauthenticated user cannot read or write a user document", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(getDoc(userDoc(db, "user_123")));
  await assertFails(setDoc(userDoc(db, "user_123"), {
    userId: "user_123",
    dailyBudget: 50,
    currencySymbol: "$",
    createdAt: new Date()
  }));
});

test("authenticated user can create and read their own profile", async () => {
  const db = testEnv.authenticatedContext("alice_1").firestore();
  await assertSucceeds(setDoc(userDoc(db, "alice_1"), {
    userId: "alice_1",
    dailyBudget: 60,
    currencySymbol: "$",
    createdAt: new Date()
  }));
  await assertSucceeds(getDoc(userDoc(db, "alice_1")));
});

test("authenticated user cannot access another user's profile", async () => {
  const db = testEnv.authenticatedContext("alice_1").firestore();
  await assertFails(getDoc(userDoc(db, "bob_2")));
  await assertFails(setDoc(userDoc(db, "bob_2"), {
    userId: "bob_2",
    dailyBudget: 40,
    currencySymbol: "$",
    createdAt: new Date()
  }));
});

test("authenticated user can manage their own expenses", async () => {
  const db = testEnv.authenticatedContext("alice_1").firestore();
  const ref = expenseDoc(db, "alice_1", "exp_1");
  await assertSucceeds(setDoc(ref, {
    userId: "alice_1",
    title: "Grocery Shopping",
    amount: 45.5,
    category: "Groceries",
    date: "2026-10-24",
    type: "expense",
    note: "Milk, eggs, vegetables",
    createdAt: new Date()
  }));
  await assertSucceeds(getDoc(ref));
  await assertSucceeds(updateDoc(ref, {
    amount: 50,
    note: "Updated groceries note"
  }));
  await assertSucceeds(deleteDoc(ref));
});

test("authenticated user cannot read or modify another user's expenses", async () => {
  const aliceDb = testEnv.authenticatedContext("alice_1").firestore();
  const bobDb = testEnv.authenticatedContext("bob_2").firestore();
  await assertSucceeds(setDoc(expenseDoc(bobDb, "bob_2", "exp_bob"), {
    userId: "bob_2",
    title: "Coffee",
    amount: 4.5,
    category: "Food & Dining",
    date: "2026-10-24",
    type: "expense",
    createdAt: new Date()
  }));

  await assertFails(getDoc(expenseDoc(aliceDb, "bob_2", "exp_bob")));
  await assertFails(setDoc(expenseDoc(aliceDb, "bob_2", "exp_hack"), {
    userId: "alice_1",
    title: "Hacked",
    amount: 100,
    category: "Other",
    date: "2026-10-24",
    type: "expense",
    createdAt: new Date()
  }));
});

test("authenticated user can manage their own savings goals", async () => {
  const db = testEnv.authenticatedContext("alice_1").firestore();
  const ref = savingsGoalDoc(db, "alice_1", "goal_1");
  await assertSucceeds(setDoc(ref, {
    userId: "alice_1",
    title: "Emergency Fund",
    targetAmount: 5000,
    currentAmount: 1200,
    targetDate: "2026-12-31",
    category: "Emergency",
    colorHex: "#4CAF50",
    createdAt: new Date()
  }));
  await assertSucceeds(getDoc(ref));
  await assertSucceeds(updateDoc(ref, { currentAmount: 1500 }));
  await assertSucceeds(deleteDoc(ref));
});

test("expense ownership cannot be reassigned and unexpected fields are rejected", async () => {
  const db = testEnv.authenticatedContext("alice_1").firestore();
  const ref = expenseDoc(db, "alice_1", "exp_owned");
  await assertSucceeds(setDoc(ref, {
    userId: "alice_1",
    title: "Groceries",
    amount: 120.5,
    category: "Groceries",
    date: "2026-10-27",
    type: "expense",
    createdAt: new Date()
  }));

  await assertFails(updateDoc(ref, { userId: "bob_2" }));
  await assertFails(updateDoc(ref, { admin: true }));
});

test("expense rules reject invalid amounts and oversized payload fields", async () => {
  const db = testEnv.authenticatedContext("alice_1").firestore();

  await assertFails(setDoc(expenseDoc(db, "alice_1", "negative"), {
    userId: "alice_1",
    title: "Invalid negative amount",
    amount: -1,
    category: "Other",
    date: "2026-10-27",
    type: "expense",
    createdAt: new Date()
  }));

  await assertFails(setDoc(expenseDoc(db, "alice_1", "too_large"), {
    userId: "alice_1",
    title: "Invalid amount",
    amount: 10000001,
    category: "Other",
    date: "2026-10-27",
    type: "expense",
    createdAt: new Date()
  }));

  await assertFails(setDoc(expenseDoc(db, "alice_1", "oversized_note"), {
    userId: "alice_1",
    title: "Invalid note",
    amount: 10,
    category: "Other",
    date: "2026-10-27",
    type: "expense",
    note: "x".repeat(501),
    createdAt: new Date()
  }));
});

test("unauthenticated users cannot access savings goals", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  const ref = savingsGoalDoc(db, "alice_1", "goal_1");
  await assertFails(getDoc(ref));
  await assertFails(setDoc(ref, {
    userId: "alice_1",
    title: "Emergency Fund",
    targetAmount: 5000,
    currentAmount: 0,
    createdAt: new Date()
  }));
});
