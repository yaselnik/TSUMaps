package com.example.tsumaps.presentation.nn

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsumaps.domain.nn.DigitClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val GRID_SIZE = 50

@Composable
fun DigitRecognitionScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val digitClassifier = remember { DigitClassifier(context) }
    var pixels by remember { mutableStateOf(Array(GRID_SIZE) { BooleanArray(GRID_SIZE) }) }
    var result by remember { mutableStateOf<Pair<String, Float>?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Нарисуйте оценку (0-9)", fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))
        DrawingPad(pixels = pixels, onPixelChange = { newPixels -> pixels = newPixels })
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = {
                pixels = Array(GRID_SIZE) { BooleanArray(GRID_SIZE) }
                result = null
            }) { Text("Очистить") }
            Button(onClick = {
                coroutineScope.launch(Dispatchers.Default) {
                    val bitmap = pixelsToBitmap(pixels)
                    val classificationResult = digitClassifier.classify(bitmap)
                    result = classificationResult
                }
            }) { Text("Распознать") }
        }
        Spacer(modifier = Modifier.height(24.dp))
        result?.let { (digit, score) ->
            Text(text = "Распознана цифра: $digit", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(text = "Уверенность: ${"%.2f".format(score * 100)}%", fontSize = 18.sp)
        }
    }
}

@Composable
private fun DrawingPad(pixels: Array<BooleanArray>, onPixelChange: (Array<BooleanArray>) -> Unit, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.fillMaxWidth().aspectRatio(1f).background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val canvasWidth = size.width.toFloat()
                    val cellSize = canvasWidth / GRID_SIZE
                    val x = (change.position.x / cellSize).toInt().coerceIn(0, GRID_SIZE - 1)
                    val y = (change.position.y / cellSize).toInt().coerceIn(0, GRID_SIZE - 1)
                    if (!pixels[y][x]) {
                        val newPixels = pixels.map { it.clone() }.toTypedArray()
                        newPixels[y][x] = true
                        onPixelChange(newPixels)
                    }
                }
            }
    ) { drawGrid(pixels) }
}

private fun DrawScope.drawGrid(pixels: Array<BooleanArray>) {
    val canvasWidth = size.width
    val cellSize = canvasWidth / GRID_SIZE
    for (y in 0 until GRID_SIZE) {
        for (x in 0 until GRID_SIZE) {
            if (pixels[y][x]) {
                drawRect(color = Color.White, topLeft = Offset(x * cellSize, y * cellSize), size = Size(cellSize, cellSize))
            }
        }
    }
}

private fun pixelsToBitmap(pixels: Array<BooleanArray>): Bitmap {
    val bitmap = Bitmap.createBitmap(GRID_SIZE, GRID_SIZE, Bitmap.Config.ARGB_8888)
    for (y in 0 until GRID_SIZE) {
        for (x in 0 until GRID_SIZE) {
            bitmap.setPixel(x, y, if (pixels[y][x]) AndroidColor.WHITE else AndroidColor.BLACK)
        }
    }
    return bitmap
}