package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.RemittanceTransaction
import com.example.ui.theme.*

/**
 * Data model representing monthly remittance inflows, outflows, and incentive rewards.
 */
data class MonthlyRemittanceData(
    val monthShortBn: String,
    val monthFullNameBn: String,
    val year: String,
    val outflowForeign: Double, // in SAR or local sending currency
    val inflowBdt: Double,       // in BDT sent to Bangladesh
    val incentiveBdt: Double,   // 2.5% Bangladesh government incentive
    val txCount: Int,
    val bkashShareBdt: Double = 0.0,
    val bankShareBdt: Double = 0.0,
    val nagadShareBdt: Double = 0.0
)

enum class ChartMetricType(val titleBn: String) {
    INFLOW_OUTFLOW("প্রবাহ (ইনফ্লো/আউটফ্লো)"),
    GOVT_INCENTIVE("২.৫% সরকারি প্রণোদনা"),
    CHANNELS("মাধ্যম বণ্টন (বিকাশ/ব্যাংক/নগদ)")
}

enum class ChartTimeframe(val labelBn: String) {
    SIX_MONTHS("শেষ ৬ মাস"),
    TWELVE_MONTHS("শেষ ১ বছর")
}

@Composable
fun RemittanceAnalyticsChartCard(
    transactions: List<RemittanceTransaction>,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    var selectedMetric by remember { mutableStateOf(ChartMetricType.INFLOW_OUTFLOW) }
    var selectedTimeframe by remember { mutableStateOf(ChartTimeframe.SIX_MONTHS) }
    
    // Process and synthesize monthly historical remittance dataset
    val monthlyDataList = remember(transactions, selectedTimeframe) {
        generateMonthlyData(transactions, selectedTimeframe)
    }

    var selectedIndex by remember(monthlyDataList) {
        mutableIntStateOf(monthlyDataList.size - 1)
    }

    val selectedData = monthlyDataList.getOrElse(selectedIndex) { monthlyDataList.last() }

    // Summary calculations
    val totalInflowBdt = remember(monthlyDataList) { monthlyDataList.sumOf { it.inflowBdt } }
    val totalGovtIncentive = remember(monthlyDataList) { monthlyDataList.sumOf { it.incentiveBdt } }
    val totalTxCount = remember(monthlyDataList) { monthlyDataList.sumOf { it.txCount } }
    val avgMonthlyBdt = if (monthlyDataList.isNotEmpty()) totalInflowBdt / monthlyDataList.size else 0.0

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("remittance_analytics_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with toggle collapse button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MagentaPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MagentaPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "মাসিক রেমিট্যান্স সামারি চার্ট",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                        Text(
                            text = "ইনফ্লো, আউটফ্লো ও সরকারি বোনাস ট্রেন্ড",
                            fontSize = 11.sp,
                            color = GrayText
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = GrayText
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(14.dp))

                    // Timeframe & View Mode Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Timeframe Pill Selector
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(SoftPinkBackground)
                                .padding(3.dp)
                        ) {
                            ChartTimeframe.entries.forEach { tf ->
                                val isSelected = selectedTimeframe == tf
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MagentaPrimary else Color.Transparent)
                                        .clickable { selectedTimeframe = tf }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = tf.labelBn,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else GrayText
                                    )
                                }
                            }
                        }

                        // Metric Type Switcher Tabs
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AmberAccent.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "+২.৫% বোনাস সহ",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Metric Category Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ChartMetricType.entries.forEach { metric ->
                            val isSelected = selectedMetric == metric
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) PurpleSecondary else Color(0xFFF3F4F6))
                                    .clickable { selectedMetric = metric }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (metric) {
                                        ChartMetricType.INFLOW_OUTFLOW -> "প্রবাহ ট্রেন্ড"
                                        ChartMetricType.GOVT_INCENTIVE -> "প্রণোদনা (+২.৫%)"
                                        ChartMetricType.CHANNELS -> "চ্যানেল বণ্টন"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else DarkText,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Top KPI Stats Ribbon
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SoftPinkBackground)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "মোট প্রাপ্ত রেমিট্যান্স",
                                fontSize = 10.sp,
                                color = GrayText
                            )
                            Text(
                                text = "৳ ${formatBdtShort(totalInflowBdt)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MagentaPrimary
                            )
                        }

                        Column {
                            Text(
                                text = "সরকারি বোনাস",
                                fontSize = 10.sp,
                                color = GrayText
                            )
                            Text(
                                text = "৳ ${formatBdtShort(totalGovtIncentive)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }

                        Column {
                            Text(
                                text = "মোট সফল লেনদেন",
                                fontSize = 10.sp,
                                color = GrayText
                            )
                            Text(
                                text = "$totalTxCount টি",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurpleSecondary
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Visual Chart Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .padding(horizontal = 4.dp)
                    ) {
                        when (selectedMetric) {
                            ChartMetricType.INFLOW_OUTFLOW -> {
                                InflowOutflowComposeChart(
                                    data = monthlyDataList,
                                    selectedIndex = selectedIndex,
                                    onSelectIndex = { selectedIndex = it }
                                )
                            }
                            ChartMetricType.GOVT_INCENTIVE -> {
                                GovtIncentiveComposeChart(
                                    data = monthlyDataList,
                                    selectedIndex = selectedIndex,
                                    onSelectIndex = { selectedIndex = it }
                                )
                            }
                            ChartMetricType.CHANNELS -> {
                                ChannelBreakdownComposeChart(
                                    data = monthlyDataList,
                                    selectedIndex = selectedIndex,
                                    onSelectIndex = { selectedIndex = it }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Interactive Tooltip / Selected Month Detail Card (Recharts style)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LightGrayBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MagentaPrimary)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "${selectedData.monthFullNameBn} ${selectedData.year}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkText
                                    )
                                }
                                Text(
                                    text = "${selectedData.txCount} টি সফল রেমিট্যান্স",
                                    fontSize = 11.sp,
                                    color = GrayText,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("📤 প্রেরিত অর্থ (আউটফ্লো)", fontSize = 10.sp, color = GrayText)
                                    Text(
                                        "${String.format("%,.0f", selectedData.outflowForeign)} SAR",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DarkText
                                    )
                                }

                                Column {
                                    Text("📥 বাংলাদেশে প্রাপ্ত (ইনফ্লো)", fontSize = 10.sp, color = GrayText)
                                    Text(
                                        "৳ ${String.format("%,.0f", selectedData.inflowBdt)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MagentaPrimary
                                    )
                                }

                                Column {
                                    Text("🎁 সরকারি ২.৫% লাভ", fontSize = 10.sp, color = GrayText)
                                    Text(
                                        "+৳ ${String.format("%,.0f", selectedData.incentiveBdt)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Legend description
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChartLegendItem(color = MagentaPrimary, label = "ইনফ্লো (BDT)")
                        Spacer(Modifier.width(16.dp))
                        ChartLegendItem(color = PurpleSecondary, label = "আউটফ্লো (SAR)")
                        Spacer(Modifier.width(16.dp))
                        ChartLegendItem(color = SuccessGreen, label = "২.৫% বোনাস")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = GrayText)
    }
}

/**
 * Visualizes Dual-Bar & Trend Line Chart for Inflow (BDT received) and Outflow (Sending currency)
 */
@Composable
private fun InflowOutflowComposeChart(
    data: List<MonthlyRemittanceData>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val maxInflow = remember(data) { (data.maxOfOrNull { it.inflowBdt } ?: 100000.0) * 1.15 }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(data) {
                detectTapGestures { offset ->
                    val barWidthArea = size.width / data.size
                    val index = (offset.x / barWidthArea).toInt().coerceIn(0, data.size - 1)
                    onSelectIndex(index)
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val bottomY = height - 24.dp.toPx()
        val chartHeight = bottomY - 12.dp.toPx()
        val numBars = data.size
        val colWidth = width / numBars
        val barGroupWidth = colWidth * 0.55f
        val individualBarWidth = barGroupWidth / 2.2f

        // Draw horizontal subtle grid lines
        val gridLines = 3
        for (i in 0..gridLines) {
            val y = bottomY - (chartHeight / gridLines) * i
            drawLine(
                color = Color(0xFFF3F4F6),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )
        }

        // Draw Bars & Base Labels
        data.forEachIndexed { i, item ->
            val centerX = i * colWidth + colWidth / 2f
            val isSelected = i == selectedIndex

            // Selection background column highlight
            if (isSelected) {
                drawRoundRect(
                    color = MagentaPrimary.copy(alpha = 0.08f),
                    topLeft = Offset(i * colWidth + 2.dp.toPx(), 4.dp.toPx()),
                    size = Size(colWidth - 4.dp.toPx(), bottomY - 4.dp.toPx()),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
            }

            // Inflow Bar Height
            val inflowFraction = (item.inflowBdt / maxInflow).toFloat().coerceIn(0.05f, 1f)
            val inflowBarH = chartHeight * inflowFraction
            val inflowTopY = bottomY - inflowBarH

            // Outflow Bar Height (normalized to scale)
            val outflowNormalized = item.outflowForeign * 32.5
            val outflowFraction = (outflowNormalized / maxInflow).toFloat().coerceIn(0.04f, 1f)
            val outflowBarH = chartHeight * outflowFraction
            val outflowTopY = bottomY - outflowBarH

            val leftOutflowX = centerX - barGroupWidth / 2f
            val rightInflowX = leftOutflowX + individualBarWidth + 2.dp.toPx()

            // Draw Outflow Bar (Purple Gradient)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = if (isSelected) listOf(PurpleSecondary, Color(0xFF4C1D95)) else listOf(PurpleSecondary.copy(alpha = 0.7f), PurpleSecondary.copy(alpha = 0.4f))
                ),
                topLeft = Offset(leftOutflowX, outflowTopY),
                size = Size(individualBarWidth, outflowBarH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // Draw Inflow Bar (Magenta Gradient)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = if (isSelected) listOf(MagentaPrimary, Color(0xFFBE185D)) else listOf(MagentaPrimary.copy(alpha = 0.75f), MagentaPrimary.copy(alpha = 0.45f))
                ),
                topLeft = Offset(rightInflowX, inflowTopY),
                size = Size(individualBarWidth, inflowBarH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // Draw Base Month Label Indicator
            drawCircle(
                color = if (isSelected) MagentaPrimary else Color(0xFF9CA3AF),
                radius = if (isSelected) 3.5.dp.toPx() else 2.dp.toPx(),
                center = Offset(centerX, bottomY + 12.dp.toPx())
            )
        }

        // Draw Inflow Trajectory Trend Line (Curved Area & Line)
        val trendPath = Path()
        val areaPath = Path()

        data.forEachIndexed { i, item ->
            val centerX = i * colWidth + colWidth / 2f
            val inflowFraction = (item.inflowBdt / maxInflow).toFloat().coerceIn(0.05f, 1f)
            val inflowTopY = bottomY - chartHeight * inflowFraction

            if (i == 0) {
                trendPath.moveTo(centerX, inflowTopY)
                areaPath.moveTo(centerX, bottomY)
                areaPath.lineTo(centerX, inflowTopY)
            } else {
                val prevX = (i - 1) * colWidth + colWidth / 2f
                val prevInflowFraction = (data[i - 1].inflowBdt / maxInflow).toFloat().coerceIn(0.05f, 1f)
                val prevY = bottomY - chartHeight * prevInflowFraction
                val cX = (prevX + centerX) / 2f
                trendPath.cubicTo(cX, prevY, cX, inflowTopY, centerX, inflowTopY)
                areaPath.cubicTo(cX, prevY, cX, inflowTopY, centerX, inflowTopY)
            }

            if (i == data.size - 1) {
                areaPath.lineTo(centerX, bottomY)
                areaPath.close()
            }
        }

        // Draw trend gradient area fill
        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(MagentaPrimary.copy(alpha = 0.12f), Color.Transparent),
                startY = 0f,
                endY = bottomY
            )
        )

        // Draw trend stroke line
        drawPath(
            path = trendPath,
            color = Color(0xFFF43F5E),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw active point highlight
        val selectedCenterX = selectedIndex * colWidth + colWidth / 2f
        val selectedInflowFraction = (data[selectedIndex].inflowBdt / maxInflow).toFloat().coerceIn(0.05f, 1f)
        val selectedInflowTopY = bottomY - chartHeight * selectedInflowFraction

        drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(),
            center = Offset(selectedCenterX, selectedInflowTopY)
        )
        drawCircle(
            color = MagentaPrimary,
            radius = 4.dp.toPx(),
            center = Offset(selectedCenterX, selectedInflowTopY)
        )
    }

    // X-Axis Month Text Labels underneath
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 168.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        data.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            Text(
                text = item.monthShortBn,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MagentaPrimary else GrayText,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Visualizes 2.5% Bangladesh Government Cash Incentive Rewards
 */
@Composable
private fun GovtIncentiveComposeChart(
    data: List<MonthlyRemittanceData>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val maxIncentive = remember(data) { (data.maxOfOrNull { it.incentiveBdt } ?: 3000.0) * 1.2 }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(data) {
                detectTapGestures { offset ->
                    val barWidthArea = size.width / data.size
                    val index = (offset.x / barWidthArea).toInt().coerceIn(0, data.size - 1)
                    onSelectIndex(index)
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val bottomY = height - 24.dp.toPx()
        val chartHeight = bottomY - 12.dp.toPx()
        val numBars = data.size
        val colWidth = width / numBars
        val barWidth = colWidth * 0.45f

        // Draw horizontal grid lines
        for (i in 0..3) {
            val y = bottomY - (chartHeight / 3) * i
            drawLine(
                color = Color(0xFFF3F4F6),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        data.forEachIndexed { i, item ->
            val centerX = i * colWidth + colWidth / 2f
            val isSelected = i == selectedIndex

            if (isSelected) {
                drawRoundRect(
                    color = SuccessGreen.copy(alpha = 0.08f),
                    topLeft = Offset(i * colWidth + 2.dp.toPx(), 4.dp.toPx()),
                    size = Size(colWidth - 4.dp.toPx(), bottomY - 4.dp.toPx()),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
            }

            val fraction = (item.incentiveBdt / maxIncentive).toFloat().coerceIn(0.05f, 1f)
            val barH = chartHeight * fraction
            val topY = bottomY - barH

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = if (isSelected) listOf(SuccessGreen, Color(0xFF047857)) else listOf(SuccessGreen.copy(alpha = 0.7f), SuccessGreen.copy(alpha = 0.35f))
                ),
                topLeft = Offset(centerX - barWidth / 2f, topY),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 168.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        data.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            Text(
                text = item.monthShortBn,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) SuccessGreen else GrayText,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Visualizes Remittance Delivery Channels Distribution (bKash vs Bank vs Nagad)
 */
@Composable
private fun ChannelBreakdownComposeChart(
    data: List<MonthlyRemittanceData>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val currentMonth = data.getOrElse(selectedIndex) { data.last() }
    val totalMonthBdt = currentMonth.inflowBdt.coerceAtLeast(1.0)
    
    val bkashRatio = (currentMonth.bkashShareBdt / totalMonthBdt).toFloat()
    val bankRatio = (currentMonth.bankShareBdt / totalMonthBdt).toFloat()
    val nagadRatio = (currentMonth.nagadShareBdt / totalMonthBdt).toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceAround
    ) {
        ChannelProgressRow(
            label = "বিকাশ (bKash Wallet)",
            amountBdt = currentMonth.bkashShareBdt,
            ratio = bkashRatio,
            color = MagentaPrimary
        )
        ChannelProgressRow(
            label = "ব্যাংক একাউন্ট (Bank A/C)",
            amountBdt = currentMonth.bankShareBdt,
            ratio = bankRatio,
            color = PurpleSecondary
        )
        ChannelProgressRow(
            label = "নগদ ওয়ালেট (Nagad)",
            amountBdt = currentMonth.nagadShareBdt,
            ratio = nagadRatio,
            color = Color(0xFFF97316)
        )
    }
}

@Composable
private fun ChannelProgressRow(
    label: String,
    amountBdt: Double,
    ratio: Float,
    color: Color
) {
    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "ratio"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = DarkText)
            Text(
                "৳ ${String.format("%,.0f", amountBdt)} (${(ratio * 100).toInt()}%)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFF3F4F6))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedRatio.coerceIn(0.02f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

/**
 * Formatter helper for BDT numbers
 */
private fun formatBdtShort(amount: Double): String {
    return when {
        amount >= 100000 -> String.format("%.1f লাখ", amount / 100000.0)
        amount >= 1000 -> String.format("%.0f হাজার", amount / 1000.0)
        else -> String.format("%,.0f", amount)
    }
}

/**
 * Synthesizes and aggregates transactions into structured monthly datasets
 */
private fun generateMonthlyData(
    transactions: List<RemittanceTransaction>,
    timeframe: ChartTimeframe
): List<MonthlyRemittanceData> {
    val baseMonths = listOf(
        MonthlyRemittanceData("মার্চ", "মার্চ", "২০২৬", 2800.0, 90860.0, 2271.5, 2, 45000.0, 45860.0, 0.0),
        MonthlyRemittanceData("এপ্রিল", "এপ্রিল", "২০২৬", 3200.0, 103840.0, 2596.0, 3, 55000.0, 48840.0, 0.0),
        MonthlyRemittanceData("মে", "মে", "২০২৬", 1500.0, 48675.0, 1216.8, 1, 48675.0, 0.0, 0.0),
        MonthlyRemittanceData("জুন", "জুন", "২০২৬", 4000.0, 129800.0, 3245.0, 2, 60000.0, 69800.0, 0.0),
        MonthlyRemittanceData("জুলাই", "জুলাই", "২০২৬", 3500.0, 113575.0, 2839.3, 2, 65000.0, 48575.0, 0.0),
        MonthlyRemittanceData("আগস্ট", "আগস্ট", "২০২৬", 8000.0, 259225.0, 6480.6, 3, 64900.0, 145800.0, 48525.0)
    )

    val extended12Months = listOf(
        MonthlyRemittanceData("সেপ্টে", "সেপ্টেম্বর", "২০২৫", 1800.0, 58410.0, 1460.2, 1, 30000.0, 28410.0, 0.0),
        MonthlyRemittanceData("অক্টো", "অক্টোবর", "২০২৫", 2200.0, 71390.0, 1784.7, 2, 40000.0, 31390.0, 0.0),
        MonthlyRemittanceData("নভে", "নভেম্বর", "২০২৫", 2000.0, 64900.0, 1622.5, 1, 64900.0, 0.0, 0.0),
        MonthlyRemittanceData("ডিসে", "ডিসেম্বর", "২০২৫", 4500.0, 146025.0, 3650.6, 3, 70000.0, 76025.0, 0.0),
        MonthlyRemittanceData("জানু", "জানুয়ারি", "২০২৬", 2100.0, 68145.0, 1703.6, 1, 35000.0, 33145.0, 0.0),
        MonthlyRemittanceData("ফেব্রু", "ফেব্রুয়ারি", "২০২৬", 2400.0, 77880.0, 1947.0, 2, 40000.0, 37880.0, 0.0)
    ) + baseMonths

    // Integrate live real-time transactions dynamically into current month (আগস্ট)
    val augustTx = transactions.filter { it.createdAt.contains("আগস্ট") || it.createdAt.contains("August") }
    if (augustTx.isNotEmpty()) {
        val totalAugInflow = augustTx.sumOf { it.recipientBdtAmount }
        val totalAugOutflow = augustTx.sumOf { it.sendAmount }
        val totalAugIncentive = augustTx.sumOf { it.govtIncentiveBdt }
        val bkashSum = augustTx.filter { it.recipientChannel.contains("বিকাশ") || it.recipientChannel.contains("bKash") }.sumOf { it.recipientBdtAmount }
        val bankSum = augustTx.filter { it.recipientChannel.contains("ব্যাংক") || it.recipientChannel.contains("Bank") }.sumOf { it.recipientBdtAmount }
        val nagadSum = augustTx.filter { it.recipientChannel.contains("নগদ") || it.recipientChannel.contains("Nagad") }.sumOf { it.recipientBdtAmount }

        val updatedAugust = MonthlyRemittanceData(
            monthShortBn = "আগস্ট",
            monthFullNameBn = "আগস্ট",
            year = "২০২৬",
            outflowForeign = totalAugOutflow.coerceAtLeast(8000.0),
            inflowBdt = totalAugInflow.coerceAtLeast(259225.0),
            incentiveBdt = totalAugIncentive.coerceAtLeast(6480.6),
            txCount = augustTx.size.coerceAtLeast(3),
            bkashShareBdt = bkashSum.coerceAtLeast(64900.0),
            bankShareBdt = bankSum.coerceAtLeast(145800.0),
            nagadShareBdt = nagadSum.coerceAtLeast(48525.0)
        )

        return when (timeframe) {
            ChartTimeframe.SIX_MONTHS -> baseMonths.dropLast(1) + updatedAugust
            ChartTimeframe.TWELVE_MONTHS -> extended12Months.dropLast(1) + updatedAugust
        }
    }

    return when (timeframe) {
        ChartTimeframe.SIX_MONTHS -> baseMonths
        ChartTimeframe.TWELVE_MONTHS -> extended12Months
    }
}
