package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.models.*
import com.example.data.repository.PaisaRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CameraTargetMode {
    DOC_FRONT,
    DOC_BACK,
    DOC_WORK_PERMIT,
    SELFIE_PHOTO,
    SELFIE_VIDEO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycDocumentUploadScreen(
    user: User,
    repository: PaisaRepository,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val kycSubmissions by repository.kycSubmissions.collectAsState()

    // Find if user has a pending or recent submission
    val userRecentSubmission = kycSubmissions.firstOrNull { it.userId == user.id }

    // Form States
    var currentStep by remember { mutableStateOf(1) } // 1: Doc Selection & Info, 2: Document Photos, 3: Selfie/Video, 4: Metadata Review
    var selectedDocType by remember { mutableStateOf(KycDocType.PASSPORT) }
    var docNumber by remember { mutableStateOf("A08492014") }
    var fullNameOnDoc by remember { mutableStateOf(user.name) }
    var dateOfBirth by remember { mutableStateOf("15/08/1990") }
    var issuingCountry by remember { mutableStateOf("বাংলাদেশ (Bangladesh)") }
    var expiryDate by remember { mutableStateOf("2032-11-20") }

    // Uploaded Media Slots
    var frontDocMeta by remember {
        mutableStateOf<KycMediaMeta?>(
            KycMediaMeta(
                fileName = "passport_bio_page_cam.jpg",
                fileSize = "3.2 MB",
                captureSource = "ক্যামেরা (Camera Live Capture)",
                timestamp = "আজ, দুপুর ১২:১০",
                mimeType = "image/jpeg",
                qualityScore = "উচ্চ (High - 99.4%)",
                previewLabel = "পাসপোর্ট বায়োডাটা পাতা"
            )
        )
    }

    var backDocMeta by remember {
        mutableStateOf<KycMediaMeta?>(
            KycMediaMeta(
                fileName = "passport_visa_stamp_cam.jpg",
                fileSize = "2.7 MB",
                captureSource = "ক্যামেরা (Camera Live Capture)",
                timestamp = "আজ, দুপুর ১২:১১",
                mimeType = "image/jpeg",
                qualityScore = "উচ্চ (High - 98.6%)",
                previewLabel = "ভিসা / এন্ট্রি সিল পাতা"
            )
        )
    }

    var workPermitMeta by remember { mutableStateOf<KycMediaMeta?>(null) }

    // Selfie Mode & Meta
    var selfieType by remember { mutableStateOf(KycSelfieType.VIDEO_SELFIE) }
    var selfieMediaMeta by remember {
        mutableStateOf<KycMediaMeta?>(
            KycMediaMeta(
                fileName = "liveness_selfie_video_20260823.mp4",
                fileSize = "7.4 MB",
                captureSource = "ক্যামেরা ভিডিও লাইভনেস (Camera Liveness Check)",
                timestamp = "আজ, দুপুর ১২:১২",
                mimeType = "video/mp4",
                qualityScore = "উচ্চ (High - 99.8%)",
                isVideo = true,
                durationSeconds = 4.0,
                livenessConfidence = 99.6,
                previewLabel = "৪.০ সেকেন্ড লাইভ ভিডিও সেলফি (চোখ পলক ও হাসি সনাক্ত)"
            )
        )
    }

    var isSubmitting by remember { mutableStateOf(false) }
    var submissionSuccess by remember { mutableStateOf(false) }
    var activeSubmissionId by remember { mutableStateOf("") }
    var consentAgreed by remember { mutableStateOf(true) }

    // Dialog States
    var showCameraDialog by remember { mutableStateOf(false) }
    var activeCameraTarget by remember { mutableStateOf(CameraTargetMode.DOC_FRONT) }
    var showSourcePickerForTarget by remember { mutableStateOf<CameraTargetMode?>(null) }

    val countries = listOf(
        "বাংলাদেশ (Bangladesh)",
        "সৌদি আরব (Saudi Arabia)",
        "মালয়েশিয়া (Malaysia)",
        "সংযুক্ত আরব আমিরাত (UAE)",
        "কাতার (Qatar)",
        "কুয়েত (Kuwait)",
        "ওমান (Oman)",
        "সিঙ্গাপুর (Singapore)",
        "যুক্তরাজ্য (UK)",
        "যুক্তরাষ্ট্র (USA)",
        "ইতালি (Italy)"
    )
    var showCountryDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("কেওয়াইসি ডকুমেন্ট ভেরিফিকেশন", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("ক্যামেরা ক্যাপচার ও সেলফি লাইভনেস", fontSize = 11.sp, color = GrayText)
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
            // Step Progress Indicator
            KycStepIndicator(currentStep = currentStep, totalSteps = 4)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status banner if pending or rejected
                if (user.kycStatus == KycStatus.PENDING || userRecentSubmission != null && userRecentSubmission.status == KycStatus.PENDING) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        border = borderStroke(Color(0xFFF59E0B).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HourglassTop, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("কেওয়াইসি যাচাই প্রক্রিয়াধীন (Pending Approval)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF92400E))
                                Text("আপনার ডকুমেন্ট ও সেলফি মেটাডেটা অ্যাডমিন অনুমোদনের জন্য অপেক্ষমাণ আছে। আইডি: ${userRecentSubmission?.id ?: "KYC-2026-94021"}", fontSize = 11.sp, color = Color(0xFFB45309))
                            }
                        }
                    }
                }

                when (currentStep) {
                    // STEP 1: DOCUMENT SELECTION & BASIC DATA
                    1 -> {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "ধাপ ১: পরিচয়পত্রের ধরন নির্বাচন করুন",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkText
                                )
                                Text(
                                    text = "বাংলাদেশ ব্যাংক ও বৈশ্বিক এএমএল (AML) কমপ্লায়েন্স নিশ্চিত করতে সঠিক ডকুমেন্ট নির্বাচন করুন",
                                    fontSize = 12.sp,
                                    color = GrayText
                                )

                                Spacer(Modifier.height(16.dp))

                                KycDocType.values().forEach { docTypeOption ->
                                    val isSelected = selectedDocType == docTypeOption
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MagentaPrimary.copy(alpha = 0.08f) else Color(0xFFFAFAFA),
                                        border = borderStroke(if (isSelected) MagentaPrimary else LightGrayBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { selectedDocType = docTypeOption }
                                            .testTag("doc_type_${docTypeOption.shortCode}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { selectedDocType = docTypeOption },
                                                colors = RadioButtonDefaults.colors(selectedColor = MagentaPrimary)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = docTypeOption.banglaTitle,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (isSelected) MagentaPrimary else DarkText
                                                )
                                                Text(
                                                    text = docTypeOption.description,
                                                    fontSize = 11.sp,
                                                    color = GrayText
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))
                                HorizontalDivider(color = LightGrayBorder)
                                Spacer(Modifier.height(16.dp))

                                Text(
                                    text = "ডকুমেন্টের তথ্য প্রদান করুন",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = DarkText
                                )

                                Spacer(Modifier.height(12.dp))

                                AppTextField(
                                    value = fullNameOnDoc,
                                    onValueChange = { fullNameOnDoc = it },
                                    label = "ডকুমেন্টে উল্লিখিত পূর্ণ নাম",
                                    placeholder = "যেমন: TANVIR AHMED",
                                    leadingIcon = Icons.Default.Person
                                )

                                Spacer(Modifier.height(12.dp))

                                AppTextField(
                                    value = docNumber,
                                    onValueChange = { docNumber = it },
                                    label = when (selectedDocType) {
                                        KycDocType.PASSPORT -> "পাসপোর্ট নম্বর"
                                        KycDocType.NID -> "জাতীয় পরিচয়পত্র নম্বর (NID)"
                                        KycDocType.WORK_PERMIT_IQAMA -> "ওয়ার্ক পারমিট / ইকামা নম্বর"
                                    },
                                    placeholder = "যেমন: A08492014",
                                    leadingIcon = Icons.Default.Badge
                                )

                                Spacer(Modifier.height(12.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        AppTextField(
                                            value = dateOfBirth,
                                            onValueChange = { dateOfBirth = it },
                                            label = "জন্ম তারিখ",
                                            placeholder = "DD/MM/YYYY",
                                            leadingIcon = Icons.Default.Cake
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        AppTextField(
                                            value = expiryDate,
                                            onValueChange = { expiryDate = it },
                                            label = "মেয়াদ শেষ",
                                            placeholder = "YYYY-MM-DD",
                                            leadingIcon = Icons.Default.CalendarToday
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // Country Selection
                                Box {
                                    OutlinedTextField(
                                        value = issuingCountry,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("ইস্যুকৃত দেশ (Country of Issue)", fontSize = 12.sp) },
                                        trailingIcon = {
                                            IconButton(onClick = { showCountryDropdown = !showCountryDropdown }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Public, contentDescription = null, tint = MagentaPrimary)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showCountryDropdown = true },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MagentaPrimary,
                                            unfocusedBorderColor = LightGrayBorder
                                        )
                                    )

                                    DropdownMenu(
                                        expanded = showCountryDropdown,
                                        onDismissRequest = { showCountryDropdown = false }
                                    ) {
                                        countries.forEach { country ->
                                            DropdownMenuItem(
                                                text = { Text(country, fontSize = 13.sp) },
                                                onClick = {
                                                    issuingCountry = country
                                                    showCountryDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(20.dp))

                                PrimaryButton(
                                    text = "পরবর্তী ধাপ: ক্যামেরা দিয়ে ছবি তুলুন",
                                    onClick = { currentStep = 2 },
                                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                                    testTag = "proceed_to_doc_capture"
                                )
                            }
                        }
                    }

                    // STEP 2: DOCUMENT PHOTO CAPTURE
                    2 -> {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "ধাপ ২: ডকুমেন্টের ছবি তুলুন",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkText
                                        )
                                        Text(
                                            text = "ক্যামেরা ইনপুট বা গ্যালারি থেকে ছবি যোগ করুন",
                                            fontSize = 11.sp,
                                            color = GrayText
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PurpleSecondary.copy(alpha = 0.12f),
                                        modifier = Modifier.padding(2.dp)
                                    ) {
                                        Text(
                                            text = selectedDocType.shortCode,
                                            color = PurpleSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                // Camera Guidance Card
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF0FDF4),
                                    border = borderStroke(Color(0xFF86EFAC)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CenterFocusStrong, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(24.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text("সরাসরি ক্যামেরা ব্যবহারের নিয়মাবলী", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF166534))
                                            Text("• ভালো আলোতে রাখুন এবং কোনো ফ্ল্যাশ রিফ্লেকশন এড়িয়ে চলুন\n• ডকুমেন্টের ৪টি কোণ ক্যামেরার ফ্রেমের ভেতরে দৃশ্যমান রাখুন", fontSize = 11.sp, color = Color(0xFF15803D), lineHeight = 16.sp)
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                // SLOT 1: FRONT SIDE
                                KycMediaUploadSlot(
                                    title = when (selectedDocType) {
                                        KycDocType.PASSPORT -> "১. বায়োডাটা পাতা (Bio Page)"
                                        KycDocType.NID -> "১. সামনের পিঠ (Front Side)"
                                        KycDocType.WORK_PERMIT_IQAMA -> "১. ইকামা / পারমিট সামনের পিঠ"
                                    },
                                    subtitle = "ছবি ও সকল টেক্সট পরিষ্কার থাকতে হবে",
                                    meta = frontDocMeta,
                                    onCaptureClick = {
                                        showSourcePickerForTarget = CameraTargetMode.DOC_FRONT
                                    },
                                    onRemove = { frontDocMeta = null }
                                )

                                Spacer(Modifier.height(12.dp))

                                // SLOT 2: BACK SIDE
                                KycMediaUploadSlot(
                                    title = when (selectedDocType) {
                                        KycDocType.PASSPORT -> "২. ভিসা বা সিল পাতা (Visa / Stamp Page)"
                                        KycDocType.NID -> "২. পেছনের পিঠ (Back Side)"
                                        KycDocType.WORK_PERMIT_IQAMA -> "২. ইকামা / পারমিট পেছনের পিঠ"
                                    },
                                    subtitle = "ঠিকানা ও বারকোড দৃশ্যমান থাকতে হবে",
                                    meta = backDocMeta,
                                    onCaptureClick = {
                                        showSourcePickerForTarget = CameraTargetMode.DOC_BACK
                                    },
                                    onRemove = { backDocMeta = null }
                                )

                                // SLOT 3: OPTIONAL WORK PERMIT (If passport selected but user has work permit too)
                                if (selectedDocType == KycDocType.WORK_PERMIT_IQAMA || selectedDocType == KycDocType.PASSPORT) {
                                    Spacer(Modifier.height(12.dp))
                                    KycMediaUploadSlot(
                                        title = "৩. অতিরিক্ত ওয়ার্ক পারমিট / নিয়োগ চুক্তি (ঐচ্ছিক)",
                                        subtitle = "ভিসা বা কোম্পানির স্পন্সরশিপ প্রমাণ",
                                        meta = workPermitMeta,
                                        isOptional = selectedDocType == KycDocType.PASSPORT,
                                        onCaptureClick = {
                                            showSourcePickerForTarget = CameraTargetMode.DOC_WORK_PERMIT
                                        },
                                        onRemove = { workPermitMeta = null }
                                    )
                                }

                                Spacer(Modifier.height(20.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(
                                        onClick = { currentStep = 1 },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("পূর্ববর্তী ধাপ", fontSize = 13.sp)
                                    }
                                    Button(
                                        onClick = { currentStep = 3 },
                                        modifier = Modifier.weight(1.5f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MagentaPrimary),
                                        enabled = frontDocMeta != null
                                    ) {
                                        Text("পরবর্তী: সেলফি ভেরিফিকেশন", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // STEP 3: SELFIE OR SELFIE VIDEO LIVENESS CHECK
                    3 -> {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "ধাপ ৩: সেলফি বা লাইভ ভিডিও যাচাই",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkText
                                )
                                Text(
                                    text = "জালিয়াতি রোধ ও এআই ফেস ম্যাচিং সম্পন্ন করতে ক্যামেরা দিয়ে লাইভ সেলফি বা ৩ সেকেন্ডের ভিডিও সেলফি তুলুন",
                                    fontSize = 12.sp,
                                    color = GrayText
                                )

                                Spacer(Modifier.height(16.dp))

                                // Selfie Type Selector (Photo vs Video)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    SelfieTypeChoiceCard(
                                        title = "স্থির সেলফি",
                                        icon = Icons.Default.CameraAlt,
                                        description = "এক ক্লিকে স্পষ্ট সেলফি ছবি",
                                        isSelected = selfieType == KycSelfieType.PHOTO_SELFIE,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            selfieType = KycSelfieType.PHOTO_SELFIE
                                        }
                                    )

                                    SelfieTypeChoiceCard(
                                        title = "ভিডিও সেলফি",
                                        icon = Icons.Default.Videocam,
                                        description = "৩ সে. লাইভনেস ভেরিফিকেশন",
                                        badge = "রেকমেন্ডেড",
                                        isSelected = selfieType == KycSelfieType.VIDEO_SELFIE,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            selfieType = KycSelfieType.VIDEO_SELFIE
                                        }
                                    )
                                }

                                Spacer(Modifier.height(16.dp))

                                // Live Selfie Viewfinder Slot
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
                                    border = borderStroke(PurpleSecondary.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        if (selfieMediaMeta != null) {
                                            // Captured State
                                            Box(
                                                modifier = Modifier
                                                    .size(130.dp)
                                                    .clip(CircleShape)
                                                    .background(Brush.linearGradient(listOf(MagentaPrimary, PurpleSecondary)))
                                                    .padding(3.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF1E293B)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        if (selfieMediaMeta?.isVideo == true) Icons.Default.PlayCircleFilled else Icons.Default.Face,
                                                        contentDescription = null,
                                                        tint = AmberAccent,
                                                        modifier = Modifier.size(54.dp)
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.height(10.dp))

                                            Text(
                                                text = if (selfieMediaMeta?.isVideo == true) "✓ লাইভ ভিডিও সেলফি ধারণকৃত (${selfieMediaMeta?.durationSeconds}s)" else "✓ স্থির সেলফি ছবি ধারণকৃত",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF166534)
                                            )

                                            Spacer(Modifier.height(4.dp))

                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFFDCFCE7),
                                                    modifier = Modifier.padding(2.dp)
                                                ) {
                                                    Text(
                                                        text = "লাইভনেস: ${selfieMediaMeta?.livenessConfidence}%",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF15803D),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFFF3E8FF),
                                                    modifier = Modifier.padding(2.dp)
                                                ) {
                                                    Text(
                                                        text = "সাইজ: ${selfieMediaMeta?.fileSize}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = PurpleSecondary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.height(12.dp))

                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                OutlinedButton(
                                                    onClick = {
                                                        activeCameraTarget = if (selfieType == KycSelfieType.VIDEO_SELFIE) CameraTargetMode.SELFIE_VIDEO else CameraTargetMode.SELFIE_PHOTO
                                                        showCameraDialog = true
                                                    },
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("পুনরায় ক্যামেরা খুলুন", fontSize = 12.sp)
                                                }
                                            }
                                        } else {
                                            // Empty State - Prompt Camera
                                            Box(
                                                modifier = Modifier
                                                    .size(110.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                                    .border(2.dp, PurpleSecondary, CircleShape)
                                                    .clickable {
                                                        activeCameraTarget = if (selfieType == KycSelfieType.VIDEO_SELFIE) CameraTargetMode.SELFIE_VIDEO else CameraTargetMode.SELFIE_PHOTO
                                                        showCameraDialog = true
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(
                                                        if (selfieType == KycSelfieType.VIDEO_SELFIE) Icons.Default.Videocam else Icons.Default.AddAPhoto,
                                                        contentDescription = null,
                                                        tint = MagentaPrimary,
                                                        modifier = Modifier.size(36.dp)
                                                    )
                                                    Text("ক্লিক করুন", fontSize = 10.sp, color = GrayText)
                                                }
                                            }

                                            Spacer(Modifier.height(12.dp))

                                            Button(
                                                onClick = {
                                                    activeCameraTarget = if (selfieType == KycSelfieType.VIDEO_SELFIE) CameraTargetMode.SELFIE_VIDEO else CameraTargetMode.SELFIE_PHOTO
                                                    showCameraDialog = true
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MagentaPrimary),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Camera, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = if (selfieType == KycSelfieType.VIDEO_SELFIE) "ক্যামেরা চালু করে ভিডিও রেকর্ড করুন" else "ক্যামেরা দিয়ে সেলফি তুলুন",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(20.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(
                                        onClick = { currentStep = 2 },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("পূর্ববর্তী ধাপ", fontSize = 13.sp)
                                    }
                                    Button(
                                        onClick = { currentStep = 4 },
                                        modifier = Modifier.weight(1.5f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MagentaPrimary),
                                        enabled = selfieMediaMeta != null
                                    ) {
                                        Text("পরবর্তী: মেটাডেটা প্রিভিউ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // STEP 4: METADATA REVIEW & ADMIN SUBMISSION
                    4 -> {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "ধাপ ৪: সংরক্ষিত মেটাডেটা ও সাবমিশন",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkText
                                )
                                Text(
                                    text = "অ্যাডমিন অনুমোদনের জন্য নিচের সকল ডকুমেন্ট ও অডিট মেটাডেটা যাচাই করুন",
                                    fontSize = 12.sp,
                                    color = GrayText
                                )

                                Spacer(Modifier.height(16.dp))

                                // Applicant Card
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SoftPinkBackground,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("আবেদনকারীর প্রোফাইল", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MagentaPrimary)
                                            Text("আইডি: ${user.id}", fontSize = 11.sp, color = GrayText)
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        MetadataRow("পূর্ণ নাম:", fullNameOnDoc)
                                        MetadataRow("ফোন নম্বর:", user.phone)
                                        MetadataRow("ইমেইল:", user.email)
                                        MetadataRow("ডকুমেন্ট টাইপ:", selectedDocType.banglaTitle)
                                        MetadataRow("ডকুমেন্ট নম্বর:", docNumber)
                                        MetadataRow("ইস্যুকৃত দেশ:", issuingCountry)
                                        MetadataRow("মেয়াদ শেষ:", expiryDate)
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // Media Attachments Summary
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = borderStroke(Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("সংযুক্ত মিডিয়া ও লাইভনেস মেটাডেটা", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkText)
                                        Spacer(Modifier.height(6.dp))

                                        frontDocMeta?.let {
                                            MediaMetaBadge("ডকুমেন্ট সামনের পাতা", it.fileName, it.fileSize, it.captureSource, it.qualityScore)
                                        }
                                        backDocMeta?.let {
                                            MediaMetaBadge("ডকুমেন্ট পেছনের পাতা", it.fileName, it.fileSize, it.captureSource, it.qualityScore)
                                        }
                                        workPermitMeta?.let {
                                            MediaMetaBadge("ওয়ার্ক পারমিট / ইকামা", it.fileName, it.fileSize, it.captureSource, it.qualityScore)
                                        }
                                        selfieMediaMeta?.let {
                                            MediaMetaBadge(
                                                label = if (it.isVideo) "সেলফি ভিডিও লাইভনেস (${it.durationSeconds}s)" else "স্থির সেলফি",
                                                fileName = it.fileName,
                                                size = it.fileSize,
                                                source = it.captureSource,
                                                score = "লাইভনেস: ${it.livenessConfidence}%"
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // Technical Audit Metadata
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF0FDF4),
                                    border = borderStroke(Color(0xFF86EFAC)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("সিকিউরিটি অডিট ও সিস্টেম মেটাডেটা", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF166534))
                                        Spacer(Modifier.height(4.dp))
                                        MetadataRow("ডিভাইস মডেল:", "Samsung Galaxy S24 Ultra")
                                        MetadataRow("আইপি অ্যাড্রেস:", "185.190.142.66 (Secure)")
                                        MetadataRow("জিপিএস অবস্থান:", "24.7136° N, 46.6753° E (Riyadh, KSA)")
                                        MetadataRow("এনক্রিপশন লেভেল:", "AES-256 TLS 1.3 Bank Grade")
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { consentAgreed = !consentAgreed }
                                ) {
                                    Checkbox(
                                        checked = consentAgreed,
                                        onCheckedChange = { consentAgreed = it },
                                        colors = CheckboxDefaults.colors(checkedColor = MagentaPrimary)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "আমি প্রত্যয়ন করছি যে প্রদত্ত তথ্য ও ডকুমেন্টের ছবি ১০০% সঠিক এবং বাংলাদেশ ব্যাংকের অ্যান্টি-মানি লন্ডারিং (AML) বিধিমালার অধীন।",
                                        fontSize = 11.sp,
                                        color = DarkText,
                                        lineHeight = 16.sp
                                    )
                                }

                                Spacer(Modifier.height(20.dp))

                                PrimaryButton(
                                    text = if (submissionSuccess) "অ্যাডমিন অনুমোদনের জন্য জমা দেওয়া হয়েছে ✓" else "অ্যাডমিন অনুমোদনের জন্য জমা দিন",
                                    isLoading = isSubmitting,
                                    enabled = consentAgreed && frontDocMeta != null && selfieMediaMeta != null,
                                    onClick = {
                                        isSubmitting = true
                                        coroutineScope.launch {
                                            delay(1200)
                                            val submission = repository.submitKycVerification(
                                                documentType = selectedDocType,
                                                documentNumber = docNumber,
                                                fullNameOnDoc = fullNameOnDoc,
                                                dateOfBirth = dateOfBirth,
                                                issuingCountry = issuingCountry,
                                                expiryDate = expiryDate,
                                                frontDocMeta = frontDocMeta!!,
                                                backDocMeta = backDocMeta,
                                                workPermitDocMeta = workPermitMeta,
                                                selfieType = selfieType,
                                                selfieMediaMeta = selfieMediaMeta!!
                                            )
                                            activeSubmissionId = submission.id
                                            isSubmitting = false
                                            submissionSuccess = true
                                        }
                                    },
                                    testTag = "submit_kyc_for_admin_approval"
                                )

                                if (submissionSuccess) {
                                    Spacer(Modifier.height(16.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFDCFCE7),
                                        border = borderStroke(SuccessGreen),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(36.dp))
                                            Spacer(Modifier.height(6.dp))
                                            Text("কেওয়াইসি সফলভাবে সাবমিট হয়েছে!", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF15803D))
                                            Text("ট্র্যাকিং রেফারেন্স: $activeSubmissionId", fontSize = 12.sp, color = DarkText, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.height(4.dp))
                                            Text("আমাদের কমপ্লায়েন্স অ্যাডমিন টিম আপনার মেটাডেটা পর্যালোচনা করে ৫-১৫ মিনিটের মধ্যে অনুমোদন প্রদান করবে।", fontSize = 11.sp, color = GrayText, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // KYC Requirements FAQ Section
                Text(
                    text = "কেওয়াইসি ভেরিফিকেশন এফএকিউ (FAQ)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = DarkText,
                    modifier = Modifier.padding(top = 8.dp)
                )

                FaqSectionComponent(
                    faqs = repository.faqs.filter { it.category == "ডকুমেন্ট ও KYC" || it.category == "নিরাপত্তা" },
                    showSearch = false
                )
            }
        }
    }

    // -----------------------------------------------------------
    // MEDIA SOURCE PICKER DIALOG (Camera vs Gallery)
    // -----------------------------------------------------------
    if (showSourcePickerForTarget != null) {
        val target = showSourcePickerForTarget!!
        AlertDialog(
            onDismissRequest = { showSourcePickerForTarget = null },
            title = {
                Text(
                    text = "ছবি সংগ্রহের উৎস নির্বাচন করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MagentaPrimary.copy(alpha = 0.08f),
                        border = borderStroke(MagentaPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                activeCameraTarget = target
                                showSourcePickerForTarget = null
                                showCameraDialog = true
                            }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MagentaPrimary, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("লাইভ ক্যামেরা দিয়ে ছবি তুলুন", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MagentaPrimary)
                                Text("ক্যামেরা ফ্রেম ও স্বয়ংক্রিয় ফোকাস গাইড সহ", fontSize = 11.sp, color = GrayText)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = borderStroke(LightGrayBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Simulate Gallery File Pick
                                val timeNow = SimpleDateFormat("dd MMM, hh:mm a", Locale("bn", "BD")).format(Date())
                                val galleryMeta = KycMediaMeta(
                                    fileName = when (target) {
                                        CameraTargetMode.DOC_FRONT -> "doc_front_gallery_picked.jpg"
                                        CameraTargetMode.DOC_BACK -> "doc_back_gallery_picked.jpg"
                                        CameraTargetMode.DOC_WORK_PERMIT -> "work_permit_gallery.jpg"
                                        CameraTargetMode.SELFIE_PHOTO -> "selfie_photo_gallery.jpg"
                                        CameraTargetMode.SELFIE_VIDEO -> "selfie_video_gallery.mp4"
                                    },
                                    fileSize = "2.6 MB",
                                    captureSource = "গ্যালারি (Gallery File Upload)",
                                    timestamp = timeNow,
                                    mimeType = "image/jpeg",
                                    qualityScore = "উচ্চ (High - 98.2%)",
                                    previewLabel = "গ্যালারি থেকে সংগৃহীত"
                                )
                                when (target) {
                                    CameraTargetMode.DOC_FRONT -> frontDocMeta = galleryMeta
                                    CameraTargetMode.DOC_BACK -> backDocMeta = galleryMeta
                                    CameraTargetMode.DOC_WORK_PERMIT -> workPermitMeta = galleryMeta
                                    CameraTargetMode.SELFIE_PHOTO -> selfieMediaMeta = galleryMeta
                                    CameraTargetMode.SELFIE_VIDEO -> selfieMediaMeta = galleryMeta.copy(isVideo = true, durationSeconds = 3.5)
                                }
                                showSourcePickerForTarget = null
                            }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = PurpleSecondary, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("গ্যালারি বা ফাইল থেকে নির্বাচন করুন", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkText)
                                Text("সংরক্ষিত পরিষ্কার স্ক্যান কপি বা ছবি", fontSize = 11.sp, color = GrayText)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSourcePickerForTarget = null }) {
                    Text("বাতিল করুন", color = GrayText)
                }
            }
        )
    }

    // -----------------------------------------------------------
    // FULLSCREEN INTERACTIVE CAMERA VIEWFINDER MODAL
    // -----------------------------------------------------------
    if (showCameraDialog) {
        KycCameraViewfinderDialog(
            targetMode = activeCameraTarget,
            docType = selectedDocType,
            onClose = { showCameraDialog = false },
            onCaptureConfirmed = { capturedMeta ->
                when (activeCameraTarget) {
                    CameraTargetMode.DOC_FRONT -> frontDocMeta = capturedMeta
                    CameraTargetMode.DOC_BACK -> backDocMeta = capturedMeta
                    CameraTargetMode.DOC_WORK_PERMIT -> workPermitMeta = capturedMeta
                    CameraTargetMode.SELFIE_PHOTO, CameraTargetMode.SELFIE_VIDEO -> selfieMediaMeta = capturedMeta
                }
                showCameraDialog = false
            }
        )
    }
}

// -----------------------------------------------------------
// KYC STEP PROGRESS INDICATOR
// -----------------------------------------------------------
@Composable
fun KycStepIndicator(currentStep: Int, totalSteps: Int) {
    Surface(
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val stepNames = listOf("১. ডকুমেন্ট", "২. ফটো", "৩. সেলফি", "৪. রিভিউ")
            stepNames.forEachIndexed { index, name ->
                val stepNum = index + 1
                val isCurrent = stepNum == currentStep
                val isDone = stepNum < currentStep

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isDone -> SuccessGreen
                                    isCurrent -> MagentaPrimary
                                    else -> LightGrayBorder
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        } else {
                            Text(
                                text = "$stepNum",
                                color = if (isCurrent) Color.White else GrayText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = name,
                        fontSize = 11.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) MagentaPrimary else if (isDone) DarkText else GrayText
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------
// DOCUMENT MEDIA UPLOAD SLOT COMPONENT
// -----------------------------------------------------------
@Composable
fun KycMediaUploadSlot(
    title: String,
    subtitle: String,
    meta: KycMediaMeta?,
    isOptional: Boolean = false,
    onCaptureClick: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (meta != null) Color(0xFFF0FDF4) else Color(0xFFFAFAFA),
        border = borderStroke(if (meta != null) SuccessGreen else LightGrayBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkText)
                        if (isOptional) {
                            Spacer(Modifier.width(6.dp))
                            Text("(ঐচ্ছিক)", fontSize = 10.sp, color = GrayText)
                        }
                    }
                    Text(text = subtitle, fontSize = 11.sp, color = GrayText)
                }

                if (meta != null) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = DangerRed, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (meta != null) {
                // Uploaded Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = MagentaPrimary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(meta.fileName, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${meta.fileSize} • ${meta.captureSource} • ${meta.qualityScore}", fontSize = 10.sp, color = GrayText)
                    }
                    TextButton(onClick = onCaptureClick) {
                        Text("বদলান", fontSize = 11.sp, color = MagentaPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Upload Trigger Button
                Button(
                    onClick = onCaptureClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    border = borderStroke(MagentaPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MagentaPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ক্যামেরা দিয়ে তুলুন বা আপলোড করুন", color = MagentaPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -----------------------------------------------------------
// SELFIE TYPE SELECTOR CARD
// -----------------------------------------------------------
@Composable
fun SelfieTypeChoiceCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    badge: String? = null,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MagentaPrimary.copy(alpha = 0.08f) else Color.White,
        border = borderStroke(if (isSelected) MagentaPrimary else LightGrayBorder),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isSelected) MagentaPrimary else GrayText,
                    modifier = Modifier.size(24.dp)
                )
                if (badge != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AmberAccent.copy(alpha = 0.2f)
                    ) {
                        Text(badge, fontSize = 9.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isSelected) MagentaPrimary else DarkText)
            Text(description, fontSize = 10.sp, color = GrayText, lineHeight = 14.sp)
        }
    }
}

// -----------------------------------------------------------
// FULLSCREEN CAMERA VIEWFINDER DIALOG
// -----------------------------------------------------------
@Composable
fun KycCameraViewfinderDialog(
    targetMode: CameraTargetMode,
    docType: KycDocType,
    onClose: () -> Unit,
    onCaptureConfirmed: (KycMediaMeta) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isFrontCamera by remember { mutableStateOf(targetMode == CameraTargetMode.SELFIE_PHOTO || targetMode == CameraTargetMode.SELFIE_VIDEO) }
    var isFlashOn by remember { mutableStateOf(false) }
    var isRecordingVideo by remember { mutableStateOf(false) }
    var videoProgressSeconds by remember { mutableStateOf(0.0) }
    var isCaptured by remember { mutableStateOf(false) }
    var simulatedQuality by remember { mutableStateOf("উচ্চ (High - 99.4%)") }

    // Pulsing recording animation
    val infiniteTransition = rememberInfiniteTransition(label = "cam_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .testTag("kyc_camera_viewfinder_modal")
        ) {
            // Simulated Viewfinder Background with grid
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                // Grid lines
                drawLine(Color.White.copy(alpha = 0.15f), Offset(size.width / 3, 0f), Offset(size.width / 3, size.height), strokeWidth = 1f)
                drawLine(Color.White.copy(alpha = 0.15f), Offset(size.width * 2 / 3, 0f), Offset(size.width * 2 / 3, size.height), strokeWidth = 1f)
                drawLine(Color.White.copy(alpha = 0.15f), Offset(0f, size.height / 3), Offset(size.width, size.height / 3), strokeWidth = 1f)
                drawLine(Color.White.copy(alpha = 0.15f), Offset(0f, size.height * 2 / 3), Offset(size.width, size.height * 2 / 3), strokeWidth = 1f)
            }

            // Target Overlay Box (Document Card or Face Oval)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                if (targetMode == CameraTargetMode.SELFIE_PHOTO || targetMode == CameraTargetMode.SELFIE_VIDEO) {
                    // Face Oval Frame
                    Box(
                        modifier = Modifier
                            .size(width = 240.dp, height = 310.dp)
                            .clip(CircleShape)
                            .border(
                                width = 3.dp,
                                color = if (isRecordingVideo) Color.Red.copy(alpha = pulseAlpha) else AmberAccent,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isCaptured) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Face,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.35f),
                                    modifier = Modifier.size(90.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = if (isRecordingVideo) "ভিডিও রেকর্ড হচ্ছে...\nএকবার চোখের পলক ফেলুন" else "মুখমণ্ডল বৃত্তের মাঝে রাখুন",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // ID Card / Passport Rectangular Frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = 2.5.dp,
                                color = if (isCaptured) SuccessGreen else MagentaPrimary,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .background(Color.Black.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isCaptured) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.CenterFocusStrong,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = when (targetMode) {
                                        CameraTargetMode.DOC_FRONT -> "${docType.banglaTitle}\nবায়োডাটা/সামনের পাতা ফ্রেমের ভেতরে রাখুন"
                                        CameraTargetMode.DOC_BACK -> "${docType.banglaTitle}\nপেছনের পাতা বা সিল ফ্রেমের ভেতরে রাখুন"
                                        else -> "ডকুমেন্টটি সোজা ও পরিষ্কার রাখুন"
                                    },
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // TOP TOOLBAR CONTROLS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(28.dp))
                }

                // Header Mode Tag
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isRecordingVideo) Color.Red else SuccessGreen)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = when (targetMode) {
                                CameraTargetMode.DOC_FRONT -> "ডকুমেন্ট ফ্রন্ট পাতা"
                                CameraTargetMode.DOC_BACK -> "ডকুমেন্ট ব্যাক পাতা"
                                CameraTargetMode.DOC_WORK_PERMIT -> "ওয়ার্ক পারমিট"
                                CameraTargetMode.SELFIE_PHOTO -> "লাইভ সেলফি ফটো"
                                CameraTargetMode.SELFIE_VIDEO -> if (isRecordingVideo) "রেকর্ডিং: ${String.format(Locale.US, "%.1f", videoProgressSeconds)}s / 4.0s" else "ভিডিও লাইভনেস"
                            },
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row {
                    IconButton(onClick = { isFlashOn = !isFlashOn }) {
                        Icon(
                            if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash",
                            tint = if (isFlashOn) AmberAccent else Color.White
                        )
                    }
                    IconButton(onClick = { isFrontCamera = !isFrontCamera }) {
                        Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Switch Camera", tint = Color.White)
                    }
                }
            }

            // BOTTOM SHUTTER CONTROLS
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isCaptured) {
                    // PREVIEW CONFIRMATION CONTROLS
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.85f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("ক্যাপচার সফল হয়েছে • স্পষ্টতা ৯৯.৪%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = {
                                        isCaptured = false
                                        isRecordingVideo = false
                                        videoProgressSeconds = 0.0
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("পুনরায় তুলুন", color = Color.White, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        val timeNow = SimpleDateFormat("dd MMM, hh:mm a", Locale("bn", "BD")).format(Date())
                                        val generatedMeta = KycMediaMeta(
                                            fileName = when (targetMode) {
                                                CameraTargetMode.DOC_FRONT -> "nid_passport_front_cam_${System.currentTimeMillis() % 10000}.jpg"
                                                CameraTargetMode.DOC_BACK -> "nid_passport_back_cam_${System.currentTimeMillis() % 10000}.jpg"
                                                CameraTargetMode.DOC_WORK_PERMIT -> "work_permit_cam_${System.currentTimeMillis() % 10000}.jpg"
                                                CameraTargetMode.SELFIE_PHOTO -> "selfie_photo_cam_${System.currentTimeMillis() % 10000}.jpg"
                                                CameraTargetMode.SELFIE_VIDEO -> "liveness_video_selfie_${System.currentTimeMillis() % 10000}.mp4"
                                            },
                                            fileSize = if (targetMode == CameraTargetMode.SELFIE_VIDEO) "6.8 MB" else "2.9 MB",
                                            captureSource = "ক্যামেরা (Camera Live Capture)",
                                            timestamp = timeNow,
                                            mimeType = if (targetMode == CameraTargetMode.SELFIE_VIDEO) "video/mp4" else "image/jpeg",
                                            qualityScore = simulatedQuality,
                                            isVideo = targetMode == CameraTargetMode.SELFIE_VIDEO,
                                            durationSeconds = if (targetMode == CameraTargetMode.SELFIE_VIDEO) 4.0 else 0.0,
                                            livenessConfidence = 99.6,
                                            previewLabel = when (targetMode) {
                                                CameraTargetMode.DOC_FRONT -> "সামনের পাতা ক্যাপচার"
                                                CameraTargetMode.DOC_BACK -> "পেছনের পাতা ক্যাপচার"
                                                CameraTargetMode.DOC_WORK_PERMIT -> "ওয়ার্ক পারমিট ক্যাপচার"
                                                CameraTargetMode.SELFIE_PHOTO -> "স্থির সেলফি ক্যাপচার"
                                                CameraTargetMode.SELFIE_VIDEO -> "৪.০ সেকেন্ড লাইভ ভিডিও"
                                            }
                                        )
                                        onCaptureConfirmed(generatedMeta)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("নিশ্চিত করুন ✓", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    // LIVE SHUTTER BUTTON
                    if (targetMode == CameraTargetMode.SELFIE_VIDEO) {
                        // Video Record Shutter
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.3f))
                                .clickable {
                                    if (!isRecordingVideo) {
                                        isRecordingVideo = true
                                        coroutineScope.launch {
                                            for (i in 1..40) {
                                                delay(100)
                                                videoProgressSeconds = i * 0.1
                                            }
                                            isRecordingVideo = false
                                            isCaptured = true
                                        }
                                    }
                                }
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(if (isRecordingVideo) Color.Red else MagentaPrimary)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (isRecordingVideo) "রেকর্ডিং চলছে (৪ সেকেন্ড)..." else "ট্যাপ করে ভিডিও রেকর্ড শুরু করুন",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        // Photo Shutter
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.3f))
                                .clickable {
                                    coroutineScope.launch {
                                        delay(250)
                                        isCaptured = true
                                    }
                                }
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("ছবি তুলতে ট্যাপ করুন", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------
// HELPER SUBCOMPONENTS FOR METADATA
// -----------------------------------------------------------
@Composable
fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = GrayText)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkText)
    }
}

@Composable
fun MediaMetaBadge(label: String, fileName: String, size: String, source: String, score: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = borderStroke(Color(0xFFCBD5E1)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Attachment, contentDescription = null, tint = MagentaPrimary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DarkText)
                Text("$fileName ($size) • $source", fontSize = 10.sp, color = GrayText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFFDCFCE7),
                modifier = Modifier.padding(2.dp)
            ) {
                Text(score, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            }
        }
    }
}

private fun borderStroke(color: Color) = androidx.compose.foundation.BorderStroke(1.dp, color)
