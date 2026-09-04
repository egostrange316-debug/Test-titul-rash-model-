package com.example.ui.tabs

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.OmrCheckResult
import com.example.ui.OmrViewModel
import com.example.ui.components.JsonViewCard
import com.example.ui.components.QuestionMatrixView
import com.example.ui.components.SimulatorBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerTab(
    viewModel: OmrViewModel,
    onNavigateToKeys: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedBitmap by viewModel.selectedBitmap.collectAsStateWithLifecycle()
    val imageSource by viewModel.imageSource.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val checkResult by viewModel.checkResult.collectAsStateWithLifecycle()
    val answerKey by viewModel.answerKey.collectAsStateWithLifecycle()
    val useGemini by viewModel.useGemini.collectAsStateWithLifecycle()
    val simulatedData by viewModel.simulatedData.collectAsStateWithLifecycle()

    var showSimulatorSheet by remember { mutableStateOf(false) }

    // Android Photo Picker (zero-permission, compliant with Play Policy)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                viewModel.setSelectedBitmap(bitmap, "Galereya")
            } catch (e: Exception) {
                Toast.makeText(context, "Rasmni yuklashda xatolik: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Camera Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.setSelectedBitmap(bitmap, "Kamera")
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "Kamerani ochishda xatolik: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Kamera ruxsati berilmadi. Galereyadan rasm yuklashingiz mumkin.", Toast.LENGTH_LONG).show()
        }
    }

    if (showSimulatorSheet) {
        SimulatorBottomSheet(
            viewModel = viewModel,
            simulatedData = simulatedData,
            onDismiss = { showSimulatorSheet = false },
            onApplyAndScan = {
                showSimulatorSheet = false
                viewModel.analyzeCurrentSheet()
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Source selection row
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Titul Varaqasini Yuklash:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Camera
                    OutlinedButton(
                        onClick = {
                            val hasCameraPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasCameraPermission) {
                                try {
                                    cameraLauncher.launch(null)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Kamerani ochishda xatolik: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Kamera", fontSize = 12.sp)
                    }

                    // Gallery
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Galereya", fontSize = 12.sp)
                    }

                    // Simulator
                    Button(
                        onClick = { showSimulatorSheet = true },
                        modifier = Modifier.weight(1.1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Simulator", fontSize = 12.sp)
                    }
                }
            }
        }

        // Active Sheet Preview
        if (selectedBitmap != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Yuklangan Titul (${imageSource ?: "Titul"})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }

                        if (imageSource == "Simulator") {
                            Text(
                                text = "Sozlash",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { showSimulatorSheet = true }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp))
                            .background(Color(0xFFF8FAFC)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = selectedBitmap!!.asImageBitmap(),
                            contentDescription = "OMR Titul",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        // Active Answer Key Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Kalit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = answerKey.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "30 ta to'g'ri javob kiritilgan",
                            fontSize = 11.5.sp,
                            color = Color.Gray
                        )
                    }
                }

                OutlinedButton(
                    onClick = onNavigateToKeys,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("O'zgartirish", fontSize = 11.5.sp)
                }
            }
        }

        // Engine Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (useGemini) "Gemini Vision AI (Tahlil)" else "Mahalliy Vision Engine",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (useGemini) "Gemini-3.1-pro-preview yuqori mantiqiy model" else "Ofline yuqori tezlikdagi kompyuter ko'rishi",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            Switch(
                checked = useGemini,
                onCheckedChange = { viewModel.setUseGemini(it) },
                thumbContent = if (useGemini) {
                    { Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp)) }
                } else null
            )
        }

        // Primary Action: Scan Button
        Button(
            onClick = { viewModel.analyzeCurrentSheet() },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            enabled = !isAnalyzing && selectedBitmap != null,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "Titul tahlil qilinmoqda...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "TITULNI TEKSHIRISH (OMR SCAN)", fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Analysis Results Display
        AnimatedVisibility(visible = checkResult != null) {
            val res = checkResult
            if (res != null) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ResultSummaryBanner(
                        result = res,
                        onOverrideId = { viewModel.overrideCurrentScanId(it) }
                    )

                    // Clean JSON display (mandated by prompt)
                    JsonViewCard(jsonContent = res.toCleanJson())

                    // Detailed question breakdown if success
                    if (res.isSuccess && res.question_details.isNotEmpty()) {
                        QuestionMatrixView(questionDetails = res.question_details)
                    }
                }
            }
        }
    }
}

@Composable
fun ResultSummaryBanner(
    result: OmrCheckResult,
    onOverrideId: (String) -> Unit = {}
) {
    val isSuccess = result.status == "SUCCESS"
    var showAdminIdDialog by remember { mutableStateOf(false) }

    if (showAdminIdDialog) {
        val keyboardController = LocalSoftwareKeyboardController.current
        var manualIdInput by remember { mutableStateOf(result.student_id ?: "") }
        AlertDialog(
            onDismissRequest = {
                keyboardController?.hide()
                showAdminIdDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Admin: 10 xonali ID kod", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = if (!isSuccess)
                            "Varaqadagi 10 talik kod xira bo'yalgan yoki o'qib bo'lmagan bo'lsa, uni qo'lda kiritishingiz mumkin. Kiritilgach, test to'liq tekshirib hisoblanadi."
                        else
                            "O'quvchining 10 xonali ID kodini tahrirlang:",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = manualIdInput,
                        onValueChange = { manualIdInput = it.filter { ch -> ch.isDigit() }.take(10) },
                        label = { Text("10 xonali ID kod") },
                        placeholder = { Text("Masalan: 7712345689") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { keyboardController?.hide() }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("${manualIdInput.length} / 10 ta raqam", color = if (manualIdInput.length == 10) Color(0xFF059669) else Color.Gray) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        if (manualIdInput.length == 10) {
                            onOverrideId(manualIdInput)
                            showAdminIdDialog = false
                        }
                    },
                    enabled = manualIdInput.length == 10
                ) {
                    Text("Qo'llash va Saqlash")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    keyboardController?.hide()
                    showAdminIdDialog = false
                }) {
                    Text("Bekor qilish")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSuccess) Color(0xFFECFDF5) else Color(0xFFFEF2F2)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSuccess) Color(0xFF10B981) else Color(0xFFEF4444)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isSuccess) Color(0xFF059669) else Color(0xFFDC2626),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSuccess) "STATUS: SUCCESS" else "STATUS: KOD_XATO",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSuccess) Color(0xFF065F46) else Color(0xFF991B1B)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSuccess) Color(0xFF10B981) else Color(0xFFEF4444))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isSuccess) "Muvaffaqiyatli" else "To'xtatildi",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!isSuccess) {
                // KOD_XATO specific explanation per prompt rules:
                Text(
                    text = "ID KOD XATOLIGI:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF991B1B)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Varaqaning 10 talik ID kod bloki to'liq belgilanmagan, xato bo'yalgan yoki o'qib bo'lmadi. Algoritm talabiga ko'ra tekshirish SHU JOYDA TO'XTATILDI. Ism-familiya va 30 ta test javoblari umuman tekshirilmadi.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Color(0xFF7F1D1D)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "student_id: null | Natija: 0%",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB91C1C)
                )
            } else {
                // Success Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "O'quvchi ID Kodi:", fontSize = 12.sp, color = Color.DarkGray)
                        Text(
                            text = result.student_id ?: "Noma'lum",
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Ball foizi:", fontSize = 12.sp, color = Color.DarkGray)
                        Text(
                            text = "${result.score_percentage}%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF059669)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { (result.correct_count / 30f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF10B981),
                    trackColor = Color(0xFFD1FAE5)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatPill("Jami", "${result.total_questions}", Color(0xFF334155))
                    StatPill("To'g'ri", "${result.correct_count}", Color(0xFF059669))
                    StatPill("Noto'g'ri", "${result.incorrect_count}", Color(0xFFDC2626))
                }
            }

            // Admin manual code entry / override button
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { showAdminIdDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSuccess) "Admin: ID kodni tahrirlash" else "Admin: 10 xonali ID kodni qo'lda kiritish",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = Color.Gray)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
