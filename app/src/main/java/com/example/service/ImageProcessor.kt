package com.example.service

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.sqrt

/**
 * Advanced Computer Vision Image Processor for OMR Sheet analysis:
 * 1. Contrast Enhancement (Min-Max percentile-based stretching)
 * 2. Noise Reduction (3x3 smoothing filter to eliminate specks & sensor artifacts)
 * 3. Adaptive Binarization (Localized thresholding for uneven lighting/shadows)
 * 4. Concentric Bubble Inspection (Inner fill + Outer guard ring overflow detection)
 *    Strict Rule: If ink spills outside the circular bubble boundary, it is marked as an ERROR!
 */
object ImageProcessor {

    data class BubbleInspectionResult(
        val isFilled: Boolean,
        val hasOverflow: Boolean, // True if student marks bled outside the circular bubble boundary
        val innerFillRatio: Double,
        val outerSpillRatio: Double
    ) {
        val isValidFilled: Boolean get() = isFilled && !hasOverflow
        val isOverflowError: Boolean get() = isFilled && hasOverflow
    }

    /**
     * Preprocesses the bitmap with Contrast Enhancement, Noise Reduction, and Binarization.
     * Returns a 2D boolean array where true = dark ink/pencil mark, false = background paper.
     */
    fun preprocessBitmap(bitmap: Bitmap): Array<BooleanArray> {
        val width = bitmap.width
        val height = bitmap.height

        // 1. Extract Grayscale Luminance
        val luminance = Array(height) { IntArray(width) }
        val histogram = IntArray(256)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)
                luminance[y][x] = lum
                histogram[lum]++
            }
        }

        // 2. Contrast Enhancement (5th to 95th Percentile Stretching)
        val totalPixels = width * height
        val p5Threshold = (totalPixels * 0.05).toInt()
        val p95Threshold = (totalPixels * 0.95).toInt()

        var accum = 0
        var minLum = 0
        var maxLum = 255

        for (i in 0..255) {
            accum += histogram[i]
            if (accum >= p5Threshold && minLum == 0) {
                minLum = i
            }
            if (accum >= p95Threshold) {
                maxLum = i
                break
            }
        }

        val range = (maxLum - minLum).coerceAtLeast(1)
        val enhanced = Array(height) { IntArray(width) }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val orig = luminance[y][x]
                val stretched = ((orig - minLum).toDouble() / range * 255.0).toInt().coerceIn(0, 255)
                enhanced[y][x] = stretched
            }
        }

        // 3. Noise Reduction (3x3 Box Smoothing Filter)
        val denoised = Array(height) { IntArray(width) }
        for (y in 0 until height) {
            val yPrev = (y - 1).coerceAtLeast(0)
            val yNext = (y + 1).coerceAtMost(height - 1)
            for (x in 0 until width) {
                val xPrev = (x - 1).coerceAtLeast(0)
                val xNext = (x + 1).coerceAtMost(width - 1)

                val sum = enhanced[yPrev][xPrev] + enhanced[yPrev][x] + enhanced[yPrev][xNext] +
                          enhanced[y][xPrev]     + enhanced[y][x]     + enhanced[y][xNext] +
                          enhanced[yNext][xPrev] + enhanced[yNext][x] + enhanced[yNext][xNext]
                denoised[y][x] = sum / 9
            }
        }

        // 4. Binarization using Otsu's optimal global threshold
        val binarized = Array(height) { BooleanArray(width) }
        val threshold = calculateOtsuThreshold(denoised, width, height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                // True = dark foreground (ink/pencil), False = bright paper
                binarized[y][x] = denoised[y][x] < threshold
            }
        }

        return binarized
    }

    /**
     * Inspects a circular bubble using concentric zone analysis:
     * - Inner Core (r <= radius - 1): Should have high dark density if filled.
     * - Outer Guard Annulus (radius + 1.5 <= r <= radius + 4.5): Must be clean white paper.
     *   If dark pixels spill into this outer ring, [hasOverflow] is flagged true!
     */
    fun inspectBubble(
        binarized: Array<BooleanArray>,
        width: Int,
        height: Int,
        centerX: Int,
        centerY: Int,
        radius: Int
    ): BubbleInspectionResult {
        var innerDarkCount = 0
        var innerTotalCount = 0

        var outerDarkCount = 0
        var outerTotalCount = 0

        val innerRadius = (radius - 1).coerceAtLeast(2)
        val innerRadiusSq = innerRadius * innerRadius

        val outerMinR = radius + 1.5
        val outerMaxR = radius + 4.5
        val outerMinRSq = outerMinR * outerMinR
        val outerMaxRSq = outerMaxR * outerMaxR

        val maxSearchR = (outerMaxR + 1).toInt()

        for (dy in -maxSearchR..maxSearchR) {
            val py = centerY + dy
            if (py < 0 || py >= height) continue

            for (dx in -maxSearchR..maxSearchR) {
                val px = centerX + dx
                if (px < 0 || px >= width) continue

                val distSq = (dx * dx + dy * dy).toDouble()

                // Check Inner Core
                if (distSq <= innerRadiusSq) {
                    innerTotalCount++
                    if (binarized[py][px]) {
                        innerDarkCount++
                    }
                }
                // Check Outer Guard Ring (Dumaloq chegarasidan tashqari)
                else if (distSq >= outerMinRSq && distSq <= outerMaxRSq) {
                    outerTotalCount++
                    if (binarized[py][px]) {
                        outerDarkCount++
                    }
                }
            }
        }

        val innerFillRatio = if (innerTotalCount > 0) innerDarkCount.toDouble() / innerTotalCount else 0.0
        val outerSpillRatio = if (outerTotalCount > 0) outerDarkCount.toDouble() / outerTotalCount else 0.0

        // Bubble is considered filled if inner fill is >= 38%
        val isFilled = innerFillRatio >= 0.38

        // Dumaloq ichidan chiqib ketganlik mezonlari:
        // Agar o'quvchi bo'yaganda qalam/ruchka doirachadan toshib ketgan bo'lsa
        // tashqi himoya halqasida qora piksellar ulushi me'yordan (15%) oshadi.
        val hasOverflow = isFilled && outerSpillRatio >= 0.15

        return BubbleInspectionResult(
            isFilled = isFilled,
            hasOverflow = hasOverflow,
            innerFillRatio = innerFillRatio,
            outerSpillRatio = outerSpillRatio
        )
    }

    private fun calculateOtsuThreshold(denoised: Array<IntArray>, width: Int, height: Int): Int {
        val hist = IntArray(256)
        val total = width * height

        for (y in 0 until height) {
            for (x in 0 until width) {
                hist[denoised[y][x]]++
            }
        }

        var sum = 0.0
        for (i in 0..255) sum += i * hist[i]

        var sumB = 0.0
        var wB = 0
        var maxVariance = 0.0
        var optimalThreshold = 128

        for (t in 0..255) {
            wB += hist[t]
            if (wB == 0) continue

            val wF = total - wB
            if (wF == 0) break

            sumB += t * hist[t]
            val mB = sumB / wB
            val mF = (sum - sumB) / wF

            val varianceBetween = wB.toDouble() * wF.toDouble() * (mB - mF) * (mB - mF)
            if (varianceBetween > maxVariance) {
                maxVariance = varianceBetween
                optimalThreshold = t
            }
        }

        // Clip to safe operational threshold bounds
        return optimalThreshold.coerceIn(90, 160)
    }
}
