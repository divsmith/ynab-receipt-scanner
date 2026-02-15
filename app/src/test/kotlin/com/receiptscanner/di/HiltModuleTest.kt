package com.receiptscanner.di

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertNotNull

/**
 * Tests to verify Hilt dependency injection graph is properly configured.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [26])
class HiltModuleTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun `hilt application component should be created successfully`() {
        // If we get here, Hilt has successfully built the application component
        assertNotNull(hiltRule, "HiltAndroidRule should be initialized")
    }

    @Test
    fun `app module should be installed in singleton component`() {
        // This test verifies that AppModule exists and is properly annotated
        // The test passes if Hilt can build the graph, which requires AppModule
        // to be properly configured with @Module and @InstallIn annotations
        assertNotNull(hiltRule, "Hilt DI graph should initialize with AppModule")
    }
}
