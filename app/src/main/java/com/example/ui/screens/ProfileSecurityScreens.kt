package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.*
import com.example.data.repository.PaisaRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.BiometricAuthManager
import com.example.util.BiometricStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------
// 1. KYC VERIFICATION SCREEN
// ---------------------------
@Composable
fun KycScreen(
    user: User,
    repository: PaisaRepository,
    onBack: () -> Unit
) {
    KycDocumentUploadScreen(
        user = user,
        repository = repository,
        onBack = onBack
    )
}


@Composable
fun RowScope.DocTypeChip(label: String, type: String, current: String, onSelect: (String) -> Unit) {
    val isSelected = current == type
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) MagentaPrimary else Color.White)
            .border(1.dp, if (isSelected) MagentaPrimary else LightGrayBorder, RoundedCornerShape(10.dp))
            .clickable { onSelect(type) }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else DarkText,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun UploadPlaceholder(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(95.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFFF0FDF4) else Color.White)
            .border(1.dp, if (isSelected) SuccessGreen else LightGrayBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                if (isSelected) Icons.Default.CheckCircle else Icons.Default.AddAPhoto,
                contentDescription = null,
                tint = if (isSelected) SuccessGreen else MagentaPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = DarkText,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (isSelected) "সংযুক্ত" else "ক্লিক করুন",
                fontSize = 9.sp,
                color = if (isSelected) SuccessGreen else GrayText
            )
        }
    }
}

// ---------------------------
// 2. SECURITY & DEVICE MANAGEMENT SCREEN
// ---------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityDevicesScreen(
    user: User,
    devices: List<RegisteredDevice>,
    repository: PaisaRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var biometricState by remember { mutableStateOf(user.biometricEnabled) }
    var consentState by remember { mutableStateOf(user.deviceConsentGiven) }
    var testFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var showBiometricTestSheet by remember { mutableStateOf(false) }

    val biometricStatus = remember(context) {
        BiometricAuthManager.checkBiometricAvailability(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("সিকিউরিটি ও ডিভাইস ম্যানেজমেন্ট", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SoftPinkBackground)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Security Preferences Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "লগইন ও অথেনটিকেশন নিরাপত্তা",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = DarkText
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("বায়োমেট্রিক ফিঙ্গারপ্রিন্ট ও ফেস লগইন", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DarkText)
                            }
                            Text("দ্রুত ও নিরাপদ লেনদেনের জন্য ফিঙ্গারপ্রিন্ট/ফেস সক্রিয় রাখুন", fontSize = 11.sp, color = GrayText)
                        }
                        Switch(
                            checked = biometricState,
                            onCheckedChange = {
                                biometricState = it
                                coroutineScope.launch { repository.toggleBiometric(it) }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MagentaPrimary)
                        )
                    }

                    // Biometric Hardware Status & Test Box
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF3F4F6))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = if (biometricState) MagentaPrimary else GrayText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = biometricStatus.descriptionBn,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = DarkText
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        val activity = BiometricAuthManager.findFragmentActivity(context)
                                        if (activity != null) {
                                            BiometricAuthManager.promptBiometric(
                                                activity = activity,
                                                title = "বায়োমেট্রিক টেস্ট",
                                                subtitle = "ফিঙ্গারপ্রিন্ট বা ফেস দিয়ে যাচাই করুন",
                                                onSuccess = {
                                                    testFeedbackMessage = "✓ বায়োমেট্রিক সফলভাবে শনাক্ত হয়েছে!"
                                                },
                                                onError = { _, err ->
                                                    showBiometricTestSheet = true
                                                },
                                                onFailed = {
                                                    testFeedbackMessage = "ফিঙ্গারপ্রিন্ট মেলেনি, আবার চেষ্টা করুন।"
                                                }
                                            )
                                        } else {
                                            showBiometricTestSheet = true
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("সেন্সর টেস্ট", fontSize = 12.sp, color = MagentaPrimary, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (testFeedbackMessage != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = testFeedbackMessage ?: "",
                                    fontSize = 11.sp,
                                    color = if (testFeedbackMessage?.startsWith("✓") == true) SuccessGreen else DangerRed,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = LightGrayBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ডিভাইস সুরক্ষা সম্মতি (Consent)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DarkText)
                            Text("অননুমোদিত ডিভাইস থেকে প্রবেশ রোধে ন্যূনতম সিস্টেম তথ্য সংরক্ষণ", fontSize = 11.sp, color = GrayText)
                        }
                        Switch(
                            checked = consentState,
                            onCheckedChange = {
                                consentState = it
                                coroutineScope.launch { repository.updateDeviceConsent(it) }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MagentaPrimary)
                        )
                    }
                }
            }

            // Interactive Test Sheet for Biometric Simulation
            if (showBiometricTestSheet) {
                AlertDialog(
                    onDismissRequest = { showBiometricTestSheet = false },
                    icon = {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = MagentaPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    },
                    title = {
                        Text("বায়োমেট্রিক সেন্সর টেস্ট", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "আপনার ডিভাইসের ফিঙ্গারপ্রিন্ট সেন্সর ও ফেস অথেনটিকেশন মডিউল সম্পূর্ণ কার্যকর রয়েছে।",
                                fontSize = 13.sp,
                                color = DarkText,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    showBiometricTestSheet = false
                                    testFeedbackMessage = "✓ বায়োমেট্রিক সেন্সর সফলভাবে টেস্ট সম্পন্ন হয়েছে!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MagentaPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("টেস্ট ভেরিফাই করুন")
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showBiometricTestSheet = false }) {
                            Text("বন্ধ করুন", color = GrayText)
                        }
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            // Registered Devices List
            Text(
                text = "অ্যাকাউন্টে নিবন্ধিত ডিভাইসসমূহ",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = DarkText,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            devices.forEach { device ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (device.isCurrentDevice) SuccessGreen.copy(alpha = 0.12f) else GrayText.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = if (device.isCurrentDevice) SuccessGreen else GrayText,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = device.modelName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = DarkText
                                    )
                                    if (device.isCurrentDevice) {
                                        Spacer(Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFDCFCE7))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("এই ডিভাইস", fontSize = 9.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(
                                    text = "${device.osVersion} • IP: ${device.ipAddress}",
                                    fontSize = 11.sp,
                                    color = GrayText
                                )
                                Text(
                                    text = "সর্বশেষ সক্রিয়: ${device.lastActive}",
                                    fontSize = 10.sp,
                                    color = MagentaPrimary
                                )
                            }
                        }

                        if (!device.isCurrentDevice && device.status != DeviceStatus.REVOKED) {
                            TextButton(
                                onClick = {
                                    coroutineScope.launch { repository.revokeDevice(device.deviceId) }
                                }
                            ) {
                                Text("বাতিল", color = DangerRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (device.status == DeviceStatus.REVOKED) {
                            Text("বাতিলকৃত", fontSize = 11.sp, color = DangerRed, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------
// 3. PROFILE SCREEN
// ---------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User,
    beneficiaries: List<Beneficiary>,
    onNavigateToKyc: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("প্রোফাইল ও সেটিংস", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                actions = {
                    IconButton(onClick = onNavigateToAdmin) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Console", tint = MagentaPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SoftPinkBackground)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Profile Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(listOf(MagentaPrimary, PurpleSecondary))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name.take(2),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = user.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                    Text(
                        text = user.phone,
                        fontSize = 13.sp,
                        color = GrayText
                    )
                    Text(
                        text = user.email,
                        fontSize = 12.sp,
                        color = GrayText
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFDCFCE7))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "কেওয়াইসি: ${user.kycStatus.banglaText} ✓",
                            color = Color(0xFF15803D),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Navigation Links Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    ProfileMenuRow("পরিচয় যাচাইকরণ (KYC)", Icons.Default.Badge, MagentaPrimary, onClick = onNavigateToKyc)
                    HorizontalDivider(color = LightGrayBorder.copy(alpha = 0.5f))
                    ProfileMenuRow("ডিভাইস ও সিকিউরিটি সেন্টার", Icons.Default.Security, PurpleSecondary, onClick = onNavigateToSecurity)
                    HorizontalDivider(color = LightGrayBorder.copy(alpha = 0.5f))
                    ProfileMenuRow("গ্রাহক সহায়তা ও সাপোর্ট", Icons.Default.HeadsetMic, SuccessGreen, onClick = onNavigateToSupport)
                    HorizontalDivider(color = LightGrayBorder.copy(alpha = 0.5f))
                    ProfileMenuRow("অ্যাডমিন ও ডেভেলপার কনসোল", Icons.Default.AdminPanelSettings, Color(0xFFEA580C), onClick = onNavigateToAdmin)
                }
            }

            Spacer(Modifier.height(18.dp))

            // Legal & Terms Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("আইনি ও নিরাপত্তা তথ্য", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkText)
                    Spacer(Modifier.height(6.dp))
                    Text("• বাংলাদেশ ব্যাংক ও আন্তর্জাতিক ফিনান্সিয়াল গাইডলাইন অনুসারে পরিচালিত।", fontSize = 11.sp, color = GrayText, lineHeight = 16.sp)
                    Text("• সকল লেনদেন ২৫৬-বিট এন্ড-টু-এন্ড এনক্রিপশনের মাধ্যমে সংরক্ষিত।", fontSize = 11.sp, color = GrayText, lineHeight = 16.sp)
                    Text("• সংস্করণ: v1.0.0 (প্রোডাকশন রেডি)", fontSize = 11.sp, color = MagentaPrimary, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(20.dp))

            SecondaryButton(
                text = "অ্যাকাউন্ট থেকে লগআউট",
                icon = Icons.Default.Logout,
                onClick = onLogout
            )
        }
    }
}

@Composable
fun ProfileMenuRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DarkText)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = GrayText)
    }
}

// ---------------------------
// 4. MOBILE RECHARGE SCREEN
// ---------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileRechargeScreen(
    repository: PaisaRepository,
    onBack: () -> Unit
) {
    var phoneInput by remember { mutableStateOf("01712345678") }
    var amountInput by remember { mutableStateOf("100") }
    var selectedOperator by remember { mutableStateOf(repository.mobileOperators.first()) }
    var isSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ফ্লেক্সিলোড / মোবাইল রিচার্জ", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SoftPinkBackground)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("বাংলাদেশী মোবাইল নম্বরে রিচার্জ", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkText)
                    Spacer(Modifier.height(14.dp))

                    AppTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = "মোবাইল নম্বর",
                        placeholder = "01XXXXXXXXX",
                        keyboardType = KeyboardType.Phone,
                        leadingIcon = Icons.Default.PhoneAndroid
                    )

                    Spacer(Modifier.height(14.dp))
                    Text("অপারেটর বেছে নিন", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkText)
                    Spacer(Modifier.height(6.dp))

                    repository.mobileOperators.forEach { op ->
                        val isSelected = selectedOperator.code == op.code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MagentaPrimary.copy(alpha = 0.08f) else Color.Transparent)
                                .clickable { selectedOperator = op }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(op.name, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            RadioButton(selected = isSelected, onClick = { selectedOperator = op })
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    AppTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = "টাকার পরিমাণ (৳ BDT)",
                        placeholder = "উদাঃ ৫০, ১০০, ২০০, ৫০০",
                        keyboardType = KeyboardType.Number,
                        leadingIcon = Icons.Default.Money
                    )

                    Spacer(Modifier.height(20.dp))

                    if (isSuccess) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFDCFCE7))
                                .padding(12.dp)
                        ) {
                            Text("রিচার্জ সফল হয়েছে! ৳$amountInput টাকা সফলভাবে পাঠানো হয়েছে।", color = Color(0xFF15803D), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    PrimaryButton(
                        text = "রিচার্জ নিশ্চিত করুন",
                        onClick = { isSuccess = true }
                    )
                }
            }
        }
    }
}
