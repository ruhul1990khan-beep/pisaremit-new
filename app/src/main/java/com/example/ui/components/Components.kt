package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.*
import com.example.ui.theme.*

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    testTag: String = "primary_button"
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MagentaPrimary,
            contentColor = Color.White,
            disabledContainerColor = MagentaPrimary.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    testTag: String = "secondary_button"
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MagentaPrimary),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MagentaPrimary
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MagentaPrimary
            )
        }
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    testTag: String = "app_text_field"
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = DarkText,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = GrayText.copy(alpha = 0.7f), fontSize = 14.sp) },
            leadingIcon = leadingIcon?.let {
                { Icon(it, contentDescription = null, tint = MagentaPrimary, modifier = Modifier.size(20.dp)) }
            },
            trailingIcon = trailingIcon,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MagentaPrimary,
                unfocusedBorderColor = LightGrayBorder,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                errorBorderColor = DangerRed
            ),
            singleLine = true
        )
        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = DangerRed,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun StatusBadge(status: TransactionStatus) {
    val (bgColor, textColor) = when (status) {
        TransactionStatus.COMPLETED -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        TransactionStatus.PROCESSING -> Color(0xFFDBEAFE) to Color(0xFF1D4ED8)
        TransactionStatus.PENDING -> Color(0xFFFEF3C7) to Color(0xFFB45309)
        TransactionStatus.APPROVED -> Color(0xFFCCFBF1) to Color(0xFF0F766E)
        TransactionStatus.REJECTED -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)
        TransactionStatus.REFUND_REQUESTED -> Color(0xFFF3E8FF) to Color(0xFF7E22CE)
        TransactionStatus.REFUNDED -> Color(0xFFF3F4F6) to Color(0xFF374151)
        TransactionStatus.CANCELLED -> Color(0xFFF3F4F6) to Color(0xFF4B5563)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.banglaText,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RateCard(
    rate: ExchangeRate,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3E8FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = rate.flagEmoji, fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${rate.countryName} (${rate.currencyCode})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = DarkText
                    )
                    Text(
                        text = "ফি: ৳০ • সর্বনিম্ন: ${rate.minAmount.toInt()} ${rate.currencyCode}",
                        fontSize = 12.sp,
                        color = GrayText
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "১ ${rate.currencyCode} =",
                    fontSize = 11.sp,
                    color = GrayText
                )
                Text(
                    text = "৳ ${String.format("%.2f", rate.sendingRate)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = MagentaPrimary
                )
            }
        }
    }
}

@Composable
fun TransactionCard(
    transaction: RemittanceTransaction,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("transaction_card_${transaction.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MagentaPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = null,
                            tint = MagentaPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = transaction.recipientName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DarkText
                        )
                        Text(
                            text = transaction.recipientChannel,
                            fontSize = 12.sp,
                            color = GrayText
                        )
                    }
                }
                StatusBadge(status = transaction.status)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = LightGrayBorder.copy(alpha = 0.5f))
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "আইডি: ${transaction.id}",
                        fontSize = 11.sp,
                        color = GrayText,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = transaction.createdAt,
                        fontSize = 11.sp,
                        color = GrayText
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "৳ ${String.format("%,.2f", transaction.recipientBdtAmount)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SuccessGreen
                    )
                    Text(
                        text = "প্রেরিত: ${transaction.sendAmount.toInt()} ${transaction.sendCurrency}",
                        fontSize = 11.sp,
                        color = GrayText
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = GrayText
                )
            }
        }
        if (!actionText.isNullOrBlank()) {
            Text(
                text = actionText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MagentaPrimary,
                modifier = Modifier.clickable { onActionClick() }
            )
        }
    }
}

@Composable
fun NoticeCard(notice: Notice, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notice.isPriority) Color(0xFFFFF1F2) else Color.White
        ),
        border = if (notice.isPriority) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECDD3)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (notice.isPriority) DangerRed.copy(alpha = 0.15f) else PurpleSecondary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (notice.isPriority) Icons.Default.Campaign else Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = if (notice.isPriority) DangerRed else PurpleSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notice.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = notice.date,
                        fontSize = 10.sp,
                        color = GrayText
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = notice.message,
                    fontSize = 12.sp,
                    color = GrayText,
                    lineHeight = 17.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun OfferCard(
    offer: Offer,
    onApply: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(MagentaPrimary, PurpleSecondary)
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = offer.badgeText,
                    color = AmberAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = offer.promoCode,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = offer.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = DarkText
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = offer.description,
                fontSize = 12.sp,
                color = GrayText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "মেয়াদ: ${offer.validUntil}",
                    fontSize = 10.sp,
                    color = GrayText
                )
                TextButton(
                    onClick = onApply,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Text("ব্যবহার করুন", fontSize = 12.sp, color = MagentaPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ----------------------------------------------------
// FAQ COMPONENT (Accordion, Search & Category Filter)
// ----------------------------------------------------
@Composable
fun FaqSectionComponent(
    faqs: List<FaqItem>,
    modifier: Modifier = Modifier,
    initialCategory: String? = null,
    showSearch: Boolean = true
) {
    var searchQuery by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    val categories = androidx.compose.runtime.remember(faqs) {
        listOf("সকল") + faqs.map { it.category }.distinct()
    }
    var selectedCategory by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initialCategory ?: "সকল") }

    val filteredFaqs = androidx.compose.runtime.remember(faqs, searchQuery, selectedCategory) {
        faqs.filter { faq ->
            val matchesCategory = selectedCategory == "সকল" || faq.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                    faq.question.contains(searchQuery, ignoreCase = true) ||
                    faq.answer.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (showSearch) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("প্রশ্ন বা উত্তর খুঁজুন...", fontSize = 13.sp, color = GrayText) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search FAQ", tint = GrayText)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = GrayText)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .testTag("faq_search_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = MagentaPrimary,
                    unfocusedBorderColor = LightGrayBorder
                ),
                singleLine = true
            )
        }

        // Category Chips
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            items(categories.size) { index ->
                val cat = categories[index]
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) MagentaPrimary else Color.White)
                        .border(
                            1.dp,
                            if (isSelected) MagentaPrimary else LightGrayBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("faq_category_${cat}")
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) Color.White else DarkText,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        if (filteredFaqs.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = GrayText.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "কোনো প্রশ্ন পাওয়া যায়নি",
                        fontSize = 13.sp,
                        color = GrayText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            filteredFaqs.forEach { faq ->
                var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { expanded = !expanded }
                        .testTag("faq_card_${faq.question.take(10)}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (expanded) MagentaPrimary.copy(alpha = 0.4f) else LightGrayBorder.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (faq.category) {
                                                "লিমিট ও সীমা" -> Color(0xFFEFF6FF)
                                                "প্রসেসিং সময়" -> Color(0xFFFEF3C7)
                                                "ডকুমেন্ট ও KYC" -> Color(0xFFF3E8FF)
                                                "প্রণোদনা ও ফি" -> Color(0xFFDCFCE7)
                                                else -> Color(0xFFFFF0F7)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        when (faq.category) {
                                            "লিমিট ও সীমা" -> Icons.Default.Speed
                                            "প্রসেসিং সময়" -> Icons.Default.Timer
                                            "ডকুমেন্ট ও KYC" -> Icons.Default.Badge
                                            "প্রণোদনা ও ফি" -> Icons.Default.Percent
                                            else -> Icons.Default.Security
                                        },
                                        contentDescription = null,
                                        tint = when (faq.category) {
                                            "লিমিট ও সীমা" -> Color(0xFF2563EB)
                                            "প্রসেসিং সময়" -> Color(0xFFD97706)
                                            "ডকুমেন্ট ও KYC" -> PurpleSecondary
                                            "প্রণোদনা ও ফি" -> SuccessGreen
                                            else -> MagentaPrimary
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFF3F4F6))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = faq.category,
                                            fontSize = 9.sp,
                                            color = GrayText,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = faq.question,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = DarkText
                                    )
                                }
                            }
                            Icon(
                                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (expanded) "Collapse" else "Expand",
                                tint = if (expanded) MagentaPrimary else GrayText,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                        AnimatedVisibility(visible = expanded) {
                            Column {
                                HorizontalDivider(
                                    color = LightGrayBorder.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                                Text(
                                    text = faq.answer,
                                    fontSize = 12.sp,
                                    color = Color(0xFF374151),
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
