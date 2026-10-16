package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun test14DayConsistencyCalculation() {
        val totalTasks = 20
        val completedTasks = 16
        val score = (completedTasks.toFloat() / totalTasks.toFloat()) * 100f
        assertEquals(80f, score, 0.01f)
    }

    @Test
    fun testStreakWithFreezeLogic() {
        // Daily completion percentages: [80%, 100%, 0% (0 tasks scheduled - freeze), 90%]
        val scores = listOf(80f, 100f, null, 90f) // null = 0 tasks scheduled
        var streak = 0
        var freezeUsed = false

        for (score in scores) {
            if (score == null) {
                if (!freezeUsed) {
                    freezeUsed = true
                } else {
                    break
                }
            } else if (score >= 70f) {
                streak++
                freezeUsed = false
            } else {
                break
            }
        }

        assertEquals(3, streak)
        assertTrue(streak >= 3)
    }

    @Test
    fun testGamificationLevelProgression() {
        val level1 = com.example.data.repository.GamificationRepository.calculateLevelInfo(150)
        assertEquals(1, level1.first)
        assertEquals("Novice Striver", level1.second)

        val level2 = com.example.data.repository.GamificationRepository.calculateLevelInfo(450)
        assertEquals(2, level2.first)
        assertEquals("Daily Builder", level2.second)

        val level4 = com.example.data.repository.GamificationRepository.calculateLevelInfo(1200)
        assertEquals(4, level4.first)
        assertEquals("Rhythm Architect", level4.second)
    }

    @Test
    fun testScreenItemsNonNull() {
        val items = com.example.ui.navigation.Screen.items
        assertEquals(5, items.size)
        items.forEach { screen ->
            assertNotNull(screen)
            assertNotNull(screen.route)
            assertTrue(screen.route.isNotBlank())
            assertNotNull(screen.title)
            assertTrue(screen.title.isNotBlank())
        }
    }
}
