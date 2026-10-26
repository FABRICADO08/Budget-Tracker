package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.database.AppDatabase
import com.example.data.local.entity.Loan
import com.example.data.local.entity.Transaction
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.components.MonthlySpendingBarChart
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.ChartboardScreen
import com.example.ui.screens.CategoryBudgetsScreen
import com.example.ui.screens.RecurringTransactionsScreen
import com.example.ui.theme.BudgetTrackerTheme
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.AiChatViewModel
import com.example.ui.viewmodel.ScanSlipState
import com.example.ui.viewmodel.ScannedSlipData
import com.example.ui.viewmodel.SlipScannerViewModel
import com.example.ui.viewmodel.TransactionViewModel
import com.example.util.BiometricHelper
import com.example.util.BudgetPreferences
import com.example.util.SecurityPreferences
import com.example.util.StatementExporter
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            configureAppCheck(this, intent)
        } catch (ignored: Exception) {}
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BudgetTrackerTheme {
                val context = LocalContext.current
                val auth = Firebase.auth
                var currentUser by remember { mutableStateOf(auth.currentUser) }

                // Listen to Firebase Auth state
                DisposableEffect(auth) {
                    val authListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { fbAuth ->
                        currentUser = fbAuth.currentUser
                    }
                    auth.addAuthStateListener(authListener)
                    onDispose { auth.removeAuthStateListener(authListener) }
                }

                // If user is not authenticated with Google, show Google Sign-In Gate
                if (currentUser == null) {
                    AuthScreen(
                        onAuthSuccess = {
                            currentUser = auth.currentUser
                        }
                    )
                } else {
                    // Authenticated: check if Fingerprint / Biometric Lock is active
                    val activity = context as FragmentActivity
                    var isUnlocked by remember {
                        mutableStateOf(!SecurityPreferences.isBiometricEnabled(context))
                    }

                    if (SecurityPreferences.isBiometricEnabled(context) && !isUnlocked) {
                        BiometricLockScreen(
                            activity = activity,
                            userName = currentUser?.displayName ?: currentUser?.email ?: "User",
                            onUnlocked = { isUnlocked = true }
                        )
                    } else {
                        // User is authenticated and biometric passed: display application
                        val db = remember { AppDatabase.getInstance(context) }
                        val transactionViewModel: TransactionViewModel = viewModel(
                            factory = TransactionViewModel.Factory(db.transactionDao(), db.loanDao())
                        )
                        val slipScannerViewModel: SlipScannerViewModel = viewModel()
                        val aiChatViewModel: AiChatViewModel = viewModel()

                        TransactionApp(
                            database = db,
                            viewModel = transactionViewModel,
                            scannerViewModel = slipScannerViewModel,
                            aiChatViewModel = aiChatViewModel,
                            currentUser = currentUser!!,
                            onSignOut = {
                                isUnlocked = false
                                currentUser = null
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Biometric / Fingerprint Lock Screen shown on startup when security lock is enabled.
 */
@Composable
fun BiometricLockScreen(
    activity: FragmentActivity,
    userName: String,
    onUnlocked: () -> Unit
) {
    var errorText by remember { mutableStateOf<String?>(null) }

    // Automatically prompt for fingerprint on launch
    LaunchedEffect(Unit) {
        BiometricHelper.showBiometricPrompt(
            activity = activity,
            title = "Unlock BudgetTracker",
            subtitle = "Verify fingerprint to access financial records",
            onSuccess = onUnlocked,
            onError = { err -> errorText = err }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "BudgetTracker Locked",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Welcome back, $userName\nPlease verify your fingerprint or screen lock to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = {
                    errorText = null
                    BiometricHelper.showBiometricPrompt(
                        activity = activity,
                        title = "Unlock BudgetTracker",
                        subtitle = "Verify fingerprint to access financial records",
                        onSuccess = onUnlocked,
                        onError = { err -> errorText = err }
                    )
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Scan Fingerprint", fontWeight = FontWeight.Bold)
            }

            if (errorText != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorText!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionApp(
    database: AppDatabase,
    viewModel: TransactionViewModel,
    scannerViewModel: SlipScannerViewModel,
    aiChatViewModel: AiChatViewModel,
    currentUser: FirebaseUser,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var selectedTab by remember { mutableIntStateOf(0) } // Expenses, Analytics, Loans, AI, Budgets, Recurring bills
    var aiPendingPrompt by remember { mutableStateOf<String?>(null) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddLoanDialog by remember { mutableStateOf(false) }
    var showProfileSecurityDialog by remember { mutableStateOf(false) }
    var loanToMarkRepaid by remember { mutableStateOf<Loan?>(null) }
    var scannedDataForDialog by remember { mutableStateOf<ScannedSlipData?>(null) }

    val transactions by viewModel.transactions.collectAsState()
    val loans by viewModel.loans.collectAsState()
    val scanState by scannerViewModel.scanState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val filteredTransactions = remember(transactions, searchQuery) {
        if (searchQuery.isBlank()) {
            transactions
        } else {
            val query = searchQuery.trim().lowercase(Locale.getDefault())
            transactions.filter {
                it.category.lowercase(Locale.getDefault()).contains(query) ||
                it.description.lowercase(Locale.getDefault()).contains(query)
            }
        }
    }

    val totalExpense = transactions.filter { it.type == "expense" }.sumOf { it.amount }
    val totalPendingLoans = loans.filter { !it.isRepaid }.sumOf { it.amount }

    // Zero-permission Android Photo Picker for receipt slips
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scannerViewModel.scanSlipImage(context, uri)
        }
    }

    // Auto-open AddTransactionDialog when slip is successfully analyzed
    LaunchedEffect(scanState) {
        if (scanState is ScanSlipState.Success) {
            scannedDataForDialog = (scanState as ScanSlipState.Success).data
            showAddExpenseDialog = true
            scannerViewModel.resetState()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (selectedTab) {
                                    0 -> Icons.Default.AccountBalanceWallet
                                    1 -> Icons.Default.Insights
                                    2 -> Icons.Default.Handshake
                                    4 -> Icons.Default.AccountBalanceWallet
                                    5 -> Icons.Default.CalendarMonth
                                    else -> Icons.Default.AutoAwesome
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = when (selectedTab) {
                                    0 -> "Expense Tracker"
                                    1 -> "Financial Chartboard"
                                    2 -> "Loans Given"
                                    4 -> "Category Budgets"
                                    5 -> "Recurring Bills"
                                    else -> "Gemini AI Chatbot"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (selectedTab) {
                                    0 -> "Total: R%.2f".format(totalExpense)
                                    1 -> "Monthly Spending Analytics"
                                    2 -> "Owed: R%.2f".format(totalPendingLoans)
                                    4 -> "Plan spending by category"
                                    5 -> "Upcoming payments & income"
                                    else -> "Google Search Grounded"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Quick Chatbot Access Button
                    IconButton(
                        onClick = { selectedTab = 3 },
                        modifier = Modifier.testTag("top_ai_chatbot_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Gemini Chatbot",
                            tint = if (selectedTab == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (selectedTab == 0) {
                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("scan_slip_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = "Scan Till Slip",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Export & Share Statement Button
                    IconButton(
                        onClick = {
                            val currentBudget = BudgetPreferences.getMonthlyBudgetCap(context)
                            StatementExporter.shareFinancialStatement(context, transactions, loans, currentBudget)
                        },
                        modifier = Modifier.testTag("export_statement_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export Financial Statement",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // User Account & Fingerprint Security Button
                    IconButton(
                        onClick = { showProfileSecurityDialog = true },
                        modifier = Modifier.testTag("user_profile_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (currentUser.displayName?.take(1) ?: currentUser.email?.take(1) ?: "U").uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = "Expenses"
                        )
                    },
                    label = { Text("Expenses") },
                    modifier = Modifier.testTag("tab_expenses")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = "Chartboard"
                        )
                    },
                    label = { Text("Chartboard") },
                    modifier = Modifier.testTag("tab_chartboard")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Handshake,
                            contentDescription = "Loans"
                        )
                    },
                    label = { Text("Loans") },
                    modifier = Modifier.testTag("tab_loans")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Chatbot"
                        )
                    },
                    label = { Text("AI Chatbot") },
                    modifier = Modifier.testTag("tab_ai_advisor")
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Category Budgets"
                        )
                    },
                    label = { Text("Budgets") },
                    modifier = Modifier.testTag("tab_category_budgets")
                )
                NavigationBarItem(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Recurring bills") },
                    label = { Text("Bills") },
                    modifier = Modifier.testTag("tab_recurring_bills")
                )
            }
        },
        floatingActionButton = {
            when (selectedTab) {
                0 -> {
                    FloatingActionButton(
                        onClick = {
                            scannedDataForDialog = null
                            showAddExpenseDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("add_transaction_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Expense"
                        )
                    }
                }
                2 -> {
                    FloatingActionButton(
                        onClick = { showAddLoanDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("add_loan_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Loan"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (selectedTab) {
                0 -> {
                    // Tab 0: Expenses with Dynamic Bar Chart, Monthly Summary, Slip Scanner CTA & List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Current Month Total Card
                        item {
                            MonthlyExpenditureSummary(viewModel = viewModel, userId = currentUser.uid)
                        }

                        // 2. Dynamic Monthly Spending Bar Chart
                        item {
                            MonthlySpendingBarChart(transactions = transactions)
                        }

                        // 2.5 Quick Chartboard CTA
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTab = 1 },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Insights,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Open Full Chartboard",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Interactive breakdown, trends & loan recovery",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Explore →",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 3. Quick Scan Slip CTA Card
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DocumentScanner,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Scan Till Slip / Receipt",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "AI automatically extracts total, store & date",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "Scan",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 4. Section Header with match count
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RECENT TRANSACTIONS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                if (searchQuery.isNotBlank()) {
                                    Text(
                                        text = "${filteredTransactions.size} found",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 5. Search Bar to filter entries by category or description
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search by category or description...") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear search",
                                                tint = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("transaction_search_input")
                            )
                        }

                        // 6. Quick Filter Chips
                        item {
                            val filterCategories = listOf("All", "Food & Dining", "Groceries", "Transportation", "Bills", "Shopping")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                filterCategories.forEach { cat ->
                                    val isSelected = if (cat == "All") searchQuery.isEmpty() else searchQuery.equals(cat, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            searchQuery = if (cat == "All" || isSelected) "" else cat
                                        },
                                        label = { Text(cat, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }

                        if (transactions.isEmpty()) {
                            item {
                                EmptyTransactionsView(
                                    onAddClick = {
                                        scannedDataForDialog = null
                                        showAddExpenseDialog = true
                                    },
                                    onScanClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )
                            }
                        } else if (filteredTransactions.isEmpty()) {
                            item {
                                NoSearchResultsView(
                                    query = searchQuery,
                                    onClear = { searchQuery = "" }
                                )
                            }
                        } else {
                            items(filteredTransactions, key = { it.id }) { transaction ->
                                TransactionListItem(
                                    transaction = transaction,
                                    onDelete = { viewModel.deleteTransaction(transaction) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Tab 1: Financial Chartboard
                    ChartboardScreen(
                        transactions = transactions,
                        loans = loans,
                        onAskAiAdvisor = { prompt ->
                            aiPendingPrompt = prompt
                            selectedTab = 3
                        }
                    )
                }
                2 -> {
                    // Tab 2: Loans Given
                    LoansScreen(
                        loans = loans,
                        onRequestMarkRepaid = { loan -> loanToMarkRepaid = loan },
                        onUnmarkRepaid = { loan -> viewModel.unmarkLoanRepaid(loan) },
                        onDeleteLoan = { loan -> viewModel.deleteLoan(loan) },
                        onAddLoanClick = { showAddLoanDialog = true }
                    )
                }
                3 -> {
                    // Tab 3: AI Financial Advisor Chatbot
                    AiChatScreen(
                        viewModel = aiChatViewModel,
                        initialPrompt = aiPendingPrompt,
                        onPromptConsumed = { aiPendingPrompt = null }
                    )
                }
                4 -> {
                    CategoryBudgetsScreen(database = database, transactions = transactions)
                }
                5 -> {
                    RecurringTransactionsScreen(database = database)
                }
            }
        }
    }

    // Modal when scanning slip
    if (scanState is ScanSlipState.Scanning) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = {
                Text(
                    text = "Scanning Till Slip...",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Column {
                        Text(
                            text = "Analyzing receipt with Gemini AI",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Detecting total amount (R), merchant, and date...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        )
    }

    // Error dialog if scanning failed
    if (scanState is ScanSlipState.Error) {
        val errorMsg = (scanState as ScanSlipState.Error).message
        AlertDialog(
            onDismissRequest = { scannerViewModel.resetState() },
            title = { Text("Slip Scan Notice") },
            text = { Text(errorMsg) },
            confirmButton = {
                Button(onClick = { scannerViewModel.resetState() }) {
                    Text("OK")
                }
            }
        )
    }

    if (showAddExpenseDialog) {
        AddTransactionDialog(
            initialAmount = scannedDataForDialog?.amount,
            initialCategory = scannedDataForDialog?.category ?: "Food & Dining",
            initialDescription = scannedDataForDialog?.let {
                if (it.note.isNotBlank()) "${it.merchant} (${it.note})" else it.merchant
            } ?: "",
            initialDate = scannedDataForDialog?.date,
            onDismiss = {
                showAddExpenseDialog = false
                scannedDataForDialog = null
            },
            onConfirm = { amount, category, description, date ->
                viewModel.insertTransaction(
                    amount = amount,
                    category = category,
                    description = description,
                    date = date
                )
                showAddExpenseDialog = false
                scannedDataForDialog = null
            }
        )
    }

    if (showAddLoanDialog) {
        AddLoanDialog(
            onDismiss = { showAddLoanDialog = false },
            onConfirm = { name, amount, lentDate, dueDate, note ->
                viewModel.insertLoan(
                    borrowerName = name,
                    amount = amount,
                    lentDate = lentDate,
                    dueDate = dueDate,
                    note = note
                )
                showAddLoanDialog = false
            }
        )
    }

    if (loanToMarkRepaid != null) {
        MarkRepaidDialog(
            loan = loanToMarkRepaid!!,
            onDismiss = { loanToMarkRepaid = null },
            onConfirm = { repaidDate ->
                viewModel.markLoanAsRepaid(loanToMarkRepaid!!, repaidDate)
                loanToMarkRepaid = null
            }
        )
    }

    // Account & Fingerprint Security Settings Dialog
    if (showProfileSecurityDialog) {
        ProfileSecurityDialog(
            currentUser = currentUser,
            transactions = transactions,
            loans = loans,
            onDismiss = { showProfileSecurityDialog = false },
            onSignOut = {
                showProfileSecurityDialog = false
                signOut(
                    context = context,
                    credentialManager = credentialManager,
                    onSignOutComplete = onSignOut,
                    scope = coroutineScope
                )
            }
        )
    }
}

/**
 * Account & Fingerprint Security Settings Dialog.
 */
@Composable
fun ProfileSecurityDialog(
    currentUser: FirebaseUser,
    transactions: List<Transaction> = emptyList(),
    loans: List<Loan> = emptyList(),
    onDismiss: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var isBiometricEnabled by remember {
        mutableStateOf(SecurityPreferences.isBiometricEnabled(context))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Account & Security",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User Info Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (currentUser.displayName?.take(1) ?: currentUser.email?.take(1) ?: "U").uppercase(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser.displayName ?: "Google User",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentUser.email ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Biometric / Fingerprint Setting
                Text(
                    text = "BIOMETRIC SECURITY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = if (isBiometricEnabled) IncomeGreen else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Fingerprint / App Lock",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = if (isBiometricEnabled) "Enabled: prompt fingerprint whenever opening app" else "Disabled: tap to lock app with fingerprint",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { enable ->
                            if (enable) {
                                if (activity != null && BiometricHelper.isBiometricSupported(context)) {
                                    BiometricHelper.showBiometricPrompt(
                                        activity = activity,
                                        title = "Enable Fingerprint Lock",
                                        subtitle = "Verify fingerprint to enable app lock",
                                        onSuccess = {
                                            SecurityPreferences.setBiometricEnabled(context, true)
                                            isBiometricEnabled = true
                                            Toast.makeText(context, "Fingerprint Lock Enabled", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    // Device fallback
                                    SecurityPreferences.setBiometricEnabled(context, true)
                                    isBiometricEnabled = true
                                    Toast.makeText(context, "App Lock Enabled", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                SecurityPreferences.setBiometricEnabled(context, false)
                                isBiometricEnabled = false
                                Toast.makeText(context, "Fingerprint Lock Disabled", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // Database & Cloud Status
                HorizontalDivider()

                Text(
                    text = "DATA PERSISTENCE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "SQLite Room & Cloud Synced",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "All expenses, loans & settings stay saved locally and synced with your Google account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Export & Share Financial Statement Option
                HorizontalDivider()

                Text(
                    text = "REPORTS & STATEMENTS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val currentBudget = BudgetPreferences.getMonthlyBudgetCap(context)
                            StatementExporter.shareFinancialStatement(context, transactions, loans, currentBudget)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Share Financial Statement",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Export monthly totals, expenses & loan records",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "Export →",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSignOut,
                colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed)
            ) {
                Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sign Out")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

/**
 * Calculates and displays the total expenditure for the current month in South African Rand (R).
 */
@Composable
fun MonthlyExpenditureSummary(
    viewModel: TransactionViewModel,
    modifier: Modifier = Modifier,
    userId: String? = null
) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsState()
    val loans by viewModel.loans.collectAsState()

    var monthlyBudget by remember(userId) {
        mutableStateOf(if (userId.isNullOrBlank()) BudgetPreferences.getMonthlyBudgetCap(context) else BudgetPreferences.getMonthlyBudgetCap(context, userId))
    }
    var showEditBudgetDialog by remember { mutableStateOf(false) }

    val currentYearMonth = remember {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }
    val currentMonthTitle = remember {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    }

    val monthTransactions = remember(transactions, currentYearMonth) {
        transactions.filter { it.date.startsWith(currentYearMonth) && it.type == "expense" }
    }
    val totalExpenditure = remember(monthTransactions) {
        monthTransactions.sumOf { it.amount }
    }

    val currentDay = remember {
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    }
    val dailyAvg = if (currentDay > 0) totalExpenditure / currentDay else 0.0

    val budgetProgress = if (monthlyBudget > 0) (totalExpenditure / monthlyBudget).toFloat() else 0f
    val isOverBudget = monthlyBudget > 0 && totalExpenditure > monthlyBudget
    val remainingBudget = (monthlyBudget - totalExpenditure).coerceAtLeast(0.0)
    val overBudgetAmount = (totalExpenditure - monthlyBudget).coerceAtLeast(0.0)

    if (showEditBudgetDialog) {
        EditBudgetDialog(
            currentBudget = monthlyBudget,
            onDismiss = { showEditBudgetDialog = false },
            onConfirm = { newBudget ->
                if (userId.isNullOrBlank()) BudgetPreferences.setMonthlyBudgetCap(context, newBudget)
                else BudgetPreferences.setMonthlyBudgetCap(context, userId, newBudget)
                monthlyBudget = newBudget
                showEditBudgetDialog = false
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_expenditure_card"),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF064E3B), // Deep emerald
                            Color(0xFF047857), // Forest emerald
                            Color(0xFF0D9488)  // Modern teal
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = currentMonthTitle.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = CircleShape
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Total Expenditure",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.82f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "R%.2f".format(totalExpenditure),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    // Quick export / share statement icon
                    IconButton(
                        onClick = {
                            StatementExporter.shareFinancialStatement(context, transactions, loans, monthlyBudget)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export statement",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Monthly Budget Target Section
                Surface(
                    color = Color.Black.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Budget Target: ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = "R%.0f".format(monthlyBudget),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { showEditBudgetDialog = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit budget target",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Edit Target",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { budgetProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isOverBudget) Color(0xFFF87171) else if (budgetProgress > 0.8f) Color(0xFFFBBF24) else Color(0xFF34D399),
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isOverBudget) "⚠️ Over limit by R%.2f".format(overBudgetAmount) else "R%.2f remaining".format(remainingBudget),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isOverBudget) Color(0xFFFCA5A5) else Color(0xFF6EE7B7)
                            )
                            Text(
                                text = "%.0f%% used".format(budgetProgress * 100),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stats footer inside the card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${monthTransactions.size} transactions recorded",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "Avg R%.0f/day".format(dailyAvg),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6EE7B7) // Mint accent
                    )
                }
            }
        }
    }
}

/**
 * Dialog enabling users to customize their monthly spending budget target in South African Rand (R).
 */
@Composable
fun EditBudgetDialog(
    currentBudget: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var budgetText by remember { mutableStateOf(if (currentBudget > 0) "%.0f".format(currentBudget) else "5000") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Set Monthly Budget Target", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    text = "Set your planned spending cap for the month in South African Rand (R). The app will track your daily pace and alert you as you near the limit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = {
                        budgetText = it
                        isError = it.toDoubleOrNull() == null || (it.toDoubleOrNull() ?: 0.0) <= 0
                    },
                    label = { Text("Monthly Budget (R)") },
                    prefix = { Text("R ") },
                    isError = isError,
                    supportingText = if (isError) { { Text("Enter a valid amount greater than 0") } } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = budgetText.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        onConfirm(amount)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Save Target")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LoansScreen(
    loans: List<Loan>,
    onRequestMarkRepaid: (Loan) -> Unit,
    onUnmarkRepaid: (Loan) -> Unit,
    onDeleteLoan: (Loan) -> Unit,
    onAddLoanClick: () -> Unit
) {
    var filterStatus by remember { mutableIntStateOf(0) } // 0: All, 1: Pending, 2: Repaid

    val pendingTotal = loans.filter { !it.isRepaid }.sumOf { it.amount }
    val repaidTotal = loans.filter { it.isRepaid }.sumOf { it.amount }

    val filteredLoans = remember(loans, filterStatus) {
        when (filterStatus) {
            1 -> loans.filter { !it.isRepaid }
            2 -> loans.filter { it.isRepaid }
            else -> loans
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Loans Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MONEY OWED TO YOU",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "R%.2f".format(pendingTotal),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (pendingTotal > 0) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        color = IncomeGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Recovered",
                                style = MaterialTheme.typography.labelSmall,
                                color = IncomeGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "R%.2f".format(repaidTotal),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }
                    }
                }

                val totalLoanedAll = pendingTotal + repaidTotal
                val recoveryRate = if (totalLoanedAll > 0) (repaidTotal / totalLoanedAll).toFloat() else 0f
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recovery Rate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.0f%%".format(recoveryRate * 100),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { recoveryRate.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = IncomeGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Filter chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterStatus == 0,
                        onClick = { filterStatus = 0 },
                        label = { Text("All (${loans.size})") }
                    )
                    FilterChip(
                        selected = filterStatus == 1,
                        onClick = { filterStatus = 1 },
                        label = { Text("Pending (${loans.count { !it.isRepaid }})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFD97706),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = filterStatus == 2,
                        onClick = { filterStatus = 2 },
                        label = { Text("Repaid (${loans.count { it.isRepaid }})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IncomeGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Loan List or Empty State
        if (filteredLoans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Handshake,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (filterStatus == 1) "No pending loans" else if (filterStatus == 2) "No repaid loans yet" else "No Loans Recorded",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Keep track of friends or colleagues you gave money to and mark the date when paid back.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onAddLoanClick,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Record a Loan")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredLoans, key = { it.id }) { loan ->
                    LoanListItem(
                        loan = loan,
                        onMarkRepaid = { onRequestMarkRepaid(loan) },
                        onUnmarkRepaid = { onUnmarkRepaid(loan) },
                        onDelete = { onDeleteLoan(loan) }
                    )
                }
            }
        }
    }
}

@Composable
fun LoanListItem(
    loan: Loan,
    onMarkRepaid: () -> Unit,
    onUnmarkRepaid: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("loan_item_${loan.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (loan.isRepaid) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (loan.isRepaid) IncomeGreen.copy(alpha = 0.15f) else Color(0xFFD97706).copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (loan.isRepaid) Icons.Default.CheckCircle else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (loan.isRepaid) IncomeGreen else Color(0xFFD97706),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = loan.borrowerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Lent on: ${loan.lentDate}" + if (loan.dueDate.isNotBlank()) " • Due: ${loan.dueDate}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "R%.2f".format(loan.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (loan.isRepaid) IncomeGreen else Color(0xFFD97706)
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (loan.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Note: ${loan.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (loan.isRepaid) IncomeGreen.copy(alpha = 0.12f) else Color(0xFFD97706).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (loan.isRepaid) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = if (loan.isRepaid) IncomeGreen else Color(0xFFD97706),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (loan.isRepaid) "BROUGHT BACK: ${loan.repaidDate}" else "PENDING REPAYMENT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (loan.isRepaid) IncomeGreen else Color(0xFFD97706)
                        )
                    }
                }

                if (!loan.isRepaid) {
                    val context = LocalContext.current
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                val reminderText = "Hi ${loan.borrowerName}, friendly reminder regarding the loan of R${"%.2f".format(loan.amount)} lent on ${loan.lentDate}${if (loan.dueDate.isNotBlank()) " (due ${loan.dueDate})" else ""}. Thank you!"
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, reminderText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Send Reminder to ${loan.borrowerName}"))
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Remind", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remind", fontSize = 12.sp)
                        }

                        Button(
                            onClick = onMarkRepaid,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IncomeGreen,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Brought Back", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onUnmarkRepaid,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Unmark", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Dialog that prompts the user to select or confirm the exact date when the loan was brought back.
 */
@Composable
fun MarkRepaidDialog(
    loan: Loan,
    onDismiss: () -> Unit,
    onConfirm: (repaidDate: String) -> Unit
) {
    val today = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    var returnDate by remember { mutableStateOf(today) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Mark as Brought Back",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "When did ${loan.borrowerName} return the R%.2f loan?".format(loan.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                DatePickerField(
                    label = "Date Returned",
                    dateValue = returnDate,
                    onDateSelected = { returnDate = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(returnDate) },
                colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Confirm Date")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddLoanDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Double, lentDate: String, dueDate: String, note: String) -> Unit
) {
    val today = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var lentDate by remember { mutableStateOf(today) }
    var dueDate by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Record Loan Given",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorText = null
                    },
                    label = { Text("Person's Name") },
                    placeholder = { Text("e.g. Sipho, Sarah, John") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorText = null
                    },
                    label = { Text("Amount Lent (R)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                DatePickerField(
                    label = "Date Lent",
                    dateValue = lentDate,
                    onDateSelected = { lentDate = it }
                )

                DatePickerField(
                    label = "Expected Return Date (optional)",
                    dateValue = dueDate,
                    onDateSelected = { dueDate = it },
                    placeholder = "Select expected return date",
                    allowClear = true,
                    onClear = { dueDate = "" }
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Reason / Note (optional)") },
                    placeholder = { Text("e.g. Fuel money, Emergency groceries") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (name.isBlank()) {
                        errorText = "Please enter the person's name"
                        return@Button
                    }
                    if (amount == null || amount <= 0) {
                        errorText = "Please enter a valid amount greater than 0"
                        return@Button
                    }
                    onConfirm(name.trim(), amount, lentDate, dueDate.trim(), note.trim())
                }
            ) {
                Text("Save Loan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddTransactionDialog(
    initialAmount: Double? = null,
    initialCategory: String = "Food & Dining",
    initialDescription: String = "",
    initialDate: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, category: String, description: String, date: String) -> Unit
) {
    val today = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    var amountText by remember {
        mutableStateOf(if (initialAmount != null && initialAmount > 0) "%.2f".format(initialAmount) else "")
    }
    var category by remember { mutableStateOf(initialCategory) }
    var description by remember { mutableStateOf(initialDescription) }
    var date by remember { mutableStateOf(initialDate ?: today) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetCategories = listOf(
        "Food & Dining",
        "Transportation",
        "Groceries",
        "Bills",
        "Shopping",
        "Entertainment",
        "Health",
        "Other"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialAmount != null && initialAmount > 0) "Confirm Scanned Slip" else "New Expense",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("Amount (R)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_amount_input")
                )

                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        errorMessage = null
                    },
                    label = { Text("Description / Store") },
                    placeholder = { Text("e.g. Woolworths, Pick n Pay, Fuel") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_description_input")
                )

                DatePickerField(
                    label = "Transaction Date",
                    dateValue = date,
                    onDateSelected = { date = it }
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmount = amountText.toDoubleOrNull()
                    if (parsedAmount == null || parsedAmount <= 0) {
                        errorMessage = "Please enter a valid amount greater than 0"
                        return@Button
                    }
                    if (description.isBlank()) {
                        errorMessage = "Please enter a description"
                        return@Button
                    }
                    onConfirm(parsedAmount, category, description.trim(), date)
                },
                modifier = Modifier.testTag("dialog_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Reusable DatePicker text field that triggers the Material 3 DatePickerDialog upon click.
 */
@Composable
fun DatePickerField(
    label: String,
    dateValue: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select date",
    allowClear: Boolean = false,
    onClear: () -> Unit = {}
) {
    var showDatePicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = dateValue,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (allowClear && dateValue.isNotBlank()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear date",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Pick Date",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDatePicker = true }
    )

    if (showDatePicker) {
        AppDatePickerDialog(
            initialDate = dateValue,
            onDateSelected = { selected ->
                onDateSelected(selected)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

/**
 * Material 3 DatePickerDialog wrapper converting selected epoch UTC millis to YYYY-MM-DD.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    initialDate: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = remember(initialDate) {
        try {
            if (initialDate.isNotBlank()) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                sdf.parse(initialDate)?.time
            } else null
        } catch (e: Exception) {
            null
        } ?: System.currentTimeMillis()
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = millis
                        }
                        val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }.format(calendar.time)
                        onDateSelected(formattedDate)
                    }
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun TransactionListItem(
    transaction: Transaction,
    onDelete: () -> Unit
) {
    val categoryColor = remember(transaction.category) {
        getCategoryColor(transaction.category)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_item_${transaction.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(categoryColor.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getTransactionIcon(transaction.category),
                    contentDescription = transaction.category,
                    tint = categoryColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description.ifBlank { transaction.category },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = categoryColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = transaction.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = transaction.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = if (transaction.type == "expense") ExpenseRed.copy(alpha = 0.1f) else IncomeGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "%sR%.2f".format(if (transaction.type == "expense") "-" else "+", transaction.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (transaction.type == "expense") ExpenseRed else IncomeGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("delete_transaction_${transaction.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Transaction",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyTransactionsView(
    onAddClick: () -> Unit,
    onScanClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Expenses Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Scan a till slip with Gemini AI or manually record your first expenditure.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onScanClick,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan Slip")
                    }
                    Button(
                        onClick = onAddClick,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("empty_add_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Manual")
                    }
                }
            }
        }
    }
}

@Composable
fun NoSearchResultsView(
    query: String,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No results for \"$query\"",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Try searching for another category or store description.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onClear,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Clear Search")
            }
        }
    }
}

fun getCategoryColor(category: String): Color {
    return when (category.lowercase(Locale.getDefault())) {
        "food & dining", "food" -> Color(0xFFEA580C)
        "groceries" -> Color(0xFF16A34A)
        "transportation", "transport" -> Color(0xFF0284C7)
        "bills", "bills & utilities", "utilities" -> Color(0xFFE11D48)
        "shopping" -> Color(0xFF9333EA)
        "entertainment" -> Color(0xFFD97706)
        "health", "fitness" -> Color(0xFF0D9488)
        else -> Color(0xFF64748B)
    }
}

fun getTransactionIcon(category: String): ImageVector {
    return when (category.lowercase(Locale.getDefault())) {
        "food & dining", "food" -> Icons.Default.Fastfood
        "transportation", "transport" -> Icons.Default.DirectionsCar
        "groceries" -> Icons.Default.ShoppingCart
        "bills", "bills & utilities", "utilities" -> Icons.Default.Receipt
        "shopping" -> Icons.Default.LocalAtm
        "entertainment" -> Icons.Default.Movie
        "health", "fitness" -> Icons.Default.FitnessCenter
        else -> Icons.Default.HomeRepairService
    }
}
