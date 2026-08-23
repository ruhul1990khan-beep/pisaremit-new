package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AccountStatus(val banglaText: String) {
    ACTIVE("সক্রিয়"),
    SUSPENDED("স্থগিত"),
    BLOCKED("ব্লকড"),
    KYC_PENDING("কেওয়াইসি অপেক্ষমাণ")
}

enum class KycStatus(val banglaText: String) {
    NOT_SUBMITTED("জমা দেওয়া হয়নি"),
    PENDING("যাচাই প্রক্রিয়াধীন"),
    VERIFIED("যাচাইকৃত"),
    REJECTED("প্রত্যাখ্যাত"),
    RESUBMISSION_REQUIRED("পুনরায় জমা দিতে হবে")
}

enum class TransactionStatus(val banglaText: String, val colorHex: Long) {
    PENDING("অপেক্ষমাণ", 0xFFF59E0B),
    PROCESSING("প্রক্রিয়াধীন", 0xFF2563EB),
    APPROVED("অনুমোদিত", 0xFF0D9488),
    COMPLETED("সম্পন্ন হয়েছে", 0xFF16A34A),
    REJECTED("বাতিলকৃত", 0xFFDC2626),
    REFUND_REQUESTED("রিফান্ড আবেদন", 0xFF9333EA),
    REFUNDED("রিফান্ড সম্পন্ন", 0xFF4B5563),
    CANCELLED("ব্যবহারকারী বাতিল করেছে", 0xFF6B7280)
}

enum class DeviceStatus(val banglaText: String) {
    TRUSTED("বিশ্বস্ত ডিভাইস"),
    PENDING("অনুমোদনের অপেক্ষায়"),
    BLOCKED("ব্লকড"),
    REVOKED("অ্যাক্সেস বাতিল")
}

data class User(
    val id: String = "USR-10023",
    val name: String = "তানভীর আহমেদ",
    val phone: String = "+8801712345678",
    val email: String = "tanvir.ahmed@example.com",
    val country: String = "সৌদি আরব (SAR)",
    val nationality: String = "বাংলাদেশী",
    val kycStatus: KycStatus = KycStatus.VERIFIED,
    val accountStatus: AccountStatus = AccountStatus.ACTIVE,
    val totalTransferredBdt: Double = 345200.00,
    val totalTransactionsCount: Int = 14,
    val memberSince: String = "জানুয়ারি ২০২৪",
    val avatarUrl: String = "",
    val biometricEnabled: Boolean = true,
    val deviceConsentGiven: Boolean = true
)

@Entity(tableName = "exchange_rates")
data class ExchangeRate(
    @PrimaryKey val currencyCode: String, // e.g., "SAR"
    val countryName: String,               // e.g., "সৌদি আরব"
    val currencySymbol: String,           // e.g., "﷼"
    val flagEmoji: String,                 // e.g., "🇸🇦"
    val sendingRate: Double,              // e.g., 32.45
    val receivingRate: Double = 1.0,      // BDT rate
    val feePercent: Double = 0.0,
    val flatFee: Double = 5.0,             // sending currency flat fee
    val minAmount: Double = 50.0,
    val maxAmount: Double = 25000.0,
    val lastUpdated: String = "আজ, দুপুর ১২:৩০",
    val isActive: Boolean = true,
    val displayOrder: Int = 1
)

@Entity(tableName = "beneficiaries")
data class Beneficiary(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val channelType: String, // "বিকাশ", "নগদ", "রকেট", "ব্যাংক অ্যাকাউন্ট"
    val accountOrBankName: String,
    val accountNumber: String,
    val branchName: String = "",
    val district: String = "ঢাকা",
    val relationship: String = "পরিবার",
    val isFavorite: Boolean = true
)

data class RemittanceQuote(
    val sendAmount: Double,
    val sendCurrency: String,
    val rate: Double,
    val serviceFee: Double,
    val discountAmount: Double,
    val couponApplied: String?,
    val totalPayable: Double,
    val recipientBdtAmount: Double,
    val govtIncentiveBdt: Double // 2.5% Bangladesh govt remittance incentive
)

@Entity(tableName = "transactions")
data class RemittanceTransaction(
    @PrimaryKey val id: String, // e.g., "RM-2026-894210"
    val senderName: String,
    val senderPhone: String,
    val recipientName: String,
    val recipientPhone: String,
    val recipientChannel: String,
    val recipientAccount: String,
    val sendAmount: Double,
    val sendCurrency: String,
    val exchangeRate: Double,
    val serviceFee: Double,
    val discount: Double,
    val totalPayable: Double,
    val recipientBdtAmount: Double,
    val govtIncentiveBdt: Double,
    val paymentMethodName: String,
    val paymentAccountInfo: String,
    val status: TransactionStatus,
    val createdAt: String,
    val completedAt: String? = null,
    val receiptId: String = "REC-${System.currentTimeMillis() % 1000000}",
    val idempotencyKey: String,
    val adminNote: String? = null,
    val statusTimeline: List<TimelineEvent> = emptyList()
)

data class TimelineEvent(
    val title: String,
    val description: String,
    val timestamp: String,
    val isCompleted: Boolean
)

@Entity(tableName = "payment_methods")
data class PaymentMethod(
    @PrimaryKey val id: String,
    val name: String,
    val shortCode: String,
    val iconType: String, // "BANK", "BKASH", "NAGAD", "AGENT", "CASH"
    val accountName: String,
    val accountNumber: String,
    val instructions: String,
    val minLimit: Double,
    val maxLimit: Double,
    val requiresProofUpload: Boolean,
    val isActive: Boolean,
    val displayOrder: Int
)

@Entity(tableName = "offers")
data class Offer(
    @PrimaryKey val id: String,
    val promoCode: String,
    val title: String,
    val description: String,
    val discountPercent: Double,
    val maxDiscount: Double,
    val minSendAmount: Double,
    val validUntil: String,
    val badgeText: String,
    val isActive: Boolean
)

@Entity(tableName = "notices")
data class Notice(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val category: String, // "গুরুত্বপূর্ণ", "লেনদেন", "কেওয়াইসি", "রক্ষণাবেক্ষণ"
    val date: String,
    val isPriority: Boolean,
    val isRead: Boolean = false
)

data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val time: String,
    val type: String, // "TRANSACTION", "KYC", "SECURITY", "OFFER"
    val isRead: Boolean
)

data class SupportTicket(
    val id: String,
    val subject: String,
    val transactionRef: String? = null,
    val category: String = "রেমিট্যান্স লেনদেন",
    val priority: String = "সাধারণ",
    val status: String, // "খোলা", "উত্তর দেওয়া হয়েছে", "প্রক্রিয়াধীন", "সমাধানকৃত"
    val createdAt: String,
    val linkedUserId: String = "USR-10023",
    val attachmentName: String? = null,
    val messages: List<TicketMessage>
)

data class TicketMessage(
    val senderName: String,
    val isAgent: Boolean,
    val message: String,
    val timestamp: String,
    val linkedTxId: String? = null
)

data class FaqItem(
    val question: String,
    val answer: String,
    val category: String
)

@Entity(tableName = "devices")
data class RegisteredDevice(
    @PrimaryKey val deviceId: String,
    val modelName: String,
    val osVersion: String,
    val appVersion: String,
    val ipAddress: String,
    val lastActive: String,
    val status: DeviceStatus,
    val isCurrentDevice: Boolean,
    val consentTimestamp: String
)

data class SystemSettings(
    val appName: String = "পয়সা রেমিট (Paisa Remit)",
    val helplinePhone: String = "+880 9612-888999",
    val whatsappNumber: String = "+966 50 123 4567",
    val supportEmail: String = "support@paisaremit.com",
    val telegramHandle: String = "@PaisaRemitSupport",
    val officeHours: String = "সকাল ৮:০০ - রাত ১১:০০ (প্রতিদিন)",
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "সিস্টেম বর্তমানে নিয়মিত আপগ্রেডেশনের জন্য রক্ষণাবেক্ষণে আছে। অতি শীঘ্রই সেবা চালু হবে।",
    val minAppVersion: String = "1.0.0",
    val latestAppVersion: String = "1.0.0",
    val forceUpdate: Boolean = false,
    val govtIncentiveRate: Double = 0.025, // 2.5%
    val dailyTransferLimitBdt: Double = 500000.0,
    val kycRequiredForTransfer: Boolean = true,
    val testOtpMode: Boolean = true // Test mode enabled for easy instant OTP verification
)

data class AdminAuditLog(
    val id: String,
    val adminName: String,
    val action: String,
    val targetType: String,
    val targetId: String,
    val timestamp: String,
    val notes: String
)

data class MobileRechargeOperator(
    val code: String,
    val name: String,
    val prefix: String,
    val colorHex: Long
)

enum class KycDocType(val banglaTitle: String, val shortCode: String, val description: String) {
    NID(
        banglaTitle = "জাতীয় পরিচয়পত্র (NID)",
        shortCode = "NID",
        description = "স্মার্ট কার্ড বা লেমিনেটেড জাতীয় পরিচয়পত্র (উভয় পিঠ আবশ্যক)"
    ),
    PASSPORT(
        banglaTitle = "আন্তর্জাতিক পাসপোর্ট (Passport)",
        shortCode = "PASSPORT",
        description = "বায়োডাটা পাতা ও ভিসা/এন্ট্রি সিল পাতা (নূন্যতম ৬ মাস মেয়াদ)"
    ),
    WORK_PERMIT_IQAMA(
        banglaTitle = "ওয়ার্ক পারমিট / ইকামা (Work Permit / Iqama)",
        shortCode = "WORK_PERMIT",
        description = "প্রবাসের রেসিডেন্ট আইডি / ইকামা কার্ড বা বৈধ কাজের অনুমতিপত্র"
    )
}

enum class KycSelfieType(val banglaTitle: String, val description: String) {
    PHOTO_SELFIE(
        banglaTitle = "স্থির সেলফি ছবি (Live Selfie Photo)",
        description = "ক্যামেরার দিকে সরাসরি তাকিয়ে একটি স্পষ্ট সেলফি ছবি তুলুন"
    ),
    VIDEO_SELFIE(
        banglaTitle = "লাইভ ভিডিও সেলফি (Selfie Video / Liveness Check)",
        description = "৩-৫ সেকেন্ডের লাইভ ভিডিও যেখানে চোখ পলক ও হাসিমুখ সনাক্ত হবে"
    )
}

data class KycMediaMeta(
    val fileName: String,
    val fileSize: String,
    val captureSource: String, // "ক্যামেরা (Camera Live Capture)", "গ্যালারি (Gallery Upload)"
    val timestamp: String,
    val mimeType: String,
    val qualityScore: String = "উচ্চ (High - 98%)",
    val isVideo: Boolean = false,
    val durationSeconds: Double = 0.0,
    val livenessConfidence: Double = 99.2,
    val previewLabel: String = ""
)

data class KycSubmissionRecord(
    val id: String, // e.g. "KYC-2026-88129"
    val userId: String,
    val userName: String,
    val userPhone: String,
    val userEmail: String,
    val documentType: KycDocType,
    val documentNumber: String,
    val fullNameOnDoc: String,
    val dateOfBirth: String,
    val issuingCountry: String,
    val expiryDate: String,
    val frontDocMeta: KycMediaMeta,
    val backDocMeta: KycMediaMeta?,
    val workPermitDocMeta: KycMediaMeta? = null,
    val selfieType: KycSelfieType,
    val selfieMediaMeta: KycMediaMeta,
    val deviceModel: String = "Samsung Galaxy S24 Ultra",
    val ipAddress: String = "185.190.142.66",
    val gpsCoordinates: String = "24.7136° N, 46.6753° E (Riyadh, KSA)",
    val submittedAt: String,
    val status: KycStatus = KycStatus.PENDING,
    val adminNotes: String? = null,
    val reviewedBy: String? = null,
    val reviewedAt: String? = null
)
