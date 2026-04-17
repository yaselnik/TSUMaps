package com.example.tsumaps.presentation.nn

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
import com.example.tsumaps.domain.nn.DigitRecognizer5x5

const val GRID_SIZE_5x5 = 5

@Composable
fun DigitRecognitionScreen5x5() {
    val context = LocalContext.current
    val recognizer = remember { DigitRecognizer5x5(context) }
    
    var pixels by remember { mutableStateOf(Array(GRID_SIZE_5x5) { BooleanArray(GRID_SIZE_5x5) }) }
    var result by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Нарисуйте цифру (5x5)", fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))
        
        DrawingPad5x5(
            pixels = pixels,
            onPixelChange = { newPixels -> pixels = newPixels }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = {
                pixels = Array(GRID_SIZE_5x5) { BooleanArray(GRID_SIZE_5x5) }
                result = null
            }) { Text("Очистить") }
            
            Button(onClick = {
                result = recognizer.recognize(pixels)
            }) { Text("Распознать") }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        result?.let {
            Text("Распознана цифра: $it", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DrawingPad5x5(
    pixels: Array<BooleanArray>,
    onPixelChange: (Array<BooleanArray>) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.fillMaxWidth().aspectRatio(1f).background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val cellSize = size.width.toFloat() / GRID_SIZE_5x5
                    val x = (change.position.x / cellSize).toInt().coerceIn(0, GRID_SIZE_5x5 - 1)
                    val y = (change.position.y / cellSize).toInt().coerceIn(0, GRID_SIZE_5x5 - 1)
                    
                    if (!pixels[y][x]) {
                        val newPixels = pixels.map { it.clone() }.toTypedArray()
                        newPixels[y][x] = true
                        onPixelChange(newPixels)
                    }
                }
            }
    ) {
        drawGrid5x5(pixels)
    }
}

private fun DrawScope.drawGrid5x5(pixels: Array<BooleanArray>) {
    val cellSize = size.width / GRID_SIZE_5x5
    for (y in 0 until GRID_SIZE_5x5) {
        for (x in 0 until GRID_SIZE_5x5) {
            if (pixels[y][x]) {
                drawRect(
                    color = Color.White,
                    topLeft = Offset(x * cellSize, y * cellSize),
                    size = Size(cellSize, cellSize)
                )
            }
        }
    }
}