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
import androidx.compose.foundation.text.KeyboardOptions
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemittanceScreen(
    repository: PaisaRepository,
    rates: List<ExchangeRate>,
    beneficiaries: List<Beneficiary>,
    paymentMethods: List<PaymentMethod>,
    onBack: () -> Unit,
    onTransactionSuccess: (RemittanceTransaction) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var currentStep by remember { mutableStateOf(1) } // 1: Country & Currency, 2: Amount & Rate, 3: Beneficiary, 4: Payment Method, 5: Review, 6: Success Receipt

    // Form States
    val appStoredCurrency by repository.selectedCurrency.collectAsState()
    var selectedCurrency by remember { mutableStateOf(appStoredCurrency.ifBlank { "SAR" }) }
    var currencyValidationError by remember { mutableStateOf<String?>(null) }

    var selectedBeneficiary by remember { mutableStateOf<Beneficiary?>(beneficiaries.firstOrNull()) }
    var showAddBeneficiaryModal by remember { mutableStateOf(false) }

    // New Beneficiary Form
    var newBenName by remember { mutableStateOf("") }
    var newBenPhone by remember { mutableStateOf("") }
    var newBenChannel by remember { mutableStateOf("বিকাশ") }
    var newBenBank by remember { mutableStateOf("বিকাশ পার্সোনাল") }
    var newBenAccount by remember { mutableStateOf("") }
    var newBenDistrict by remember { mutableStateOf("ঢাকা") }
    var newBenRelation by remember { mutableStateOf("পরিবার") }

    // Amount & Promo
    var sendAmountText by remember { mutableStateOf("2000") }
    var promoCodeText by remember { mutableStateOf("") }
    var promoApplied by remember { mutableStateOf(false) }

    // Payment Method
    var selectedPaymentMethod by remember { mutableStateOf<PaymentMethod?>(paymentMethods.firstOrNull()) }
    var proofUploaded by remember { mutableStateOf(false) }

    // Security PIN
    var securityPin by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var createdTransaction by remember { mutableStateOf<RemittanceTransaction?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val currentRateObj = rates.find { it.currencyCode == selectedCurrency }
        ?: rates.firstOrNull()
        ?: ExchangeRate("SAR", "সৌদি আরব", "﷼", "🇸🇦", 32.45)
    val sendAmountNum = sendAmountText.toDoubleOrNull() ?: 1000.0

    val quote = remember(sendAmountNum, selectedCurrency, promoApplied, rates) {
        repository.calculateQuote(
            sendAmount = sendAmountNum,
            currencyCode = selectedCurrency.ifBlank { "SAR" },
            currentRates = rates,
            promoCode = if (promoApplied) promoCodeText else null
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentStep) {
                            1 -> "১. দেশ ও মুদ্রা নির্বাচন"
                            2 -> "২. টাকার পরিমাণ ও রেট"
                            3 -> "৩. প্রাপক (বেনিফিশিয়ারি)"
                            4 -> "৪. পেমেন্ট পদ্ধতি"
                            5 -> "৫. পর্যালোচনা ও নিশ্চিতকরণ"
                            else -> "রসিদ ও ট্র্যাকিং"
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (currentStep in 2..5) {
                        IconButton(onClick = { currentStep-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else if (currentStep == 1) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = DarkText
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SoftPinkBackground)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Step Progress Indicator
                if (currentStep < 6) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..5).forEach { step ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(if (step <= currentStep) MagentaPrimary else LightGrayBorder)
                            )
                        }
                    }
                }

                when (currentStep) {
                    // STEP 1: Country & Currency Selection
                    1 -> {
                        Text(
                            text = "কোন দেশ থেকে টাকা পাঠাচ্ছেন?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        rates.forEach { rate ->
                            val isSelected = rate.currencyCode == selectedCurrency
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MagentaPrimary else LightGrayBorder,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        selectedCurrency = rate.currencyCode
                                        currencyValidationError = null
                                        repository.setSelectedCurrency(rate.currencyCode)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFFFF0F7) else Color.White
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = rate.flagEmoji, fontSize = 24.sp)
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "${rate.countryName} (${rate.currencyCode})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = DarkText
                                            )
                                            Text(
                                                text = "১ ${rate.currencyCode} = ৳${String.format("%.2f", rate.sendingRate)}",
                                                fontSize = 12.sp,
                                                color = MagentaPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            selectedCurrency = rate.currencyCode
                                            currencyValidationError = null
                                            repository.setSelectedCurrency(rate.currencyCode)
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = MagentaPrimary)
                                    )
                                }
                            }
                        }

                        if (currencyValidationError != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFDC2626))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = currencyValidationError ?: "",
                                        color = Color(0xFFDC2626),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        PrimaryButton(
                            text = "পরবর্তী ধাপ (টাকার পরিমাণ)",
                            onClick = {
                                if (selectedCurrency.isBlank()) {
                                    currencyValidationError = "দয়া করে একটি মুদ্রা নির্বাচন করুন।"
                                } else {
                                    currencyValidationError = null
                                    repository.setSelectedCurrency(selectedCurrency)
                                    currentStep = 2
                                }
                            },
                            testTag = "remit_step1_next_button"
                        )
                    }

                    // STEP 2: Amount Calculation & Promo Code
                    2 -> {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "পাঠানোর পরিমাণ (${currentRateObj.currencyCode})",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkText
                                    )
                                    Text(
                                        text = "${currentRateObj.flagEmoji} ${currentRateObj.countryName}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = GrayText
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = sendAmountText,
                                    onValueChange = { sendAmountText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    leadingIcon = {
                                        Text(
                                            text = currentRateObj.currencySymbol,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MagentaPrimary,
                                            modifier = Modifier.padding(start = 12.dp)
                                        )
                                    },
                                    singleLine = true
                                )

                                Spacer(Modifier.height(16.dp))

                                // Exchange Rate Info
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF3E8FF))
                                        .padding(10.dp)
                                    ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("বর্তমান রেট:", fontSize = 12.sp, color = PurpleSecondary)
                                        Text("১ ${currentRateObj.currencyCode} = ৳${String.format("%.2f", currentRateObj.sendingRate)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PurpleSecondary)
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                // Promo Code Input
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = promoCodeText,
                                        onValueChange = { promoCodeText = it },
                                        placeholder = { Text("প্রোমোকোড (উদাঃ PAISANEW)", fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = { promoApplied = promoCodeText.isNotBlank() },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PurpleSecondary)
                                    ) {
                                        Text(if (promoApplied) "প্রয়োগকৃত" else "প্রয়োগ", fontSize = 12.sp)
                                    }
                                }

                                Spacer(Modifier.height(18.dp))
                                HorizontalDivider(color = LightGrayBorder)
                                Spacer(Modifier.height(14.dp))

                                // Breakdown
                                CalculationRow("মূল রেমিট্যান্স:", "৳ ${String.format("%,.2f", sendAmountNum * quote.rate)}")
                                CalculationRow("+ ২.৫% সরকারি প্রণোদনা:", "৳ ${String.format("%,.2f", quote.govtIncentiveBdt)}", color = SuccessGreen)
                                CalculationRow("সার্ভিস ফি:", "${quote.serviceFee} ${quote.sendCurrency}")
                                if (quote.discountAmount > 0) {
                                    CalculationRow("প্রোমোকোড ডিসকাউন্ট:", "-${quote.discountAmount} ${quote.sendCurrency}", color = SuccessGreen)
                                }
                                CalculationRow("সর্বমোট প্রদেয়:", "${quote.totalPayable} ${quote.sendCurrency}", isBold = true)

                                Spacer(Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFDCFCE7))
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("প্রাপক মোট পাবেন:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF15803D))
                                        Text("৳ ${String.format("%,.2f", quote.recipientBdtAmount)}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF15803D))
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        PrimaryButton(
                            text = "পরবর্তী ধাপ (প্রাপক নির্বাচন)",
                            onClick = { currentStep = 3 },
                            testTag = "remit_step2_next_button"
                        )

                        Spacer(Modifier.height(20.dp))

                        Text(
                            text = "রেমিট্যান্স লিমিট ও সময়সীমা (FAQ)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DarkText,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        FaqSectionComponent(
                            faqs = repository.faqs.filter { it.category == "লিমিট ও সীমা" || it.category == "প্রসেসিং সময়" || it.category == "প্রণোদনা ও ফি" },
                            showSearch = false
                        )
                    }

                    // STEP 3: Beneficiary Selection
                    3 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "সংরক্ষিত প্রাপকগণ",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )
                            TextButton(onClick = { showAddBeneficiaryModal = true }) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MagentaPrimary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("নতুন প্রাপক", color = MagentaPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        beneficiaries.forEach { ben ->
                            val isSelected = selectedBeneficiary?.id == ben.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MagentaPrimary else LightGrayBorder,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedBeneficiary = ben },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFFFF0F7) else Color.White
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) MagentaPrimary else Color(0xFFF3E8FF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = ben.name.take(1),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = if (isSelected) Color.White else MagentaPrimary
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = ben.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = DarkText
                                            )
                                            Text(
                                                text = "${ben.channelType} • ${ben.accountNumber}",
                                                fontSize = 12.sp,
                                                color = GrayText
                                            )
                                            Text(
                                                text = "সম্পর্ক: ${ben.relationship} | জেলা: ${ben.district}",
                                                fontSize = 11.sp,
                                                color = MagentaPrimary
                                            )
                                        }
                                    }
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedBeneficiary = ben },
                                        colors = RadioButtonDefaults.colors(selectedColor = MagentaPrimary)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        PrimaryButton(
                            text = "পরবর্তী ধাপ (পেমেন্ট পদ্ধতি)",
                            enabled = selectedBeneficiary != null,
                            onClick = { currentStep = 4 },
                            testTag = "remit_step3_next_button"
                        )
                    }

                    // STEP 4: Payment Method Selection
                    4 -> {
                        Text(
                            text = "পেমেন্ট মাধ্যম বেছে নিন",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        paymentMethods.forEach { method ->
                            val isSelected = selectedPaymentMethod?.id == method.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MagentaPrimary else LightGrayBorder,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedPaymentMethod = method },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFFFF0F7) else Color.White
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.AccountBalance,
                                                contentDescription = null,
                                                tint = MagentaPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                text = method.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = DarkText
                                            )
                                        }
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedPaymentMethod = method },
                                            colors = RadioButtonDefaults.colors(selectedColor = MagentaPrimary)
                                        )
                                    }

                                    if (isSelected) {
                                        Spacer(Modifier.height(10.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White)
                                                .padding(10.dp)
                                        ) {
                                            Column {
                                                Text("হিসাব নং / IBAN: ${method.accountNumber}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PurpleSecondary)
                                                Text("হিসাবের নাম: ${method.accountName}", fontSize = 11.sp, color = GrayText)
                                                Spacer(Modifier.height(4.dp))
                                                Text("নির্দেশনা: ${method.instructions}", fontSize = 11.sp, color = DarkText)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        PrimaryButton(
                            text = "পরবর্তী ধাপ (পর্যালোচনা)",
                            enabled = selectedPaymentMethod != null,
                            onClick = { currentStep = 5 },
                            testTag = "remit_step4_next_button"
                        )
                    }

                    // STEP 5: Review & Confirmation
                    5 -> {
                        val context = LocalContext.current
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "লেনদেন বিবরণী নিশ্চিত করুন",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkText
                                )
                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider(color = LightGrayBorder)
                                Spacer(Modifier.height(12.dp))

                                ReviewItem("প্রাপকের নাম", selectedBeneficiary?.name ?: "")
                                ReviewItem("প্রাপকের মাধ্যম", "${selectedBeneficiary?.channelType} (${selectedBeneficiary?.accountNumber})")
                                ReviewItem("প্রেরিত পরিমাণ", "${quote.sendAmount} ${quote.sendCurrency}")
                                ReviewItem("এক্সচেঞ্জ রেট", "১ ${quote.sendCurrency} = ৳${String.format("%.2f", quote.rate)}")
                                ReviewItem("সরকারি প্রণোদনা (+২.৫%)", "৳ ${String.format("%,.2f", quote.govtIncentiveBdt)}")
                                ReviewItem("প্রাপক পাবেন", "৳ ${String.format("%,.2f", quote.recipientBdtAmount)}", isHighlight = true)
                                ReviewItem("পেমেন্ট পদ্ধতি", selectedPaymentMethod?.name ?: "")
                                ReviewItem("আপনার মোট প্রদেয়", "${quote.totalPayable} ${quote.sendCurrency}", isBold = true)

                                Spacer(Modifier.height(16.dp))

                                AppTextField(
                                    value = securityPin,
                                    onValueChange = { if (it.length <= 4) securityPin = it },
                                    label = "৪ সংখ্যার পিন (PIN) অথবা বায়োমেট্রিক নিশ্চিতকরণ",
                                    placeholder = "••••",
                                    leadingIcon = Icons.Default.Lock,
                                    keyboardType = KeyboardType.NumberPassword,
                                    testTag = "remit_pin_input"
                                )

                                Spacer(Modifier.height(10.dp))

                                // Biometric 1-tap Authorization Button
                                OutlinedButton(
                                    onClick = {
                                        val activity = BiometricAuthManager.findFragmentActivity(context)
                                        if (activity != null) {
                                            BiometricAuthManager.promptBiometric(
                                                activity = activity,
                                                title = "রেমিট্যান্স অনুমোদন",
                                                subtitle = "ফিঙ্গারপ্রিন্ট দিয়ে ৳${String.format("%,.2f", quote.recipientBdtAmount)} পাঠানো নিশ্চিত করুন",
                                                onSuccess = {
                                                    isSubmitting = true
                                                    coroutineScope.launch {
                                                        val tx = repository.createTransaction(
                                                            beneficiary = selectedBeneficiary!!,
                                                            quote = quote,
                                                            paymentMethod = selectedPaymentMethod!!
                                                        )
                                                        createdTransaction = tx
                                                        isSubmitting = false
                                                        currentStep = 6
                                                    }
                                                }
                                            )
                                        } else {
                                            // Direct simulation for preview mode
                                            isSubmitting = true
                                            coroutineScope.launch {
                                                val tx = repository.createTransaction(
                                                    beneficiary = selectedBeneficiary!!,
                                                    quote = quote,
                                                    paymentMethod = selectedPaymentMethod!!
                                                )
                                                createdTransaction = tx
                                                isSubmitting = false
                                                currentStep = 6
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MagentaPrimary)
                                ) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("বায়োমেট্রিক / ফিঙ্গারপ্রিন্ট দিয়ে সরাসরি অনুমোদন", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        PrimaryButton(
                            text = "চূড়ান্ত পাঠান ও নিশ্চিত করুন",
                            isLoading = isSubmitting,
                            enabled = securityPin.length >= 4 || securityPin.isEmpty(),
                            onClick = {
                                isSubmitting = true
                                coroutineScope.launch {
                                    val tx = repository.createTransaction(
                                        beneficiary = selectedBeneficiary!!,
                                        quote = quote,
                                        paymentMethod = selectedPaymentMethod!!
                                    )
                                    createdTransaction = tx
                                    isSubmitting = false
                                    currentStep = 6
                                }
                            },
                            testTag = "remit_confirm_button"
                        )
                    }

                    // STEP 6: Success Receipt & Tracking
                    6 -> {
                        createdTransaction?.let { tx ->
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFDCFCE7)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "রেমিট্যান্স আবেদন সফল!",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkText
                                    )
                                    Text(
                                        text = "ট্রানজেকশন আইডি: ${tx.id}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MagentaPrimary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )

                                    Spacer(Modifier.height(16.dp))
                                    HorizontalDivider(color = LightGrayBorder)
                                    Spacer(Modifier.height(16.dp))

                                    ReviewItem("প্রাপক", tx.recipientName)
                                    ReviewItem("হিসাব নং", tx.recipientAccount)
                                    ReviewItem("প্রাপকের টাকা", "৳ ${String.format("%,.2f", tx.recipientBdtAmount)}", isHighlight = true)
                                    ReviewItem("প্রদেয় পরিমাণ", "${tx.sendAmount} ${tx.sendCurrency}")
                                    ReviewItem("স্ট্যাটাস", tx.status.banglaText)
                                    ReviewItem("তারিখ ও সময়", tx.createdAt)

                                    Spacer(Modifier.height(20.dp))

                                    // Tracking timeline
                                    Text(
                                        text = "লাইভ ট্র্যাকিং স্ট্যাটাস",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = DarkText,
                                        modifier = Modifier.align(Alignment.Start)
                                    )
                                    Spacer(Modifier.height(10.dp))

                                    tx.statusTimeline.forEach { event ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                if (event.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = if (event.isCompleted) SuccessGreen else GrayText,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                Text(event.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DarkText)
                                                Text(event.description, fontSize = 11.sp, color = GrayText)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(20.dp))
                            PrimaryButton(
                                text = "হোমে ফিরে যান",
                                onClick = { onTransactionSuccess(tx) },
                                testTag = "remit_done_button"
                            )
                        }
                    }
                }
            }
        }

        // Add Beneficiary Modal Dialog
        if (showAddBeneficiaryModal) {
            AlertDialog(
                onDismissRequest = { showAddBeneficiaryModal = false },
                title = { Text("নতুন প্রাপক যুক্ত করুন", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppTextField(
                            value = newBenName,
                            onValueChange = { newBenName = it },
                            label = "প্রাপকের পূর্ণ নাম",
                            placeholder = "উদাঃ মোঃ আব্দুল করিম"
                        )
                        AppTextField(
                            value = newBenPhone,
                            onValueChange = { newBenPhone = it },
                            label = "মোবাইল নম্বর",
                            placeholder = "017XXXXXXXX",
                            keyboardType = KeyboardType.Phone
                        )
                        AppTextField(
                            value = newBenChannel,
                            onValueChange = { newBenChannel = it },
                            label = "চ্যানেল (বিকাশ / নগদ / রকেট / ব্যাংক)",
                            placeholder = "বিকাশ"
                        )
                        AppTextField(
                            value = newBenAccount,
                            onValueChange = { newBenAccount = it },
                            label = "অ্যাকাউন্ট বা মোবাইল ব্যাংকিং নম্বর",
                            placeholder = "017XXXXXXXX"
                        )
                        AppTextField(
                            value = newBenDistrict,
                            onValueChange = { newBenDistrict = it },
                            label = "জেলা",
                            placeholder = "উদাঃ ঢাকা / সিলেট / কুমিল্লা"
                        )
                        AppTextField(
                            value = newBenRelation,
                            onValueChange = { newBenRelation = it },
                            label = "সম্পর্ক",
                            placeholder = "উদাঃ মা, বাবা, স্ত্রী, ভাই"
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newBenName.isNotBlank() && newBenAccount.isNotBlank()) {
                                coroutineScope.launch {
                                    val newBen = Beneficiary(
                                        id = "BEN-" + (100 + (Math.random() * 900).toInt()),
                                        name = newBenName,
                                        phone = newBenPhone,
                                        channelType = newBenChannel,
                                        accountOrBankName = newBenBank,
                                        accountNumber = newBenAccount,
                                        district = newBenDistrict,
                                        relationship = newBenRelation,
                                        isFavorite = true
                                    )
                                    repository.saveBeneficiary(newBen)
                                    selectedBeneficiary = newBen
                                    showAddBeneficiaryModal = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MagentaPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddBeneficiaryModal = false }) {
                        Text("বাতিল", color = GrayText)
                    }
                }
            )
        }
    }
}

@Composable
fun CalculationRow(
    label: String,
    value: String,
    color: Color = DarkText,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = GrayText)
        Text(text = value, fontSize = 13.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium, color = color)
    }
}

@Composable
fun ReviewItem(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = GrayText)
        Text(
            text = value,
            fontSize = if (isHighlight) 15.sp else 13.sp,
            fontWeight = if (isHighlight || isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlight) SuccessGreen else DarkText
        )
    }
}
