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

    /** The dashboard authors the range as one value, "min,max". */
    @Test
    fun `between is inclusive at both ends`() {
        assertTrue(matches("a", FilterOperator.BETWEEN, "100,500", mapOf("a" to 300)))
        assertTrue("the lower edge", matches("a", FilterOperator.BETWEEN, "100,500", mapOf("a" to 100)))
        assertTrue("the upper edge", matches("a", FilterOperator.BETWEEN, "100,500", mapOf("a" to 500)))
        assertFalse(matches("a", FilterOperator.BETWEEN, "100,500", mapOf("a" to 99.99)))
        assertFalse(matches("a", FilterOperator.BETWEEN, "100,500", mapOf("a" to 500.01)))
    }

    /** As strings, "9" > "50" and this range would hold nothing. */
    @Test
    fun `between compares as numbers, with spaces around either bound`() {
        assertTrue(matches("a", FilterOperator.BETWEEN, "9,100", mapOf("a" to 50L)))
        assertTrue(matches("a", FilterOperator.BETWEEN, "9,100", mapOf("a" to "50")))
        assertTrue(matches("a", FilterOperator.BETWEEN, " 1.5 , 2.5 ", mapOf("a" to 2)))
    }

    /** Swapped, as iOS does, rather than refused. */
    @Test
    fun `between bounds given the wrong way round are swapped`() {
        assertTrue(matches("a", FilterOperator.BETWEEN, "500,100", mapOf("a" to 300)))
        assertFalse(matches("a", FilterOperator.BETWEEN, "500,100", mapOf("a" to 50)))
    }

    @Test
    fun `between never matches a range that is not two numbers`() {
        listOf("100", "100,", ",100", "abc", "a,b", "100,abc", "50,100,150", "", 100L).forEach { range ->
            assertFalse("'$range'", matches("a", FilterOperator.BETWEEN, range, mapOf("a" to 100)))
        }
    }

    @Test
    fun `between never matches an event value that is not a number`() {
        assertFalse(matches("a", FilterOperator.BETWEEN, "100,500", mapOf("a" to "cheap")))
        assertFalse(matches("a", FilterOperator.BETWEEN, "100,500", mapOf("a" to true)))
        assertFalse(matches("a", FilterOperator.BETWEEN, "100,500", mapOf("a" to listOf(300))))
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

    @Test
    fun `under any, one passing filter is enough`() {
        val filters = listOf(
            MetadataFilter("price", FilterOperator.GREATER_THAN, 100),
            MetadataFilter("currency", FilterOperator.EQUALS, "USD")
        )
        val any = FilterLogic.ANY
        assertTrue(FilterEvaluator.matches(filters, mapOf("price" to 150, "currency" to "EUR"), any))
        assertTrue(FilterEvaluator.matches(filters, mapOf("price" to 50, "currency" to "USD"), any))
        assertTrue(FilterEvaluator.matches(filters, mapOf("currency" to "USD"), any))
        assertFalse(FilterEvaluator.matches(filters, mapOf("price" to 50, "currency" to "EUR"), any))
        assertFalse(FilterEvaluator.matches(filters, emptyMap(), any))
    }

    /** No conditions means nothing to fail, under either logic. */
    @Test
    fun `under any, no filters still matches`() {
        assertTrue(FilterEvaluator.matches(emptyList(), emptyMap(), FilterLogic.ANY))
    }
}
