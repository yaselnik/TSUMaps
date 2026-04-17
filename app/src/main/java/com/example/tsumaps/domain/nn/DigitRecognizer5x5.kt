package com.example.tsumaps.domain.nn

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class DigitRecognizer5x5(context: Context) {

    private val network: SimpleNeuralNetwork

    init {
        val (wih, who) = loadWeightsFromFile(context, "my_nn_weights.json")
        network = SimpleNeuralNetwork(
            inputNodes = 25,
            hiddenNodes = 30,
            outputNodes = 10,
            weightsInputHidden = wih,
            weightsHiddenOutput = who
        )
    }

    private fun loadWeightsFromFile(context: Context, fileName: String): Pair<Array<DoubleArray>, Array<DoubleArray>> {
        val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)
        val wihJson = jsonObject.getJSONArray("weights_input_hidden")
        val whoJson = jsonObject.getJSONArray("weights_hidden_output")
        val weightsInputHidden = jsonArrayTo2DDoubleArray(wihJson)
        val weightsHiddenOutput = jsonArrayTo2DDoubleArray(whoJson)
        return Pair(weightsInputHidden, weightsHiddenOutput)
    }
    
    private fun jsonArrayTo2DDoubleArray(jsonArray: JSONArray): Array<DoubleArray> {
        val rows = jsonArray.length()
        val cols = if (rows > 0) jsonArray.getJSONArray(0).length() else 0
        
        val array = Array(rows) { DoubleArray(cols) }
        for (i in 0 until rows) {
            val row = jsonArray.getJSONArray(i)
            for (j in 0 until cols) {
                array[i][j] = row.getDouble(j)
            }
        }
        return array
    }
    
    fun recognize(inputGrid: Array<BooleanArray>): Int {
        val inputArray = inputGrid.flatMap { row -> row.map { if (it) 1.0 else 0.0 } }.toDoubleArray()
        val outputs = network.predict(inputArray)
        return outputs.indices.maxByOrNull { outputs[it] } ?: -1
    }
}