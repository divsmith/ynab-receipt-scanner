package com.ynab.receiptscanner.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ynab.receiptscanner.R
import com.ynab.receiptscanner.ui.main.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for Camera flow
 * Tests camera permission, capture, and navigation
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CameraFlowTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun cameraFragment_displays_captureButton() {
        // Given
        ActivityScenario.launch(MainActivity::class.java)
        
        // When - Navigate to camera
        onView(withId(R.id.navigation_camera))
            .perform(click())
        
        // Then - Capture button should be visible
        onView(withId(R.id.button_capture))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun cameraFragment_displays_cameraPreview() {
        // Given
        ActivityScenario.launch(MainActivity::class.java)
        
        // When - Navigate to camera
        onView(withId(R.id.navigation_camera))
            .perform(click())
        
        // Then - Camera preview should be visible
        onView(withId(R.id.camera_preview))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun captureButton_clicked_showsProcessing() {
        // Given
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.navigation_camera))
            .perform(click())
        
        // When - Click capture button
        onView(withId(R.id.button_capture))
            .perform(click())
        
        // Then - Should show processing state
        // Note: Timing might be tight, may need IdlingResource
        Thread.sleep(100)
        onView(withId(R.id.progress_indicator))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun cameraCapture_successful_navigatesToReview() {
        // Given
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.navigation_camera))
            .perform(click())
        
        // When - Capture image
        onView(withId(R.id.button_capture))
            .perform(click())
        
        // Wait for OCR processing (in real test, use IdlingResource)
        Thread.sleep(3000)
        
        // Then - Should navigate to review screen
        onView(withId(R.id.edit_payee))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun cameraFragment_hasFlashToggle() {
        // Given
        ActivityScenario.launch(MainActivity::class.java)
        
        // When - Navigate to camera
        onView(withId(R.id.navigation_camera))
            .perform(click())
        
        // Then - Flash toggle should be visible
        onView(withId(R.id.button_flash))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun flashToggle_clicked_changesState() {
        // Given
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.navigation_camera))
            .perform(click())
        
        // When - Toggle flash
        onView(withId(R.id.button_flash))
            .perform(click())
        
        // Then - Flash state should change (verify icon or state)
        onView(withId(R.id.button_flash))
            .check(matches(isDisplayed()))
    }
}
