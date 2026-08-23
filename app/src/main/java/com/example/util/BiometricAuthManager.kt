package com.example.util

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

sealed class BiometricStatus {
    object Available : BiometricStatus()
    object NotEnrolled : BiometricStatus()
    object NoHardware : BiometricStatus()
    object Unavailable : BiometricStatus()

    val isAvailable: Boolean get() = this is Available
    
    val descriptionBn: String
        get() = when (this) {
            is Available -> "বায়োমেট্রিক (ফিঙ্গারপ্রিন্ট/ফেস) প্রস্তুত"
            is NotEnrolled -> "ডিভাইসে কোনো ফিঙ্গারপ্রিন্ট বা ফেস রেজিস্টার করা নেই"
            is NoHardware -> "ডিভাইসে বায়োমেট্রিক সেন্সর সমর্থিত নয়"
            is Unavailable -> "বায়োমেট্রিক সেন্সর বর্তমানে অনুপলব্ধ"
        }
}

object BiometricAuthManager {

    /**
     * Checks whether biometric authentication is supported and enrolled on the device.
     */
    fun checkBiometricAvailability(context: Context): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK

        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Available
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NotEnrolled
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NoHardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.Unavailable
            else -> BiometricStatus.Unavailable
        }
    }

    /**
     * Finds the parent FragmentActivity from any Context.
     */
    fun findFragmentActivity(context: Context): FragmentActivity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is FragmentActivity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }

    /**
     * Triggers the native Android BiometricPrompt for Fingerprint or Face authentication.
     */
    fun promptBiometric(
        activity: FragmentActivity,
        title: String = "পয়সা রেমিট বায়োমেট্রিক যাচাই",
        subtitle: String = "আপনার ফিঙ্গারপ্রিন্ট বা ফেস আনলক স্ক্যান করুন",
        negativeButtonText: String = "পিন বা ওটিপি ব্যবহার করুন",
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit = { _, _ -> },
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
