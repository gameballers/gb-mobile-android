package com.gameball.gameball.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GameballPushTest {

    @Test
    fun pushWithoutIsGbIsNotGameballs() {
        assertNull(GameballPush.from(mapOf("title" to "Sale", "gbClickToken" to "abc")))
    }

    @Test
    fun isGbIsMatchedCaseInsensitively() {
        assertNotNull(GameballPush.from(mapOf("isGB" to "TRUE")))
    }

    @Test
    fun keyIsTheClickToken() {
        val push = GameballPush.from(mapOf("isGB" to "true", "gbClickToken" to "abc", "google.message_id" to "0:1"))!!

        assertEquals("abc", push.clickToken)
        assertEquals("abc", push.key)
    }

    @Test
    fun blankClickTokenFallsBackToTheMessageId() {
        val push = GameballPush.from(mapOf("isGB" to "true", "gbClickToken" to " ", "google.message_id" to "0:1"))!!

        assertNull(push.clickToken)
        assertEquals("0:1", push.key)
    }
}
