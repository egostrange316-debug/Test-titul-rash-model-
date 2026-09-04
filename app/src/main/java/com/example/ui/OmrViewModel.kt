package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.OmrScanEntity
import com.example.data.PreferencesManager
import com.example.data.ScanRepository
import com.example.model.AnswerKey
import com.example.model.OmrCheckResult
import com.example.model.QuestionResult
import com.example.service.GeminiOmrService
import com.example.service.LocalOmrEngine
import com.example.service.OmrSheetSimulator
import com.example.service.PdfGenerator
import com.example.service.SimulatedSheetData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class OmrViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = ScanRepository(database.omrScanDao())
    private val prefsManager = PreferencesManager(application)

    val scansHistory: StateFlow<List<OmrScanEntity>> = repository.allScans
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _answerKey = MutableStateFlow(prefsManager.getAnswerKey())
    val answerKey: StateFlow<AnswerKey> = _answerKey.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _selectedBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedBitmap: StateFlow<Bitmap?> = _selectedBitmap.asStateFlow()

    private val _imageSource = MutableStateFlow<String?>("Simulator")
    val imageSource: StateFlow<String?> = _imageSource.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _checkResult = MutableStateFlow<OmrCheckResult?>(null)
    val checkResult: StateFlow<OmrCheckResult?> = _checkResult.asStateFlow()

    private val _useGemini = MutableStateFlow(true)
    val useGemini: StateFlow<Boolean> = _useGemini.asStateFlow()

    private val _simulatedData = MutableStateFlow(OmrSheetSimulator.createPresetValid(_answerKey.value))
    val simulatedData: StateFlow<SimulatedSheetData> = _simulatedData.asStateFlow()

    init {
        // Generate an initial simulated sample sheet for immediate testing
        generateBitmapFromSimulator(_simulatedData.value)
    }

    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    fun setUseGemini(value: Boolean) {
        _useGemini.value = value
    }

    fun setSelectedBitmap(bitmap: Bitmap, sourceName: String) {
        _selectedBitmap.value = bitmap
        _imageSource.value = sourceName
        _checkResult.value = null
    }

    fun loadSimulatorPreset(presetType: String) {
        val currentKey = _answerKey.value
        val preset = when (presetType) {
            "VALID_HIGH" -> OmrSheetSimulator.createPresetValid(currentKey)
            "INVALID_ID_MISSING" -> OmrSheetSimulator.createPresetInvalidId("Bitta ustun belgilanmagan")
            "INVALID_ID_DOUBLE" -> OmrSheetSimulator.createPresetDoubleMarkedId()
            "INVALID_ID_OVERFLOW" -> OmrSheetSimulator.createPresetIdOverflow()
            "OVERFLOW_ANSWERS" -> OmrSheetSimulator.createPresetAnswerOverflow(currentKey)
            else -> OmrSheetSimulator.createPresetValid(currentKey)
        }
        _simulatedData.value = preset
        generateBitmapFromSimulator(preset)
        _checkResult.value = null
    }

    fun updateSimulatedAnswer(questionNum: Int, answer: String) {
        val current = _simulatedData.value
        val newAnswers = current.studentAnswers.toMutableMap()
        if (answer.isEmpty()) {
            newAnswers.remove(questionNum)
        } else {
            newAnswers[questionNum] = answer
        }
        val updated = current.copy(studentAnswers = newAnswers)
        _simulatedData.value = updated
        generateBitmapFromSimulator(updated)
    }

    fun updateSimulatedIdCol(colIdx: Int, digit: Int) {
        val current = _simulatedData.value
        val newCols = current.idColumns.toMutableList()
        if (colIdx in 0..9) {
            newCols[colIdx] = listOf(digit)
            val updated = current.copy(idColumns = newCols)
            _simulatedData.value = updated
            generateBitmapFromSimulator(updated)
        }
    }

    fun clearSimulatedIdCol(colIdx: Int) {
        val current = _simulatedData.value
        val newCols = current.idColumns.toMutableList()
        if (colIdx in 0..9) {
            newCols[colIdx] = emptyList() // intentionally missing -> triggers KOD_XATO
            val updated = current.copy(idColumns = newCols)
            _simulatedData.value = updated
            generateBitmapFromSimulator(updated)
        }
    }

    /**
     * Admin o'zi 10 xonali kodni yozma kiritishi uchun.
     */
    fun setCustomId10Digits(code10: String) {
        val digits = code10.filter { it.isDigit() }.take(10)
        if (digits.length == 10) {
            val newCols = digits.map { listOf(it.digitToInt()) }
            val current = _simulatedData.value
            val updated = current.copy(idColumns = newCols, idOverflowColumns = emptySet())
            _simulatedData.value = updated
            generateBitmapFromSimulator(updated)

            val res = _checkResult.value
            if (res != null) {
                _checkResult.value = res.copy(student_id = digits)
            }
        }
    }

    /**
     * Skaner natijasida chiqqan ID kodni admin tomonidan o'zgartirish / to'g'rilash
     */
    fun overrideCurrentScanId(newId: String) {
        val res = _checkResult.value ?: return
        val digits = newId.filter { it.isDigit() }.take(10)
        if (digits.length == 10) {
            val updatedRes = if (res.status == "KOD_XATO") {
                val key = _answerKey.value
                val details = mutableListOf<QuestionResult>()
                var correctCount = 0
                for (q in 1..30) {
                    val marked = _simulatedData.value.studentAnswers[q] ?: "BELGILANMAGAN"
                    val correctOption = key.getAnswerFor(q)
                    val isCorrect = marked == correctOption
                    if (isCorrect) correctCount++
                    details.add(QuestionResult(q, marked, correctOption, isCorrect))
                }
                val pct = (correctCount / 30.0) * 100.0
                res.copy(
                    status = "SUCCESS",
                    student_id = digits,
                    total_questions = 30,
                    correct_count = correctCount,
                    incorrect_count = 30 - correctCount,
                    score_percentage = pct,
                    question_details = details
                )
            } else {
                res.copy(student_id = digits)
            }
            _checkResult.value = updatedRes
            viewModelScope.launch {
                repository.saveScanResult(updatedRes)
            }
        }
    }

    private fun generateBitmapFromSimulator(data: SimulatedSheetData) {
        viewModelScope.launch(Dispatchers.Default) {
            val bitmap = OmrSheetSimulator.renderToBitmap(data)
            withContext(Dispatchers.Main) {
                _selectedBitmap.value = bitmap
                _imageSource.value = "Simulator"
            }
        }
    }

    fun updateAnswerKey(newKey: AnswerKey) {
        _answerKey.value = newKey
        prefsManager.saveAnswerKey(newKey)
    }

    fun updateSingleAnswer(questionNum: Int, option: String) {
        val current = _answerKey.value
        val map = current.answers.toMutableMap()
        map[questionNum] = option
        val updated = current.copy(answers = map)
        updateAnswerKey(updated)
    }

    fun randomizeAnswerKey() {
        val options = listOf("A", "B", "C", "D")
        val map = mutableMapOf<Int, String>()
        for (i in 1..30) {
            map[i] = options.random()
        }
        updateAnswerKey(_answerKey.value.copy(answers = map))
    }

    fun resetAnswerKey() {
        updateAnswerKey(AnswerKey())
    }

    fun analyzeCurrentSheet() {
        val bitmap = _selectedBitmap.value
        if (bitmap == null) {
            Toast.makeText(getApplication(), "Iltimos, avval rasm tanlang yoki simulatorni yuklang", Toast.LENGTH_SHORT).show()
            return
        }

        _isAnalyzing.value = true
        _checkResult.value = null

        viewModelScope.launch {
            val key = _answerKey.value
            val result = if (_useGemini.value) {
                val geminiRes = GeminiOmrService.analyzeWithGemini(bitmap, key)
                geminiRes.getOrElse {
                    LocalOmrEngine.analyzeSheet(bitmap, key)
                }
            } else {
                LocalOmrEngine.analyzeSheet(bitmap, key)
            }

            _checkResult.value = result
            _isAnalyzing.value = false

            // Save to database
            saveScanToHistory(result, bitmap)
        }
    }

    private suspend fun saveScanToHistory(result: OmrCheckResult, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        try {
            // Save thumbnail or image to file
            val file = File(getApplication<Application>().cacheDir, "scan_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
            }
            repository.saveScanResult(result, file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteScan(scan: OmrScanEntity) {
        viewModelScope.launch {
            repository.deleteScan(scan)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun downloadPdf(context: Context) {
        val uri = PdfGenerator.saveToPublicDownloads(context)
        if (uri != null) {
            Toast.makeText(context, "OMR Titul PDF Downloads jildiga saqlandi!", Toast.LENGTH_LONG).show()
        } else {
            // Fallback: share/open
            PdfGenerator.sharePdf(context)
        }
    }

    fun sharePdf(context: Context) {
        PdfGenerator.sharePdf(context)
    }

    fun openPdf(context: Context) {
        PdfGenerator.openPdf(context)
    }
}
