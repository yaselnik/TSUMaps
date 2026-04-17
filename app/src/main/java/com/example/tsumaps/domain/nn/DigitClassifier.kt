package com.example.tsumaps.domain.nn

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

class DigitClassifier(
    private val context: Context,
    private val modelName: String = "mnist_cnn.tflite",
    private val scoreThreshold: Float = 0.5f,
) {
    private var classifier: ImageClassifier? = null

    init {
        setupClassifier()
    }

    private fun setupClassifier() {
        try {
            val baseOptions = BaseOptions.builder().useNnapi().build()
            val options = ImageClassifier.ImageClassifierOptions.builder()
                .setBaseOptions(baseOptions)
                .setMaxResults(1)
                .setScoreThreshold(scoreThreshold)
                .build()

            classifier = ImageClassifier.createFromFileAndOptions(context, modelName, options)
        } catch (e: Exception) {
            Log.e("DigitClassifier", "Ошибка инициализации классификатора", e)
        }
    }

    fun classify(bitmap: Bitmap): Pair<String, Float>? {
        if (classifier == null) {
            Log.e("DigitClassifier", "Классификатор не инициализирован.")
            return null
        }

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(28, 28, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))

        val results = classifier?.classify(tensorImage)

        return results?.firstOrNull()?.categories?.firstOrNull()?.let {
            Pair(it.label, it.score)
        }
    }
}