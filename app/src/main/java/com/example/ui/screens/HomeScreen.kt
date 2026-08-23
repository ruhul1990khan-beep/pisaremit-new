package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.models.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    user: User,
    rates: List<ExchangeRate>,
    transactions: List<RemittanceTransaction>,
    offers: List<Offer>,
    notices: List<Notice>,
    faqs: List<FaqItem> = emptyList(),
    unreadNotifCount: Int,
    onNavigateToSendMoney: () -> Unit,
    onNavigateToRates: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToOffers: () -> Unit,
    onNavigateToNotices: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToKyc: () -> Unit,
    onNavigateToRecharge: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onSelectTransaction: (RemittanceTransaction) -> Unit
) {
    var quickSendAmount by remember { mutableStateOf("1000") }
    val selectedRate = rates.firstOrNull { it.currencyCode == "SAR" } ?: rates.firstOrNull() ?: ExchangeRate("SAR", "সৌদি আরব", "﷼", "🇸🇦", 32.45)
    val sendAmountNum = quickSendAmount.toDoubleOrNull() ?: 1000.0
    val rawBdt = sendAmountNum * selectedRate.sendingRate
    val incentiveBdt = rawBdt * 0.025
    val totalRecipientBdt = rawBdt + incentiveBdt

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftPinkBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 90.dp)
    ) {
        // Top Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MagentaPrimary, PurpleSecondary)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onNavigateToKyc() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "তা",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MagentaPrimary
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "আসসালামু আলাইকুম,",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = user.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = AmberAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = user.kycStatus.banglaText,
                                    fontSize = 10.sp,
                                    color = AmberAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Admin Mode Badge Button
                        IconButton(
                            onClick = onNavigateToAdmin,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .testTag("home_admin_icon")
                        ) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Console",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        // Notifications
                        Box {
                            IconButton(
                                onClick = onNavigateToNotifications,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .testTag("home_notifications_icon")
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (unreadNotifCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .background(AmberAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$unreadNotifCount",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkText
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Hero Summary Cards
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "মোট প্রেরিত রেমিট্যান্স",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "৳ ${String.format("%,.2f", user.totalTransferredBdt)}",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AmberAccent)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "+২.৫% প্রণোদনা",
                                color = DarkText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(14.dp))

            // Quick Calculator Widget
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "রেমিট্যান্স ক্যালকুলেটর",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DarkText
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "১ ${selectedRate.currencyCode} = ৳${String.format("%.2f", selectedRate.sendingRate)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MagentaPrimary
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = quickSendAmount,
                            onValueChange = { quickSendAmount = it },
                            label = { Text("পাঠানোর পরিমাণ (${selectedRate.currencyCode})") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MagentaPrimary,
                                unfocusedBorderColor = LightGrayBorder
                            )
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0FDF4))
                            .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "প্রাপক বাংলাদেশে পাবেন",
                                    fontSize = 11.sp,
                                    color = GrayText
                                )
                                Text(
                                    text = "৳ ${String.format("%,.2f", totalRecipientBdt)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SuccessGreen
                                )
                            }
                            Text(
                                text = "সরকারি বোনাস: ৳${String.format("%.2f", incentiveBdt)} সহ",
                                fontSize = 10.sp,
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    PrimaryButton(
                        text = "এখনই টাকা পাঠান",
                        onClick = onNavigateToSendMoney,
                        icon = Icons.Default.Send,
                        testTag = "home_send_money_button"
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Action Grid
            Text(
                text = "প্রধান সেবাসমূহ",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionItem(
                    title = "টাকা পাঠান",
                    icon = Icons.Default.Send,
                    bgColor = MagentaPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSendMoney
                )
                QuickActionItem(
                    title = "লাইভ রেট",
                    icon = Icons.Default.TrendingUp,
                    bgColor = PurpleSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToRates
                )
                QuickActionItem(
                    title = "লেনদেন",
                    icon = Icons.Default.ReceiptLong,
                    bgColor = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToTransactions
                )
                QuickActionItem(
                    title = "অফার",
                    icon = Icons.Default.LocalOffer,
                    bgColor = AmberAccent,
                    iconTint = DarkText,
                    textColor = DarkText,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToOffers
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionItem(
                    title = "কেওয়াইসি",
                    icon = Icons.Default.Badge,
                    bgColor = Color(0xFF0D9488),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToKyc
                )
                QuickActionItem(
                    title = "রিচার্জ",
                    icon = Icons.Default.PhoneAndroid,
                    bgColor = Color(0xFF7C3AED),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToRecharge
                )
                QuickActionItem(
                    title = "নোটিশ",
                    icon = Icons.Default.Campaign,
                    bgColor = Color(0xFFEA580C),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToNotices
                )
                QuickActionItem(
                    title = "সাপোর্ট",
                    icon = Icons.Default.HeadsetMic,
                    bgColor = SuccessGreen,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSupport
                )
            }

            Spacer(Modifier.height(22.dp))

            // Promotional Hero Image Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clickable { onNavigateToSendMoney() },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_banner_1787426523927),
                    contentDescription = "Paisa Remit Promotion",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(20.dp))

            // Active Offers
            SectionHeader(
                title = "স্পেশাল অফার ও ডিসকাউন্ট",
                actionText = "সবগুলো দেখুন",
                onActionClick = onNavigateToOffers
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(offers) { offer ->
                    OfferCard(offer = offer, onApply = onNavigateToSendMoney)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Recent Transactions
            SectionHeader(
                title = "সাম্প্রতিক লেনদেনসমূহ",
                actionText = "ইতিহাস দেখুন",
                onActionClick = onNavigateToTransactions
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                transactions.take(3).forEach { tx ->
                    TransactionCard(
                        transaction = tx,
                        onClick = { onSelectTransaction(tx) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Important Notices
            SectionHeader(
                title = "জরুরী নোটিশ",
                actionText = "সব নোটিশ",
                onActionClick = onNavigateToNotices
            )
            notices.take(1).forEach { notice ->
                NoticeCard(notice = notice, onClick = onNavigateToNotices)
            }

            if (faqs.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SectionHeader(
                    title = "সাধারণ জিজ্ঞাসা (FAQ)",
                    subtitle = "লিমিট, সময় ও ডকুমেন্ট নিয়মাবলী",
                    actionText = "সব FAQ",
                    onActionClick = onNavigateToSupport
                )
                FaqSectionComponent(
                    faqs = faqs.take(4),
                    showSearch = false
                )
            }
        }
    }
}

@Composable
fun QuickActionItem(
    title: String,
    icon: ImageVector,
    bgColor: Color,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.White,
    textColor: Color = DarkText,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
