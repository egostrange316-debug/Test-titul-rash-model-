package com.example.ui.tabs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.OmrScanEntity
import com.example.ui.OmrViewModel
import com.example.ui.components.JsonViewCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class HistorySortOption(val title: String) {
    DATE_DESC("Yangi sana ↓"),
    DATE_ASC("Eski sana ↑"),
    SCORE_DESC("Yuqori ball ↓"),
    SCORE_ASC("Past ball ↑"),
    ID_ASC("Talaba ID (A-Z)")
}

@Composable
fun HistoryTab(
    viewModel: OmrViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scans by viewModel.scansHistory.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedSort by remember { mutableStateOf(HistorySortOption.DATE_DESC) }
    var selectedStatusFilter by remember { mutableIntStateOf(0) } // 0: Barchasi, 1: SUCCESS, 2: KOD_XATO
    var selectedScanForDetail by remember { mutableStateOf<OmrScanEntity?>(null) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    // Filtering and Sorting pipeline
    val processedScans = remember(scans, searchQuery, selectedSort, selectedStatusFilter) {
        var list = scans

        // 1. Status Filter
        if (selectedStatusFilter == 1) {
            list = list.filter { it.status == "SUCCESS" }
        } else if (selectedStatusFilter == 2) {
            list = list.filter { it.status == "KOD_XATO" }
        }

        // 2. Search Query
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                (it.studentId ?: "").contains(searchQuery, ignoreCase = true) ||
                it.status.contains(searchQuery, ignoreCase = true)
            }
        }

        // 3. Sorting
        when (selectedSort) {
            HistorySortOption.DATE_DESC -> list.sortedByDescending { it.timestamp }
            HistorySortOption.DATE_ASC -> list.sortedBy { it.timestamp }
            HistorySortOption.SCORE_DESC -> list.sortedByDescending { it.scorePercentage }
            HistorySortOption.SCORE_ASC -> list.sortedBy { it.scorePercentage }
            HistorySortOption.ID_ASC -> list.sortedBy { it.studentId ?: "" }
        }
    }

    // Detail Dialog
    if (selectedScanForDetail != null) {
        val scan = selectedScanForDetail!!
        val isSuccess = scan.status == "SUCCESS"
        val dateStr = SimpleDateFormat("dd.MM.yyyy, HH:mm:ss", Locale.getDefault()).format(Date(scan.timestamp))

        AlertDialog(
            onDismissRequest = { selectedScanForDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isSuccess) Color(0xFF10B981) else Color(0xFFEF4444),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSuccess) "Talaba ID: ${scan.studentId}" else "Xatolik: KOD_XATO",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Stats Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSuccess) Color(0xFFECFDF5) else Color(0xFFFEF2F2)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Sana: $dateStr",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (isSuccess) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("To'g'ri javoblar:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${scan.correctCount} / ${scan.totalQuestions}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Noto'g'ri / Bo'sh:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${scan.incorrectCount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Ball foizi:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${scan.scorePercentage}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            } else {
                                Text(
                                    text = "10 talik ID kod to'liq yoki doirachaga mos ravishda belgilanmagani sababli tekshirish to'xtatilgan.",
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF991B1B)
                                )
                            }
                        }
                    }

                    // Raw JSON View
                    Text("Tizimning toza JSON javobi:", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    JsonViewCard(jsonContent = scan.rawJson)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("OMR JSON", scan.rawJson))
                        Toast.makeText(context, "JSON nusxalandi", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nusxalash")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedScanForDetail = null }) {
                    Text("Yopish")
                }
            }
        )
    }

    // Clear confirmation dialog
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Tarixni tozalash") },
            text = { Text("Haqiqatan ham barcha tekshiruv natijalarini o'chirib tashlamoqchimisiz?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearConfirmation = false
                    }
                ) {
                    Text("O'chirish", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Bekor qilish")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search & Clear / Share Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ID yoki status bo'yicha...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Tozalash", modifier = Modifier.size(18.dp))
                        }
                    }
                } else null,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (scans.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))

                // Export / Share History CSV
                IconButton(
                    onClick = {
                        val report = buildString {
                            appendLine("📊 OMR TEST TEKSHIRUVLARI HISOBOTI")
                            appendLine("Sana: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}")
                            appendLine("Jami varaqalar: ${processedScans.size} ta")
                            appendLine("--------------------------------------------")
                            appendLine("№ | Talaba ID | Natija | Foiz | Holat | Sana")
                            processedScans.forEachIndexed { idx, item ->
                                val d = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()).format(Date(item.timestamp))
                                appendLine("${idx + 1}. ID: ${item.studentId ?: "XATO"} | ${item.correctCount}/${item.totalQuestions} | ${item.scorePercentage}% | ${item.status} | $d")
                            }
                        }
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, report)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Hisobotni ulashish"))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Hisobotni ulashish",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = { showClearConfirmation = true }) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Barchasini o'chirish",
                        tint = Color(0xFFDC2626)
                    )
                }
            }
        }

        // Sorting Option Chips (Sana, Ball, ID)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))

            HistorySortOption.values().forEach { option ->
                FilterChip(
                    selected = selectedSort == option,
                    onClick = { selectedSort = option },
                    label = { Text(option.title, fontSize = 11.5.sp) }
                )
            }
        }

        // Status Filter Row (Barchasi, SUCCESS, KOD_XATO)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FilterList, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))

            FilterChip(
                selected = selectedStatusFilter == 0,
                onClick = { selectedStatusFilter = 0 },
                label = { Text("Barchasi (${scans.size})", fontSize = 11.5.sp) }
            )
            FilterChip(
                selected = selectedStatusFilter == 1,
                onClick = { selectedStatusFilter = 1 },
                label = { Text("Muvaffaqiyatli (${scans.count { it.status == "SUCCESS" }})", fontSize = 11.5.sp) }
            )
            FilterChip(
                selected = selectedStatusFilter == 2,
                onClick = { selectedStatusFilter = 2 },
                label = { Text("Xato Kodlar (${scans.count { it.status == "KOD_XATO" }})", fontSize = 11.5.sp) }
            )
        }

        // Results List or Empty Placeholder
        if (processedScans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isEmpty()) "Hozircha tekshiruvlar tarixi mavjud emas" else "Qidiruv bo'yicha hech narsa topilmadi",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(processedScans, key = { it.id }) { scan ->
                    ScanHistoryItem(
                        scan = scan,
                        onClick = { selectedScanForDetail = scan },
                        onDelete = { viewModel.deleteScan(scan) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanHistoryItem(
    scan: OmrScanEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isSuccess = scan.status == "SUCCESS"
    val dateStr = remember(scan.timestamp) {
        SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault()).format(Date(scan.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (isSuccess) Color(0xFF10B981) else Color(0xFFEF4444),
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isSuccess) "ID: ${scan.studentId}" else "KOD_XATO",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (isSuccess) FontFamily.Monospace else FontFamily.Default,
                            color = if (isSuccess) Color(0xFF0F172A) else Color(0xFFDC2626)
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = if (isSuccess) "${scan.correctCount}/${scan.totalQuestions} to'g'ri (${scan.scorePercentage}%)" else "ID kod xatoligi sababli to'xtatilgan",
                        fontSize = 12.5.sp,
                        color = if (isSuccess) Color(0xFF059669) else Color(0xFFB91C1C),
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "O'chirish",
                    tint = Color.LightGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
