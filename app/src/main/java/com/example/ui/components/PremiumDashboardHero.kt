package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PremiumDashboardHero(
    userName: String,
    monthSpend: Double,
    transactionCount: Int,
    onBudgetsClick: () -> Unit,
    onBillsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF075E43), Color(0xFF003B32), Color(0xFF082C35))
                    )
                )
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.White.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("GOOD TO SEE YOU", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB7F7D8), fontWeight = FontWeight.Bold)
                        Text("Hi, ${userName.ifBlank { "there" }}", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF8EF0C1))
                }
                Text(monthLabel, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.78f))
                Text(
                    String.format(Locale.US, "R%,.2f", monthSpend),
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text("spent this month", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.82f))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(color = Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFFB7F7D8))
                            Spacer(Modifier.width(7.dp))
                            Text("$transactionCount transactions", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onBudgetsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB7F7D8), contentColor = Color(0xFF003B32)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("My budgets", fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = onBillsClick, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Upcoming bills")
                    }
                }
            }
        }
    }
}
