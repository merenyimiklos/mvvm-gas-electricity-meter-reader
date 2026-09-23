package hu.merenyimiklos.meterreader.domain

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import hu.merenyimiklos.meterreader.model.MeterType
import hu.merenyimiklos.meterreader.model.OcrResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OcrMeterReader(private val context: Context) {
    suspend fun read(uri: Uri, meterType: MeterType): OcrResult {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val recognizedText = suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    recognizer.close()
                    if (continuation.isActive) continuation.resume(result.text)
                }
                .addOnFailureListener { error ->
                    recognizer.close()
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
        }
        return OcrResult(extractLikelyMeterValue(recognizedText, meterType), recognizedText)
    }

    private fun extractLikelyMeterValue(text: String, meterType: MeterType): Double? {
        val numberRegex = Regex("""(?<!\d)\d{1,7}(?:[.,]\d{1,3})?(?!\d)""")
        return numberRegex.findAll(text).mapNotNull { match ->
            val normalized = match.value.replace(',', '.')
            val value = normalized.toDoubleOrNull() ?: return@mapNotNull null
            val parts = normalized.split('.')
            val integerDigits = parts.first().length
            val decimalDigits = parts.getOrNull(1)?.length ?: 0
            var score = integerDigits * 2
            if (integerDigits in 4..6) score += 12
            if (integerDigits == 7) score += 4
            if (decimalDigits in 1..3) score += 2
            if (meterType == MeterType.GAS && decimalDigits > 0) score += 2
            if (value <= 0.0) score -= 5
            Candidate(value, score)
        }.maxByOrNull { it.score }?.value
    }

    private data class Candidate(val value: Double, val score: Int)
}
