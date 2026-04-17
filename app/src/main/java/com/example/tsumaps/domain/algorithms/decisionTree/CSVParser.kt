package com.example.tsumaps.domain.algorithms.decisionTree

import android.content.Context
import com.example.tsumaps.domain.models.Sample
import java.io.File

class CSVParser {
    private fun parseCSV(csvContent: String): List<Sample> {
        val lines = csvContent.trim().split("\n")
        if (lines.size < 2) return emptyList()

        return lines.drop(1).map { line ->
            val values = line.split(",").map { it.trim() }
            Sample(
                location = values[0],
                budget = values[1],
                timeAvailable = values[2],
                foodType = values[3],
                queueTolerance = values[4],
                weather = values[5],
                recommendedPlace = values[6]
            )
        }
    }

    fun parseCSVFile(filePath: String): List<Sample> {
        val content = File(filePath).readText()
        return parseCSV(content)
    }

    fun parseAsset(context: Context, assetName: String): List<Sample> {
        val content = context.assets.open(assetName).bufferedReader().use { it.readText() }
        return parseCSV(content)
    }

    fun parseCSVText(csvContent: String): List<Sample> {
        return parseCSV(csvContent)
    }
}