package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.R
import com.example.ui.components.AppTextField
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.theme.*
import com.example.util.BiometricAuthManager
import com.example.util.BiometricStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(1800)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MagentaPrimary, PurpleSecondary)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CurrencyExchange,
                    contentDescription = "Paisa Remit Logo",
                    tint = MagentaPrimary,
                    modifier = Modifier.size(54.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "পয়সা রেমিট",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "বিশ্বস্ত ও দ্রুততম রেমিট্যান্স সেবা",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(Modifier.height(40.dp))
            CircularProgressIndicator(
                color = AmberAccent,
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit
) {
    var currentPage by remember { mutableStateOf(0) }
    val pages = listOf(
        Triple(
            "সেরা এক্সচেঞ্জ রেট ও ০ ফি",
            "সৌদি আরব, আমিরাত, মালয়েশিয়া ও কুয়েত থেকে বাংলাদেশে সর্বোচ্চ এক্সচেঞ্জ রেটে টাকা পাঠান।",
            Icons.Default.MonetizationOn
        ),
        Triple(
            "মুহূর্তেই বিকাশ ও ব্যাংকে জমা",
            "সরাসরি বিকাশ, নগদ, রকেট এবং যেকোনো ব্যাংক অ্যাকাউন্টে মুহূর্তের মধ্যে রেমিট্যান্স পৌঁছায়।",
            Icons.Default.FlashOn
        ),
        Triple(
            "২.৫% সরকারি নগদ প্রণোদনা",
            "বৈধ চ্যানেলে পাঠানো সকল রেমিট্যান্সে সরাসরি বাংলাদেশ সরকারের ২.৫% নগদ বোনাস যুক্ত হবে।",
            Icons.Default.Security
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onFinishOnboarding) {
                Text("এড়িয়ে যান", color = GrayText, fontSize = 14.sp)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(SoftPinkBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    pages[currentPage].third,
                    contentDescription = null,
                    tint = MagentaPrimary,
                    modifier = Modifier.size(68.dp)
                )
            }
            Spacer(Modifier.height(36.dp))
            Text(
                text = pages[currentPage].first,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = pages[currentPage].second,
                fontSize = 14.sp,
                color = GrayText,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(30.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (index == currentPage) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (index == currentPage) MagentaPrimary else LightGrayBorder)
                    )
                }
            }
        }

        PrimaryButton(
            text = if (currentPage < pages.size - 1) "পরবর্তী" else "শুরু করুন",
            onClick = {
                if (currentPage < pages.size - 1) {
                    currentPage++
                } else {
                    onFinishOnboarding()
                }
            },
            icon = if (currentPage == pages.size - 1) Icons.Default.Check else Icons.Default.ArrowForward
        )
    }
}

@Composable
fun LoginOtpScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var phoneNumber by remember { mutableStateOf("+966 50 123 4567") }
    var otpCode by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showConsentDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isBiometricScanning by remember { mutableStateOf(false) }
    var biometricScanSuccess by remember { mutableStateOf(false) }
    val testOtp = "123456"

    val biometricStatus = remember(context) {
        BiometricAuthManager.checkBiometricAvailability(context)
    }

    // Biometric Trigger Function
    val triggerBiometricAuth = {
        val activity = BiometricAuthManager.findFragmentActivity(context)
        if (activity != null) {
            BiometricAuthManager.promptBiometric(
                activity = activity,
                title = "পয়সা রেমিট বায়োমেট্রিক লগইন",
                subtitle = "ফিঙ্গারপ্রিন্ট বা ফেস আনলক দিয়ে নিশ্চিত করুন",
                negativeButtonText = "ওটিপি ব্যবহার করুন",
                onSuccess = {
                    onLoginSuccess()
                },
                onError = { _, _ ->
                    // Fallback to interactive in-app biometric verification dialog
                    isBiometricScanning = true
                },
                onFailed = {
                    errorMessage = "বায়োমেট্রিক যাচাই ব্যর্থ হয়েছে, পুনরায় চেষ্টা করুন।"
                }
            )
        } else {
            // Context doesn't have FragmentActivity (e.g. preview mode), launch interactive scanner
            isBiometricScanning = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftPinkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MagentaPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LockPerson,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "স্বাগতম পয়সা রেমিট-এ",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
            Text(
                text = if (!isOtpSent) "মোবাইল ওটিপি বা বায়োমেট্রিক সেন্সর দিয়ে লগইন করুন" else "আপনার মোবাইলে প্রেরিত ৬ সংখ্যার ওটিপি (OTP) কোডটি দিন",
                fontSize = 13.sp,
                color = GrayText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )

            // Biometric Fingerprint & Face Unlock Quick Access Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PurpleSecondary),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { triggerBiometricAuth() }
                    .testTag("biometric_login_quick_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = "Biometric Login",
                                tint = AmberAccent,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "বায়োমেট্রিক দিয়ে এক ক্লিকে লগইন",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Face,
                                    contentDescription = "Face Unlock",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "ফিঙ্গারপ্রিন্ট বা ফেস আনলক স্পর্শ করুন",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Standard Mobile OTP Input Box
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (!isOtpSent) {
                        AppTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = "মোবাইল নম্বর",
                            placeholder = "উদাঃ +966 50 123 4567",
                            leadingIcon = Icons.Default.Phone,
                            keyboardType = KeyboardType.Phone,
                            testTag = "login_phone_input"
                        )
                        Spacer(Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFEF3C7))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "💡 টেস্ট মোড: যেকোনো নম্বর ব্যবহার করুন। ওটিপি কোড: $testOtp",
                                fontSize = 11.sp,
                                color = Color(0xFF92400E)
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                        PrimaryButton(
                            text = "ওটিপি (OTP) পাঠান",
                            isLoading = isLoading,
                            onClick = {
                                if (phoneNumber.length >= 8) {
                                    isLoading = true
                                    errorMessage = null
                                    isOtpSent = true
                                    isLoading = false
                                } else {
                                    errorMessage = "সঠিক মোবাইল নম্বর প্রবেশ করান"
                                }
                            },
                            testTag = "send_otp_button"
                        )
                    } else {
                        AppTextField(
                            value = otpCode,
                            onValueChange = { if (it.length <= 6) otpCode = it },
                            label = "৬ সংখ্যার ওটিপি কোড",
                            placeholder = "উদাঃ 123456",
                            leadingIcon = Icons.Default.Key,
                            keyboardType = KeyboardType.Number,
                            isError = errorMessage != null,
                            errorMessage = errorMessage,
                            testTag = "otp_input_field"
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "নম্বর পরিবর্তন",
                                color = MagentaPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    isOtpSent = false
                                    otpCode = ""
                                    errorMessage = null
                                }
                            )
                            Text(
                                text = "কোড পাননি? পুনরায় পাঠান",
                                color = GrayText,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        PrimaryButton(
                            text = "যাচাই করুন ও প্রবেশ করুন",
                            isLoading = isLoading,
                            onClick = {
                                if (otpCode == testOtp || otpCode.length == 6) {
                                    showConsentDialog = true
                                } else {
                                    errorMessage = "ভুল ওটিপি কোড! টেস্ট কোড: $testOtp ব্যবহার করুন।"
                                }
                            },
                            testTag = "verify_otp_button"
                        )
                    }
                }
            }
        }

        // Interactive Biometric Scanning Modal Sheet
        if (isBiometricScanning) {
            AlertDialog(
                onDismissRequest = { isBiometricScanning = false },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (biometricScanSuccess) Color(0xFFDCFCE7) else MagentaPrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (biometricScanSuccess) Icons.Default.CheckCircle else Icons.Default.Fingerprint,
                            contentDescription = "Biometric Sensor",
                            tint = if (biometricScanSuccess) SuccessGreen else MagentaPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = if (biometricScanSuccess) "বায়োমেট্রিক সফল হয়েছে!" else "ফিঙ্গারপ্রিন্ট / ফেস সেন্সর",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (biometricScanSuccess)
                                "আপনার বায়োমেট্রিক পরিচয় সফলভাবে প্রমাণিত হয়েছে।"
                            else
                                "ডিভাইসের ফিঙ্গারপ্রিন্ট সেন্সরে আঙুল রাখুন অথবা ক্যামেরা দিয়ে ফেস আনলক যাচাই করুন।",
                            fontSize = 13.sp,
                            color = DarkText,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        if (!biometricScanSuccess) {
                            Button(
                                onClick = {
                                    biometricScanSuccess = true
                                    coroutineScope.launch {
                                        delay(700)
                                        isBiometricScanning = false
                                        biometricScanSuccess = false
                                        onLoginSuccess()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MagentaPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("আঙুলের ছাপ স্ক্যান করুন (সিমুলেশন)")
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { isBiometricScanning = false }) {
                        Text("ওটিপি দিয়ে লগইন করুন", color = GrayText)
                    }
                }
            )
        }

        // Consent Dialog for Device Protection
        if (showConsentDialog) {
            AlertDialog(
                onDismissRequest = { showConsentDialog = false },
                icon = {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = MagentaPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "ডিভাইস সুরক্ষা ও নিরাপত্তা সম্মতি",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "পয়সা রেমিট আপনার অ্যাকাউন্টের অননুমোদিত প্রবেশ রোধ ও সর্বোচ্চ নিরাপত্তা দিতে ডিভাইসের মৌলিক তথ্য (মডেল, ওএস সংস্করণ) সংরক্ষণ করে।",
                            fontSize = 13.sp,
                            color = DarkText,
                            lineHeight = 19.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "• কোনো ব্যক্তিগত ফাইল বা অননুমোদিত ডেটা পর্যবেক্ষণ করা হয় না।\n• আপনি যেকোনো সময় সিকিউরিটি সেন্টার থেকে এই ডিভাইসের অ্যাক্সেস বাতিল করতে পারবেন।",
                            fontSize = 12.sp,
                            color = GrayText,
                            lineHeight = 18.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConsentDialog = false
                            onLoginSuccess()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MagentaPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("আমি সম্মত ও চালিয়ে যান", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConsentDialog = false }) {
                        Text("বাতিল", color = GrayText)
                    }
                }
            )
        }
    }
}
