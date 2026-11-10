package com.gameball.gameball.inappmessaging.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterEvaluatorTest {

    private fun matches(
        name: String, op: FilterOperator, value: Any, metadata: Map<String, Any?>
    ) = FilterEvaluator.matches(listOf(MetadataFilter(name, op, value)), metadata)

    @Test
    fun `no filters always matches`() {
        assertTrue(FilterEvaluator.matches(emptyList(), emptyMap()))
        assertTrue(FilterEvaluator.matches(emptyList(), mapOf("a" to 1)))
    }

    @Test
    fun `equals and notEquals`() {
        assertTrue(matches("c", FilterOperator.EQUALS, "USD", mapOf("c" to "USD")))
        assertFalse(matches("c", FilterOperator.EQUALS, "USD", mapOf("c" to "EUR")))
        assertTrue(matches("c", FilterOperator.NOT_EQUALS, "USD", mapOf("c" to "EUR")))
        assertFalse(matches("c", FilterOperator.NOT_EQUALS, "USD", mapOf("c" to "USD")))
    }

    @Test
    fun `the four ordering operators`() {
        assertTrue(matches("p", FilterOperator.GREATER_THAN, 100, mapOf("p" to 150)))
        assertFalse(matches("p", FilterOperator.GREATER_THAN, 100, mapOf("p" to 100)))
        assertTrue(matches("p", FilterOperator.GREATER_OR_EQUAL, 100, mapOf("p" to 100)))
        assertTrue(matches("p", FilterOperator.LESS_THAN, 100, mapOf("p" to 99)))
        assertFalse(matches("p", FilterOperator.LESS_THAN, 100, mapOf("p" to 100)))
        assertTrue(matches("p", FilterOperator.LESS_OR_EQUAL, 100, mapOf("p" to 100)))
    }

    @Test
    fun `contains works on strings and on lists`() {
        assertTrue(matches("t", FilterOperator.CONTAINS, "sale", mapOf("t" to "summer sale")))
        assertFalse(matches("t", FilterOperator.CONTAINS, "sale", mapOf("t" to "summer")))
        assertTrue(matches("t", FilterOperator.CONTAINS, "b", mapOf("t" to listOf("a", "b"))))
        assertFalse(matches("t", FilterOperator.CONTAINS, "z", mapOf("t" to listOf("a", "b"))))
    }

    /** A campaign authored with "quantity": "2" must match an int 2. */
    @Test
    fun `comparisons coerce across numeric types and stringly-typed numbers`() {
        assertTrue(matches("q", FilterOperator.EQUALS, "2", mapOf("q" to 2)))
        assertTrue(matches("q", FilterOperator.EQUALS, 2, mapOf("q" to "2")))
        assertTrue(matches("q", FilterOperator.EQUALS, 2L, mapOf("q" to 2.0)))
        assertTrue(matches("p", FilterOperator.GREATER_THAN, "100", mapOf("p" to 150.5)))
        assertTrue(matches("p", FilterOperator.GREATER_THAN, 100, mapOf("p" to "150")))
    }

    /** A filter is a requirement; absence is failure, otherwise filters are decorative. */
    @Test
    fun `a missing property never matches, for any operator`() {
        FilterOperator.values().forEach { operator ->
            assertFalse(
                "$operator should not match a missing property",
                matches("absent", operator, "anything", mapOf("other" to 1))
            )
        }
    }

    @Test
    fun `a null property value never matches`() {
        FilterOperator.values().forEach { operator ->
            assertFalse(
                "$operator should not match a null property",
                matches("p", operator, "anything", mapOf("p" to null))
            )
        }
    }

    /** Falling back to string comparison here would produce nonsense: "9" > "100". */
    @Test
    fun `ordering operators refuse a non-numeric value rather than comparing strings`() {
        assertFalse(matches("t", FilterOperator.GREATER_THAN, "abc", mapOf("t" to "def")))
        assertFalse(matches("t", FilterOperator.LESS_THAN, 100, mapOf("t" to "cheap")))
        assertFalse(matches("t", FilterOperator.GREATER_OR_EQUAL, "x", mapOf("t" to 5)))
    }

    @Test
    fun `booleans compare by their string form`() {
        assertTrue(matches("b", FilterOperator.EQUALS, true, mapOf("b" to true)))
        assertTrue(matches("b", FilterOperator.EQUALS, "true", mapOf("b" to true)))
        assertFalse(matches("b", FilterOperator.EQUALS, true, mapOf("b" to false)))
    }

    @Test
    fun `every filter must pass`() {
        val filters = listOf(
            MetadataFilter("price", FilterOperator.GREATER_THAN, 100),
            MetadataFilter("currency", FilterOperator.EQUALS, "USD")
        )
        assertTrue(FilterEvaluator.matches(filters, mapOf("price" to 150, "currency" to "USD")))
        assertFalse(FilterEvaluator.matches(filters, mapOf("price" to 150, "currency" to "EUR")))
        assertFalse(FilterEvaluator.matches(filters, mapOf("price" to 50, "currency" to "USD")))
        assertFalse(FilterEvaluator.matches(filters, mapOf("price" to 150)))
    }
}
