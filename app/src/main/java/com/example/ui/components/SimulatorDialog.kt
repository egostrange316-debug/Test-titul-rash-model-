package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.SimulatedSheetData
import com.example.ui.OmrViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorBottomSheet(
    viewModel: OmrViewModel,
    simulatedData: SimulatedSheetData,
    onDismiss: () -> Unit,
    onApplyAndScan: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyboardController = LocalSoftwareKeyboardController.current

    ModalBottomSheet(
        onDismissRequest = {
            keyboardController?.hide()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "OMR Titul Test Simulatori",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Titul varaqasini ekranda sozlashingiz yoki tayyor testlarni tekshirishingiz mumkin",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                IconButton(onClick = {
                    keyboardController?.hide()
                    onDismiss()
                }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Yopish")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Preset Scenarios
            Text(
                text = "Tayyor Test Stsenariylari:",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.loadSimulatorPreset("VALID_HIGH") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF059669))
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "To'g'ri ID (25/30)", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.loadSimulatorPreset("INVALID_ID_MISSING") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                ) {
                    Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "KOD_XATO (Chala ID)", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.loadSimulatorPreset("INVALID_ID_DOUBLE") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706))
                ) {
                    Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "KOD_XATO (2 ta belgi)", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.loadSimulatorPreset("INVALID_ID_OVERFLOW") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE11D48))
                ) {
                    Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "KOD_XATO (ID chetdan toshgan)", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.loadSimulatorPreset("OVERFLOW_ANSWERS") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF7C3AED))
                ) {
                    Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Javob doiradan toshgan", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 10-column ID Block Editor
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "10 Xonali ID Kod:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (simulatedData.isIdStrictlyValid) "Kod: ${simulatedData.formattedId}" else "XATO: To'liq emas!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (simulatedData.isIdStrictlyValid) Color(0xFF059669) else Color(0xFFDC2626)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    var adminIdInput by remember(simulatedData.formattedId) {
                        mutableStateOf(if (simulatedData.isIdStrictlyValid) (simulatedData.formattedId ?: "") else "")
                    }

                    OutlinedTextField(
                        value = adminIdInput,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }.take(10)
                            adminIdInput = digits
                            if (digits.length == 10) {
                                viewModel.setCustomId10Digits(digits)
                            }
                        },
                        label = { Text("Admin: 10 xonali kodni yozma kiritish", fontSize = 12.sp) },
                        placeholder = { Text("Masalan: 7712345689", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { keyboardController?.hide() }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text(
                                text = "10 ta raqam terilganda doirachalar avtomatik bo'yaladi (${adminIdInput.length}/10)",
                                fontSize = 11.sp
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 10 Columns
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (col in 0..9) {
                            val markedList = simulatedData.idColumns.getOrElse(col) { emptyList() }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (markedList.size == 1) "${markedList.first()}" else if (markedList.isEmpty()) "—" else "!",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (markedList.size == 1) Color(0xFF1E3A8A) else Color.Red
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                for (digit in 0..9) {
                                    val isSelected = digit in markedList
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(1.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color(0xFF0F172A) else Color.White)
                                            .border(1.dp, if (isSelected) Color(0xFF0F172A) else Color.Gray, CircleShape)
                                            .clickable {
                                                if (isSelected) {
                                                    viewModel.clearSimulatedIdCol(col)
                                                } else {
                                                    viewModel.updateSimulatedIdCol(col, digit)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$digit",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Apply & Scan Button
            Button(
                onClick = {
                    keyboardController?.hide()
                    onApplyAndScan()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Simulator Rasmiga Aylantirish va Tekshirish", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
