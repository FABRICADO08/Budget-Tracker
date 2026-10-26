package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "SlipScannerViewModel"

data class ScannedSlipData(
    val amount: Double,
    val merchant: String,
    val category: String,
    val date: String,
    val note: String
)

sealed interface ScanSlipState {
    data object Idle : ScanSlipState
    data object Scanning : ScanSlipState
    data class Success(val data: ScannedSlipData) : ScanSlipState
    data class Error(val message: String) : ScanSlipState
}

class SlipScannerViewModel : ViewModel() {

    private val scanModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val _scanState = MutableStateFlow<ScanSlipState>(ScanSlipState.Idle)
    val scanState: StateFlow<ScanSlipState> = _scanState.asStateFlow()

    fun scanSlipImage(context: Context, imageUri: Uri) {
        _scanState.value = ScanSlipState.Scanning

        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    decodeSampledBitmapFromUri(context, imageUri, 1280, 1280)
                }

                if (bitmap == null) {
                    _scanState.value = ScanSlipState.Error("Could not load receipt image.")
                    return@launch
                }

                val prompt = content {
                    image(bitmap)
                    text(
                        "You are an expert receipt OCR parser for South Africa. " +
                        "Examine this till slip or receipt photo carefully. " +
                        "Extract: " +
                        "1. The final total expenditure amount paid (a positive number, South African Rand). " +
                        "2. The merchant/store name (e.g. Shoprite, Pick n Pay, Woolworths, Checkers, Shell, Engen, etc.). " +
                        "3. The date shown on the slip in YYYY-MM-DD format (if missing, leave blank). " +
                        "4. The most appropriate category strictly from: ['Groceries', 'Food & Dining', 'Transportation', 'Bills', 'Shopping', 'Entertainment', 'Health', 'Other']. " +
                        "5. A brief summary note of key items purchased. " +
                        "Return ONLY valid JSON without markdown fences with the exact structure: " +
                        "{\"totalAmount\": 150.50, \"merchant\": \"Shoprite\", \"date\": \"2026-10-24\", \"category\": \"Groceries\", \"note\": \"Milk, Bread, Fruits\"}"
                    )
                }

                val response = withTimeout(60_000L) {
                    scanModel.generateContent(prompt)
                }

                val text = response.text?.trim().orEmpty()
                val parsed = parseReceiptJson(text)

                if (parsed != null && parsed.amount > 0) {
                    _scanState.value = ScanSlipState.Success(parsed)
                } else if (parsed != null) {
                    _scanState.value = ScanSlipState.Success(parsed)
                } else {
                    _scanState.value = ScanSlipState.Error("Could not detect total amount on slip. Please try a clearer picture or enter manually.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Receipt scan failed", e)
                _scanState.value = ScanSlipState.Error(
                    e.localizedMessage ?: "Failed to scan receipt. Please try again."
                )
            }
        }
    }

    fun resetState() {
        _scanState.value = ScanSlipState.Idle
    }

    private fun parseReceiptJson(rawJson: String): ScannedSlipData? {
        return try {
            val clean = rawJson
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val json = JSONObject(clean)
            val amount = json.optDouble("totalAmount", 0.0)
            val merchant = json.optString("merchant", "").trim()
            val category = json.optString("category", "Groceries").trim()
            val rawDate = json.optString("date", "").trim()
            val note = json.optString("note", "").trim()

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val date = if (rawDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) rawDate else today

            ScannedSlipData(
                amount = amount,
                merchant = merchant.ifBlank { "Receipt Purchase" },
                category = if (isValidCategory(category)) category else "Groceries",
                date = date,
                note = note
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON: $rawJson", e)
            null
        }
    }

    private fun isValidCategory(category: String): Boolean {
        return category in listOf(
            "Food & Dining",
            "Transportation",
            "Groceries",
            "Bills",
            "Shopping",
            "Entertainment",
            "Health",
            "Other"
        )
    }

    private fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
