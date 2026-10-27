const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
let testEnv;

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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("unauthenticated user cannot read or write user document", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthedDb.collection("users").doc("user_123").get());
  await assertFails(unauthedDb.collection("users").doc("user_123").set({
    userId: "user_123",
    dailyBudget: 50.0,
    currencySymbol: "$",
    createdAt: new Date()
  }));
});

test("authenticated user can create and read their own user profile", async () => {
  const aliceDb = testEnv.authenticatedContext("alice_1").firestore();
  await assertSucceeds(aliceDb.collection("users").doc("alice_1").set({
    userId: "alice_1",
    dailyBudget: 60.0,
    currencySymbol: "$",
    createdAt: new Date()
  }));
  await assertSucceeds(aliceDb.collection("users").doc("alice_1").get());
});

test("authenticated user cannot access another user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext("alice_1").firestore();
  await assertFails(aliceDb.collection("users").doc("bob_2").get());
  await assertFails(aliceDb.collection("users").doc("bob_2").set({
    userId: "bob_2",
    dailyBudget: 40.0,
    currencySymbol: "$",
    createdAt: new Date()
  }));
});

test("authenticated user can manage their own expenses", async () => {
  const aliceDb = testEnv.authenticatedContext("alice_1").firestore();
  const expenseRef = aliceDb.collection("users").doc("alice_1").collection("expenses").doc("exp_1");
  await assertSucceeds(expenseRef.set({
    userId: "alice_1",
    title: "Grocery Shopping",
    amount: 45.5,
    category: "Groceries",
    date: "2026-10-24",
    type: "expense",
    note: "Milk, eggs, vegetables",
    createdAt: new Date()
  }));

  await assertSucceeds(expenseRef.get());

  await assertSucceeds(expenseRef.update({
    amount: 50.0,
    note: "Updated groceries note"
  }));

  await assertSucceeds(expenseRef.delete());
});

test("authenticated user cannot read or modify another user's expenses", async () => {
  const aliceDb = testEnv.authenticatedContext("alice_1").firestore();
  const bobDb = testEnv.authenticatedContext("bob_2").firestore();
  
  // Set up as Bob via testEnv admin or Bob
  await assertSucceeds(bobDb.collection("users").doc("bob_2").collection("expenses").doc("exp_bob").set({
    userId: "bob_2",
    title: "Coffee",
    amount: 4.5,
    category: "Food & Dining",
    date: "2026-10-24",
    type: "expense",
    createdAt: new Date()
  }));

  // Alice tries to read Bob's expense
  await assertFails(aliceDb.collection("users").doc("bob_2").collection("expenses").doc("exp_bob").get());
  // Alice tries to write to Bob's expenses
  await assertFails(aliceDb.collection("users").doc("bob_2").collection("expenses").doc("exp_hack").set({
    userId: "alice_1",
    title: "Hacked",
    amount: 100.0,
    category: "Other",
    date: "2026-10-24",
    type: "expense",
    createdAt: new Date()
  }));
});

test("authenticated user can manage their savings goals", async () => {
  const aliceDb = testEnv.authenticatedContext("alice_1").firestore();
  const goalRef = aliceDb.collection("users").doc("alice_1").collection("savingsGoals").doc("goal_1");
  await assertSucceeds(goalRef.set({
    userId: "alice_1",
    title: "Emergency Fund",
    targetAmount: 5000.0,
    currentAmount: 1200.0,
    targetDate: "2026-12-31",
    category: "Emergency",
    colorHex: "#4CAF50",
    createdAt: new Date()
  }));

  await assertSucceeds(goalRef.get());

  await assertSucceeds(goalRef.update({
    currentAmount: 1500.0
  }));

  await assertSucceeds(goalRef.delete());
});


test("expense ownership cannot be reassigned and unexpected fields are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext("alice_1").firestore();
  const expenseRef = aliceDb.collection("users").doc("alice_1").collection("expenses").doc("exp_owned");
  await assertSucceeds(expenseRef.set({
    userId: "alice_1",
    title: "Groceries",
    amount: 120.5,
    category: "Groceries",
    date: "2026-10-27",
    type: "expense",
    createdAt: new Date()
  }));

  await assertFails(expenseRef.update({ userId: "bob_2" }));
  await assertFails(expenseRef.update({ admin: true }));
});

test("expense rules reject invalid amounts and oversized payload fields", async () => {
  const aliceDb = testEnv.authenticatedContext("alice_1").firestore();
  const expenses = aliceDb.collection("users").doc("alice_1").collection("expenses");

  await assertFails(expenses.doc("negative").set({
    userId: "alice_1",
    title: "Invalid negative amount",
    amount: -1,
    category: "Other",
    date: "2026-10-27",
    type: "expense",
    createdAt: new Date()
  }));

  await assertFails(expenses.doc("too_large").set({
    userId: "alice_1",
    title: "Invalid amount",
    amount: 10000001,
    category: "Other",
    date: "2026-10-27",
    type: "expense",
    createdAt: new Date()
  }));

  await assertFails(expenses.doc("oversized_note").set({
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
  const goal = db.collection("users").doc("alice_1").collection("savingsGoals").doc("goal_1");
  await assertFails(goal.get());
  await assertFails(goal.set({
    userId: "alice_1",
    title: "Emergency Fund",
    targetAmount: 5000,
    currentAmount: 0,
    createdAt: new Date()
  }));
});
