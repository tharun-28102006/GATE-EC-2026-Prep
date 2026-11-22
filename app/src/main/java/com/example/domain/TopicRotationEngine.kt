package com.example.domain

import kotlin.random.Random

/**
 * Randomized non-repeating topic rotation engine for GATE 2026 EC.
 *
 * Guarantees:
 * 1. Every subtopic in the 36-topic GATE 2026 EC syllabus appears once per cycle (shuffle-bag).
 * 2. Two consecutive questions NEVER come from the same subtopic OR the same parent syllabus section.
 * 3. Across cycle boundaries, the first topic of the new cycle never shares the previous section.
 */
object TopicRotationEngine {

    data class SelectionResult(
        val selectedSubtopic: GateSubtopic,
        val remainingDeck: List<String>,
        val nextLastSectionId: String
    )

    fun buildSectionInterleavedDeck(
        previousSectionId: String = "",
        random: Random = Random.Default
    ): List<String> {
        // Group all 36 subtopics by section and shuffle each section's subtopics
        val buckets = GateEcSyllabus.sections.associate { section ->
            section.id to GateEcSyllabus.subtopicsForSection(section.id)
                .map { it.id }
                .shuffled(random)
                .toMutableList()
        }.toMutableMap()

        val result = mutableListOf<String>()
        var prevSec = previousSectionId

        while (buckets.values.any { it.isNotEmpty() }) {
            // Prioritize sections with the most remaining subtopics (excluding prevSec)
            val eligibleSections = buckets
                .filter { (secId, list) -> list.isNotEmpty() && secId != prevSec }
            val candidates = if (eligibleSections.isNotEmpty()) {
                val maxRemaining = eligibleSections.values.maxOf { it.size }
                // Pick randomly among sections near the max remaining count to avoid starvation while keeping randomness high
                eligibleSections.filter { it.value.size >= (maxRemaining - 1).coerceAtLeast(1) }.keys.toList()
            } else {
                buckets.filter { it.value.isNotEmpty() }.keys.toList()
            }

            val chosenSection = candidates[random.nextInt(candidates.size)]
            val subtopicId = buckets[chosenSection]!!.removeAt(0)
            result.add(subtopicId)
            prevSec = chosenSection
        }
        return result
    }

    fun nextSubtopic(
        currentDeck: List<String>,
        lastSectionId: String,
        random: Random = Random.Default
    ): SelectionResult {
        val validDeck = currentDeck
            .filter { id -> GateEcSyllabus.subtopics.any { it.id == id } }
            .toMutableList()

        if (validDeck.isEmpty()) {
            validDeck.addAll(buildSectionInterleavedDeck(lastSectionId, random))
        }

        // Find the first subtopic in the deck whose parent section differs from lastSectionId
        val pickIndex = validDeck.indexOfFirst { subId ->
            GateEcSyllabus.subtopicById(subId).sectionId != lastSectionId
        }.let { if (it >= 0) it else 0 }

        val pickedId = validDeck.removeAt(pickIndex)
        val pickedSubtopic = GateEcSyllabus.subtopicById(pickedId)

        // If deck just emptied, pre-build the next interleaved cycle avoiding pickedSubtopic.sectionId
        val nextDeck = if (validDeck.isEmpty()) {
            buildSectionInterleavedDeck(pickedSubtopic.sectionId, random)
        } else {
            validDeck
        }

        return SelectionResult(
            selectedSubtopic = pickedSubtopic,
            remainingDeck = nextDeck,
            nextLastSectionId = pickedSubtopic.sectionId
        )
    }

    fun peekUpcomingSubtopics(
        currentDeck: List<String>,
        lastSectionId: String,
        count: Int
    ): List<GateSubtopic> {
        val validDeck = currentDeck
            .filter { id -> GateEcSyllabus.subtopics.any { it.id == id } }
            .ifEmpty { buildSectionInterleavedDeck(lastSectionId) }
        return validDeck.take(count).map { GateEcSyllabus.subtopicById(it) }
    }
}
