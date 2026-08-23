package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.*
import com.example.data.repository.PaisaRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    repository: PaisaRepository,
    user: User,
    rates: List<ExchangeRate>,
    transactions: List<RemittanceTransaction>,
    devices: List<RegisteredDevice>,
    systemSettings: SystemSettings,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val auditLogs by repository.auditLogs.collectAsState()
    val kycSubmissions by repository.kycSubmissions.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Overview, 1: Transactions, 2: Rates, 3: KYC, 4: Audit Logs

    val tabs = listOf("ড্যাশবোর্ড", "লেনদেন ব্যবস্থাপনা", "রেট নিয়ন্ত্রণ", "কেওয়াইসি রিভিউ", "অডিট লগ")

    // Modals & Action States
    var selectedTxForAction by remember { mutableStateOf<RemittanceTransaction?>(null) }
    var actionNote by remember { mutableStateOf("") }
    var showTxStatusDialog by remember { mutableStateOf(false) }

    // KYC Admin Review States
    var selectedKycForReview by remember { mutableStateOf<KycSubmissionRecord?>(null) }
    var kycAdminActionType by remember { mutableStateOf<String?>(null) } // "APPROVE", "REJECT", "RESUBMIT", "METADATA_INSPECT"
    var kycAdminNoteInput by remember { mutableStateOf("") }
    var kycFilterStatus by remember { mutableStateOf<String>("ALL") } // "ALL", "PENDING", "VERIFIED", "REJECTED"

    // Rate Edit States
    var editingCurrency by remember { mutableStateOf("SAR") }
    var newRateValue by remember { mutableStateOf("32.45") }
    var newFeeValue by remember { mutableStateOf("5.0") }
    var rateUpdateSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("পয়সা রেমিট • অ্যাডমিন প্যানেল", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("ম্যানেজমেন্ট ও অপারেশনাল কন্ট্রোল", fontSize = 11.sp, color = GrayText)
                    }
                },
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
        ) {
            // Scrollable Admin Tab Bar
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = MagentaPrimary,
                edgePadding = 12.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    // TAB 0: OVERVIEW STATS
                    0 -> {
                        Text(
                            text = "আজকের অপারেশনাল স্ট্যাটিস্টিকস",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AdminStatCard("মোট গ্রাহক", "১,৪৫০+", Icons.Default.People, MagentaPrimary, Modifier.weight(1f))
                            AdminStatCard("যাচাইকৃত (KYC)", "১,২৯০", Icons.Default.Verified, SuccessGreen, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AdminStatCard("মোট লেনদেন ভলিউম", "৳ ৪.২ কোটি", Icons.Default.MonetizationOn, PurpleSecondary, Modifier.weight(1f))
                            AdminStatCard("সক্রিয় ডিভাইস", "${devices.size}", Icons.Default.Devices, Color(0xFF0284C7), Modifier.weight(1f))
                        }

                        Spacer(Modifier.height(18.dp))

                        // Maintenance Switch Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("সিস্টেম মেইনটেন্যান্স মোড", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("চালু থাকলে সাধারণ ব্যবহারকারীদের জন্য রক্ষণাবেক্ষণ নোটিশ দেখাবে।", fontSize = 11.sp, color = GrayText)
                                    }
                                    Switch(
                                        checked = systemSettings.maintenanceMode,
                                        onCheckedChange = { repository.adminToggleMaintenance(it) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DangerRed)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Quick Recent Pending Transactions
                        Text("জরুরী অনুমোদনের অপেক্ষমাণ লেনদেন", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkText)
                        Spacer(Modifier.height(8.dp))
                        transactions.filter { it.status == TransactionStatus.PENDING || it.status == TransactionStatus.PROCESSING }.forEach { tx ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        selectedTxForAction = tx
                                        showTxStatusDialog = true
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${tx.id} • ${tx.recipientName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("৳${String.format("%,.2f", tx.recipientBdtAmount)} (${tx.sendAmount} ${tx.sendCurrency})", fontSize = 12.sp, color = MagentaPrimary)
                                    }
                                    StatusBadge(status = tx.status)
                                }
                            }
                        }
                    }

                    // TAB 1: TRANSACTION MANAGEMENT
                    1 -> {
                        Text(
                            text = "সকল গ্রাহক লেনদেন পরিচালনা",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "স্ট্যাটাস পরিবর্তন বা অনুমোদন করতে যেকোনো লেনদেনে ট্যাপ করুন:",
                            fontSize = 12.sp,
                            color = GrayText,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        transactions.forEach { tx ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable {
                                        selectedTxForAction = tx
                                        showTxStatusDialog = true
                                    }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = tx.id, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MagentaPrimary)
                                        StatusBadge(status = tx.status)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text("প্রেরক: ${tx.senderName} (${tx.senderPhone})", fontSize = 12.sp, color = DarkText)
                                    Text("প্রাপক: ${tx.recipientName} • ${tx.recipientChannel}", fontSize = 12.sp, color = GrayText)
                                    Text("টাকা: ৳${String.format("%,.2f", tx.recipientBdtAmount)} (${tx.sendAmount} ${tx.sendCurrency})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                    if (!tx.adminNote.isNullOrBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text("অ্যাডমিন নোট: ${tx.adminNote}", fontSize = 11.sp, color = PurpleSecondary)
                                    }
                                }
                            }
                        }
                    }

                    // TAB 2: LIVE RATES MANAGEMENT
                    2 -> {
                        Text(
                            text = "মুদ্রা রেট পরিবর্তন ও সার্ভিস ফি নিয়ন্ত্রণ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("নির্বাচিত মুদ্রার রেট পরিবর্তন করুন", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("SAR", "AED", "MYR", "QAR", "KWD", "USD").forEach { code ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (editingCurrency == code) MagentaPrimary else Color(0xFFF3E8FF))
                                                .clickable {
                                                    editingCurrency = code
                                                    val found = rates.find { it.currencyCode == code }
                                                    if (found != null) {
                                                        newRateValue = found.sendingRate.toString()
                                                        newFeeValue = found.flatFee.toString()
                                                    }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = code,
                                                color = if (editingCurrency == code) Color.White else DarkText,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                AppTextField(
                                    value = newRateValue,
                                    onValueChange = { newRateValue = it },
                                    label = "১ $editingCurrency সমান কত টাকা (BDT)",
                                    placeholder = "উদাঃ ৩২.৪৫",
                                    keyboardType = KeyboardType.Number
                                )

                                Spacer(Modifier.height(10.dp))

                                AppTextField(
                                    value = newFeeValue,
                                    onValueChange = { newFeeValue = it },
                                    label = "সার্ভিস ফি ($editingCurrency)",
                                    placeholder = "উদাঃ ৫.০",
                                    keyboardType = KeyboardType.Number
                                )

                                Spacer(Modifier.height(16.dp))

                                if (rateUpdateSuccess) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFDCFCE7))
                                            .padding(10.dp)
                                    ) {
                                        Text("রেট সফলভাবে আপডেট হয়েছে!", fontSize = 12.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(10.dp))
                                }

                                PrimaryButton(
                                    text = "লাইভ রেট সংরক্ষণ করুন",
                                    onClick = {
                                        val rateDbl = newRateValue.toDoubleOrNull() ?: 32.45
                                        val feeDbl = newFeeValue.toDoubleOrNull() ?: 5.0
                                        coroutineScope.launch {
                                            repository.adminUpdateRate(editingCurrency, rateDbl, feeDbl)
                                            rateUpdateSuccess = true
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // TAB 3: KYC REVIEW
                    3 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "কেওয়াইসি ডকুমেন্ট ও সেলফি অনুমোদন",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MagentaPrimary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "মোট ফাইল: ${kycSubmissions.size}",
                                    fontSize = 11.sp,
                                    color = MagentaPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Status Filter Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("ALL" to "সকল", "PENDING" to "অপেক্ষমাণ", "VERIFIED" to "অনুমোদিত", "REJECTED" to "বাতিল").forEach { (code, label) ->
                                val isSelected = kycFilterStatus == code
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MagentaPrimary else Color.White,
                                    border = borderStroke(if (isSelected) MagentaPrimary else LightGrayBorder),
                                    modifier = Modifier.clickable { kycFilterStatus = code }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else DarkText,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        val filteredKycList = kycSubmissions.filter { submission ->
                            when (kycFilterStatus) {
                                "PENDING" -> submission.status == KycStatus.PENDING
                                "VERIFIED" -> submission.status == KycStatus.VERIFIED
                                "REJECTED" -> submission.status == KycStatus.REJECTED
                                else -> true
                            }
                        }

                        if (filteredKycList.isEmpty()) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("এই ক্যাটাগরিতে কোনো কেওয়াইসি সাবমিশন নেই", fontSize = 13.sp, color = GrayText)
                                }
                            }
                        } else {
                            filteredKycList.forEach { kycRecord ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        // Header
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(kycRecord.userName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkText)
                                                Text("${kycRecord.userPhone} • ${kycRecord.userEmail}", fontSize = 11.sp, color = GrayText)
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = when (kycRecord.status) {
                                                    KycStatus.VERIFIED -> Color(0xFFDCFCE7)
                                                    KycStatus.PENDING -> Color(0xFFFEF3C7)
                                                    KycStatus.REJECTED -> Color(0xFFFEE2E2)
                                                    else -> Color(0xFFF1F5F9)
                                                }
                                            ) {
                                                Text(
                                                    text = kycRecord.status.banglaText,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (kycRecord.status) {
                                                        KycStatus.VERIFIED -> Color(0xFF15803D)
                                                        KycStatus.PENDING -> Color(0xFFB45309)
                                                        KycStatus.REJECTED -> DangerRed
                                                        else -> DarkText
                                                    },
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(8.dp))
                                        HorizontalDivider(color = LightGrayBorder.copy(alpha = 0.6f))
                                        Spacer(Modifier.height(8.dp))

                                        // Document Details
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("ডকুমেন্ট টাইপ:", fontSize = 11.sp, color = GrayText)
                                                Text(kycRecord.documentType.banglaTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MagentaPrimary)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("নম্বর ও মেয়াদ:", fontSize = 11.sp, color = GrayText)
                                                Text("${kycRecord.documentNumber} (Exp: ${kycRecord.expiryDate})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkText)
                                            }
                                        }

                                        Spacer(Modifier.height(10.dp))

                                        // Media Files Grid
                                        Text("সংযুক্ত মিডিয়া ও লাইভনেস ক্যাপচার:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GrayText)
                                        Spacer(Modifier.height(4.dp))

                                        // Front doc
                                        AdminMediaFileRow("সামনের পাতা", kycRecord.frontDocMeta)
                                        // Back doc
                                        kycRecord.backDocMeta?.let { AdminMediaFileRow("পেছনের পাতা", it) }
                                        // Work permit
                                        kycRecord.workPermitDocMeta?.let { AdminMediaFileRow("ওয়ার্ক পারমিট", it) }
                                        // Selfie or Video
                                        AdminMediaFileRow(
                                            if (kycRecord.selfieMediaMeta.isVideo) "লাইভ ভিডিও সেলফি (${kycRecord.selfieMediaMeta.durationSeconds}s)" else "স্থির সেলফি ছবি",
                                            kycRecord.selfieMediaMeta
                                        )

                                        Spacer(Modifier.height(10.dp))

                                        // Device & GPS Info Bar
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFF8FAFC),
                                            border = borderStroke(Color(0xFFE2E8F0)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("ডিভাইস: ${kycRecord.deviceModel}", fontSize = 10.sp, color = DarkText)
                                                    Text("আইপি: ${kycRecord.ipAddress} • জিপিএস: ${kycRecord.gpsCoordinates}", fontSize = 9.sp, color = GrayText)
                                                }
                                                TextButton(
                                                    onClick = {
                                                        selectedKycForReview = kycRecord
                                                        kycAdminActionType = "METADATA_INSPECT"
                                                    }
                                                ) {
                                                    Text("মেটাডেটা", fontSize = 11.sp, color = PurpleSecondary, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        if (kycRecord.adminNotes != null) {
                                            Spacer(Modifier.height(6.dp))
                                            Text("অ্যাডমিন মন্তব্য: ${kycRecord.adminNotes}", fontSize = 11.sp, color = GrayText, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                        }

                                        Spacer(Modifier.height(14.dp))

                                        // Action Buttons
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    selectedKycForReview = kycRecord
                                                    kycAdminActionType = "APPROVE"
                                                    kycAdminNoteInput = "ডকুমেন্ট ও সেলফি নিখুঁতভাবে যাচাইকৃত ও অনুমোদিত।"
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("অনুমোদন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    selectedKycForReview = kycRecord
                                                    kycAdminActionType = "RESUBMIT"
                                                    kycAdminNoteInput = "ডকুমেন্টের উভয় পিঠ ও সেলফি ভিডিও পরিষ্কার আলোতে পুনরায় তুলুন।"
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("রি-সাবমিট", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    selectedKycForReview = kycRecord
                                                    kycAdminActionType = "REJECT"
                                                    kycAdminNoteInput = "প্রদত্ত ছবি অস্পষ্ট বা তথ্যে গরমিল রয়েছে।"
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("বাতিল", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }

                    // TAB 4: AUDIT LOGS
                    4 -> {
                        Text(
                            text = "অডিট লগ ও সিকিউরিটি হিস্ট্রি",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        auditLogs.forEach { log ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(log.action, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PurpleSecondary)
                                        Text(log.timestamp, fontSize = 10.sp, color = GrayText)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text("অপারেটর: ${log.adminName}", fontSize = 11.sp, color = DarkText)
                                    Text("বিবরণ: ${log.notes}", fontSize = 11.sp, color = GrayText)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Transaction Action Dialog
        if (showTxStatusDialog && selectedTxForAction != null) {
            val tx = selectedTxForAction!!
            AlertDialog(
                onDismissRequest = { showTxStatusDialog = false },
                title = { Text("লেনদেন পরিচালনা (${tx.id})", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("প্রাপক: ${tx.recipientName} (${tx.recipientAccount})", fontSize = 13.sp)
                        Text("টাকা: ৳${String.format("%,.2f", tx.recipientBdtAmount)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)

                        AppTextField(
                            value = actionNote,
                            onValueChange = { actionNote = it },
                            label = "অ্যাডমিন নোট",
                            placeholder = "উদাঃ সুইফট কনফার্মেশন সম্পন্ন"
                        )

                        Text("নতুন স্ট্যাটাস নির্ধারণ করুন:", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        repository.adminUpdateTransactionStatus(tx.id, TransactionStatus.COMPLETED, actionNote.ifBlank { "লেনদেন সফলভাবে সম্পন্ন করা হয়েছে।" })
                                        showTxStatusDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("সম্পন্ন", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        repository.adminUpdateTransactionStatus(tx.id, TransactionStatus.PROCESSING, actionNote.ifBlank { "ব্যাংকিং চ্যানেলে প্রক্রিয়াকরণ চলমান।" })
                                        showTxStatusDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("প্রক্রিয়াধীন", fontSize = 11.sp)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        repository.adminUpdateTransactionStatus(tx.id, TransactionStatus.REFUNDED, actionNote.ifBlank { "গ্রাহকের মূল অ্যাকাউন্টে রিফান্ড সম্পন্ন।" })
                                        showTxStatusDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B7280)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("রিফান্ড", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        repository.adminUpdateTransactionStatus(tx.id, TransactionStatus.REJECTED, actionNote.ifBlank { "ভুল অ্যাকাউন্ট নম্বরের কারণে বাতিল।" })
                                        showTxStatusDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("বাতিল", fontSize = 11.sp)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showTxStatusDialog = false }) {
                        Text("বন্ধ করুন", color = GrayText)
                    }
                }
            )
        }

        // KYC Action Dialogs
        if (selectedKycForReview != null && kycAdminActionType != null) {
            val kyc = selectedKycForReview!!
            when (kycAdminActionType) {
                "APPROVE" -> {
                    AlertDialog(
                        onDismissRequest = {
                            selectedKycForReview = null
                            kycAdminActionType = null
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("কেওয়াইসি অনুমোদন নিশ্চিতকরণ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("আবেদনকারী: ${kyc.userName} (${kyc.documentType.banglaTitle})", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("ডকুমেন্ট নম্বর: ${kyc.documentNumber} • সেলফি লাইভনেস: ${kyc.selfieMediaMeta.livenessConfidence}%", fontSize = 12.sp, color = DarkText)

                                AppTextField(
                                    value = kycAdminNoteInput,
                                    onValueChange = { kycAdminNoteInput = it },
                                    label = "অ্যাডমিন অনুমোদন মন্তব্য",
                                    placeholder = "মন্তব্য লিখুন..."
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        repository.adminApproveKycRecord(kyc.id, kycAdminNoteInput)
                                        selectedKycForReview = null
                                        kycAdminActionType = null
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("অনুমোদন সম্পন্ন করুন ✓", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                selectedKycForReview = null
                                kycAdminActionType = null
                            }) {
                                Text("বাতিল", color = GrayText)
                            }
                        }
                    )
                }

                "REJECT" -> {
                    AlertDialog(
                        onDismissRequest = {
                            selectedKycForReview = null
                            kycAdminActionType = null
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Cancel, contentDescription = null, tint = DangerRed, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("কেওয়াইসি আবেদন প্রত্যাখ্যান", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("আবেদনকারী: ${kyc.userName} (${kyc.id})", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("প্রত্যাখ্যানের সুনির্দিষ্ট কারণ উল্লেখ করুন যা গ্রাহকের অ্যাপ নোটিফিকেশনে প্রেরিত হবে:", fontSize = 12.sp, color = GrayText)

                                AppTextField(
                                    value = kycAdminNoteInput,
                                    onValueChange = { kycAdminNoteInput = it },
                                    label = "বাতিলকরণের কারণ",
                                    placeholder = "যেমন: ডকুমেন্টের ছবি ঝাপসা বা আলো কম..."
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        repository.adminRejectKycRecord(kyc.id, kycAdminNoteInput)
                                        selectedKycForReview = null
                                        kycAdminActionType = null
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("প্রত্যাখ্যান নিশ্চিত করুন", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                selectedKycForReview = null
                                kycAdminActionType = null
                            }) {
                                Text("ফিরে যান", color = GrayText)
                            }
                        }
                    )
                }

                "RESUBMIT" -> {
                    AlertDialog(
                        onDismissRequest = {
                            selectedKycForReview = null
                            kycAdminActionType = null
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("পুনরায় জমার অনুরোধ (Resubmit Request)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("আবেদনকারী: ${kyc.userName}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("গ্রাহককে কোন অংশে সংশোধনী এনে পুনরায় ছবি/ভিডিও তুলতে হবে তা জানান:", fontSize = 12.sp, color = GrayText)

                                AppTextField(
                                    value = kycAdminNoteInput,
                                    onValueChange = { kycAdminNoteInput = it },
                                    label = "পুনরায় আপলোডের নির্দেশনা",
                                    placeholder = "যেমন: পাসপোর্টের ভিসা সিল পাতা স্পষ্টভাবে পুনরায় তুলুন"
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        repository.adminRequestKycResubmission(kyc.id, kycAdminNoteInput)
                                        selectedKycForReview = null
                                        kycAdminActionType = null
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("অনুরোধ পাঠান", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                selectedKycForReview = null
                                kycAdminActionType = null
                            }) {
                                Text("বাতিল", color = GrayText)
                            }
                        }
                    )
                }

                "METADATA_INSPECT" -> {
                    AlertDialog(
                        onDismissRequest = {
                            selectedKycForReview = null
                            kycAdminActionType = null
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = PurpleSecondary, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("কেওয়াইসি সিকিউরিটি অডিট মেটাডেটা", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AdminMetaRow("সাবমিশন রেফারেন্স:", kyc.id)
                                AdminMetaRow("ইউজার আইডি:", kyc.userId)
                                AdminMetaRow("ফোন ও ইমেইল:", "${kyc.userPhone} • ${kyc.userEmail}")
                                AdminMetaRow("রেজিস্টার্ড ডিভাইস:", kyc.deviceModel)
                                AdminMetaRow("আইপি অ্যাড্রেস:", kyc.ipAddress)
                                AdminMetaRow("জিপিএস লোকেশন:", kyc.gpsCoordinates)
                                AdminMetaRow("সাবমিশন টাইম:", kyc.submittedAt)
                                AdminMetaRow("ডকুমেন্ট টাইপ:", kyc.documentType.banglaTitle)
                                AdminMetaRow("ডকুমেন্ট নম্বর:", kyc.documentNumber)
                                AdminMetaRow("জন্ম তারিখ:", kyc.dateOfBirth)
                                AdminMetaRow("ইস্যুকৃত দেশ:", kyc.issuingCountry)
                                AdminMetaRow("মেয়াদ:", kyc.expiryDate)
                                AdminMetaRow("সেলফি ভেরিফিকেশন:", if (kyc.selfieMediaMeta.isVideo) "ভিডিও লাইভনেস (${kyc.selfieMediaMeta.durationSeconds}s)" else "স্থির সেলফি")
                                AdminMetaRow("লাইভনেস স্কোর:", "${kyc.selfieMediaMeta.livenessConfidence}% (AI Verified)")
                                AdminMetaRow("এনক্রিপশন সাইফার:", "SHA-256 HMAC / AES-256-GCM")
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    selectedKycForReview = null
                                    kycAdminActionType = null
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("ঠিক আছে")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminMediaFileRow(label: String, meta: KycMediaMeta) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF1F5F9),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (meta.isVideo) Icons.Default.Videocam else Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = MagentaPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DarkText)
                Text("${meta.fileName} (${meta.fileSize}) • ${meta.captureSource}", fontSize = 9.sp, color = GrayText)
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFFDCFCE7),
                modifier = Modifier.padding(1.dp)
            ) {
                Text(
                    text = meta.qualityScore,
                    fontSize = 9.sp,
                    color = Color(0xFF15803D),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
fun AdminMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = GrayText)
        Spacer(Modifier.width(8.dp))
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkText, textAlign = TextAlign.End)
    }
}

private fun borderStroke(color: Color) = androidx.compose.foundation.BorderStroke(1.dp, color)

@Composable
fun AdminStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(text = title, fontSize = 12.sp, color = GrayText)
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = DarkText)
        }
    }
}
