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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

// ---------------------------
// 1. EXCHANGE RATES SCREEN
// ---------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatesScreen(
    rates: List<ExchangeRate>,
    onBack: () -> Unit,
    onNavigateToSendMoney: (String) -> Unit
) {
    var searchCurrency by remember { mutableStateOf("") }
    var calcAmount by remember { mutableStateOf("1000") }
    var selectedCurrencyCode by remember { mutableStateOf("SAR") }

    val filteredRates = rates.filter {
        it.countryName.contains(searchCurrency, ignoreCase = true) ||
                it.currencyCode.contains(searchCurrency, ignoreCase = true)
    }

    val activeRate = rates.find { it.currencyCode == selectedCurrencyCode } ?: rates.firstOrNull() ?: ExchangeRate("SAR", "সৌদি আরব", "﷼", "🇸🇦", 32.45)
    val amountNum = calcAmount.toDoubleOrNull() ?: 1000.0
    val rawBdt = amountNum * activeRate.sendingRate
    val incentiveBdt = rawBdt * 0.025
    val totalRecipientBdt = rawBdt + incentiveBdt

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("লাইভ এক্সচেঞ্জ রেট", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
            // Interactive Calculator Box
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "মুদ্রা রূপান্তর ক্যালকুলেটর",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = DarkText
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = calcAmount,
                            onValueChange = { calcAmount = it },
                            label = { Text("পরিমাণ (${activeRate.currencyCode})") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MagentaPrimary.copy(alpha = 0.1f))
                                .padding(horizontal = 14.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = "${activeRate.flagEmoji} ${activeRate.currencyCode}",
                                fontWeight = FontWeight.Bold,
                                color = MagentaPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFDCFCE7))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("প্রাপক মোট পাবেন:", fontSize = 13.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Medium)
                                Text("৳ ${String.format("%,.2f", totalRecipientBdt)}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF15803D))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "• বর্তমান রেট: ১ ${activeRate.currencyCode} = ৳${String.format("%.2f", activeRate.sendingRate)}\n• সরকারি ২.৫% প্রণোদনা অন্তর্ভুক্ত (৳${String.format("%.2f", incentiveBdt)})",
                                fontSize = 11.sp,
                                color = Color(0xFF166534),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    PrimaryButton(
                        text = "এই রেটে টাকা পাঠান",
                        onClick = { onNavigateToSendMoney(selectedCurrencyCode) },
                        icon = Icons.Default.Send
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Currency Search Bar
            AppTextField(
                value = searchCurrency,
                onValueChange = { searchCurrency = it },
                label = "সকল দেশের রেট তালিকা",
                placeholder = "দেশ বা মুদ্রার নাম লিখুন (উদাঃ সৌদি, AED)...",
                leadingIcon = Icons.Default.Search
            )

            Spacer(Modifier.height(12.dp))

            filteredRates.forEach { rate ->
                RateCard(
                    rate = rate,
                    onClick = {
                        selectedCurrencyCode = rate.currencyCode
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ---------------------------
// 2. TRANSACTION HISTORY SCREEN
// ---------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    transactions: List<RemittanceTransaction>,
    onBack: () -> Unit,
    onSelectTransaction: (RemittanceTransaction) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    val filterOptions = listOf(
        "ALL" to "সকল",
        "PENDING" to "অপেক্ষমাণ",
        "PROCESSING" to "প্রক্রিয়াধীন",
        "COMPLETED" to "সম্পন্ন",
        "REJECTED" to "বাতিল"
    )

    val filteredTransactions = transactions.filter { tx ->
        val matchesFilter = when (selectedFilter) {
            "ALL" -> true
            "PENDING" -> tx.status == TransactionStatus.PENDING
            "PROCESSING" -> tx.status == TransactionStatus.PROCESSING
            "COMPLETED" -> tx.status == TransactionStatus.COMPLETED
            "REJECTED" -> tx.status == TransactionStatus.REJECTED
            else -> true
        }
        val matchesSearch = tx.recipientName.contains(searchQuery, ignoreCase = true) ||
                tx.id.contains(searchQuery, ignoreCase = true) ||
                tx.recipientAccount.contains(searchQuery)
        matchesFilter && matchesSearch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("লেনদেনের ইতিহাস ও চার্ট", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SoftPinkBackground)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
            // 1. Interactive Monthly Remittance Flow & Incentive Trend Chart
            item {
                RemittanceAnalyticsChartCard(
                    transactions = transactions
                )
            }

            // 2. Search Input & Header
            item {
                Column {
                    Text(
                        text = "সকল লেনদেন ফিল্টার করুন",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("আইডি বা প্রাপকের নাম খুঁজুন...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MagentaPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MagentaPrimary,
                            unfocusedBorderColor = LightGrayBorder,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )
                }
            }

            // 3. Status Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    items(filterOptions) { (key, label) ->
                        val isSelected = selectedFilter == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) MagentaPrimary else Color.White)
                                .border(1.dp, if (isSelected) MagentaPrimary else LightGrayBorder, RoundedCornerShape(20.dp))
                                .clickable { selectedFilter = key }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else DarkText,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // 4. Transaction List Items or Empty State
            if (filteredTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = GrayText.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text("কোনো লেনদেন পাওয়া যায়নি", fontSize = 14.sp, color = GrayText, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            } else {
                items(filteredTransactions) { tx ->
                    TransactionCard(
                        transaction = tx,
                        onClick = { onSelectTransaction(tx) }
                    )
                }
            }
        }
    }
}

// ---------------------------
// 3. OFFERS SCREEN
// ---------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersScreen(
    offers: List<Offer>,
    onBack: () -> Unit,
    onApplyOffer: (Offer) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("স্পেশাল অফার ও প্রমোশন", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SoftPinkBackground)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(offers) { offer ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(MagentaPrimary, PurpleSecondary)
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = offer.badgeText,
                                    color = AmberAccent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.25f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = offer.promoCode,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = offer.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = DarkText
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = offer.description,
                                fontSize = 13.sp,
                                color = GrayText,
                                lineHeight = 19.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "মেয়াদ: ${offer.validUntil}",
                                    fontSize = 11.sp,
                                    color = GrayText
                                )
                                Button(
                                    onClick = { onApplyOffer(offer) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MagentaPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("রেমিট্যান্সে প্রয়োগ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------
// 4. NOTICES SCREEN
// ---------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticesScreen(
    notices: List<Notice>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("বিজ্ঞপ্তি ও নোটিশ", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SoftPinkBackground)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(notices) { notice ->
                NoticeCard(notice = notice)
            }
        }
    }
}

// ---------------------------
// 5. SUPPORT & LIVE CHAT SCREEN
// ---------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    repository: PaisaRepository,
    systemSettings: SystemSettings,
    onBack: () -> Unit
) {
    val user by repository.currentUser.collectAsState()
    val transactions by repository.getTransactions().collectAsState(initial = emptyList())
    val tickets by repository.supportTickets.collectAsState()
    val liveChatMessages by repository.liveChatMessages.collectAsState()
    val isAgentTyping by repository.isAgentTyping.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Live Chat, 1: Support Form, 2: Tickets, 3: Contact & FAQ
    val tabTitles = listOf("লাইভ চ্যাট", "সহায়তা আবেদন", "টিকিট সমূহ (${tickets.size})", "যোগাযোগ ও FAQ")

    // State for viewing/replying to an existing ticket
    var activeTicketDetail by remember { mutableStateOf<SupportTicket?>(null) }
    var ticketReplyText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "কাস্টমার সাপোর্ট ও লাইভ হেল্প",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = DarkText
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "অনলাইন সাপোর্ট ডেস্ক • আইডি: ${user.id}",
                                fontSize = 11.sp,
                                color = GrayText
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("support_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DarkText)
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
            // Linked Account Context Banner (Visible across Support Hub)
            LinkedAccountContextCard(user = user)

            // Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = MagentaPrimary,
                edgePadding = 12.dp,
                divider = { HorizontalDivider(color = LightGrayBorder.copy(alpha = 0.5f)) }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (selectedTab == index) MagentaPrimary else GrayText
                            )
                        },
                        modifier = Modifier.testTag("support_tab_$index")
                    )
                }
            }

            // Tab Content Body
            when (selectedTab) {
                0 -> LiveChatSection(
                    user = user,
                    messages = liveChatMessages,
                    isAgentTyping = isAgentTyping,
                    transactions = transactions,
                    onSendMessage = { text, txRef ->
                        repository.sendLiveChatMessage(text, txRef)
                    },
                    onSwitchToForm = { selectedTab = 1 }
                )
                1 -> SupportRequestFormSection(
                    user = user,
                    transactions = transactions,
                    onSubmitRequest = { subject, message, category, priority, txRef, attachment ->
                        repository.submitSupportRequest(
                            subject = subject,
                            message = message,
                            category = category,
                            priority = priority,
                            txRef = txRef,
                            attachmentName = attachment
                        )
                        selectedTab = 2 // Switch to tickets view
                    }
                )
                2 -> SupportTicketsSection(
                    tickets = tickets,
                    onSelectTicket = { activeTicketDetail = it },
                    onOpenNewForm = { selectedTab = 1 }
                )
                3 -> ContactAndFaqSection(
                    systemSettings = systemSettings,
                    repository = repository
                )
            }
        }

        // Active Ticket Detail & Thread Modal
        if (activeTicketDetail != null) {
            val currentTicket = tickets.find { it.id == activeTicketDetail?.id } ?: activeTicketDetail!!
            AlertDialog(
                onDismissRequest = {
                    activeTicketDetail = null
                    ticketReplyText = ""
                },
                title = {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "টিকিট #${currentTicket.id}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MagentaPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when (currentTicket.status) {
                                            "উত্তর দেওয়া হয়েছে" -> Color(0xFFDCFCE7)
                                            "প্রক্রিয়াধীন" -> Color(0xFFEFF6FF)
                                            else -> Color(0xFFFEF3C7)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = currentTicket.status,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (currentTicket.status) {
                                        "উত্তর দেওয়া হয়েছে" -> Color(0xFF15803D)
                                        "প্রক্রিয়াধীন" -> Color(0xFF1D4ED8)
                                        else -> Color(0xFFB45309)
                                    }
                                )
                            }
                        }
                        Text(
                            text = currentTicket.subject,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (currentTicket.transactionRef != null) {
                            Text(
                                text = "সংযুক্ত ট্রানজেকশন: #${currentTicket.transactionRef}",
                                fontSize = 11.sp,
                                color = PurpleSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        currentTicket.messages.forEach { msg ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (msg.isAgent) Color(0xFFF3E8FF) else Color(0xFFFFF0F7))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = msg.senderName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (msg.isAgent) PurpleSecondary else MagentaPrimary
                                        )
                                        Text(
                                            text = msg.timestamp,
                                            fontSize = 10.sp,
                                            color = GrayText
                                        )
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = msg.message,
                                        fontSize = 12.sp,
                                        color = DarkText,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = ticketReplyText,
                            onValueChange = { ticketReplyText = it },
                            placeholder = { Text("আপনার উত্তর লিখুন...", fontSize = 12.sp, color = GrayText) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ticket_reply_input"),
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (ticketReplyText.isNotBlank()) {
                                repository.sendTicketReply(currentTicket.id, ticketReplyText)
                                ticketReplyText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MagentaPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("send_ticket_reply_button")
                    ) {
                        Text("উত্তর পাঠান", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeTicketDetail = null }) {
                        Text("বন্ধ করুন", color = GrayText, fontSize = 12.sp)
                    }
                }
            )
        }
    }
}

// ------------------------------------------------------------------
// LINKED ACCOUNT CONTEXT CARD (Displays User Information Context)
// ------------------------------------------------------------------
@Composable
fun LinkedAccountContextCard(user: User) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("support_linked_account_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, LightGrayBorder.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MagentaPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MagentaPrimary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = DarkText
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFDCFCE7))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "KYC ${user.kycStatus.banglaText}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "আইডি: ${user.id} • ফোন: ${user.phone} • কান্ট্রি: ${user.country.take(10)}",
                    fontSize = 10.sp,
                    color = GrayText
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF3E8FF))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = PurpleSecondary, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("লিঙ্কযুক্ত", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PurpleSecondary)
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// TAB 1: INTERACTIVE LIVE CHAT
// ------------------------------------------------------------------
@Composable
fun LiveChatSection(
    user: User,
    messages: List<TicketMessage>,
    isAgentTyping: Boolean,
    transactions: List<RemittanceTransaction>,
    onSendMessage: (text: String, txRef: String?) -> Unit,
    onSwitchToForm: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var selectedTxRef by remember { mutableStateOf<String?>(null) }
    var showTxPicker by remember { mutableStateOf(false) }

    val quickPrompts = listOf(
        "⚡ টাকা পৌঁছাতে দেরি হচ্ছে কেন?",
        "🆔 কেওয়াইসি ভেরিফিকেশন সহায়তা",
        "💳 বিকাশ ক্যাশ-ইন সমস্যা",
        "🎁 ২.৫% সরকারি প্রণোদনা তথ্য"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("live_chat_container")
    ) {
        // Live Agent Status Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF0FDF4))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "সাপোর্ট এজেন্ট লাইভ • গড় উত্তর সময় < ১ মিনিট",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF166534)
                )
            }
            Text(
                text = "ফর্ম পূরণ করুন",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MagentaPrimary,
                modifier = Modifier
                    .clickable { onSwitchToForm() }
                    .testTag("switch_to_support_form_button")
            )
        }

        // Selected Transaction Link Pill (if attached)
        if (selectedTxRef != null) {
            Surface(
                color = PurpleSecondary.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = PurpleSecondary, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "সংযুক্ত ট্রানজেকশন: #$selectedTxRef",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurpleSecondary
                        )
                    }
                    IconButton(
                        onClick = { selectedTxRef = null },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = GrayText, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            reverseLayout = false
        ) {
            items(messages) { msg ->
                ChatBubble(msg = msg)
                Spacer(Modifier.height(6.dp))
            }

            if (isAgentTyping) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(PurpleSecondary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = null, tint = PurpleSecondary, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = PurpleSecondary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "সাপোর্ট প্রতিনিধি উত্তর লিখছেন...",
                                    fontSize = 11.sp,
                                    color = GrayText,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Suggestions Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(quickPrompts) { prompt ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF3F4F6))
                        .clickable { onSendMessage(prompt.substringAfter(" "), selectedTxRef) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("quick_prompt_${prompt.take(6)}")
                ) {
                    Text(text = prompt, fontSize = 11.sp, color = DarkText, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Bottom Chat Input Bar
        Surface(
            color = Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Link Transaction button
                IconButton(
                    onClick = { showTxPicker = true },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("live_chat_attach_tx_button")
                ) {
                    Icon(
                        Icons.Default.AttachMoney,
                        contentDescription = "Attach Transaction",
                        tint = if (selectedTxRef != null) MagentaPrimary else GrayText
                    )
                }

                // Message Text Field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("আপনার বার্তা লিখুন...", fontSize = 13.sp, color = GrayText) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .testTag("live_chat_input_field"),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF9FAFB),
                        unfocusedContainerColor = Color(0xFFF9FAFB),
                        focusedBorderColor = MagentaPrimary,
                        unfocusedBorderColor = LightGrayBorder
                    ),
                    maxLines = 3
                )

                // Send Button
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText.trim(), selectedTxRef)
                            inputText = ""
                            selectedTxRef = null
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MagentaPrimary)
                        .testTag("live_chat_send_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    // Transaction Picker Dialog
    if (showTxPicker) {
        AlertDialog(
            onDismissRequest = { showTxPicker = false },
            title = { Text("লেনদেন নির্বাচন করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "যে লেনদেন সম্পর্কে সহায়তা চান তা সিলেক্ট করুন:",
                        fontSize = 12.sp,
                        color = GrayText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                        items(transactions) { tx ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        selectedTxRef = tx.id
                                        showTxPicker = false
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedTxRef == tx.id) Color(0xFFFFF0F7) else Color(0xFFF9FAFB)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "#${tx.id}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MagentaPrimary)
                                        Text(text = "${tx.recipientName} (${tx.recipientChannel})", fontSize = 11.sp, color = DarkText)
                                    }
                                    Text(
                                        text = "৳${String.format(java.util.Locale.US, "%,.0f", tx.recipientBdtAmount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = DarkText
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTxPicker = false }) {
                    Text("বন্ধ করুন", color = GrayText)
                }
            }
        )
    }
}

// ------------------------------------------------------------------
// CHAT BUBBLE COMPONENT
// ------------------------------------------------------------------
@Composable
fun ChatBubble(msg: TicketMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isAgent) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        if (msg.isAgent) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(PurpleSecondary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SupportAgent, contentDescription = null, tint = PurpleSecondary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (msg.isAgent) Alignment.Start else Alignment.End,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Card(
                shape = RoundedCornerShape(
                    topStart = 14.dp,
                    topEnd = 14.dp,
                    bottomStart = if (msg.isAgent) 2.dp else 14.dp,
                    bottomEnd = if (msg.isAgent) 14.dp else 2.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (msg.isAgent) Color.White else MagentaPrimary
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    if (msg.linkedTxId != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (msg.isAgent) Color(0xFFF3E8FF) else Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ট্রানজেকশন #${msg.linkedTxId}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (msg.isAgent) PurpleSecondary else Color.White
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(
                        text = msg.message,
                        fontSize = 13.sp,
                        color = if (msg.isAgent) DarkText else Color.White,
                        lineHeight = 18.sp
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${msg.senderName} • ${msg.timestamp}",
                fontSize = 9.sp,
                color = GrayText,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

// ------------------------------------------------------------------
// TAB 2: FORMAL SUPPORT REQUEST FORM (Links account details)
// ------------------------------------------------------------------
@Composable
fun SupportRequestFormSection(
    user: User,
    transactions: List<RemittanceTransaction>,
    onSubmitRequest: (
        subject: String,
        message: String,
        category: String,
        priority: String,
        txRef: String?,
        attachment: String?
    ) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("রেমিট্যান্স লেনদেন বিলম্ব") }
    var selectedPriority by remember { mutableStateOf("সাধারণ") }
    var selectedTxRef by remember { mutableStateOf<String?>(null) }
    var attachedFileName by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val categories = listOf(
        "রেমিট্যান্স লেনদেন বিলম্ব",
        "কেওয়াইসি ডকুমেন্ট যাচাই",
        "পেমেন্ট ও ব্যাংক ডিপোজিট",
        "এক্সচেঞ্জ রেট ও ২.৫% প্রণোদনা",
        "অ্যাকাউন্ট ও নিরাপত্তা",
        "অন্যান্য সমস্যা"
    )

    val priorities = listOf("সাধারণ", "জরুরি", "অতি জরুরি")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
            .testTag("support_request_form")
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "নতুন সহায়তা অনুরোধ জমা দিন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DarkText
                )
                Text(
                    text = "আপনার অ্যাকাউন্ট বিবরণী ও ডিভাইস তথ্য স্বয়ংক্রিয়ভাবে এই টিকিটের সাথে সংযুক্ত থাকবে।",
                    fontSize = 11.sp,
                    color = GrayText,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                // Category Selection
                Text(text = "সমস্যার বিভাগ (Category)*", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkText)
                Spacer(Modifier.height(6.dp))
                var categoryExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { categoryExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("support_form_category_dropdown"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF9FAFB))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = selectedCategory, fontSize = 13.sp, color = DarkText)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = GrayText)
                        }
                    }
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, fontSize = 13.sp) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Linked Transaction (Optional)
                Text(text = "সংযুক্ত রেমিট্যান্স ট্রানজেকশন (ঐচ্ছিক)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkText)
                Spacer(Modifier.height(6.dp))
                var txExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { txExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("support_form_tx_dropdown"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF9FAFB))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val selectedTxObj = transactions.find { it.id == selectedTxRef }
                            Text(
                                text = if (selectedTxObj != null) "#${selectedTxObj.id} • ৳${String.format(java.util.Locale.US, "%,.0f", selectedTxObj.recipientBdtAmount)} (${selectedTxObj.recipientName})" else "কোনো লেনদেন নির্বাচন করা হয়নি",
                                fontSize = 12.sp,
                                color = if (selectedTxRef != null) MagentaPrimary else GrayText,
                                maxLines = 1
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = GrayText)
                        }
                    }
                    DropdownMenu(
                        expanded = txExpanded,
                        onDismissRequest = { txExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("কোনো লেনদেন যুক্ত করব না", fontSize = 12.sp, color = GrayText) },
                            onClick = {
                                selectedTxRef = null
                                txExpanded = false
                            }
                        )
                        transactions.forEach { tx ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("#${tx.id} • ৳${String.format(java.util.Locale.US, "%,.0f", tx.recipientBdtAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("${tx.recipientName} (${tx.recipientChannel})", fontSize = 11.sp, color = GrayText)
                                    }
                                },
                                onClick = {
                                    selectedTxRef = tx.id
                                    txExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Priority Selection
                Text(text = "অগ্রাধিকার (Priority)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkText)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    priorities.forEach { p ->
                        val isSelected = selectedPriority == p
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) {
                                        when (p) {
                                            "অতি জরুরি" -> Color(0xFFFEE2E2)
                                            "জরুরি" -> Color(0xFFFEF3C7)
                                            else -> Color(0xFFEFF6FF)
                                        }
                                    } else Color(0xFFF9FAFB)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) {
                                        when (p) {
                                            "অতি জরুরি" -> Color(0xFFDC2626)
                                            "জরুরি" -> Color(0xFFD97706)
                                            else -> Color(0xFF2563EB)
                                        }
                                    } else LightGrayBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedPriority = p }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = p,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) {
                                    when (p) {
                                        "অতি জরুরি" -> Color(0xFFDC2626)
                                        "জরুরি" -> Color(0xFFD97706)
                                        else -> Color(0xFF2563EB)
                                    }
                                } else DarkText
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Subject
                AppTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = "বিষয় (Subject)*",
                    placeholder = "উদাঃ বিকাশ একাউন্টে টাকা পৌঁছায়নি",
                    testTag = "support_form_subject"
                )

                Spacer(Modifier.height(12.dp))

                // Message Body
                Text(text = "সমস্যার বিস্তারিত বিবরণ*", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkText)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    placeholder = { Text("আপনার সমস্যাটি বিস্তারিত লিখুন যাতে সাপোর্ট টিম দ্রুত সহায়তা করতে পারে...", fontSize = 12.sp, color = GrayText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("support_form_message"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF9FAFB),
                        unfocusedContainerColor = Color(0xFFF9FAFB),
                        focusedBorderColor = MagentaPrimary,
                        unfocusedBorderColor = LightGrayBorder
                    )
                )

                Spacer(Modifier.height(12.dp))

                // Simulated Attachment Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF3F4F6))
                        .clickable { attachedFileName = "Bank_Payment_Receipt_${System.currentTimeMillis() % 1000}.jpg" }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = MagentaPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = attachedFileName ?: "পেমেন্ট স্লিপ / স্ক্রিনশট যুক্ত করুন (ঐচ্ছিক)",
                            fontSize = 12.sp,
                            fontWeight = if (attachedFileName != null) FontWeight.Bold else FontWeight.Normal,
                            color = if (attachedFileName != null) DarkText else GrayText
                        )
                    }
                    if (attachedFileName != null) {
                        IconButton(onClick = { attachedFileName = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = GrayText, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Submit Button
                PrimaryButton(
                    text = "সহায়তা অনুরোধ পাঠান",
                    onClick = {
                        if (subject.isNotBlank() && message.isNotBlank()) {
                            onSubmitRequest(
                                subject,
                                message,
                                selectedCategory,
                                selectedPriority,
                                selectedTxRef,
                                attachedFileName
                            )
                            showSuccessDialog = true
                        }
                    },
                    enabled = subject.isNotBlank() && message.isNotBlank(),
                    testTag = "submit_support_request_button"
                )
            }
        }
    }
}

// ------------------------------------------------------------------
// TAB 3: SUPPORT TICKETS & HISTORY
// ------------------------------------------------------------------
@Composable
fun SupportTicketsSection(
    tickets: List<SupportTicket>,
    onSelectTicket: (SupportTicket) -> Unit,
    onOpenNewForm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
            .testTag("support_tickets_section")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "আপনার সাপোর্ট টিকিটসমূহ (${tickets.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = DarkText
            )
            TextButton(
                onClick = onOpenNewForm,
                modifier = Modifier.testTag("tickets_new_ticket_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MagentaPrimary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("নতুন রিকোয়েস্ট", color = MagentaPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        if (tickets.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = GrayText.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(text = "কোনো সাপোর্ট টিকিট নেই", fontSize = 13.sp, color = GrayText)
                }
            }
        } else {
            tickets.forEach { ticket ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelectTicket(ticket) }
                        .testTag("support_ticket_card_${ticket.id}"),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "টিকিট #${ticket.id}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MagentaPrimary
                                )
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFF3F4F6))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = ticket.category, fontSize = 9.sp, color = GrayText, fontWeight = FontWeight.Bold)
                                    }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when (ticket.status) {
                                            "উত্তর দেওয়া হয়েছে" -> Color(0xFFDCFCE7)
                                            "প্রক্রিয়াধীন" -> Color(0xFFEFF6FF)
                                            else -> Color(0xFFFEF3C7)
                                        }
                                    )
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = ticket.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (ticket.status) {
                                        "উত্তর দেওয়া হয়েছে" -> Color(0xFF15803D)
                                        "প্রক্রিয়াধীন" -> Color(0xFF1D4ED8)
                                        else -> Color(0xFFB45309)
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = ticket.subject,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = DarkText
                        )

                        if (ticket.transactionRef != null) {
                            Text(
                                text = "সংযুক্ত ট্রানজেকশন: #${ticket.transactionRef}",
                                fontSize = 11.sp,
                                color = PurpleSecondary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        HorizontalDivider(
                            color = LightGrayBorder.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        val lastMsg = ticket.messages.lastOrNull()
                        if (lastMsg != null) {
                            Text(
                                text = "${if (lastMsg.isAgent) "সাপোর্ট এজেন্ট: " else "আপনি: "}${lastMsg.message}",
                                fontSize = 11.sp,
                                color = if (lastMsg.isAgent) PurpleSecondary else GrayText,
                                maxLines = 2
                            )
                        }

                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = ticket.createdAt, fontSize = 10.sp, color = GrayText)
                            Text(text = "বিস্তারিত দেখুন ও উত্তর দিন →", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MagentaPrimary)
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// TAB 4: CONTACT & FAQ
// ------------------------------------------------------------------
@Composable
fun ContactAndFaqSection(
    systemSettings: SystemSettings,
    repository: PaisaRepository
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        // Direct Contact Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "সরাসরি সাপোর্ট চ্যানেল",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = DarkText
                )
                Text(
                    text = "অফিস সময়: ${systemSettings.officeHours}",
                    fontSize = 12.sp,
                    color = GrayText,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                SupportContactRow(
                    title = "হোয়াটসঅ্যাপ সহায়তা",
                    value = systemSettings.whatsappNumber,
                    icon = Icons.Default.Chat,
                    iconColor = Color(0xFF25D366)
                )
                HorizontalDivider(color = LightGrayBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                SupportContactRow(
                    title = "হটলাইন হেল্পলাইন",
                    value = systemSettings.helplinePhone,
                    icon = Icons.Default.Phone,
                    iconColor = MagentaPrimary
                )
                HorizontalDivider(color = LightGrayBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                SupportContactRow(
                    title = "ইমেইল সাপোর্ট",
                    value = systemSettings.supportEmail,
                    icon = Icons.Default.Email,
                    iconColor = Color(0xFFEA4335)
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // Frequently Asked Questions
        Text(
            text = "সাধারণ জিজ্ঞাসাসমূহ (FAQ)",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = DarkText,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FaqSectionComponent(
            faqs = repository.faqs,
            showSearch = true
        )
    }
}

@Composable
fun SupportContactRow(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = title, fontSize = 12.sp, color = GrayText)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkText)
        }
    }
}
