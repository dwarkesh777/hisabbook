package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.CategoryPaymentShare
import com.example.viewmodel.PaymentMethodStats
import java.text.DecimalFormat

// Color palette matching the premium design system
private val OnlineColor = Color(0xFF3B82F6) // Recharts Electric Blue
private val CashColor = Color(0xFF10B981)   // Recharts Emerald Green
private val BackgroundSlate = Color(0xFF0F172A)
private val LightCardBorder = Color(0xFFE2E8F0)
private val TextPrimary = Color(0xFF1E293B)
private val TextSecondary = Color(0xFF64748B)

@Composable
fun AnalyticsScreen(
    stats: PaymentMethodStats,
    currencyFormatter: DecimalFormat
) {
    val totalExpense = stats.onlineTotal + stats.cashTotal
    val onlinePercentage = if (totalExpense > 0) (stats.onlineTotal / totalExpense).toFloat() else 0f
    val cashPercentage = if (totalExpense > 0) (stats.cashTotal / totalExpense).toFloat() else 0f

    var selectedSourceFilter by remember { mutableStateOf("All") } // "All", "Online", "Cash"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) // Soft, premium background slate
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Expense Source Analysis",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Visualize and understand your spending split across online payment networks and cash transactions.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Source Toggle Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1F5F9))
                    ) {
                        listOf("All", "Online", "Cash").forEach { option ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selectedSourceFilter == option) Color.White else Color.Transparent)
                                    .clickable { selectedSourceFilter = option }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedSourceFilter == option) TextPrimary else TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        if (totalExpense == 0.0) {
            // Empty State
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LightCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFFEFF6FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Analytics,
                                contentDescription = "Empty",
                                tint = OnlineColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Analytics Data Yet",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add some transactions with payment sources to view stunning distribution charts here.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // KPI Summary Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Online Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedSourceFilter = "Online" },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (selectedSourceFilter == "Online") 2.dp else 1.dp,
                            color = if (selectedSourceFilter == "Online") OnlineColor else LightCardBorder
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CreditCard,
                                    contentDescription = "Online",
                                    tint = OnlineColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "${(onlinePercentage * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = OnlineColor
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Online Source",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                            Text(
                                text = currencyFormatter.format(stats.onlineTotal),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                            )
                        }
                    }

                    // Cash Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedSourceFilter = "Cash" },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (selectedSourceFilter == "Cash") 2.dp else 1.dp,
                            color = if (selectedSourceFilter == "Cash") CashColor else LightCardBorder
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Payments,
                                    contentDescription = "Cash",
                                    tint = CashColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "${(cashPercentage * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = CashColor
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Cash Source",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                            Text(
                                text = currencyFormatter.format(stats.cashTotal),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                            )
                        }
                    }
                }
            }

            // High-Fidelity Donut Chart Module (Recharts style)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Distribution Share",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Relative split of total expenses",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // High-Fidelity Donut Display
                        Box(
                            modifier = Modifier.size(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Donut Ring using standard custom drawing / compose animations
                            val animSweep by animateFloatAsState(
                                targetValue = 360f,
                                animationSpec = tween(durationMillis = 1000),
                                label = "sweep"
                            )

                            androidx.compose.foundation.Canvas(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val strokeWidth = 24.dp.toPx()
                                val radius = (size.minDimension - strokeWidth) / 2
                                val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)

                                // Online Arc
                                drawArc(
                                    color = OnlineColor,
                                    startAngle = -90f,
                                    sweepAngle = (onlinePercentage * 360f) * (animSweep / 360f),
                                    useCenter = false,
                                    topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
                                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = strokeWidth,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )

                                // Cash Arc
                                drawArc(
                                    color = CashColor,
                                    startAngle = -90f + (onlinePercentage * 360f),
                                    sweepAngle = (cashPercentage * 360f) * (animSweep / 360f),
                                    useCenter = false,
                                    topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
                                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = strokeWidth,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                            }

                            // Center Text displaying total or selected source
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Total Spend",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                )
                                Text(
                                    text = currencyFormatter.format(totalExpense),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Custom Recharts Legends with Percentages
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(OnlineColor))
                                Column {
                                    Text("Online", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                                    Text("${(onlinePercentage * 100).toInt()}%", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(CashColor))
                                Column {
                                    Text("Cash", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                                    Text("${(cashPercentage * 100).toInt()}%", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                                }
                            }
                        }
                    }
                }
            }

            // Category Grouped Bar Chart (Recharts style)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Source Split by Category",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Comparative category metrics",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }

                        // Custom Recharts-style Horizontal comparative bar chart
                        val filteredBreakdown = remember(stats.categoryBreakdown, selectedSourceFilter) {
                            stats.categoryBreakdown.filter {
                                when (selectedSourceFilter) {
                                    "Online" -> it.onlineAmount > 0
                                    "Cash" -> it.cashAmount > 0
                                    else -> true
                                }
                            }
                        }

                        if (filteredBreakdown.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No transactions in selected filter.", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                filteredBreakdown.forEach { catShare ->
                                    val catTotal = catShare.onlineAmount + catShare.cashAmount
                                    val maxVal = stats.categoryBreakdown.maxOfOrNull { it.onlineAmount + it.cashAmount } ?: 1.0

                                    // Let's create a beautiful container
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Category Name & Total Label
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Category,
                                                    contentDescription = null,
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = catShare.category,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary
                                                    )
                                                )
                                            }
                                            Text(
                                                text = currencyFormatter.format(
                                                    when (selectedSourceFilter) {
                                                        "Online" -> catShare.onlineAmount
                                                        "Cash" -> catShare.cashAmount
                                                        else -> catTotal
                                                    }
                                                ),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            )
                                        }

                                        // Recharts-style comparative nested bar lines
                                        if (selectedSourceFilter == "All" || selectedSourceFilter == "Online") {
                                            val onlineWidthFraction = (catShare.onlineAmount / maxVal).toFloat().coerceIn(0.01f, 1f)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(onlineWidthFraction)
                                                        .height(8.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            Brush.horizontalGradient(
                                                                listOf(OnlineColor.copy(alpha = 0.7f), OnlineColor)
                                                            )
                                                        )
                                                )
                                                if (selectedSourceFilter == "All") {
                                                    Text(
                                                        text = "Online",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 8.sp,
                                                            color = TextSecondary
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        if (selectedSourceFilter == "All" || selectedSourceFilter == "Cash") {
                                            val cashWidthFraction = (catShare.cashAmount / maxVal).toFloat().coerceIn(0.01f, 1f)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(cashWidthFraction)
                                                        .height(8.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            Brush.horizontalGradient(
                                                                listOf(CashColor.copy(alpha = 0.7f), CashColor)
                                                            )
                                                        )
                                                )
                                                if (selectedSourceFilter == "All") {
                                                    Text(
                                                        text = "Cash",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 8.sp,
                                                            color = TextSecondary
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
