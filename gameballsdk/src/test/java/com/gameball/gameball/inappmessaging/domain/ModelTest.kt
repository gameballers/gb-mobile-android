package com.gameball.gameball.inappmessaging.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelTest {

    @Test
    fun `the three drawable types map to themselves`() {
        assertEquals(MessageType.SLIDEUP, MessageType.from(1))
        assertEquals(MessageType.MODAL, MessageType.from(2))
        assertEquals(MessageType.FULLSCREEN, MessageType.from(3))
        assertTrue(MessageType.from(1).isSupported)
    }

    /**
     * 4 (htmlFullscreen) and 5 (emailCapture) are known-but-unimplemented; anything else is
     * a type this SDK version predates. Both keep the campaign and mark it unsupported, so
     * selection can filter it and let a lower-priority campaign win.
     */
    @Test
    fun `known-but-unimplemented and unknown types are both unsupported`() {
        assertFalse(MessageType.from(4).isSupported)
        assertFalse(MessageType.from(5).isSupported)
        assertFalse(MessageType.from(99).isSupported)
        assertFalse(MessageType.from(0).isSupported)
        assertFalse(MessageType.from(-1).isSupported)
    }

    @Test
    fun `filter operators accept the backend spellings case-insensitively`() {
        assertEquals(FilterOperator.EQUALS, FilterOperator.from("equals"))
        assertEquals(FilterOperator.EQUALS, FilterOperator.from("Is"))
        assertEquals(FilterOperator.EQUALS, FilterOperator.from("EQUALS"))
        assertEquals(FilterOperator.NOT_EQUALS, FilterOperator.from("notEquals"))
        assertEquals(FilterOperator.NOT_EQUALS, FilterOperator.from("IsNot"))
        assertEquals(FilterOperator.GREATER_THAN, FilterOperator.from("greaterThan"))
        assertEquals(FilterOperator.GREATER_OR_EQUAL, FilterOperator.from("greaterThanOrEqual"))
        assertEquals(FilterOperator.GREATER_OR_EQUAL, FilterOperator.from("greater_than_or_equals"))
        assertEquals(FilterOperator.LESS_THAN, FilterOperator.from("lessThan"))
        assertEquals(FilterOperator.LESS_OR_EQUAL, FilterOperator.from("lessThanOrEqual"))
        assertEquals(FilterOperator.CONTAINS, FilterOperator.from("Contains"))
    }

    @Test
    fun `an unrecognised operator is null so that one filter drops, not the campaign`() {
        assertNull(FilterOperator.from("startsWith"))
        assertNull(FilterOperator.from(""))
        assertNull(FilterOperator.from(null))
    }

    @Test
    fun `text alignment accepts all five spellings case-insensitively`() {
        assertEquals(TextAlign.START, TextAlign.from("start"))
        assertEquals(TextAlign.END, TextAlign.from("END"))
        assertEquals(TextAlign.CENTER, TextAlign.from("Center"))
        assertEquals(TextAlign.LEFT, TextAlign.from("left"))
        assertEquals(TextAlign.RIGHT, TextAlign.from("right"))
        assertNull(TextAlign.from("justified"))
        assertNull(TextAlign.from(null))
    }
}
