package com.calculator.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.calculator.core.ui.R
import com.calculator.core.ui.theme.NeonCyan
import com.calculator.core.ui.theme.ElectricViolet
import com.calculator.core.ui.theme.TextPrimary
import com.calculator.core.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun PremiumOverlay(
    onUnlockSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdDialog by remember { mutableStateOf(false) }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🔒 " + stringResource(R.string.prem_title),
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.prem_description),
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { showAdDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "📺 " + stringResource(R.string.prem_btn_watch),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }

    if (showAdDialog) {
        SimulatedAdDialog(
            onAdCompleted = {
                showAdDialog = false
                onUnlockSuccess()
            },
            onDismiss = { showAdDialog = false }
        )
    }
}

@Composable
fun SimulatedAdDialog(
    onAdCompleted: () -> Unit,
    onDismiss: () -> Unit
) {
    var timeLeft by remember { mutableStateOf(5) }
    var adFinished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
        adFinished = true
    }

    Dialog(onDismissRequest = { if (adFinished) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.ad_dialog_title),
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x0AFFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🎁 " + stringResource(R.string.ad_dialog_success_title),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.ad_dialog_success_desc),
                            color = TextSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (!adFinished) {
                    LinearProgressIndicator(
                        progress = (5 - timeLeft) / 5f,
                        color = NeonCyan,
                        trackColor = Color(0x1AFFFFFF),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "..." + timeLeft,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                } else {
                    Button(
                        onClick = onAdCompleted,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.ad_dialog_ok),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumTimerBadge(
    unlockUntilTimestamp: Long,
    modifier: Modifier = Modifier
) {
    var remainingTimeText by remember { mutableStateOf("") }

    LaunchedEffect(unlockUntilTimestamp) {
        while (true) {
            val diff = unlockUntilTimestamp - System.currentTimeMillis()
            if (diff <= 0) {
                remainingTimeText = "00:00:00"
                break
            }
            val hours = diff / (3600 * 1000)
            val minutes = (diff % (3600 * 1000)) / (60 * 1000)
            val seconds = (diff % (60 * 1000)) / 1000
            remainingTimeText = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
            delay(1000L)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x1100E5FF))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = "⚡ PRO: $remainingTimeText",
            color = NeonCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
