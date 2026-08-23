package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.NotificationItem
import com.example.data.models.RemittanceTransaction
import com.example.data.repository.PaisaRepository
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    repository: PaisaRepository,
    transactions: List<RemittanceTransaction>,
    onBack: () -> Unit,
    onSelectTransaction: (RemittanceTransaction) -> Unit
) {
    val context = LocalContext.current
    val notifications by repository.notifications.collectAsState()
    val fcmToken by repository.fcmToken.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") }
    var showTokenDetails by remember { mutableStateOf(false) }
    var showTestPushDialog by remember { mutableStateOf(false) }

    val filterCategories = listOf(
        "ALL" to "সমস্ত নোটিফিকেশন",
        "TRANSACTION" to "রেমিট্যান্স লেনদেন",
        "STATUS" to "স্ট্যাটাস আপডেট",
        "SECURITY" to "ডিভাইস ও নিরাপত্তা",
        "OFFER" to "অফার ও রেট"
    )

    val filteredList = when (selectedFilter) {
        "ALL" -> notifications
        "TRANSACTION" -> notifications.filter { it.type.equals("TRANSACTION", ignoreCase = true) || it.type.equals("REMITTANCE", ignoreCase = true) }
        "STATUS" -> notifications.filter { it.type.equals("STATUS", ignoreCase = true) }
        "SECURITY" -> notifications.filter { it.type.equals("SECURITY", ignoreCase = true) || it.type.equals("KYC", ignoreCase = true) }
        "OFFER" -> notifications.filter { it.type.equals("OFFER", ignoreCase = true) || it.type.equals("PROMO", ignoreCase = true) }
        else -> notifications
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("বিজ্ঞপ্তি ও লাইভ অ্যালার্ট (FCM)", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("রিয়েল-টাইম রেমিট্যান্স নোটিফিকেশন", fontSize = 11.sp, color = GrayText)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("notif_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            repository.markAllNotificationsAsRead()
                            Toast.makeText(context, "সমস্ত নোটিফিকেশন পঠিত হিসেবে চিহ্নিত হয়েছে", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("mark_all_read_button")
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = "Mark all read", tint = MagentaPrimary)
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
            // FCM Connection & Live Status Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "FCM পুশ ইঞ্জিন সক্রিয় (Real-time Live)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )
                        }

                        IconButton(
                            onClick = { showTokenDetails = !showTokenDetails },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                if (showTokenDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle token details",
                                tint = GrayText
                            )
                        }
                    }

                    Text(
                        text = "রেমিট্যান্স ট্রান্সফার সফল হলে এবং প্রাপক টাকা পেলে স্বয়ংক্রিয়ভাবে ইনস্ট্যান্ট পুশ অ্যালার্ট পাবেন।",
                        fontSize = 11.5.sp,
                        color = GrayText,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )

                    // Expandable FCM Device Token & Test Controls
                    AnimatedVisibility(visible = showTokenDetails) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "ডিভাইস এফসিএম টোকেন (FCM Registration Token):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = fcmToken,
                                fontSize = 10.sp,
                                color = GrayText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("FCM Token", fcmToken)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "FCM টোকেন কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("কপি টোকেন", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { showTestPushDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MagentaPrimary),
                                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("টেস্ট পুশ অ্যালার্ট", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterCategories) { (key, label) ->
                    val isSelected = selectedFilter == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) MagentaPrimary else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MagentaPrimary else LightGrayBorder,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedFilter = key }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else DarkText
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Notification List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = GrayText.copy(alpha = 0.6f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "কোনো বিজ্ঞপ্তি নেই",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrayText
                        )
                        Text(
                            text = "নতুন রেমিট্যান্স ট্রান্সফার এবং স্ট্যাটাস আপডেট এখানে দেখানো হবে।",
                            fontSize = 12.sp,
                            color = GrayText.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredList, key = { it.id }) { notif ->
                        NotificationItemCard(
                            item = notif,
                            onClick = {
                                repository.markNotificationAsRead(notif.id)
                                // Try finding matching transaction if any
                                val matchedTx = transactions.firstOrNull { tx ->
                                    notif.body.contains(tx.id) || notif.title.contains(tx.id)
                                }
                                if (matchedTx != null) {
                                    onSelectTransaction(matchedTx)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Test Push Modal Dialog
        if (showTestPushDialog) {
            var customTitle by remember { mutableStateOf("রেমিট্যান্স বিতরণ সম্পন্ন! 🎉") }
            var customBody by remember { mutableStateOf("আপনার ৳৬৫,০০০ টাকা প্রাপকের বিকাশ অ্যাকাউন্টে (01712984561) তাৎক্ষণিক জমা হয়েছে।") }
            var selectedType by remember { mutableStateOf("TRANSACTION") }

            AlertDialog(
                onDismissRequest = { showTestPushDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = MagentaPrimary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("টেস্ট FCM পুশ নোটিফিকেশন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "এই টেস্ট পুশটি ডিভাইসের নোটিফিকেশন বারে লাইভ প্রদর্শিত হবে এবং ইনবক্সে সেভ হবে।",
                            fontSize = 12.sp,
                            color = GrayText
                        )

                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = { customTitle = it },
                            label = { Text("বিজ্ঞপ্তির শিরোনাম") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = customBody,
                            onValueChange = { customBody = it },
                            label = { Text("বিজ্ঞপ্তির বার্তা") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            minLines = 2
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (customTitle.isNotBlank() && customBody.isNotBlank()) {
                                repository.dispatchFcmAlert(
                                    title = customTitle,
                                    body = customBody,
                                    type = selectedType
                                )
                                Toast.makeText(context, "FCM পুশ সফলভাবে প্রেরিত হয়েছে!", Toast.LENGTH_SHORT).show()
                                showTestPushDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MagentaPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("এখনই পাঠান", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTestPushDialog = false }) {
                        Text("বাতিল", color = GrayText)
                    }
                }
            )
        }
    }
}

@Composable
fun NotificationItemCard(
    item: NotificationItem,
    onClick: () -> Unit
) {
    val (icon, iconBg, iconColor, badgeText) = when (item.type.uppercase()) {
        "TRANSACTION", "REMITTANCE" -> Quadruple(
            Icons.Default.Paid,
            MagentaPrimary.copy(alpha = 0.12f),
            MagentaPrimary,
            "রেমিট্যান্স"
        )
        "STATUS" -> Quadruple(
            Icons.Default.CheckCircle,
            SuccessGreen.copy(alpha = 0.12f),
            SuccessGreen,
            "স্ট্যাটাস"
        )
        "SECURITY" -> Quadruple(
            Icons.Default.Shield,
            Color(0xFFDC2626).copy(alpha = 0.12f),
            Color(0xFFDC2626),
            "নিরাপত্তা"
        )
        "KYC" -> Quadruple(
            Icons.Default.VerifiedUser,
            PurpleSecondary.copy(alpha = 0.12f),
            PurpleSecondary,
            "কেওয়াইসি"
        )
        "OFFER", "PROMO" -> Quadruple(
            Icons.Default.LocalOffer,
            AmberAccent.copy(alpha = 0.15f),
            Color(0xFFD97706),
            "অফার"
        )
        else -> Quadruple(
            Icons.Default.Notifications,
            MagentaPrimary.copy(alpha = 0.12f),
            MagentaPrimary,
            "বিজ্ঞপ্তি"
        )
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isRead) Color.White else Color(0xFFFFF1F5)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isRead) 1.dp else 2.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(iconColor.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = iconColor
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.time,
                            fontSize = 11.sp,
                            color = GrayText
                        )
                        if (!item.isRead) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MagentaPrimary)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = item.title,
                    fontWeight = if (item.isRead) FontWeight.SemiBold else FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DarkText
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = item.body,
                    fontSize = 12.5.sp,
                    color = GrayText,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
