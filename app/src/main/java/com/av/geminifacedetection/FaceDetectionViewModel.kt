package com.av.geminifacedetection

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FaceDetectionViewModel : ViewModel() {
    private val _uiState: MutableStateFlow<UiState> =
        MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> =
        _uiState.asStateFlow()

    val systemInstruction = content {
        text("""
        User Photo Validation Instructions:
        
        Criteria for Valid Photo:
        
        1. Single Face Detection: The photo must contain only one distinct face. Multiple faces will lead to rejection.
        2. Eye Openness: Both eyes of the user must be fully open and clearly visible.
        3. Minimum Face Size Requirement: The face width must occupy at least 40% of the total image width (MinFaceSize: 0.4).
        4. Liveliness Check: The photo should show a live person and not a static image, illustration, or reprinted photo.
        5. Clarity: The image should be clear, readable, and free of any blurriness or pixelation.
        6. No Obstructive Accessories: The user should not wear any accessories (e.g., glasses, masks) that cover or obscure facial features.
        7. Full Face Visibility: All key facial parts – both eyes, both ears, mouth, forehead, nose, and chin – must be clearly visible in the image.
        
        Output JSON Specification:
        
        - If the photo meets all criteria, return:
        {
            "resultText": "accepted",
            "isAccepted": true
        }
        
        - If the photo fails any of the criteria, return:
        {
            "resultText": "[Brief reason why the photo is unacceptable]",
            "isAccepted": false
        }
        
        Example Error Messages:
        - "Multiple faces detected in the photo."
        - "Eyes are not fully open."
        - "Face does not meet the minimum size requirement."
        - "Image is blurry or pixelated."
        - "Obstructive accessories detected on the face."
        - "Incomplete visibility of key facial features."
    """.trimIndent())
    }

    private val generativeModel = GenerativeModel(
        "gemini-1.5-flash",
        com.av.geminifacedetection.BuildConfig.API_KEY, // Securely retrieve API key
        generationConfig = generationConfig {
            temperature = 1f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 8192
            responseMimeType = "text/plain"
        },
        systemInstruction = systemInstruction // Use the defined instructions
    )

    fun sendPrompt(
        bitmap: Bitmap
    ) {
        _uiState.value = UiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(
                    content {
                        image(bitmap)
                        text("")
                    }
                )
                response.text?.let { outputContent ->
                    _uiState.value = UiState.Success(outputContent)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "")
            }
        }
    }
}