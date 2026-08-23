package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.PaisaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RemittanceNavigationTest {

    private lateinit var context: Context
    private lateinit var repository: PaisaRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = PaisaRepository(context)
    }

    @Test
    fun testCurrencySelectionUpdatesAppState() = runBlocking {
        // Initial selected currency
        assertEquals("SAR", repository.selectedCurrency.value)

        // Select AED
        repository.setSelectedCurrency("AED")
        assertEquals("AED", repository.selectedCurrency.value)

        // Select MYR
        repository.setSelectedCurrency("MYR")
        assertEquals("MYR", repository.selectedCurrency.value)

        // Select USD
        repository.setSelectedCurrency("USD")
        assertEquals("USD", repository.selectedCurrency.value)
    }

    @Test
    fun testAmountCalculationMatchesSelectedCurrency() = runBlocking {
        val rates = repository.getExchangeRates().first()
        val myrRate = rates.first { it.currencyCode == "MYR" }

        val quote = repository.calculateQuote(
            sendAmount = 500.0,
            currencyCode = "MYR",
            currentRates = rates,
            promoCode = null
        )

        assertEquals("MYR", quote.sendCurrency)
        assertEquals(500.0, quote.sendAmount, 0.01)
        assertEquals(myrRate.sendingRate, quote.rate, 0.01)
        
        val expectedBaseBdt = 500.0 * myrRate.sendingRate
        val expectedIncentive = expectedBaseBdt * 0.025
        val expectedRecipientBdt = expectedBaseBdt + expectedIncentive

        assertEquals(expectedIncentive, quote.govtIncentiveBdt, 0.01)
        assertEquals(expectedRecipientBdt, quote.recipientBdtAmount, 0.01)
    }

    @Test
    fun testBeneficiariesAndPaymentMethodsAvailability() = runBlocking {
        val beneficiaries = repository.getBeneficiaries().first()
        assertTrue("Beneficiary list should not be empty", beneficiaries.isNotEmpty())

        val paymentMethods = repository.getPaymentMethods().first()
        assertTrue("Payment methods should not be empty", paymentMethods.isNotEmpty())
    }

    @Test
    fun testFaqItemsCoverLimitsProcessingTimesAndKycRequirements() {
        val faqs = repository.faqs
        assertTrue("FAQs should not be empty", faqs.isNotEmpty())

        // Verify remittance limits FAQ
        val limitsFaq = faqs.find { it.category == "লিমিট ও সীমা" }
        assertNotNull("Should contain remittance limits FAQ", limitsFaq)
        assertTrue("Limits FAQ should mention limits or amounts", limitsFaq!!.answer.contains("সীমা") || limitsFaq.answer.contains("টাকা"))

        // Verify processing times FAQ
        val processingTimesFaq = faqs.find { it.category == "প্রসেসিং সময়" }
        assertNotNull("Should contain processing times FAQ", processingTimesFaq)
        assertTrue("Processing times FAQ should mention duration/time", processingTimesFaq!!.answer.contains("মিনিট") || processingTimesFaq.answer.contains("ঘণ্টা"))

        // Verify KYC & document requirements FAQ
        val kycDocFaq = faqs.find { it.category == "ডকুমেন্ট ও KYC" }
        assertNotNull("Should contain KYC document requirements FAQ", kycDocFaq)
        assertTrue("KYC FAQ should mention Passport or NID", kycDocFaq!!.answer.contains("পাসপোর্ট") || kycDocFaq.answer.contains("NID"))
    }

    @Test
    fun testSubmitSupportRequestLinksAccountDetailsAndCreatesTicket() = runBlocking {
        val initialCount = repository.supportTickets.value.size
        val currentUser = repository.currentUser.value

        val newTicket = repository.submitSupportRequest(
            subject = "লেনদেনের স্ট্যাটাস আপডেট সমস্যা",
            message = "আমার পাঠানো রেমিট্যান্স এখনো বিকাশ ওয়ালেটে পৌঁছায়নি। দয়া করে চেক করুন।",
            category = "রেমিট্যান্স লেনদেন বিলম্ব",
            priority = "জরুরি",
            txRef = "RM-2026-894210",
            attachmentName = "bank_receipt.pdf"
        )

        val updatedTickets = repository.supportTickets.value
        assertEquals(initialCount + 1, updatedTickets.size)

        val createdTicket = updatedTickets.find { it.id == newTicket.id }
        assertNotNull("Created ticket should be in the repository", createdTicket)
        assertEquals("লেনদেনের স্ট্যাটাস আপডেট সমস্যা", createdTicket!!.subject)
        assertEquals("রেমিট্যান্স লেনদেন বিলম্ব", createdTicket.category)
        assertEquals("জরুরি", createdTicket.priority)
        assertEquals("RM-2026-894210", createdTicket.transactionRef)
        assertEquals("bank_receipt.pdf", createdTicket.attachmentName)
        assertEquals(currentUser.id, createdTicket.linkedUserId)
        assertTrue("Ticket should have initial user message", createdTicket.messages.isNotEmpty())
        assertEquals("আমার পাঠানো রেমিট্যান্স এখনো বিকাশ ওয়ালেটে পৌঁছায়নি। দয়া করে চেক করুন।", createdTicket.messages.first().message)
    }

    @Test
    fun testSendLiveChatMessageUpdatesMessagesWithTransactionLink() = runBlocking {
        val initialMessagesCount = repository.liveChatMessages.value.size
        val currentUser = repository.currentUser.value

        repository.sendLiveChatMessage(
            messageText = "আমার সৌদি রিয়াল রেমিট্যান্স কতক্ষণের মধ্যে পৌঁছাবে?",
            linkedTxId = "RM-2026-894210"
        )

        val updatedMessages = repository.liveChatMessages.value
        assertTrue("Live chat messages should increase", updatedMessages.size > initialMessagesCount)

        val userMessage = updatedMessages.find { it.message == "আমার সৌদি রিয়াল রেমিট্যান্স কতক্ষণের মধ্যে পৌঁছাবে?" }
        assertNotNull("User message should be posted in live chat", userMessage)
        assertEquals(currentUser.name, userMessage!!.senderName)
        assertEquals(false, userMessage.isAgent)
        assertEquals("RM-2026-894210", userMessage.linkedTxId)
    }
}
