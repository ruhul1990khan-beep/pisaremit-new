package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.RemittanceDao
import com.example.data.models.*
import com.example.service.PaisaFirebaseMessagingService
import com.example.util.FcmNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PaisaRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val dao: RemittanceDao = database.remittanceDao()
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // FCM Registration Token Flow
    private val _fcmToken = MutableStateFlow("fcm_token_paisa_live_966_sar_bdt_2026_init")
    val fcmToken: StateFlow<String> = _fcmToken.asStateFlow()

    // Current User Session
    private val _currentUser = MutableStateFlow(
        User(
            id = "USR-2026-0941",
            name = "তানভীর আহমেদ",
            phone = "+966 50 123 4567",
            email = "tanvir.ahmed@example.com",
            country = "সৌদি আরব (SAR)",
            nationality = "বাংলাদেশী",
            kycStatus = KycStatus.VERIFIED,
            accountStatus = AccountStatus.ACTIVE,
            totalTransferredBdt = 385500.00,
            totalTransactionsCount = 8,
            memberSince = "মার্চ ২০২৪",
            biometricEnabled = true,
            deviceConsentGiven = true
        )
    )
    val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    // System Settings (Admin controlled)
    private val _systemSettings = MutableStateFlow(SystemSettings())
    val systemSettings: StateFlow<SystemSettings> = _systemSettings.asStateFlow()

    // Selected Currency App State
    private val _selectedCurrency = MutableStateFlow("SAR")
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    fun setSelectedCurrency(currencyCode: String) {
        if (currencyCode.isNotBlank()) {
            _selectedCurrency.value = currencyCode
        }
    }

    // Notifications
    private val _notifications = MutableStateFlow<List<NotificationItem>>(
        listOf(
            NotificationItem(
                id = "NOTIF-1",
                title = "লেনদেন সম্পন্ন হয়েছে!",
                body = "আপনার ৳৬৫,০০০ টাকার রেমিট্যান্স (RM-2026-894210) সফলভাবে জমা হয়েছে।",
                time = "১০ মিনিট আগে",
                type = "TRANSACTION",
                isRead = false
            ),
            NotificationItem(
                id = "NOTIF-2",
                title = "বিশেষ এক্সচেঞ্জ রেট অফার",
                body = "আজ SAR থেকে BDT এক্সচেঞ্জ রেট ৩২.৫০ টাকা! প্রোমোকোড 'BOISHAKH' ব্যবহার করুন।",
                time = "২ ঘণ্টা আগে",
                type = "OFFER",
                isRead = false
            ),
            NotificationItem(
                id = "NOTIF-3",
                title = "জাতীয় পরিচয়পত্র যাচাই সম্পন্ন",
                body = "আপনার কেওয়াইসি (KYC) প্রোফাইল সফলভাবে ভেরিফাইড হয়েছে।",
                time = "গতকাল",
                type = "KYC",
                isRead = true
            ),
            NotificationItem(
                id = "NOTIF-4",
                title = "নতুন ডিভাইস নিবন্ধন নিরাপত্তা",
                body = "আপনার অ্যাকাউন্টে একটি নতুন ডিভাইস যুক্ত হয়েছে।",
                time = "২ দিন আগে",
                type = "SECURITY",
                isRead = true
            )
        )
    )
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    // Support Tickets
    private val _supportTickets = MutableStateFlow<List<SupportTicket>>(
        listOf(
            SupportTicket(
                id = "TCK-8821",
                subject = "বিকাশ ক্যাশইন বিলম্ব সংক্রান্ত অনুসন্ধান",
                transactionRef = "RM-2026-894210",
                category = "রেমিট্যান্স লেনদেন",
                priority = "জরুরি",
                status = "উত্তর দেওয়া হয়েছে",
                createdAt = "২১ আগস্ট ২০২৬, সকাল ১০:১৫",
                linkedUserId = "USR-2026-0941",
                messages = listOf(
                    TicketMessage(
                        senderName = "তানভীর আহমেদ",
                        isAgent = false,
                        message = "আমার রেমিট্যান্স ট্রানজেকশনটি সম্পন্ন দেখাচ্ছে কিন্তু প্রাপক এখনো বিকাশ এসএমএস পাননি।",
                        timestamp = "সকাল ১০:১৫",
                        linkedTxId = "RM-2026-894210"
                    ),
                    TicketMessage(
                        senderName = "পয়সা সাপোর্ট (সালমান)",
                        isAgent = true,
                        message = "প্রিয় তানভীর সাহেব (USR-2026-0941), বাংলাদেশ ব্যাংকের সুইফট সেটেলমেন্ট সম্পন্ন হয়েছে। প্রাপকের বিকাশ অ্যাকাউন্ট ব্যালেন্স চেক করতে বলুন। ধন্যবাদ।",
                        timestamp = "সকাল ১০:২৮"
                    )
                )
            )
        )
    )
    val supportTickets: StateFlow<List<SupportTicket>> = _supportTickets.asStateFlow()

    // KYC Submissions (Document & Selfie / Selfie-Video Verification Records for Admin Review)
    private val _kycSubmissions = MutableStateFlow<List<KycSubmissionRecord>>(
        listOf(
            KycSubmissionRecord(
                id = "KYC-2026-94021",
                userId = "USR-10023",
                userName = "তানভীর আহমেদ",
                userPhone = "+8801712345678",
                userEmail = "tanvir.ahmed@example.com",
                documentType = KycDocType.PASSPORT,
                documentNumber = "A08492014",
                fullNameOnDoc = "TANVIR AHMED",
                dateOfBirth = "15/08/1990",
                issuingCountry = "বাংলাদেশ (Bangladesh)",
                expiryDate = "2032-11-20",
                frontDocMeta = KycMediaMeta(
                    fileName = "passport_bio_page_cam_20260823.jpg",
                    fileSize = "3.2 MB",
                    captureSource = "ক্যামেরা (Camera Live Capture)",
                    timestamp = "২২ আগস্ট ২০২৬, সকাল ১০:২০",
                    mimeType = "image/jpeg",
                    qualityScore = "উচ্চ (High - 99.4%)",
                    previewLabel = "পাসপোর্ট বায়োডাটা পাতা"
                ),
                backDocMeta = KycMediaMeta(
                    fileName = "passport_visa_stamp_cam_20260823.jpg",
                    fileSize = "2.8 MB",
                    captureSource = "ক্যামেরা (Camera Live Capture)",
                    timestamp = "২২ আগস্ট ২০২৬, সকাল ১০:২১",
                    mimeType = "image/jpeg",
                    qualityScore = "উচ্চ (High - 98.6%)",
                    previewLabel = "ভিসা / এন্ট্রি সিল পাতা"
                ),
                workPermitDocMeta = KycMediaMeta(
                    fileName = "iqama_permit_card_20260823.jpg",
                    fileSize = "2.1 MB",
                    captureSource = "ক্যামেরা (Camera Live Capture)",
                    timestamp = "২২ আগস্ট ২০২৬, সকাল ১০:২২",
                    mimeType = "image/jpeg",
                    qualityScore = "উচ্চ (High - 97.9%)",
                    previewLabel = "সৌদি রেসিডেন্ট কার্ড (ইকামা)"
                ),
                selfieType = KycSelfieType.VIDEO_SELFIE,
                selfieMediaMeta = KycMediaMeta(
                    fileName = "liveness_selfie_video_20260823.mp4",
                    fileSize = "7.4 MB",
                    captureSource = "ক্যামেরা ভিডিও লাইভনেস (Camera Liveness Check)",
                    timestamp = "২২ আগস্ট ২০২৬, সকাল ১০:২৩",
                    mimeType = "video/mp4",
                    qualityScore = "উচ্চ (High - 99.8%)",
                    isVideo = true,
                    durationSeconds = 4.0,
                    livenessConfidence = 99.6,
                    previewLabel = "৪ সেকেন্ড লাইভ ভিডিও সেলফি (চোখ পলক ও হাসি সনাক্ত)"
                ),
                deviceModel = "Samsung Galaxy S24 Ultra (Android 14)",
                ipAddress = "185.190.142.66",
                gpsCoordinates = "24.7136° N, 46.6753° E (Riyadh, KSA)",
                submittedAt = "২২ আগস্ট ২০২৬, সকাল ১০:২৩",
                status = KycStatus.PENDING,
                adminNotes = "ডকুমেন্ট ও সেলফি ভিডিও যাচাইয়ের জন্য অপেক্ষমাণ।"
            ),
            KycSubmissionRecord(
                id = "KYC-2026-92104",
                userId = "USR-20914",
                userName = "মোহাম্মদ রফিকুল ইসলাম",
                userPhone = "+966 55 987 6543",
                userEmail = "rafiq.islam92@gmail.com",
                documentType = KycDocType.WORK_PERMIT_IQAMA,
                documentNumber = "IQ-2490192841",
                fullNameOnDoc = "MD RAFIQUL ISLAM",
                dateOfBirth = "04/11/1988",
                issuingCountry = "সৌদি আরব (Saudi Arabia)",
                expiryDate = "2027-04-15",
                frontDocMeta = KycMediaMeta(
                    fileName = "iqama_front_cam_20260821.jpg",
                    fileSize = "2.9 MB",
                    captureSource = "ক্যামেরা (Camera Live Capture)",
                    timestamp = "২১ আগস্ট ২০২৬, বিকাল ০৩:১০",
                    mimeType = "image/jpeg",
                    qualityScore = "উচ্চ (High - 97.2%)",
                    previewLabel = "ইকামা সামনের পিঠ"
                ),
                backDocMeta = KycMediaMeta(
                    fileName = "iqama_back_cam_20260821.jpg",
                    fileSize = "2.6 MB",
                    captureSource = "ক্যামেরা (Camera Live Capture)",
                    timestamp = "২১ আগস্ট ২০২৬, বিকাল ০৩:১১",
                    mimeType = "image/jpeg",
                    qualityScore = "উচ্চ (High - 96.8%)",
                    previewLabel = "ইকামা পেছনের পিঠ"
                ),
                selfieType = KycSelfieType.PHOTO_SELFIE,
                selfieMediaMeta = KycMediaMeta(
                    fileName = "selfie_photo_cam_20260821.jpg",
                    fileSize = "2.2 MB",
                    captureSource = "ক্যামেরা (Camera Live Capture)",
                    timestamp = "২১ আগস্ট ২০২৬, বিকাল ০৩:১২",
                    mimeType = "image/jpeg",
                    qualityScore = "উচ্চ (High - 98.4%)",
                    isVideo = false,
                    livenessConfidence = 98.7,
                    previewLabel = "লাইভ সেলফি ছবি"
                ),
                deviceModel = "Xiaomi Redmi Note 13 Pro",
                ipAddress = "176.44.82.119",
                gpsCoordinates = "21.4858° N, 39.1925° E (Jeddah, KSA)",
                submittedAt = "২১ আগস্ট ২০২৬, বিকাল ০৩:১২",
                status = KycStatus.VERIFIED,
                adminNotes = "ডকুমেন্ট ও ফেস ম্যাচ ১০০% সঠিক। অনুমোদিত。",
                reviewedBy = "সিনিয়র কমপ্লায়েন্স অফিসার (জাহিদ হাসান)",
                reviewedAt = "২১ আগস্ট ২০২৬, বিকাল ০৩:২৫"
            )
        )
    )
    val kycSubmissions: StateFlow<List<KycSubmissionRecord>> = _kycSubmissions.asStateFlow()

    // Live Chat Messages (Real-Time Support Session)
    private val _liveChatMessages = MutableStateFlow<List<TicketMessage>>(
        listOf(
            TicketMessage(
                senderName = "পয়সা কাস্টমার সাপোর্ট (রাফি)",
                isAgent = true,
                message = "আসসালামু আলাইকুম তানভীর সাহেব! পয়সা রেমিট লাইভ সাপোর্টে আপনাকে স্বাগতম। আপনার অ্যাকাউন্ট (ID: USR-2026-0941, ফোন: +966 50 123 4567, KYC: ভেরিফাইড) সংযুক্ত রয়েছে। আপনাকে কীভাবে সাহায্য করতে পারি?",
                timestamp = "এইমাত্র"
            )
        )
    )
    val liveChatMessages: StateFlow<List<TicketMessage>> = _liveChatMessages.asStateFlow()

    private val _isAgentTyping = MutableStateFlow(false)
    val isAgentTyping: StateFlow<Boolean> = _isAgentTyping.asStateFlow()

    // Comprehensive FAQ Items covering limits, processing times, document requirements, incentives, and security
    val faqs = listOf(
        // Remittance Limits
        FaqItem(
            question = "দৈনিক ও মাসিক রেমিট্যান্স পাঠানোর সীমা (Limits) কত?",
            answer = "বাংলাদেশ ব্যাংক এবং বৈশ্বিক এএমএল (AML) নীতিমালা অনুযায়ী একজন ব্যক্তি একক ট্রানজেকশনে সর্বোচ্চ ৩,০০,০০০ টাকা এবং দৈনিক সর্বোচ্চ ৫,০০,০০০ টাকা পাঠাতে পারবেন। মাসিক লেনদেন সীমা সর্বোচ্চ ১৫,০০,০০০ টাকা। কেওয়াইসি (KYC) যাচাই সম্পন্ন হলে সীমা স্বয়ংক্রিয়ভাবে বর্ধিত হয়।",
            category = "লিমিট ও সীমা"
        ),
        FaqItem(
            question = "বিকাশ, নগদ ও রকেটে ওয়ালেট লিমিট কত?",
            answer = "বিকাশ ও নগদ অ্যাকাউন্টে প্রতি লেনদেনে সর্বোচ্চ ৫০,০০০ টাকা এবং দিনে সর্বোচ্চ ২,৫০,০০০ টাকা জমা গ্রহণ করা যায়। ব্যাংক অ্যাকাউন্টের ক্ষেত্রে উচ্চতর সীমা প্রযোজ্য।",
            category = "লিমিট ও সীমা"
        ),
        // Processing Times
        FaqItem(
            question = "রেমিট্যান্স প্রসেসিং সময় (Processing Time) কতক্ষণ লাগে?",
            answer = "১. মোবাইল ওয়ালেট (বিকাশ, নগদ, রকেট): তাৎক্ষণিক (১ থেকে ৩ মিনিটের মধ্যে)।\n২. ব্যাংক ডিপোজিট (NPSB / BEFTN): এনপিএসবি সক্রিয় ব্যাংকে তাৎক্ষণিক অথবা সাধারণ ব্যাংকিং সময়ে সর্বোচ্চ ২-৪ ঘণ্টা।\n৩. ক্যাশ পিক-আপ: প্রেরক নিশ্চিত করার পরপরই গোপন পিন নিয়ে যেকোনো অনুমোদিত ব্যাংক শাখা থেকে সাথে সাথে ক্যাশ উত্তোলন সম্ভব।",
            category = "প্রসেসিং সময়"
        ),
        FaqItem(
            question = "ছুটির দিনে বা রাতে টাকা পাঠালে কি সাথে সাথে যাবে?",
            answer = "হ্যাঁ! পয়সা রেমিটের অটোমেটেড রেমিট্যান্স সুইফট গেটওয়ের মাধ্যমে ২৪/৭ দিন-রাত ও সরকারি ছুটির দিনেও বিকাশ ও নগদ ওয়ালেটে তাৎক্ষণিকভাবে রেমিট্যান্স জমা হয়।",
            category = "প্রসেসিং সময়"
        ),
        // KYC & Document Requirements
        FaqItem(
            question = "কেওয়াইসি (KYC) যাচাইয়ের জন্য কি কি ডকুমেন্ট প্রয়োজন?",
            answer = "১. বৈধ পাসপোর্ট (Passport) অথবা জাতীয় পরিচয়পত্র (NID) অথবা প্রবাসের বৈধ রেসিডেন্ট কার্ড/ইকামা (Iqama/Resident ID)।\n২. ডকুমেন্টের সামনের ও পেছনের স্পষ্ট ছবি।\n৩. অ্যাপের মাধ্যমে তোলা লাইভ সেলফি (Face Liveness Verification)।\nসাধারণত কৃত্রিম বুদ্ধিমত্তা ও এআই ভেরিফিকেশনের মাধ্যমে ৫-১৫ মিনিটের মধ্যে কেওয়াইসি অনুমোদন হয়ে যায়।",
            category = "ডকুমেন্ট ও KYC"
        ),
        FaqItem(
            question = "কেওয়াইসি (KYC) কেন বাধ্যতামূলক?",
            answer = "বাংলাদেশ ব্যাংক ও আন্তর্জাতিক অ্যান্টি-মানি লন্ডারিং (AML) এবং কাউন্টার-টেরোরিজম ফাইন্যান্সিং (CTF) আইন মেনে আপনার অর্থ প্রেরণ সম্পূর্ণ নিরাপদ ও বৈধ চ্যানেলে পরিচালিত করতেই কেওয়াইসি ভেরিফিকেশন বাধ্যতামূলক।",
            category = "ডকুমেন্ট ও KYC"
        ),
        // Incentives & Rates
        FaqItem(
            question = "২.৫% সরকারি প্রণোদনা কি স্বয়ংক্রিয়ভাবে যোগ হবে?",
            answer = "হ্যাঁ, গণপ্রজাতন্ত্রী বাংলাদেশ সরকার কর্তৃক ঘোষিত বৈধ চ্যানেলে রেমিট্যান্সের উপর ২.৫% নগদ প্রণোদনা স্বয়ংক্রিয়ভাবে আপনার প্রেরিত টাকার সাথে যোগ হয়ে প্রাপকের অ্যাকাউন্টে ক্রেডিট হয়। কোনো অতিরিক্ত আবেদন করতে হয় না।",
            category = "প্রণোদনা ও ফি"
        ),
        FaqItem(
            question = "রেমিট্যান্স প্রেরণে কোনো গোপন চার্জ বা ফি আছে কি?",
            answer = "না, পয়সা রেমিটে কোনো গোপন চার্জ নেই। এক্সচেঞ্জ রেট এবং ন্যূনতম সার্ভিস ফি সম্পূর্ণ স্বচ্ছভাবে লেনদেন নিশ্চিত করার আগেই স্ক্রিনে প্রদর্শিত হয়। অফার বা প্রোমোকোড ব্যবহার করলে ফি সম্পূর্ণ ফ্রি হতে পারে।",
            category = "প্রণোদনা ও ফি"
        ),
        // Security & Device
        FaqItem(
            question = "অ্যাকাউন্ট ও লেনদেনের নিরাপত্তা কিভাবে নিশ্চিত করা হয়?",
            answer = "সকল তথ্য ২৫৬-বিট এসএসএল এনক্রিপশনের মাধ্যমে সংরক্ষিত। এছাড়া বায়োমেট্রিক ফিঙ্গারপ্রিন্ট/ফেস আনলক, ডিভাইসের আইপি/আইডি ট্র্যাকিং এবং ওটিপি (OTP) দ্বি-স্তরীয় নিরাপত্তার মাধ্যমে প্রতিটি লেনদেন সুরক্ষিত থাকে।",
            category = "নিরাপত্তা"
        )
    )

    // Operators for Mobile Recharge
    val mobileOperators = listOf(
        MobileRechargeOperator("GP", "গ্রামীণফোন (Grameenphone)", "017 / 013", 0xFF00A651),
        MobileRechargeOperator("ROBI", "রবি (Robi)", "018", 0xFFED1C24),
        MobileRechargeOperator("BL", "বাংলালিংক (Banglalink)", "019 / 014", 0xFFFF6600),
        MobileRechargeOperator("AIRTEL", "এয়ারটেল (Airtel)", "016", 0xFFED1B2F),
        MobileRechargeOperator("TT", "টেলিটক (Teletalk)", "015", 0xFF0072BC)
    )

    // Audit Logs for Admin
    private val _auditLogs = MutableStateFlow<List<AdminAuditLog>>(
        listOf(
            AdminAuditLog("AUD-101", "সুপার অ্যাডমিন (রাকিব)", "এক্সচেঞ্জ রেট আপডেট", "ExchangeRate", "SAR", "আজ দুপুর ১২:৩০", "সৌদি রিয়াল রেট ৩২.২০ থেকে ৩২.৪৫ টাকায় বৃদ্ধি করা হয়েছে।"),
            AdminAuditLog("AUD-102", "কেওয়াইসি অফিসার (ফারহানা)", "কেওয়াইসি অনুমোদন", "KYC", "USR-2026-0941", "আজ সকাল ১১:১০", "পাসপোর্ট ও সেলফি সঠিকভাবে ভেরিফাই করা হয়েছে।"),
            AdminAuditLog("AUD-103", "ট্রানজেকশন অফিসার (আসিফ)", "লেনদেন সম্পন্ন", "Transaction", "RM-2026-894210", "আজ সকাল ১০:৪৫", "ব্যাংক ড্রাফট সফলভাবে রিলিজ হয়েছে।")
        )
    )
    val auditLogs: StateFlow<List<AdminAuditLog>> = _auditLogs.asStateFlow()

    init {
        seedInitialData()
        initFcm()
    }

    private fun initFcm() {
        FcmNotificationManager.createNotificationChannels(context)
        FcmNotificationManager.fetchFcmToken { token ->
            _fcmToken.value = token
        }

        // Listen to incoming real-time FCM messages and append to in-app notification inbox
        repositoryScope.launch {
            PaisaFirebaseMessagingService.incomingMessages.collect { fcmMsg ->
                val notif = NotificationItem(
                    id = "NOTIF-${System.currentTimeMillis() % 100000}",
                    title = fcmMsg.title,
                    body = fcmMsg.body,
                    time = "এইমাত্র",
                    type = fcmMsg.type,
                    isRead = false
                )
                _notifications.value = listOf(notif) + _notifications.value
            }
        }
    }

    private fun seedInitialData() {
        repositoryScope.launch {
            // Seed Exchange Rates
            val initialRates = listOf(
                ExchangeRate("SAR", "সৌদি আরব", "﷼", "🇸🇦", 32.45, 1.0, 0.0, 5.0, 50.0, 25000.0, "আজ, দুপুর ১২:৩০", true, 1),
                ExchangeRate("AED", "সংযুক্ত আরব আমিরাত", "د.إ", "🇦🇪", 33.10, 1.0, 0.0, 5.0, 50.0, 25000.0, "আজ, দুপুর ১২:৩০", true, 2),
                ExchangeRate("MYR", "মালয়েশিয়া", "RM", "🇲🇾", 27.60, 1.0, 0.0, 8.0, 50.0, 30000.0, "আজ, দুপুর ১২:৩০", true, 3),
                ExchangeRate("QAR", "কাতার", "ر.ق", "🇶🇦", 33.35, 1.0, 0.0, 5.0, 50.0, 25000.0, "আজ, দুপুর ১২:৩০", true, 4),
                ExchangeRate("OMR", "ওমান", "ر.ع.", "🇴🇲", 315.80, 1.0, 0.0, 1.0, 10.0, 5000.0, "আজ, দুপুর ১২:৩০", true, 5),
                ExchangeRate("KWD", "কুয়েত", "د.ك", "🇰🇼", 396.40, 1.0, 0.0, 1.0, 10.0, 4000.0, "আজ, দুপুর ১২:৩০", true, 6),
                ExchangeRate("BHD", "বাহরাইন", ".د.ب", "🇧🇭", 322.50, 1.0, 0.0, 1.0, 10.0, 5000.0, "আজ, দুপুর ১২:৩০", true, 7),
                ExchangeRate("SGD", "সিঙ্গাপুর", "S$", "🇸🇬", 91.20, 1.0, 0.0, 3.0, 50.0, 15000.0, "আজ, দুপুর ১২:৩০", true, 8),
                ExchangeRate("GBP", "যুক্তরাজ্য (UK)", "£", "🇬🇧", 154.60, 1.0, 0.0, 2.0, 20.0, 10000.0, "আজ, দুপুর ১২:৩০", true, 9),
                ExchangeRate("USD", "যুক্তরাষ্ট্র (USA)", "$", "🇺🇸", 121.50, 1.0, 0.0, 2.0, 20.0, 10000.0, "আজ, দুপুর ১২:৩০", true, 10),
                ExchangeRate("EUR", "ইউরোপীয় ইউনিয়ন", "€", "🇪🇺", 131.80, 1.0, 0.0, 2.0, 20.0, 10000.0, "আজ, দুপুর ১২:৩০", true, 11)
            )
            dao.insertExchangeRates(initialRates)

            // Seed Beneficiaries
            val initialBeneficiaries = listOf(
                Beneficiary("BEN-1", "মোছাঃ রোকেয়া বেগম", "01712984561", "বিকাশ", "বিকাশ পার্সোনাল", "01712984561", "", "কুমিল্লা", "মা", true),
                Beneficiary("BEN-2", "মোঃ রফিকুল ইসলাম", "01819876543", "ব্যাংক অ্যাকাউন্ট", "ইসলামী ব্যাংক বাংলাদেশ লিঃ", "205014502018745", "চকবাজার শাখা", "চট্টগ্রাম", "বাবা", true),
                Beneficiary("BEN-3", "নুসরাত জাহান", "01911223344", "নগদ", "নগদ পার্সোনাল", "01911223344", "", "ঢাকা", "স্ত্রী", true),
                Beneficiary("BEN-4", "মোঃ তারেক হাসান", "01678541236", "ব্যাংক অ্যাকাউন্ট", "ডাচ্-বাংলা ব্যাংক লিঃ", "1151010045892", "মতিঝিল শাখা", "ঢাকা", "ভাই", false)
            )
            initialBeneficiaries.forEach { dao.insertBeneficiary(it) }

            // Seed Payment Methods
            val initialPaymentMethods = listOf(
                PaymentMethod(
                    id = "PM-1",
                    name = "সৌদি ন্যাশনাল ব্যাংক (SNB AlAhli) ট্রান্সফার",
                    shortCode = "SNB",
                    iconType = "BANK",
                    accountName = "Paisa Remit Financial Services Co.",
                    accountNumber = "SA84 1000 0012 3456 7890 1234",
                    instructions = "আপনার ব্যাংক অ্যাপ থেকে উপরের আইবানে নির্ধারিত টাকা পাঠান এবং পেমেন্ট রেফারেন্স হিসাবে আপনার মোবাইল নম্বর দিন।",
                    minLimit = 50.0,
                    maxLimit = 25000.0,
                    requiresProofUpload = true,
                    isActive = true,
                    displayOrder = 1
                ),
                PaymentMethod(
                    id = "PM-2",
                    name = "আল রাজি ব্যাংক (Al Rajhi Bank) ট্রান্সফার",
                    shortCode = "ALRAJHI",
                    iconType = "BANK",
                    accountName = "Paisa Remit KSA LLC",
                    accountNumber = "SA42 8000 0234 5678 9012 3456",
                    instructions = "আল রাজি অ্যাপ থেকে দ্রুত ট্রান্সফার করুন। রেফারেন্সে ট্রানজেকশন আইডি প্রদান করুন।",
                    minLimit = 50.0,
                    maxLimit = 25000.0,
                    requiresProofUpload = true,
                    isActive = true,
                    displayOrder = 2
                ),
                PaymentMethod(
                    id = "PM-3",
                    name = "এসটিসি পে (stc pay) ইনস্ট্যান্ট ট্রান্সফার",
                    shortCode = "STCPAY",
                    iconType = "BKASH",
                    accountName = "Paisa Remit stc merchant",
                    accountNumber = "0501234567",
                    instructions = "stc pay মার্চেন্ট পেমেন্টে টাকা পাঠিয়ে ট্রান্সফার রসিদ আপলোড করুন।",
                    minLimit = 50.0,
                    maxLimit = 10000.0,
                    requiresProofUpload = false,
                    isActive = true,
                    displayOrder = 3
                ),
                PaymentMethod(
                    id = "PM-4",
                    name = "ইউআরপে (Urpay) ডিজিটাল ওয়ালেট",
                    shortCode = "URPAY",
                    iconType = "NAGAD",
                    accountName = "Paisa Remit Urpay ID",
                    accountNumber = "URP-992144",
                    instructions = "Urpay অ্যাপের মাধ্যমে সরাসরি মার্চেন্ট স্ক্যান করে পেমেন্ট করুন।",
                    minLimit = 50.0,
                    maxLimit = 10000.0,
                    requiresProofUpload = false,
                    isActive = true,
                    displayOrder = 4
                )
            )
            dao.insertPaymentMethods(initialPaymentMethods)

            // Seed Offers
            val initialOffers = listOf(
                Offer("OFF-1", "PAISANEW", "নতুন ব্যবহারকারী অফার - ০ ফি!", "প্রথম রেমিট্যান্সে সম্পূর্ণ ফ্রি সার্ভিস চার্জ। কোনো লুকানো ফি নেই!", 100.0, 15.0, 100.0, "৩১ ডিসেম্বর ২০২৬", "০ ফি স্পেশাল", true),
                Offer("OFF-2", "PROBASHI50", "প্রবাসীদের জন্য ৫০% ফি ছাড়", "যেকোনো ৫০০ রিয়ালের বেশি পাঠালে সার্ভিস ফিতে ফ্ল্যাট ৫০% ইনস্ট্যান্ট ছাড় পান।", 50.0, 10.0, 500.0, "১৫ নভেম্বর ২০২৬", "৫০% ছাড়", true),
                Offer("OFF-3", "BONUS2026", "৳৫০০ বোনাস ক্যাশব্যাক ভাউচার", "৳১,০০,০০০ টাকার বেশি ব্যাংকিং চ্যানেলে পাঠালে বাংলাদেশে প্রাপকের জন্য ৳৫০০ বোনাস।", 0.0, 0.0, 3000.0, "৩০ অক্টোবর ২০২৬", "৳৫০০ বোনাস", true)
            )
            dao.insertOffers(initialOffers)

            // Seed Notices
            val initialNotices = listOf(
                Notice("NTC-1", "সরকারি ২.৫% নগদ প্রণোদনা অব্যাহত রয়েছে", "প্রবাসী বাংলাদেশীদের পাঠানো বৈধ চ্যানেলের রেমিট্যান্সে সরাসরি ২.৫% নগদ সরকারি প্রণোদনা যুক্ত হচ্ছে।", "গুরুত্বপূর্ণ", "২২ আগস্ট ২০২৬", true, false),
                Notice("NTC-2", "বিকাশ ও নগদ রেমিট্যান্স মুহূর্তেই পৌঁছে যাচ্ছে", "এখন থেকে শুক্রবার ও ছুটির দিনেও বিকাশ ও নগদ ওয়ালেটে ২৪ ঘণ্টা ইনস্ট্যান্ট টাকা বিতরণ কার্যকর।", "লেনদেন", "২০ আগস্ট ২০২৬", false, false),
                Notice("NTC-3", "আপনার কেওয়াইসি আপডেট রাখুন", "বাংলাদেশ ব্যাংক ও আন্তর্জাতিক অ্যান্টি-মানি লন্ডারিং নির্দেশনা অনুযায়ী আপনার পাসপোর্ট/আইডি মেয়াদ যাচাই করুন।", "কেওয়াইসি", "১৮ আগস্ট ২০২৬", false, true)
            )
            dao.insertNotices(initialNotices)

            // Seed Initial Transactions
            val initialTransactions = listOf(
                RemittanceTransaction(
                    id = "RM-2026-894210",
                    senderName = "তানভীর আহমেদ",
                    senderPhone = "+966 50 123 4567",
                    recipientName = "মোছাঃ রোকেয়া বেগম",
                    recipientPhone = "01712984561",
                    recipientChannel = "বিকাশ (bKash)",
                    recipientAccount = "01712984561",
                    sendAmount = 2000.0,
                    sendCurrency = "SAR",
                    exchangeRate = 32.45,
                    serviceFee = 5.0,
                    discount = 5.0,
                    totalPayable = 2000.0,
                    recipientBdtAmount = 64900.0,
                    govtIncentiveBdt = 1622.50,
                    paymentMethodName = "আল রাজি ব্যাংক (Al Rajhi)",
                    paymentAccountInfo = "SA42 8000 0234 5678",
                    status = TransactionStatus.COMPLETED,
                    createdAt = "২২ আগস্ট ২০২৬, সকাল ১০:৩০",
                    completedAt = "২২ আগস্ট ২০২৬, সকাল ১০:৪৫",
                    receiptId = "REC-894210",
                    idempotencyKey = "IDEM-894210",
                    adminNote = "অটোমেটেড সুইফট গেটওয়ে দ্বারা প্রক্রিয়া সম্পন্ন।",
                    statusTimeline = listOf(
                        TimelineEvent("আবেদন গৃহীত", "লেনদেন আবেদন সফলভাবে তৈরি হয়েছে", "১০:৩০ AM", true),
                        TimelineEvent("পেমেন্ট যাচাইকৃত", "ব্যাংক পেমেন্ট প্রাপ্তি নিশ্চিত হয়েছে", "১০:৩৫ AM", true),
                        TimelineEvent("প্রক্রিয়াকরণ", "বাংলাদেশ ব্যাংকে সুইফট বার্তা প্রেরণ", "১০:৪০ AM", true),
                        TimelineEvent("সম্পন্ন", "প্রাপকের বিকাশ অ্যাকাউন্টে ৳৬৬,৫২২.৫০ ক্রেডিট হয়েছে", "১০:৪৫ AM", true)
                    )
                ),
                RemittanceTransaction(
                    id = "RM-2026-783109",
                    senderName = "তানভীর আহমেদ",
                    senderPhone = "+966 50 123 4567",
                    recipientName = "মোঃ রফিকুল ইসলাম",
                    recipientPhone = "01819876543",
                    recipientChannel = "ইসলামী ব্যাংক বাংলাদেশ লিঃ",
                    recipientAccount = "205014502018745",
                    sendAmount = 4500.0,
                    sendCurrency = "SAR",
                    exchangeRate = 32.40,
                    serviceFee = 5.0,
                    discount = 0.0,
                    totalPayable = 4505.0,
                    recipientBdtAmount = 145800.0,
                    govtIncentiveBdt = 3645.0,
                    paymentMethodName = "সৌদি ন্যাশনাল ব্যাংক (SNB)",
                    paymentAccountInfo = "SA84 1000 0012 3456",
                    status = TransactionStatus.PROCESSING,
                    createdAt = "২২ আগস্ট ২০২৬, সকাল ১১:১৫",
                    completedAt = null,
                    receiptId = "REC-783109",
                    idempotencyKey = "IDEM-783109",
                    adminNote = "ব্যাংকিং চ্যানেলে ক্লিয়ারেন্স প্রক্রিয়াধীন।",
                    statusTimeline = listOf(
                        TimelineEvent("আবেদন গৃহীত", "লেনদেন আবেদন সফলভাবে তৈরি হয়েছে", "১১:১৫ AM", true),
                        TimelineEvent("পেমেন্ট যাচাইকৃত", "SNB ব্যাংক পেমেন্ট নিশ্চিত হয়েছে", "১১:২০ AM", true),
                        TimelineEvent("প্রক্রিয়াকরণ", "ইসলামী ব্যাংকে সরাসরি ট্রান্সফার প্রক্রিয়াধীন", "১১:২৫ AM", true),
                        TimelineEvent("সম্পন্ন", "প্রাপকের ব্যাংক অ্যাকাউন্টে ক্রেডিট হবে", "প্রক্রিয়াধীন...", false)
                    )
                ),
                RemittanceTransaction(
                    id = "RM-2026-651920",
                    senderName = "তানভীর আহমেদ",
                    senderPhone = "+966 50 123 4567",
                    recipientName = "নুসরাত জাহান",
                    recipientPhone = "01911223344",
                    recipientChannel = "নগদ (Nagad)",
                    recipientAccount = "01911223344",
                    sendAmount = 1500.0,
                    sendCurrency = "SAR",
                    exchangeRate = 32.35,
                    serviceFee = 5.0,
                    discount = 2.5,
                    totalPayable = 1502.5,
                    recipientBdtAmount = 48525.0,
                    govtIncentiveBdt = 1213.12,
                    paymentMethodName = "এসটিসি পে (stc pay)",
                    paymentAccountInfo = "0501234567",
                    status = TransactionStatus.COMPLETED,
                    createdAt = "১৮ আগস্ট ২০২৬, সন্ধ্যা ৭:২০",
                    completedAt = "১৮ আগস্ট ২০২৬, সন্ধ্যা ৭:২৫",
                    receiptId = "REC-651920",
                    idempotencyKey = "IDEM-651920",
                    adminNote = "সফল লেনদেন।",
                    statusTimeline = listOf(
                        TimelineEvent("আবেদন গৃহীত", "লেনদেন তৈরি হয়েছে", "০৭:২০ PM", true),
                        TimelineEvent("সম্পন্ন", "নগদ অ্যাকাউন্টে জমা হয়েছে", "০৭:২৫ PM", true)
                    )
                )
            )
            dao.insertTransactions(initialTransactions)

            // Seed Devices
            val initialDevices = listOf(
                RegisteredDevice("DEV-A24-01", "Samsung Galaxy S24 Ultra", "Android 15 (API 35)", "v1.0.0", "156.194.22.45 (Riyadh, SA)", "এখন সক্রিয় (বর্তমান ডিভাইস)", DeviceStatus.TRUSTED, true, "২০২৪-০৩-১৫ ১০:৩০"),
                RegisteredDevice("DEV-MI-02", "Xiaomi Redmi Note 13 Pro", "Android 14 (API 34)", "v0.9.8", "182.160.10.12 (Dhaka, BD)", "৫ দিন আগে", DeviceStatus.TRUSTED, false, "২০২৪-০২-১০ ১৪:২০")
            )
            dao.insertDevices(initialDevices)
        }
    }

    // Live Flow Queries
    fun getExchangeRates(): Flow<List<ExchangeRate>> = dao.getAllExchangeRates()
    fun getBeneficiaries(): Flow<List<Beneficiary>> = dao.getAllBeneficiaries()
    fun getTransactions(): Flow<List<RemittanceTransaction>> = dao.getAllTransactions()
    fun getPaymentMethods(): Flow<List<PaymentMethod>> = dao.getAllPaymentMethods()
    fun getOffers(): Flow<List<Offer>> = dao.getAllOffers()
    fun getNotices(): Flow<List<Notice>> = dao.getAllNotices()
    fun getDevices(): Flow<List<RegisteredDevice>> = dao.getAllDevices()

    // Calculate Quote with Server-Side Business Rules
    fun calculateQuote(
        sendAmount: Double,
        currencyCode: String,
        currentRates: List<ExchangeRate>,
        promoCode: String?
    ): RemittanceQuote {
        val rateObj = currentRates.find { it.currencyCode == currencyCode }
            ?: currentRates.firstOrNull()
            ?: ExchangeRate("SAR", "সৌদি আরব", "﷼", "🇸🇦", 32.45)

        val rate = rateObj.sendingRate
        var flatFee = rateObj.flatFee
        var discount = 0.0

        if (!promoCode.isNullOrBlank()) {
            if (promoCode.equals("PAISANEW", ignoreCase = true)) {
                discount = flatFee
            } else if (promoCode.equals("PROBASHI50", ignoreCase = true)) {
                discount = flatFee * 0.5
            }
        }

        val totalPayable = sendAmount + (flatFee - discount).coerceAtLeast(0.0)
        val rawBdt = sendAmount * rate
        val govtIncentive = rawBdt * _systemSettings.value.govtIncentiveRate
        val recipientTotalBdt = rawBdt + govtIncentive

        return RemittanceQuote(
            sendAmount = sendAmount,
            sendCurrency = currencyCode,
            rate = rate,
            serviceFee = flatFee,
            discountAmount = discount,
            couponApplied = if (discount > 0) promoCode else null,
            totalPayable = totalPayable,
            recipientBdtAmount = recipientTotalBdt,
            govtIncentiveBdt = govtIncentive
        )
    }

    // Create Remittance Transaction
    suspend fun createTransaction(
        beneficiary: Beneficiary,
        quote: RemittanceQuote,
        paymentMethod: PaymentMethod,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): RemittanceTransaction {
        val formatter = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("bn", "BD"))
        val currentTimeString = formatter.format(Date())
        val randomRef = "RM-2026-" + (100000 + (Math.random() * 900000).toInt())

        val newTx = RemittanceTransaction(
            id = randomRef,
            senderName = _currentUser.value.name,
            senderPhone = _currentUser.value.phone,
            recipientName = beneficiary.name,
            recipientPhone = beneficiary.phone,
            recipientChannel = "${beneficiary.channelType} (${beneficiary.accountOrBankName})",
            recipientAccount = beneficiary.accountNumber,
            sendAmount = quote.sendAmount,
            sendCurrency = quote.sendCurrency,
            exchangeRate = quote.rate,
            serviceFee = quote.serviceFee,
            discount = quote.discountAmount,
            totalPayable = quote.totalPayable,
            recipientBdtAmount = quote.recipientBdtAmount,
            govtIncentiveBdt = quote.govtIncentiveBdt,
            paymentMethodName = paymentMethod.name,
            paymentAccountInfo = paymentMethod.accountNumber,
            status = TransactionStatus.PENDING,
            createdAt = currentTimeString,
            completedAt = null,
            receiptId = "REC-" + (100000 + (Math.random() * 900000).toInt()),
            idempotencyKey = idempotencyKey,
            adminNote = "লেনদেন অপেক্ষমাণ আছে। পেমেন্ট যাচাই করা হচ্ছে।",
            statusTimeline = listOf(
                TimelineEvent("আবেদন তৈরি", "রেমিট্যান্স আবেদন গৃহীত হয়েছে", "এইমাত্র", true),
                TimelineEvent("পেমেন্ট যাচাই", "ব্যাংক ট্রান্সফার যাচাই প্রক্রিয়াধীন", "চলমান", false),
                TimelineEvent("বাংলাদেশ ব্যাংকে সুইফট বার্তা", "সরাসরি ব্যাংক/ওয়ালেটে পাঠানো হবে", "অপেক্ষমাণ", false),
                TimelineEvent("সম্পন্ন", "প্রাপক টাকা পাবেন", "অপেক্ষমাণ", false)
            )
        )

        dao.insertTransaction(newTx)

        // Update user state stats
        _currentUser.value = _currentUser.value.copy(
            totalTransferredBdt = _currentUser.value.totalTransferredBdt + quote.recipientBdtAmount,
            totalTransactionsCount = _currentUser.value.totalTransactionsCount + 1
        )

        // 1. Post Real-Time FCM Transfer Confirmation Alert
        val notifTitle = "রেমিট্যান্স ট্রান্সফার সফলভাবে তৈরি হয়েছে!"
        val notifBody = "আপনার ৳${String.format(Locale.US, "%,.2f", quote.recipientBdtAmount)} পাঠানোর আবেদনটি (${newTx.id}) সফলভাবে গৃহীত হয়েছে।"
        dispatchFcmAlert(
            title = notifTitle,
            body = notifBody,
            type = "TRANSACTION",
            txId = newTx.id
        )

        return newTx
    }

    suspend fun saveBeneficiary(beneficiary: Beneficiary) {
        dao.insertBeneficiary(beneficiary)
    }

    suspend fun deleteBeneficiary(id: String) {
        dao.deleteBeneficiary(id)
    }

    suspend fun updateKyc(status: KycStatus) {
        _currentUser.value = _currentUser.value.copy(kycStatus = status)
        dispatchFcmAlert(
            title = "কেওয়াইসি ভেরিফিকেশন আপডেট",
            body = "আপনার পরিচয়পত্র যাচাই স্ট্যাটাস: ${status.banglaText}",
            type = "KYC"
        )
    }

    suspend fun updateDeviceConsent(consent: Boolean) {
        _currentUser.value = _currentUser.value.copy(deviceConsentGiven = consent)
    }

    suspend fun toggleBiometric(enabled: Boolean) {
        _currentUser.value = _currentUser.value.copy(biometricEnabled = enabled)
    }

    suspend fun revokeDevice(deviceId: String) {
        dao.updateDeviceStatus(deviceId, DeviceStatus.REVOKED)
    }

    fun submitSupportTicket(subject: String, initialMessage: String, txRef: String?) {
        submitSupportRequest(
            subject = subject,
            message = initialMessage,
            category = "রেমিট্যান্স লেনদেন",
            priority = "সাধারণ",
            txRef = txRef,
            attachmentName = null
        )
    }

    fun submitSupportRequest(
        subject: String,
        message: String,
        category: String,
        priority: String,
        txRef: String?,
        attachmentName: String?
    ): SupportTicket {
        val user = _currentUser.value
        val newTicketId = "TCK-" + (1000 + (Math.random() * 9000).toInt())
        val timeString = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("bn", "BD")).format(Date())
        val initialMsg = TicketMessage(
            senderName = user.name,
            isAgent = false,
            message = message,
            timestamp = "এইমাত্র",
            linkedTxId = txRef
        )
        val newTicket = SupportTicket(
            id = newTicketId,
            subject = subject,
            transactionRef = txRef,
            category = category,
            priority = priority,
            status = "খোলা",
            createdAt = timeString,
            linkedUserId = user.id,
            attachmentName = attachmentName,
            messages = listOf(initialMsg)
        )
        _supportTickets.value = listOf(newTicket) + _supportTickets.value

        // Dispatch alert confirmation
        dispatchFcmAlert(
            title = "সাপোর্ট রিকোয়েস্ট গৃহীত হয়েছে (#$newTicketId)",
            body = "আপনার অ্যাকাউন্ট (${user.id}) লিঙ্কযুক্ত সহায়তা আবেদনটি সাপোর্ট টিমের নিকট পৌঁছেছে। শীঘ্রই সমাধান জানানো হবে।",
            type = "SUPPORT"
        )

        // Simulate Support Agent Auto-acknowledgement & investigation
        repositoryScope.launch {
            delay(2000)
            val agentResponse = when {
                txRef != null -> "ধন্যবাদ ${user.name}। আপনার ট্রানজেকশন #${txRef} এবং অ্যাকাউন্ট (${user.id}) রেকর্ড যাচাই করছি। আমাদের সিনিয়র অফিসার শীঘ্রই আপডেট দিচ্ছেন।"
                category.contains("কেওয়াইসি") -> "ধন্যবাদ ${user.name}। আপনার কেওয়াইসি প্রোফাইল (${user.kycStatus.banglaText}) পর্যালোচনায় নেওয়া হয়েছে।"
                else -> "ধন্যবাদ ${user.name}। আপনার সমস্যাটি #${newTicketId} টিকেট হিসেবে তালিকাভুক্ত করা হয়েছে। আমাদের কাস্টমার কেয়ার প্রতিনিধি সক্রিয়ভাবে কাজ করছেন।"
            }
            sendTicketReplyInternal(newTicketId, agentResponse, isAgent = true, agentName = "পয়সা সাপোর্ট (লাইভ ডেস্ক)")
        }

        return newTicket
    }

    fun sendTicketReply(ticketId: String, replyMessage: String) {
        val user = _currentUser.value
        sendTicketReplyInternal(ticketId, replyMessage, isAgent = false, agentName = user.name)

        // Trigger agent response in thread
        repositoryScope.launch {
            delay(1800)
            val autoReply = "ধন্যবাদ তানভীর সাহেব। আপনার বার্তাটি পেয়েছি, আমরা দ্রুত ব্যবস্থা নিচ্ছি।"
            sendTicketReplyInternal(ticketId, autoReply, isAgent = true, agentName = "পয়সা সাপোর্ট (অন-ডিউটি)")
        }
    }

    private fun sendTicketReplyInternal(
        ticketId: String,
        message: String,
        isAgent: Boolean,
        agentName: String
    ) {
        val timeNow = SimpleDateFormat("hh:mm a", Locale("bn", "BD")).format(Date())
        _supportTickets.value = _supportTickets.value.map { ticket ->
            if (ticket.id == ticketId) {
                val updatedMessages = ticket.messages + TicketMessage(
                    senderName = agentName,
                    isAgent = isAgent,
                    message = message,
                    timestamp = timeNow
                )
                ticket.copy(
                    status = if (isAgent) "উত্তর দেওয়া হয়েছে" else "প্রক্রিয়াধীন",
                    messages = updatedMessages
                )
            } else {
                ticket
            }
        }
    }

    fun sendLiveChatMessage(messageText: String, linkedTxId: String? = null) {
        if (messageText.isBlank()) return
        val user = _currentUser.value
        val timeNow = SimpleDateFormat("hh:mm a", Locale("bn", "BD")).format(Date())
        val userMsg = TicketMessage(
            senderName = user.name,
            isAgent = false,
            message = messageText,
            timestamp = timeNow,
            linkedTxId = linkedTxId
        )
        _liveChatMessages.value = _liveChatMessages.value + userMsg

        // Trigger Agent response with typing animation
        repositoryScope.launch {
            _isAgentTyping.value = true
            delay(1500)
            _isAgentTyping.value = false

            val replyText = generateLiveAgentReply(messageText, linkedTxId, user)
            val agentMsg = TicketMessage(
                senderName = "পয়সা সাপোর্ট (লাইভ ডেস্ক)",
                isAgent = true,
                message = replyText,
                timestamp = SimpleDateFormat("hh:mm a", Locale("bn", "BD")).format(Date())
            )
            _liveChatMessages.value = _liveChatMessages.value + agentMsg
        }
    }

    private fun generateLiveAgentReply(userMsg: String, linkedTxId: String?, user: User): String {
        val lower = userMsg.lowercase()
        return when {
            linkedTxId != null -> {
                "ধন্যবাদ ${user.name}। আপনার অ্যাকাউন্ট (${user.id}) এবং ট্রানজেকশন #${linkedTxId} সিস্টেম ডাটাবেজে ট্র্যাক করা হয়েছে। বাংলাদেশ ব্যাংক গেটওয়ে ও সংশ্লিষ্ট ব্যাংকে ফান্ড ক্লিয়ারেন্স সম্পন্ন হচ্ছে।"
            }
            lower.contains("দেরি") || lower.contains("পৌঁছায়নি") || lower.contains("পায়নি") || lower.contains("সময়") -> {
                "বিকাশ/নগদে সাধারণত ১-৩ মিনিটে টাকা জমা হয়। যদি কোনো ব্যাংক ডিপোজিটে বিলম্ব ঘটে তবে গ্রাহকের অ্যাকাউন্ট নম্বর ও ট্রানজেকশন আইডি আমাদের নিশ্চিত করুন, আমরা সরাসরি বাংলাদেশ ব্যাংক এনপিএসবি গেটওয়েতে রিকুয়েস্ট পাঠাচ্ছি।"
            }
            lower.contains("কেওয়াইসি") || lower.contains("kyc") || lower.contains("ডকুমেন্ট") || lower.contains("ভেরিফাই") -> {
                "আপনার বর্তমান কেওয়াইসি স্ট্যাটাস: ${user.kycStatus.banglaText}। পাসপোর্ট বা এনআইডির রঙিন ছবি এবং লাইভ সেলফি সাবমিট করলেই ৫-১৫ মিনিটের মধ্যে অনুমোদন সম্পন্ন হয়।"
            }
            lower.contains("প্রণোদনা") || lower.contains("বোনাস") || lower.contains("২.৫") || lower.contains("রেট") -> {
                "বাংলাদেশ সরকারের বৈধ রেমিট্যান্সের উপর ২.৫% সরকারি প্রণোদনা প্রতি ট্রানজেকশনে মূল টাকার সাথে স্বয়ংক্রিয়ভাবে সরাসরি যোগ হচ্ছে। কোনো এক্সট্রা চার্জ বা কর্তন নেই।"
            }
            lower.contains("লিমিট") || lower.contains("টাকা") -> {
                "আপনার অ্যাকাউন্টে (${user.id}) দৈনিক সর্বোচ্চ ৫,০০,০০০ টাকা এবং প্রতি ট্রানজেকশনে ৩,০০,০০০ টাকা পাঠানোর অনুমোদন রয়েছে।"
            }
            else -> {
                "ধন্যবাদ ${user.name} (আইডি: ${user.id})। আপনার বার্তাটি রেকর্ড করা হয়েছে। যেকোনো তথ্যের জন্য আমাদের ২৪/৭ সাপোর্ট টিম আপনার সাথে আছে।"
            }
        }
    }

    // Mark notifications
    fun markAllNotificationsAsRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
    }

    fun markNotificationAsRead(id: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    /**
     * Dispatches a real-time FCM notification both as an Android native system notification
     * and persists it into the app's in-app notification inbox.
     */
    fun dispatchFcmAlert(
        title: String,
        body: String,
        type: String = "TRANSACTION",
        txId: String? = null
    ) {
        // Show Android Heads-up system notification
        FcmNotificationManager.showSystemNotification(
            context = context,
            title = title,
            body = body,
            type = type,
            transactionId = txId
        )

        // Insert into live notifications flow
        val newNotif = NotificationItem(
            id = "NOTIF-${System.currentTimeMillis() % 1000000}",
            title = title,
            body = body,
            time = "এইমাত্র",
            type = type,
            isRead = false
        )
        _notifications.value = listOf(newNotif) + _notifications.value
    }

    // Admin Operations (Live for instant testing & management)
    suspend fun adminUpdateRate(currencyCode: String, newRate: Double, newFee: Double) {
        val currentRates = listOf(
            ExchangeRate(currencyCode, "দেশ", "", "", newRate, 1.0, 0.0, newFee, 50.0, 25000.0, "এইমাত্র আপডেট", true, 1)
        )
        dao.insertExchangeRates(currentRates)
        addAuditLog("এক্সচেঞ্জ রেট আপডেট", "ExchangeRate", currencyCode, "নতুন রেট: ৳$newRate, ফি: $newFee")
    }

    suspend fun adminUpdateTransactionStatus(txId: String, status: TransactionStatus, adminNote: String) {
        dao.updateTransactionStatus(txId, status)
        addAuditLog("ট্রানজেকশন স্ট্যাটাস পরিবর্তন", "Transaction", txId, "স্ট্যাটাস পরিবর্তন: ${status.banglaText}. নোট: $adminNote")

        // Dispatch Real-time FCM Status Alert to User
        val statusTitle = when (status) {
            TransactionStatus.COMPLETED -> "টাকা সফলভাবে জমা হয়েছে! 🎉"
            TransactionStatus.PROCESSING -> "রেমিট্যান্স প্রক্রিয়াধীন আছে ⏳"
            TransactionStatus.CANCELLED -> "রেমিট্যান্স বাতিল হয়েছে ⚠️"
            TransactionStatus.PENDING -> "রেমিট্যান্স অনুমোদনের অপেক্ষায় 📝"
            TransactionStatus.APPROVED -> "রেমিট্যান্স অনুমোদিত হয়েছে ✓"
            TransactionStatus.REJECTED -> "রেমিট্যান্স প্রত্যাখ্যান করা হয়েছে ✕"
            TransactionStatus.REFUND_REQUESTED -> "রিফান্ড আবেদন প্রক্রিয়াধীন 🔄"
            TransactionStatus.REFUNDED -> "টাকা রিফান্ড করা হয়েছে 💳"
            else -> "রেমিট্যান্স স্ট্যাটাস আপডেট: ${status.banglaText}"
        }
        val statusBody = "ট্রানজেকশন #${txId}: স্ট্যাটাস বর্তমানে '${status.banglaText}'। $adminNote"
        dispatchFcmAlert(
            title = statusTitle,
            body = statusBody,
            type = "STATUS",
            txId = txId
        )
    }

    suspend fun submitKycVerification(
        documentType: KycDocType,
        documentNumber: String,
        fullNameOnDoc: String,
        dateOfBirth: String,
        issuingCountry: String,
        expiryDate: String,
        frontDocMeta: KycMediaMeta,
        backDocMeta: KycMediaMeta?,
        workPermitDocMeta: KycMediaMeta? = null,
        selfieType: KycSelfieType,
        selfieMediaMeta: KycMediaMeta,
        deviceModel: String = "Samsung Galaxy S24 Ultra",
        ipAddress: String = "185.190.142.66",
        gpsCoordinates: String = "24.7136° N, 46.6753° E (Riyadh, KSA)"
    ): KycSubmissionRecord {
        val user = _currentUser.value
        val newKycId = "KYC-2026-" + (10000 + (Math.random() * 90000).toInt())
        val timeNow = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("bn", "BD")).format(Date())

        val submission = KycSubmissionRecord(
            id = newKycId,
            userId = user.id,
            userName = user.name,
            userPhone = user.phone,
            userEmail = user.email,
            documentType = documentType,
            documentNumber = documentNumber,
            fullNameOnDoc = fullNameOnDoc,
            dateOfBirth = dateOfBirth,
            issuingCountry = issuingCountry,
            expiryDate = expiryDate,
            frontDocMeta = frontDocMeta,
            backDocMeta = backDocMeta,
            workPermitDocMeta = workPermitDocMeta,
            selfieType = selfieType,
            selfieMediaMeta = selfieMediaMeta,
            deviceModel = deviceModel,
            ipAddress = ipAddress,
            gpsCoordinates = gpsCoordinates,
            submittedAt = timeNow,
            status = KycStatus.PENDING,
            adminNotes = "নতুন কেওয়াইসি সাবমিশন। ডকুমেন্ট এবং সেলফি মেটাডেটা সংরক্ষিত।"
        )

        // Prepend to submissions list
        _kycSubmissions.value = listOf(submission) + _kycSubmissions.value

        // Update current user state to PENDING
        _currentUser.value = _currentUser.value.copy(kycStatus = KycStatus.PENDING)

        // Audit Log
        addAuditLog(
            action = "কেওয়াইসি ভেরিফিকেশন সাবমিশন",
            targetType = "KYC",
            targetId = newKycId,
            notes = "গ্রাহক ${user.name} (${user.id}) ${documentType.banglaTitle} ও ${selfieType.banglaTitle} মেটাডেটা সহ জমা দিয়েছেন।"
        )

        // Dispatch Confirmation Notification
        dispatchFcmAlert(
            title = "কেওয়াইসি ডকুমেন্ট জমা সফল হয়েছে! 📄",
            body = "আপনার ${documentType.banglaTitle} এবং সেলফি মেটাডেটা অ্যাডমিন অনুমোদনের জন্য সফলভাবে সংরক্ষিত হয়েছে (আইডি: $newKycId)।",
            type = "KYC"
        )

        return submission
    }

    suspend fun adminApproveKycRecord(submissionId: String, notes: String) {
        val timeNow = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("bn", "BD")).format(Date())
        var targetUserId = _currentUser.value.id

        _kycSubmissions.value = _kycSubmissions.value.map { record ->
            if (record.id == submissionId) {
                targetUserId = record.userId
                record.copy(
                    status = KycStatus.VERIFIED,
                    adminNotes = notes.ifBlank { "ডকুমেন্ট ও সেলফি নিখুঁতভাবে যাচাইকৃত ও অনুমোদিত।" },
                    reviewedBy = "সিনিয়র কমপ্লায়েন্স অফিসার",
                    reviewedAt = timeNow
                )
            } else {
                record
            }
        }

        if (targetUserId == _currentUser.value.id) {
            _currentUser.value = _currentUser.value.copy(kycStatus = KycStatus.VERIFIED)
        }

        addAuditLog("কেওয়াইসি অনুমোদন", "KYC", submissionId, "কেওয়াইসি #$submissionId অনুমোদিত। নোট: $notes")

        dispatchFcmAlert(
            title = "কেওয়াইসি সফলভাবে অনুমোদিত! 🎉",
            body = "অভিনন্দন! আপনার পরিচয়পত্র ও সেলফি মেটাডেটা যাচাই সম্পন্ন হয়েছে। আপনার অ্যাকাউন্ট এখন সম্পূর্ণ যাচাইকৃত (Verified)।",
            type = "KYC"
        )
    }

    suspend fun adminRejectKycRecord(submissionId: String, reason: String) {
        val timeNow = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("bn", "BD")).format(Date())
        var targetUserId = _currentUser.value.id

        _kycSubmissions.value = _kycSubmissions.value.map { record ->
            if (record.id == submissionId) {
                targetUserId = record.userId
                record.copy(
                    status = KycStatus.REJECTED,
                    adminNotes = reason.ifBlank { "প্রদত্ত ছবি অস্পষ্ট বা ডকুমেন্টের তথ্য অমিল।" },
                    reviewedBy = "সিনিয়র কমপ্লায়েন্স অফিসার",
                    reviewedAt = timeNow
                )
            } else {
                record
            }
        }

        if (targetUserId == _currentUser.value.id) {
            _currentUser.value = _currentUser.value.copy(kycStatus = KycStatus.REJECTED)
        }

        addAuditLog("কেওয়াইসি প্রত্যাখ্যান", "KYC", submissionId, "কেওয়াইসি #$submissionId প্রত্যাখ্যাত। কারণ: $reason")

        dispatchFcmAlert(
            title = "কেওয়াইসি আবেদন প্রত্যাখ্যাত ✕",
            body = "আপনার কেওয়াইসি আবেদনটি প্রত্যাখ্যাত হয়েছে। কারণ: ${reason.ifBlank { "ডকুমেন্ট অস্পষ্ট অথবা তথ্যের গরমিল" }}। দয়া করে পুনরায় সঠিক তথ্য জমা দিন।",
            type = "KYC"
        )
    }

    suspend fun adminRequestKycResubmission(submissionId: String, guidance: String) {
        val timeNow = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("bn", "BD")).format(Date())
        var targetUserId = _currentUser.value.id

        _kycSubmissions.value = _kycSubmissions.value.map { record ->
            if (record.id == submissionId) {
                targetUserId = record.userId
                record.copy(
                    status = KycStatus.RESUBMISSION_REQUIRED,
                    adminNotes = guidance.ifBlank { "ডকুমেন্টের উভয় পিঠ ও সেলফি ভিডিও পরিষ্কার আলোতে পুনরায় তুলুন।" },
                    reviewedBy = "সিনিয়র কমপ্লায়েন্স অফিসার",
                    reviewedAt = timeNow
                )
            } else {
                record
            }
        }

        if (targetUserId == _currentUser.value.id) {
            _currentUser.value = _currentUser.value.copy(kycStatus = KycStatus.RESUBMISSION_REQUIRED)
        }

        addAuditLog("কেওয়াইসি রি-সাবমিশন রিকোয়েস্ট", "KYC", submissionId, "পুনরায় জমার অনুরোধ: $guidance")

        dispatchFcmAlert(
            title = "কেওয়াইসি পুনরায় জমা দিন ⚠️",
            body = "আপনার কেওয়াইসি ফাইলে সংশোধনী প্রয়োজন: ${guidance.ifBlank { "দয়া করে স্পষ্ট ছবি বা সেলফি ভিডিও আপলোড করুন" }}।",
            type = "KYC"
        )
    }

    suspend fun adminUpdateCustomerKyc(status: KycStatus) {
        _currentUser.value = _currentUser.value.copy(kycStatus = status)
        addAuditLog("গ্রাহক কেওয়াইসি পরিবর্তন", "KYC", _currentUser.value.id, "নতুন কেওয়াইসি স্ট্যাটাস: ${status.banglaText}")
        dispatchFcmAlert(
            title = "কেওয়াইসি অনুমোদন স্ট্যাটাস",
            body = "অ্যাডমিন প্যানেল থেকে আপনার KYC অ্যাকাউন্ট স্ট্যাটাস '${status.banglaText}' করা হয়েছে।",
            type = "KYC"
        )
    }

    fun adminToggleMaintenance(enabled: Boolean) {
        _systemSettings.value = _systemSettings.value.copy(maintenanceMode = enabled)
        addAuditLog("রক্ষণাবেক্ষণ মোড পরিবর্তন", "Settings", "MAINTENANCE", "Maintenance Mode: $enabled")
    }

    private fun addAuditLog(action: String, targetType: String, targetId: String, notes: String) {
        val log = AdminAuditLog(
            id = "AUD-" + (100 + (Math.random() * 900).toInt()),
            adminName = "অ্যাডমিন (ডেভেলপার কনসোল)",
            action = action,
            targetType = targetType,
            targetId = targetId,
            timestamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("bn", "BD")).format(Date()),
            notes = notes
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
    }
}
