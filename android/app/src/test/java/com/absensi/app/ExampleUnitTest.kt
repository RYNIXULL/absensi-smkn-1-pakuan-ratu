package com.absensi.app

import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun baseUrl_isNotEmpty() {
        assertNotNull(BuildConfig.BASE_URL)
        assertTrue(BuildConfig.BASE_URL.startsWith("http"))
    }
}
