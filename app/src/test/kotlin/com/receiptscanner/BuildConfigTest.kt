package com.receiptscanner

import org.junit.Test
import org.junit.Assert.*

/**
 * Tests to verify build configuration meets requirements.
 */
class BuildConfigTest {

    @Test
    fun `minSdk should be API 26 (Android 8_0)`() {
        assertEquals("Min SDK must be 26", 26, BuildConfig.MIN_SDK)
    }

    @Test
    fun `targetSdk should be 33 or higher`() {
        assertTrue(
            "Target SDK must be at least 33",
            BuildConfig.TARGET_SDK >= 33
        )
    }

    @Test
    fun `application ID should be correct`() {
        assertEquals(
            "Application ID must be com.receiptscanner",
            "com.receiptscanner",
            BuildConfig.APPLICATION_ID
        )
    }

    @Test
    fun `debug build should be debuggable`() {
        // In debug builds, DEBUG flag should be true
        assertEquals("Debug build should have DEBUG=true", "debug", BuildConfig.BUILD_TYPE.lowercase())
    }
}
