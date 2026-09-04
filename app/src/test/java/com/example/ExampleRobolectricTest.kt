package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AnswerKey
import com.example.model.OmrCheckResult
import com.example.service.LocalOmrEngine
import com.example.service.OmrSheetSimulator
import com.example.service.PdfGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("OMR Checker", appName)
  }

  @Test
  fun `test algorithmic rule 1 - invalid ID terminates immediately with KOD_XATO`() {
    val answerKey = AnswerKey()
    // Simulated sheet with missing ID column (incomplete ID)
    val invalidPreset = OmrSheetSimulator.createPresetInvalidId("Bitta ustun bo'yalmagan")
    val bitmap = OmrSheetSimulator.renderToBitmap(invalidPreset)

    val result = LocalOmrEngine.analyzeSheet(bitmap, answerKey)

    // Rule 1: ID kod to'liq bo'lmasa -> SHU JOYDA TO'XTATISH, status = "KOD_XATO", student_id = null
    assertEquals("KOD_XATO", result.status)
    assertNull(result.student_id)
    assertEquals(0, result.correct_count)
    assertEquals(0, result.incorrect_count)
    assertEquals(0.0, result.score_percentage, 0.001)
  }

  @Test
  fun `test algorithmic rule 2 - valid ID checks 30 questions and calculates score`() {
    val answerKey = AnswerKey()
    val validPreset = OmrSheetSimulator.createPresetValid(answerKey)
    val bitmap = OmrSheetSimulator.renderToBitmap(validPreset)

    val result = LocalOmrEngine.analyzeSheet(bitmap, answerKey)

    assertEquals("SUCCESS", result.status)
    assertNotNull(result.student_id)
    assertEquals(10, result.student_id?.length)
    assertEquals(30, result.total_questions)
    assertTrue(result.correct_count > 0)
    assertTrue(result.score_percentage > 0.0)

    val json = result.toCleanJson()
    assertTrue(json.contains("\"status\": \"SUCCESS\""))
    assertTrue(json.contains("\"student_id\":"))
    assertTrue(json.contains("\"total_questions\": 30"))
  }

  @Test
  fun `test PDF generation produces A4 document`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val pdfFile = PdfGenerator.generateOmrSheetPdf(context)
    assertTrue(pdfFile.exists())
    assertTrue(pdfFile.length() > 0)
  }
}

