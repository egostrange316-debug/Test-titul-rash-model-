package com.example.ui.tabs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AnswerKey
import com.example.ui.OmrViewModel

@Composable
fun AnswerKeyTab(
    viewModel: OmrViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val answerKey by viewModel.answerKey.collectAsStateWithLifecycle()

    var titleText by remember(answerKey.title) { mutableStateOf(answerKey.title) }
    var rawKeyInput by remember { mutableStateOf("") }
    var showQuickInputBox by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableIntStateOf(0) } // 0: Barchasi, 1: 1-10, 2: 11-20, 3: 21-30

    val options = listOf("A", "B", "C", "D")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Teacher Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "To'g'ri Javoblar Kaliti",
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "O'qituvchi 30 ta savol kalitini kiritish paneli",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // Status Badge
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF059669).copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("30/30 Kalit", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = titleText,
                    onValueChange = {
                        titleText = it
                        viewModel.updateAnswerKey(answerKey.copy(title = it))
                    },
                    label = { Text("Fan va Variant nomi (Masalan: Matematika, 1-Variant)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Action Row: Quick Input Toggle & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showQuickInputBox = !showQuickInputBox },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (showQuickInputBox) "Kataklarga o'tish" else "Tezkor Matnli Kiritish", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val compact = answerKey.toCompactString()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("OMR Kalit", compact))
                            Toast.makeText(context, "Kalit nusxalandi: $compact", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    }

                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "📚 Test: ${answerKey.title}\n🔑 To'g'ri javoblar kaliti (30 ta):\n${answerKey.toFormattedString()}\nQisqa kod: ${answerKey.toCompactString()}"
                                )
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Kalitni ulashish"))
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Quick String Input Section
        AnimatedVisibility(visible = showQuickInputBox) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tezkor Matnli Kalit Kiritish:",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "30 ta harfni bitta qatorda yozing yoki nusxalang (Masalan: ABCDABCD...)",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val cleanCount = rawKeyInput.uppercase().filter { it in 'A'..'D' }.length

                    OutlinedTextField(
                        value = rawKeyInput,
                        onValueChange = { rawKeyInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Masalan: ABCDABCDABCDABCDABCDABCDABCDAB") },
                        shape = RoundedCornerShape(10.dp),
                        supportingText = {
                            Text("Aniqlangan harflar: $cleanCount / 30", color = if (cleanCount >= 30) Color(0xFF059669) else Color(0xFFE11D48))
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (cleanCount == 0) {
                                Toast.makeText(context, "Hech bo'lmaganda A, B, C, D harflarini kiriting", Toast.LENGTH_SHORT).show()
                            } else {
                                val parsed = AnswerKey.fromString(rawKeyInput, title = titleText)
                                viewModel.updateAnswerKey(parsed)
                                Toast.makeText(context, "30 ta savol kaliti muvaffaqiyatli o'rnatildi!", Toast.LENGTH_SHORT).show()
                                showQuickInputBox = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kalitni Qo'llash ($cleanCount ta harf)")
                    }
                }
            }
        }

        // Quick Preset Buttons
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Tezkor Shablonlar:",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val map = (1..30).associateWith { listOf("A", "B", "C", "D")[(it - 1) % 4] }
                            viewModel.updateAnswerKey(answerKey.copy(answers = map))
                            Toast.makeText(context, "A-B-C-D tartibida belgilandi", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("A-B-C-D Tartib", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.randomizeAnswerKey()
                            Toast.makeText(context, "Tasodifiy kalit yaratildi", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tasodifiy", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val map = (1..30).associateWith { "A" }
                            viewModel.updateAnswerKey(answerKey.copy(answers = map))
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Barchasi A", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val map = (1..30).associateWith { "B" }
                            viewModel.updateAnswerKey(answerKey.copy(answers = map))
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Barchasi B", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.resetAnswerKey()
                            Toast.makeText(context, "Tiklandi", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tiklash", fontSize = 12.sp)
                    }
                }
            }
        }

        // Section Filter Chips (Barchasi, 1-10, 11-20, 21-30)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedSection == 0,
                onClick = { selectedSection = 0 },
                label = { Text("Barchasi (1-30)") }
            )
            FilterChip(
                selected = selectedSection == 1,
                onClick = { selectedSection = 1 },
                label = { Text("1 - 10 Savollar") }
            )
            FilterChip(
                selected = selectedSection == 2,
                onClick = { selectedSection = 2 },
                label = { Text("11 - 20 Savollar") }
            )
            FilterChip(
                selected = selectedSection == 3,
                onClick = { selectedSection = 3 },
                label = { Text("21 - 30 Savollar") }
            )
        }

        // 30 Questions Teacher Key Grid
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                val currentQuestions = when (selectedSection) {
                    1 -> (1..10).toList()
                    2 -> (11..20).toList()
                    3 -> (21..30).toList()
                    else -> (1..30).toList()
                }

                Text(
                    text = "Savollar Kaliti (${currentQuestions.first()}-${currentQuestions.last()}):",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(10.dp))

                for (q in currentQuestions) {
                    val currentAns = answerKey.getAnswerFor(q)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Question Label
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = String.format("%02d", q),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "- savol",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        // Options A, B, C, D Pill Selectors
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (opt in options) {
                                val isSelected = currentAns == opt
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF8FAFC)
                                        )
                                        .border(
                                            1.5.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1),
                                            CircleShape
                                        )
                                        .clickable {
                                            viewModel.updateSingleAnswer(q, opt)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = opt,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFF1E293B)
                                    )
                                }
                            }
                        }
                    }

                    if (q % 5 == 0 && q < currentQuestions.last()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = Color(0xFFF1F5F9)
                        )
                    }
                }
            }
        }
    }
}
