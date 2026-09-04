package com.example.ui.tabs

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Html
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.service.PdfGenerator
import com.example.ui.OmrViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidPdfBridge(private val onDownloadRequested: () -> Unit) {
    @JavascriptInterface
    fun downloadPdf() {
        onDownloadRequested()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PdfTab(
    viewModel: OmrViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPreview by remember { mutableStateOf(true) }
    var selectedViewMode by remember { mutableIntStateOf(1) } // Default to 1 (A4 Vektor Format) for stable, zero-GPU native rendering
    var webViewCrashed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            val bmp = Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            PdfGenerator.drawOmrSheet(canvas)
            withContext(Dispatchers.Main) {
                previewBitmap = bmp
                isLoadingPreview = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "OMR Titul Varaqasi (A4)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "30 talik test va 10 xonali ID kod bloki uchun",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Download Button
                Button(
                    onClick = { viewModel.downloadPdf(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Yuklab olish")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PDF Shaklida Yuklab Olish (A4)",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary Action Row (Open / Share)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.openPdf(context) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Chop etish / Ko'rish", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.sharePdf(context) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Ulashish", fontSize = 12.sp)
                    }
                }
            }
        }

        // View Mode Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedViewMode == 0,
                onClick = { selectedViewMode = 0 },
                label = { Text("HTML OMR Titul (Yuklab olish tugmali)") },
                leadingIcon = { Icon(Icons.Default.Html, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            FilterChip(
                selected = selectedViewMode == 1,
                onClick = { selectedViewMode = 1 },
                label = { Text("A4 Vektor Format") },
                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        // Interactive Sheet Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (selectedViewMode == 0) "HTML Titul Sahifasi (Yuklab olish tugmasi bilan):" else "A4 Vektor Chop Etish Ko'rinishi:",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (selectedViewMode == 0 && !webViewCrashed) {
                    // Interactive WebView with HTML and JavascriptInterface
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(520.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                                    setBackgroundColor(android.graphics.Color.WHITE)
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = false
                                    settings.loadWithOverviewMode = true
                                    settings.useWideViewPort = true
                                    settings.builtInZoomControls = true
                                    settings.displayZoomControls = false
                                    webViewClient = object : WebViewClient() {
                                        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                            webViewCrashed = true
                                            view?.destroy()
                                            return true
                                        }
                                    }
                                    addJavascriptInterface(
                                        AndroidPdfBridge {
                                            post { viewModel.downloadPdf(ctx) }
                                        },
                                        "AndroidInterface"
                                    )
                                    loadUrl("file:///android_asset/omr_sheet.html")
                                }
                            },
                            onRelease = { webView ->
                                webView.stopLoading()
                                webView.destroy()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    if (selectedViewMode == 0 && webViewCrashed) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Emulatorda render cheklovi sababli A4 vektor rejimiga o'tildi. Titul varaqasini bemalol yuklab olishingiz mumkin.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    }

                    // Vector Canvas Bitmap Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(595f / 842f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingPreview || previewBitmap == null) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Image(
                                bitmap = previewBitmap!!.asImageBitmap(),
                                contentDescription = "OMR Titul A4 Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }

        // Instructions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Varaqani to'ldirish qoidalari:",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                InstructionItem("1. 10 xonali ID kod blokining har bir ustunidan aniq bitta raqam to'liq bo'yalishi shart.")
                InstructionItem("2. QAT'IY QOIDA: Doirachani bo'yayotganda qalam yoki ruchka doiracha chegarasidan tashqariga chiqib ketmasligi kerak! Chiqib ketgan bo'lsa xato hisoblanadi.")
                InstructionItem("3. Agar ID kod chala bo'yalsa yoki doirachadan toshib chiqsa, tizim 'KOD_XATO' beradi va testni tekshirmaydi.")
                InstructionItem("4. Har bir savol (1 dan 30 gacha) uchun faqat bitta variant (A, B, C, D) qora/to'q ko'k rangda to'liq bo'yalishi kerak.")
                InstructionItem("5. Varaqaning 4 burchagidagi qora kvadrat belgilar (markers) kamera uchun mo'ljal hisoblanadi, ularga tegmang.")
            }
        }
    }
}

@Composable
private fun InstructionItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircleOutline,
            contentDescription = null,
            tint = Color(0xFF059669),
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
