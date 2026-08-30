package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.Content
import com.example.data.GenerateContentRequest
import com.example.data.HtmlSanitizerValidator
import com.example.data.InlineData
import com.example.data.Part
import com.example.data.PdfHelper
import com.example.data.PdfHelper.toBase64Jpeg
import com.example.data.RetrofitClient
import com.example.data.SampleDocument
import com.example.data.SampleDocuments
import com.example.data.SystemPromptConfig
import com.example.data.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EBookUiState(
    val selectedBitmaps: List<Bitmap> = emptyList(),
    val activePageIndex: Int = 0,
    val documentTitle: String = "No document loaded",
    val documentType: String = "", // "PDF", "Image", "Sample"
    val isProcessing: Boolean = false,
    val processingStatus: String = "",
    val rawGeneratedText: String = "",
    val sanitizedHtml32: String = "",
    val validationResult: ValidationResult? = null,
    val selectedTab: Int = 0, // 0: Input/OCR, 1: Raw HTML 3.2, 2: Rendered View, 3: Rule Audit
    val apiKey: String = "",
    val isApiKeyDialogOpen: Boolean = false,
    val errorMessage: String? = null,
    val successToast: String? = null,
    val sampleDocuments: List<SampleDocument> = SampleDocuments.SAMPLES,
    val selectedSample: SampleDocument? = null,
    val rawTextInputForSanitizer: String = ""
)

class EBookProcessorViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(EBookUiState())
    val uiState: StateFlow<EBookUiState> = _uiState.asStateFlow()

    private val sharedPrefs = application.getSharedPreferences("ebook_processor_prefs", Context.MODE_PRIVATE)

    init {
        val savedKey = sharedPrefs.getString("gemini_api_key", null)
        val defaultKey = savedKey ?: BuildConfig.GEMINI_API_KEY
        _uiState.update { it.copy(apiKey = defaultKey) }

        // Load the first sample document by default for a ready-to-test instant preview
        loadSampleDocument(SampleDocuments.SAMPLES.first())
    }

    fun saveApiKey(newKey: String) {
        val trimmed = newKey.trim()
        sharedPrefs.edit().putString("gemini_api_key", trimmed).apply()
        _uiState.update { it.copy(apiKey = trimmed, isApiKeyDialogOpen = false, successToast = "API Key saved successfully") }
    }

    fun openApiKeyDialog() {
        _uiState.update { it.copy(isApiKeyDialogOpen = true) }
    }

    fun closeApiKeyDialog() {
        _uiState.update { it.copy(isApiKeyDialogOpen = false) }
    }

    fun setTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun setActivePageIndex(index: Int) {
        if (index in _uiState.value.selectedBitmaps.indices) {
            _uiState.update { it.copy(activePageIndex = index) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearToast() {
        _uiState.update { it.copy(successToast = null) }
    }

    fun loadSampleDocument(sample: SampleDocument) {
        val bitmap = PdfHelper.createSampleBookPageBitmap(
            title = sample.chapterTitle,
            subtitle = sample.subtitle,
            paragraphs = sample.sampleParagraphs,
            hasColoredCallout = sample.hasColorCallout,
            fontVariationNote = sample.fontNote
        )
        _uiState.update {
            it.copy(
                selectedBitmaps = listOf(bitmap),
                activePageIndex = 0,
                documentTitle = sample.title,
                documentType = "Sample (${sample.category})",
                selectedSample = sample,
                errorMessage = null
            )
        }
    }

    fun loadPdfFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    processingStatus = "Rendering PDF pages...",
                    errorMessage = null
                )
            }
            try {
                val bitmaps = PdfHelper.extractPdfPages(getApplication(), uri)
                if (bitmaps.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            selectedBitmaps = bitmaps,
                            activePageIndex = 0,
                            documentTitle = "Uploaded PDF Document (${bitmaps.size} pages)",
                            documentType = "PDF",
                            selectedSample = null,
                            isProcessing = false,
                            processingStatus = ""
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            processingStatus = "",
                            errorMessage = "Could not render pages from the selected PDF. Please try another file."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        processingStatus = "",
                        errorMessage = "Error reading PDF: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun loadImageFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    processingStatus = "Loading image...",
                    errorMessage = null
                )
            }
            try {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        _uiState.update {
                            it.copy(
                                selectedBitmaps = listOf(bitmap),
                                activePageIndex = 0,
                                documentTitle = "Scanned Page Image",
                                documentType = "Scanned Image",
                                selectedSample = null,
                                isProcessing = false,
                                processingStatus = ""
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                errorMessage = "Failed to decode image file."
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Error reading image: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun processCurrentPage() {
        val currentState = _uiState.value
        val bitmap = currentState.selectedBitmaps.getOrNull(currentState.activePageIndex)

        if (bitmap == null) {
            _uiState.update { it.copy(errorMessage = "No document page available to process.") }
            return
        }

        val effectiveApiKey = currentState.apiKey.ifBlank { BuildConfig.GEMINI_API_KEY }
        if (effectiveApiKey.isBlank() || effectiveApiKey == "MY_GEMINI_API_KEY") {
            _uiState.update {
                it.copy(
                    isApiKeyDialogOpen = true,
                    errorMessage = "Please provide your Gemini API key in the settings to process with OCR."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    processingStatus = "Performing Multimodal OCR & Extracting Strict HTML 3.2...",
                    errorMessage = null
                )
            }

            try {
                val base64Image = withContext(Dispatchers.IO) {
                    bitmap.toBase64Jpeg()
                }

                val prompt = """
Extract text and layout structure from this uploaded document page and convert into strict, raw, and clean HTML 3.2 markup according to all system instructions.
Rules checklist:
1. Strict HTML 3.2 specifications only (no modern HTML5 tags like <main>, <section>, <article>, <header>, <footer>).
2. NO inline style="" attributes.
3. Every body paragraph wrapped in <p class="indent">.
4. Document headings mapped to proper <h1> through <h6> tags based on visual hierarchy (without class="indent").
5. Dynamic color classes: <span class="color1">, <span class="color2"> for colored text.
6. Distinct font families wrapped in <span class="font1">, <span class="font2">.
7. Return ONLY the raw HTML inside a single ```html ... ``` block with zero surrounding markdown notes or CSS.
""".trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(
                                Part(text = prompt),
                                Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                            )
                        )
                    ),
                    systemInstruction = Content(
                        parts = listOf(Part(text = SystemPromptConfig.SYSTEM_INSTRUCTION))
                    )
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(apiKey = effectiveApiKey, request = request)
                }

                if (response.error != null) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            processingStatus = "",
                            errorMessage = "Gemini API error: ${response.error.message ?: "Unknown error"}"
                        )
                    }
                    return@launch
                }

                val generatedText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!generatedText.isNullOrBlank()) {
                    val validation = HtmlSanitizerValidator.validateHtml32(generatedText)
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            processingStatus = "",
                            rawGeneratedText = generatedText,
                            sanitizedHtml32 = validation.rawExtractedHtml,
                            validationResult = validation,
                            selectedTab = 1, // Switch to Raw HTML 3.2 Code tab
                            successToast = "HTML 3.2 extracted successfully (${validation.scorePercentage}% compliance)"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            processingStatus = "",
                            errorMessage = "No text candidate returned from Gemini API."
                        )
                    }
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        processingStatus = "",
                        errorMessage = "Processing failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun updateRawTextInput(text: String) {
        _uiState.update { it.copy(rawTextInputForSanitizer = text) }
    }

    fun sanitizeRawTextDirectly() {
        val input = _uiState.value.rawTextInputForSanitizer
        if (input.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter some text or HTML to sanitize.") }
            return
        }

        val sanitized = HtmlSanitizerValidator.sanitizeToStrictHtml32(input)
        val validation = HtmlSanitizerValidator.validateHtml32(sanitized)

        _uiState.update {
            it.copy(
                rawGeneratedText = sanitized,
                sanitizedHtml32 = sanitized,
                validationResult = validation,
                selectedTab = 1,
                successToast = "Sanitized to strict HTML 3.2"
            )
        }
    }

    fun autoFixHtml32() {
        val currentHtml = _uiState.value.sanitizedHtml32
        if (currentHtml.isBlank()) return
        val fixed = HtmlSanitizerValidator.sanitizeToStrictHtml32(currentHtml)
        val validation = HtmlSanitizerValidator.validateHtml32(fixed)
        _uiState.update {
            it.copy(
                sanitizedHtml32 = fixed,
                validationResult = validation,
                successToast = "Strict HTML 3.2 Auto-Sanitizer Applied!"
            )
        }
    }
}
