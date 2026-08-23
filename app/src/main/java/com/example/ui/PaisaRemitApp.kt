package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.*
import com.example.data.repository.PaisaRepository
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*

sealed class Screen {
    object Splash : Screen()
    object Onboarding : Screen()
    object Auth : Screen()
    object Home : Screen()
    object Remittance : Screen()
    object Rates : Screen()
    object Transactions : Screen()
    object Offers : Screen()
    object Notices : Screen()
    object Notifications : Screen()
    object Support : Screen()
    object Kyc : Screen()
    object SecurityDevices : Screen()
    object Profile : Screen()
    object Recharge : Screen()
    object AdminDashboard : Screen()
}

@Composable
fun PaisaRemitApp() {
    val context = LocalContext.current
    val repository = remember { PaisaRepository(context) }

    // Request Android 13+ Notification Permission on startup for real-time FCM alerts
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    var selectedTransactionDetail by remember { mutableStateOf<RemittanceTransaction?>(null) }

    // Live Flow Collections
    val user by repository.currentUser.collectAsStateWithLifecycle()
    val rates by repository.getExchangeRates().collectAsStateWithLifecycle(initialValue = emptyList())
    val beneficiaries by repository.getBeneficiaries().collectAsStateWithLifecycle(initialValue = emptyList())
    val transactions by repository.getTransactions().collectAsStateWithLifecycle(initialValue = emptyList())
    val paymentMethods by repository.getPaymentMethods().collectAsStateWithLifecycle(initialValue = emptyList())
    val offers by repository.getOffers().collectAsStateWithLifecycle(initialValue = emptyList())
    val notices by repository.getNotices().collectAsStateWithLifecycle(initialValue = emptyList())
    val devices by repository.getDevices().collectAsStateWithLifecycle(initialValue = emptyList())
    val notifications by repository.notifications.collectAsStateWithLifecycle()
    val systemSettings by repository.systemSettings.collectAsStateWithLifecycle()

    val unreadNotifCount = notifications.count { !it.isRead }

    // Bottom Navigation destinations
    val bottomNavScreens = listOf(
        Triple(Screen.Home, "হোম", Icons.Default.Home),
        Triple(Screen.Remittance, "রেমিট্যান্স", Icons.Default.Send),
        Triple(Screen.Transactions, "লেনদেন", Icons.Default.ReceiptLong),
        Triple(Screen.Offers, "অফার", Icons.Default.LocalOffer),
        Triple(Screen.Profile, "প্রোফাইল", Icons.Default.Person)
    )

    val showBottomBar = currentScreen in listOf(
        Screen.Home,
        Screen.Transactions,
        Screen.Offers,
        Screen.Profile
    )

    // System Maintenance Screen Gate (if maintenance is active and not on admin)
    if (systemSettings.maintenanceMode && currentScreen != Screen.AdminDashboard && currentScreen != Screen.Splash) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Engineering,
                    contentDescription = null,
                    tint = MagentaPrimary,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "সিস্টেম রক্ষণাবেক্ষণে আছে",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = systemSettings.maintenanceMessage,
                    fontSize = 13.sp,
                    color = GrayText,
                    lineHeight = 19.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { currentScreen = Screen.AdminDashboard },
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleSecondary)
                ) {
                    Text("অ্যাডমিন মোডে প্রবেশ")
                }
            }
        }
        return
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    bottomNavScreens.forEach { (screen, label, icon) ->
                        val isSelected = currentScreen == screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentScreen = screen },
                            icon = {
                                Icon(
                                    icon,
                                    contentDescription = label,
                                    tint = if (isSelected) MagentaPrimary else GrayText,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MagentaPrimary else GrayText
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MagentaPrimary.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (showBottomBar) PaddingValues(bottom = 0.dp) else innerPadding)
        ) {
            when (currentScreen) {
                Screen.Splash -> {
                    SplashScreen(onSplashFinished = { currentScreen = Screen.Onboarding })
                }
                Screen.Onboarding -> {
                    OnboardingScreen(onFinishOnboarding = { currentScreen = Screen.Auth })
                }
                Screen.Auth -> {
                    LoginOtpScreen(onLoginSuccess = { currentScreen = Screen.Home })
                }
                Screen.Home -> {
                    HomeScreen(
                        user = user,
                        rates = rates,
                        transactions = transactions,
                        offers = offers,
                        notices = notices,
                        faqs = repository.faqs,
                        unreadNotifCount = unreadNotifCount,
                        onNavigateToSendMoney = { currentScreen = Screen.Remittance },
                        onNavigateToRates = { currentScreen = Screen.Rates },
                        onNavigateToTransactions = { currentScreen = Screen.Transactions },
                        onNavigateToOffers = { currentScreen = Screen.Offers },
                        onNavigateToNotices = { currentScreen = Screen.Notices },
                        onNavigateToSupport = { currentScreen = Screen.Support },
                        onNavigateToKyc = { currentScreen = Screen.Kyc },
                        onNavigateToRecharge = { currentScreen = Screen.Recharge },
                        onNavigateToNotifications = { currentScreen = Screen.Notifications },
                        onNavigateToAdmin = { currentScreen = Screen.AdminDashboard },
                        onSelectTransaction = { selectedTransactionDetail = it }
                    )
                }
                Screen.Notifications -> {
                    NotificationScreen(
                        repository = repository,
                        transactions = transactions,
                        onBack = { currentScreen = Screen.Home },
                        onSelectTransaction = {
                            selectedTransactionDetail = it
                            currentScreen = Screen.Transactions
                        }
                    )
                }
                Screen.Remittance -> {
                    RemittanceScreen(
                        repository = repository,
                        rates = rates,
                        beneficiaries = beneficiaries,
                        paymentMethods = paymentMethods,
                        onBack = { currentScreen = Screen.Home },
                        onTransactionSuccess = {
                            currentScreen = Screen.Home
                        }
                    )
                }
                Screen.Rates -> {
                    RatesScreen(
                        rates = rates,
                        onBack = { currentScreen = Screen.Home },
                        onNavigateToSendMoney = { currency ->
                            repository.setSelectedCurrency(currency)
                            currentScreen = Screen.Remittance
                        }
                    )
                }
                Screen.Transactions -> {
                    TransactionsScreen(
                        transactions = transactions,
                        onBack = { currentScreen = Screen.Home },
                        onSelectTransaction = { selectedTransactionDetail = it }
                    )
                }
                Screen.Offers -> {
                    OffersScreen(
                        offers = offers,
                        onBack = { currentScreen = Screen.Home },
                        onApplyOffer = { currentScreen = Screen.Remittance }
                    )
                }
                Screen.Notices -> {
                    NoticesScreen(
                        notices = notices,
                        onBack = { currentScreen = Screen.Home }
                    )
                }
                Screen.Support -> {
                    SupportScreen(
                        repository = repository,
                        systemSettings = systemSettings,
                        onBack = { currentScreen = Screen.Home }
                    )
                }
                Screen.Kyc -> {
                    KycScreen(
                        user = user,
                        repository = repository,
                        onBack = { currentScreen = Screen.Home }
                    )
                }
                Screen.SecurityDevices -> {
                    SecurityDevicesScreen(
                        user = user,
                        devices = devices,
                        repository = repository,
                        onBack = { currentScreen = Screen.Home }
                    )
                }
                Screen.Profile -> {
                    ProfileScreen(
                        user = user,
                        beneficiaries = beneficiaries,
                        onNavigateToKyc = { currentScreen = Screen.Kyc },
                        onNavigateToSecurity = { currentScreen = Screen.SecurityDevices },
                        onNavigateToSupport = { currentScreen = Screen.Support },
                        onNavigateToAdmin = { currentScreen = Screen.AdminDashboard },
                        onLogout = { currentScreen = Screen.Auth }
                    )
                }
                Screen.Recharge -> {
                    MobileRechargeScreen(
                        repository = repository,
                        onBack = { currentScreen = Screen.Home }
                    )
                }
                Screen.AdminDashboard -> {
                    AdminDashboardScreen(
                        repository = repository,
                        user = user,
                        rates = rates,
                        transactions = transactions,
                        devices = devices,
                        systemSettings = systemSettings,
                        onBack = { currentScreen = Screen.Home }
                    )
                }
            }

            // Transaction Detail Modal BottomSheet / Dialog
            selectedTransactionDetail?.let { tx ->
                AlertDialog(
                    onDismissRequest = { selectedTransactionDetail = null },
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("লেনদেন বিবরণী", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            StatusBadge(status = tx.status)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReviewItem("রেফারেন্স আইডি", tx.id, isBold = true)
                            ReviewItem("প্রাপক", tx.recipientName)
                            ReviewItem("অ্যাকাউন্ট নং", tx.recipientAccount)
                            ReviewItem("চ্যানেল", tx.recipientChannel)
                            ReviewItem("প্রেরিত টাকা", "${tx.sendAmount} ${tx.sendCurrency}")
                            ReviewItem("এক্সচেঞ্জ রেট", "১ ${tx.sendCurrency} = ৳${String.format("%.2f", tx.exchangeRate)}")
                            ReviewItem("সরকারি প্রণোদনা (+২.৫%)", "৳ ${String.format("%,.2f", tx.govtIncentiveBdt)}")
                            ReviewItem("বাংলাদেশে প্রাপক পেয়েছেন", "৳ ${String.format("%,.2f", tx.recipientBdtAmount)}", isHighlight = true)
                            ReviewItem("পেমেন্ট পদ্ধতি", tx.paymentMethodName)
                            ReviewItem("তারিখ", tx.createdAt)

                            if (!tx.adminNote.isNullOrBlank()) {
                                ReviewItem("অ্যাডমিন নোট", tx.adminNote)
                            }

                            Spacer(Modifier.height(8.dp))
                            Text("ট্র্যাকিং টাইমলাইন:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkText)

                            tx.statusTimeline.forEach { step ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (step.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (step.isCompleted) SuccessGreen else GrayText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("${step.title} (${step.timestamp})", fontSize = 12.sp, color = DarkText)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { selectedTransactionDetail = null },
                            colors = ButtonDefaults.buttonColors(containerColor = MagentaPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("রসিদ ডাউনলোড / সম্পন্ন")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { selectedTransactionDetail = null }) {
                            Text("বন্ধ করুন", color = GrayText)
                        }
                    }
                )
            }
        }
    }
}
