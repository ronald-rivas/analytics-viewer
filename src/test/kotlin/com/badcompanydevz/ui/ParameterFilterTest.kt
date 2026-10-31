package com.badcompanydevz.ui

import com.badcompanydevz.model.AnalyticsEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParameterFilterTest {
    private val event = AnalyticsEvent("purchase", listOf("item" to "book", "source" to "app"))

    @Test
    fun noKnownParametersKeepsEventsAndParametersVisible() {
        val filter = ParameterFilter(emptyMap(), "")

        assertTrue(filter.matches(event))
        assertEquals(event.params, filter.visibleParams(event))
    }

    @Test
    fun deselectingEverythingHidesEventsAndParametersEvenWhenSearching() {
        val filter = ParameterFilter(mapOf("item" to null, "source" to null), "purchase")

        assertFalse(filter.matches(event))
        assertTrue(filter.visibleParams(event).isEmpty())
    }

    @Test
    fun selectedParameterMatchesAndOnlySelectedParametersAreVisible() {
        val filter = ParameterFilter(mapOf("item" to "boo", "source" to null), "")

        assertTrue(filter.matches(event))
        assertEquals(listOf("item" to "book"), filter.visibleParams(event))
        assertFalse(ParameterFilter(mapOf("item" to "other", "source" to null), "").matches(event))
    }

    @Test
    fun allFilledValuesMustMatch() {
        assertTrue(ParameterFilter(mapOf("item" to "BOOK", "source" to "app"), "").matches(event))
        assertFalse(ParameterFilter(mapOf("item" to "book", "source" to "web"), "").matches(event))
        assertFalse(ParameterFilter(mapOf("missing" to ""), "").matches(event))
    }

    @Test
    fun eventSearchMatchesNameOrTitleIndependentlyOfParameterValues() {
        val titled = AnalyticsEvent("purchase", event.params + ("eventLabel" to "Checkout"))
        val filter = ParameterFilter(mapOf("item" to "other"), "CHECK")

        assertTrue(filter.matches(titled))
        assertTrue(ParameterFilter(mapOf("item" to "other"), "PURCHASE").matches(titled))
        assertFalse(ParameterFilter(mapOf("item" to "other"), "unknown").matches(titled))
    }
}