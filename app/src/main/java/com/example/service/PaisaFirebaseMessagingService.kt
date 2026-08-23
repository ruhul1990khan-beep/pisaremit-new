package com.example.service

import android.util.Log
import com.example.util.FcmNotificationManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class FcmMessagePayload(
    val title: String,
    val body: String,
    val type: String,
    val transactionId: String? = null,
    val amountBdt: String? = null,
    val recipient: String? = null,
    val status: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class PaisaFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "PaisaFCM"

        private val _incomingMessages = MutableSharedFlow<FcmMessagePayload>(extraBufferCapacity = 10)
        val incomingMessages = _incomingMessages.asSharedFlow()

        var latestToken: String? = null
            private set
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received: $token")
        latestToken = token
        // In production backend, this registers token with customer ID:
        // ApiService.registerFcmToken(userId, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message from: ${remoteMessage.from}")

        // 1. Extract payload data
        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val title = data["title"]
            ?: notification?.title
            ?: "পয়সা রেমিট অ্যালার্ট"

        val body = data["body"]
            ?: notification?.body
            ?: "আপনার অ্যাকাউন্টে নতুন আপডেট রয়েছে।"

        val type = data["type"] ?: "TRANSACTION"
        val transactionId = data["transaction_id"]
        val amountBdt = data["amount_bdt"]
        val recipient = data["recipient"]
        val status = data["status"]

        // 2. Post Android System Notification (Heads-up banner & status bar)
        FcmNotificationManager.showSystemNotification(
            context = applicationContext,
            title = title,
            body = body,
            type = type,
            transactionId = transactionId
        )

        // 3. Emit into App Live Flow for instant in-app update
        val payload = FcmMessagePayload(
            title = title,
            body = body,
            type = type,
            transactionId = transactionId,
            amountBdt = amountBdt,
            recipient = recipient,
            status = status
        )
        _incomingMessages.tryEmit(payload)
    }
}
