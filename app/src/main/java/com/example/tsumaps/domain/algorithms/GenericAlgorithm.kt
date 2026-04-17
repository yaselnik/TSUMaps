package com.example.tsumaps.domain.algorithms

import com.example.tsumaps.domain.models.Place
import com.example.tsumaps.domain.models.UserRequest
import com.example.tsumaps.domain.models.Point
import com.example.tsumaps.domain.map.MapGrid
import kotlin.random.Random
import kotlinx.coroutines.yield

class GenericAlgorithm (
    private val startPoint: Point,
    private val places: MutableMap<Int, Place>,
    private val request: UserRequest,
    private val populationSize: Int = 75,
    private val generations: Int = 150,
    private val tournamentSize: Int = 5,
    private val mutationRate: Double = 0.2,
    private val elitismCount: Int = 5,
    private val grid: MapGrid,
) {

    private val distanceMatrix = mutableMapOf<Pair<Int, Int>, Double>()
    private val distanceFromStart = mutableMapOf<Int, Double> ()
    private val placesToVisit = mutableMapOf<Int, Place>()


    init {
        for (key in places.keys) {
            for (item in request.choice) {
                if (places[key]!!.menu.contains(item)) {
                    placesToVisit[key] = places[key] ?: continue
                    break;
                }
            }
        }

        val astar = Astar(grid)
        for (place1 in placesToVisit.values) {
            for (place2 in placesToVisit.values) {
                if (place1 == place2) continue

                val dist = astar.calculatePath(place1.location, place2.location)
                distanceMatrix[Pair(place1.id, place2.id)] = dist
                distanceMatrix[Pair(place2.id, place1.id)] = dist
            }
            distanceFromStart[place1.id] = astar.calculatePath(startPoint, place1.location)
        }
    }

    fun fitness(route: List<Int>): Double {
        if (route.isEmpty()) return 0.0

        var dist = 0.001
        dist += distanceFromStart[route[0]] ?: 0.0

        for (i in 1 until route.size) {
            dist += distanceMatrix[Pair(route[i - 1], route[i])] ?: 0.0
        }

        val covering = route.mapNotNull { id -> placesToVisit[id]?.menu }.flatten().toSet()

        val missing = request.choice.filter { !covering.contains(it) }.toSet()

        if (missing.isNotEmpty()) {
            val penalty = missing.size * 1000.0
            return 1.0 / (dist + penalty)
        }
        return 1.0 / dist
    }

    fun initPopulation() : List<List<Int>> {
        val population = mutableListOf<List<Int>>()

        for (i in 0 until populationSize) {
            val route = randomConstruction()
            population.add(route)
        }

        return population
    }

    private fun randomConstruction(): List<Int> {
        val route = placesToVisit.keys.shuffled().toMutableList()

        var collectedItems = emptySet<String>()
        var cutoffIndex = route.size

        for ((index, id) in route.withIndex()) {
            val city = placesToVisit[id] ?: continue
            collectedItems = collectedItems + city.menu

            if (collectedItems.containsAll(request.choice)) {
                cutoffIndex = index + 1
                break
            }
        }

        return route.subList(0, cutoffIndex)
    }

    private fun tournamentSelect(population: List<List<Int>>, fitnesses: List<Double>): List<Int> {
        if (population.isEmpty()) return emptyList()
        val n = population.size
        val tournament = List(tournamentSize.coerceAtLeast(1)) {
            val idx = Random.nextInt(n)
            population[idx] to fitnesses[idx]
        }

        return tournament.maxByOrNull { it.second }?.first ?: population[0]
    }

    private fun orderCrossover(parent1: List<Int>, parent2: List<Int>): Pair<List<Int>, List<Int>> {
        if (parent1.isEmpty() || parent2.isEmpty() || parent1.size < 2 || parent2.size < 2) {
            return parent1.toList() to parent2.toList()
        }
        val n = minOf(parent1.size, parent2.size)
        if (n < 2) {
            return parent1.toList() to parent2.toList()
        }
        val cutPoints = (0 until n).shuffled().take(2).sorted()
        if (cutPoints.size < 2) {
            return parent1.toList() to parent2.toList()
        }
        val start = cutPoints[0]
        val end = cutPoints[1]

        val child1 = mutableListOf<Int>()
        child1.addAll(parent1.subList(start, end + 1))

        parent2.forEach {
            if (it !in child1) child1.add(it)
        }

        val child2 = mutableListOf<Int>()
        child2.addAll(parent2.subList(start, end + 1))

        parent1.forEach {
            if (it !in child2) child2.add(it)
        }

        return child1 to child2
    }

    private fun mutate(route: MutableList<Int>) {
        if (Random.nextDouble() >= mutationRate) return

        when (Random.nextInt(3)) {
            0 -> inversionMutation(route)
            1 -> swapMutation(route)
            2 -> insertMutation(route)
        }

        trimRouteIfPossible(route)
    }



    private fun inversionMutation(route: MutableList<Int>) {
        if (route.size < 2) return
        val (i, j) = (0 until route.size).shuffled().take(2).sorted()
        route.subList(i, j + 1).reverse()
    }

    private fun swapMutation(route: MutableList<Int>) {
        if (route.size < 2) return
        val (i, j) = (0 until route.size).shuffled().take(2)
        val temp = route[i]
        route[i] = route[j]
        route[j] = temp
    }

    private fun insertMutation(route: MutableList<Int>) {
        if (route.size < 2) return
        val from = Random.nextInt(route.size)
        var to = Random.nextInt(route.size)
        val element = route.removeAt(from)
        if (from < to) {
            to -= 1
        }
        route.add(to.coerceIn(0, route.size), element)
    }

    private fun trimRouteIfPossible(route: MutableList<Int>) {
        var collectedItems = emptySet<String>()
        var cutoffIndex = route.size

        for ((index, id) in route.withIndex()) {
            val city = placesToVisit[id] ?: continue
            collectedItems = collectedItems + city.menu

            if (collectedItems.containsAll(request.choice)) {
                cutoffIndex = index + 1
                break
            }
        }

        if (cutoffIndex < route.size) {
            route.subList(cutoffIndex, route.size).clear()
        }
    }

    suspend fun evolve(): List<Int> {
        if (placesToVisit.isEmpty()) {
            return emptyList()
        }
        var population = initPopulation()
        var bestRoute = listOf<Int>()
        var bestFitness = 0.0

        for (generation in 0 until generations) {
            yield()
            val fitnesses = population.map { fitness(it) }

            val bestIndex = fitnesses.indices.maxByOrNull { fitnesses[it] } ?: 0
            val currentBestRoute = population[bestIndex]
            val currentBestFitness = fitnesses[bestIndex]

            if (currentBestFitness > bestFitness) {
                bestFitness = currentBestFitness
                bestRoute = currentBestRoute
            }

            val newPopulation = mutableListOf<List<Int>>()

            val eliteIndices = fitnesses.indices
                .sortedByDescending { fitnesses[it] }
                .take(elitismCount)

            for (idx in eliteIndices) {
                newPopulation.add(population[idx].toList())
            }

            while (newPopulation.size < populationSize) {
                val parent1 = tournamentSelect(population, fitnesses)
                val parent2 = tournamentSelect(population, fitnesses)
                if (parent1.isEmpty() || parent2.isEmpty()) break

                val (child1, child2) = orderCrossover(parent1, parent2)

                val mutatedChild1 = child1.toMutableList().apply { mutate(this) }
                val mutatedChild2 = child2.toMutableList().apply { mutate(this) }

                newPopulation.add(mutatedChild1)
                if (newPopulation.size < populationSize) {
                    newPopulation.add(mutatedChild2)
                }
            }

            population = newPopulation
        }

        return bestRoute
    }
}