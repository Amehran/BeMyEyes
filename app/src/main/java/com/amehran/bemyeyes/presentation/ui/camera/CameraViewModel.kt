package com.amehran.bemyeyes.presentation.ui.camera

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amehran.bemyeyes.domain.model.Detection
import com.amehran.bemyeyes.domain.repository.ObjectDetector
import com.amehran.bemyeyes.domain.repository.TextToSpeechManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val objectDetector: ObjectDetector,
    private val textToSpeechManager: TextToSpeechManager
) : ViewModel() {

    private val _detections = MutableStateFlow<List<Detection>>(emptyList())
    val detections = _detections.asStateFlow()

    fun detect(bitmap: Bitmap) {
        objectDetector.detect(bitmap)
            .onEach { detections ->
                _detections.value = detections
                detections.firstOrNull()?.let { textToSpeechManager.speak(it.label) }
            }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeechManager.shutdown()
    }
}
