package com.example.tsumaps.domain.algorithms.decisionTree

import com.example.tsumaps.domain.models.Sample
import kotlin.math.log2
import kotlin.text.get

sealed class DecisionTreeNode {
    data class Node(val attribute: String,
                    val children: Map<String, DecisionTreeNode>) : DecisionTreeNode()
    data class Leaf(val result: String) : DecisionTreeNode()
}

class DecisionTree (
    var root: DecisionTreeNode
){
    private fun entropy(samples: List<Sample>): Double {
        if (samples.isEmpty()) return 0.0

        val total = samples.size
        val placeCounts = samples.groupingBy { it.recommendedPlace }.eachCount()

        return -placeCounts.values.map { count ->
            val p = count.toDouble() / total
            if (p > 0) p * log2(p) else 0.0
        }.sum()
    }

    private fun extractAttribute(sample: Sample, attr: String): String = when (attr) {
        "location" -> sample.location
        "budget" -> sample.budget
        "time_available" -> sample.timeAvailable
        "food_type" -> sample.foodType
        "queue_tolerance" -> sample.queueTolerance
        "weather" -> sample.weather
        else -> throw IllegalArgumentException("Unknown attribute $attr")
    }

    private fun informationGain(
        samples: List<Sample>,
        attribute: String
    ): Double {
        val totalEntropy = entropy(samples)

        val total = samples.size.toDouble()
        val weightedEntropy = samples.groupBy { extractAttribute(it, attribute) }
            .values
            .sumOf { subset -> (subset.size / total) * entropy(subset) }

        return totalEntropy - weightedEntropy
    }
    
    private fun build(samples: List<Sample>, attributes: List<String>): DecisionTreeNode {
        val places = samples.map { it.recommendedPlace }.distinct()
        if (places.size == 1) {
            return DecisionTreeNode.Leaf(places.first())
        }
        
        if (samples.isEmpty() || attributes.isEmpty()) {
            val mostCommonPlace = samples
                .groupingBy { it.recommendedPlace }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key ?: "Unknown"
            return DecisionTreeNode.Leaf(mostCommonPlace)
        }
        
        val bestAttribute = attributes.maxByOrNull {
            informationGain(samples, it)
        } ?: attributes.first()
        
        val values = samples.map { extractAttribute(it, bestAttribute) }.distinct()
        val children = mutableMapOf<String, DecisionTreeNode>()

        for (value in values) {
            val subset = samples.filter { extractAttribute(it, bestAttribute) == value }
            val remainingAttributes = attributes.filter { it != bestAttribute }
            children[value] = build(subset, remainingAttributes)
        }

        return DecisionTreeNode.Node(bestAttribute, children)
    }

    fun buildTree(samples: List<Sample>, attributes: List<String>) {
        root = build(samples, attributes)
    }

    private fun classify(sample: Sample, tree: DecisionTreeNode): String {
        return when (tree) {
            is DecisionTreeNode.Leaf -> tree.result
            is DecisionTreeNode.Node -> {
                val value = extractAttribute(sample, tree.attribute);
                val child = tree.children[value]
                child?.let { classify(sample, it) }
                    ?: tree.children.values.firstOrNull()?.let { classify(sample, it) }
                    ?: "Unknown"
            }
        }
    }

    fun classify(sample: Sample): String {
        return classify(sample, root)
    }
}