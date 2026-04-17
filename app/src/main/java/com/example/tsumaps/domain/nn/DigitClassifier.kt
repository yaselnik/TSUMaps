package com.example.tsumaps.domain.nn

import kotlin.math.exp

class SimpleNeuralNetwork(
    private val inputNodes: Int,
    private val hiddenNodes: Int,
    private val outputNodes: Int,
    private var weightsInputHidden: Array<DoubleArray>,
    private var weightsHiddenOutput: Array<DoubleArray>
) {
    private fun sigmoid(x: Double): Double {
        return 1.0 / (1.0 + exp(-x))
    }

    fun predict(inputArray: DoubleArray): DoubleArray {
        val hiddenInputs = DoubleArray(hiddenNodes)
        for (i in 0 until hiddenNodes) {
            var sum = 0.0
            for (j in 0 until inputNodes) {
                sum += weightsInputHidden[i][j] * inputArray[j]
            }
            hiddenInputs[i] = sum
        }
        val hiddenOutputs = hiddenInputs.map { sigmoid(it) }.toDoubleArray()

        val finalInputs = DoubleArray(outputNodes)
        for (i in 0 until outputNodes) {
            var sum = 0.0
            for (j in 0 until hiddenNodes) {
                sum += weightsHiddenOutput[i][j] * hiddenOutputs[j]
            }
            finalInputs[i] = sum
        }
        return finalInputs.map { sigmoid(it) }.toDoubleArray()
    }
}