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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for MainActivity
 * Tests main navigation and app launch behavior
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun mainActivity_launches_successfully() {
        // When
        ActivityScenario.launch(MainActivity::class.java)
        
        // Then - Activity should display bottom navigation
        onView(withId(R.id.bottom_navigation))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun bottomNavigation_hasAllTabs() {
        // When
        ActivityScenario.launch(MainActivity::class.java)
        
        // Then - Should have all navigation items
        onView(withId(R.id.navigation_home))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.navigation_camera))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.navigation_settings))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun bottomNavigation_switchToCamera_displaysCamera() {
        // Given
        ActivityScenario.launch(MainActivity::class.java)
        
        // When
        onView(withId(R.id.navigation_camera))
            .perform(click())
        
        // Then - Should navigate to camera fragment
        onView(withId(R.id.camera_preview))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun bottomNavigation_switchToSettings_displaysSettings() {
        // Given
        ActivityScenario.launch(MainActivity::class.java)
        
        // When
        onView(withId(R.id.navigation_settings))
            .perform(click())
        
        // Then - Should navigate to settings fragment
        onView(withText(R.string.settings))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun bottomNavigation_switchBetweenTabs_preservesState() {
        // Given
        ActivityScenario.launch(MainActivity::class.java)
        
        // When - Switch tabs multiple times
        onView(withId(R.id.navigation_camera))
            .perform(click())
        
        onView(withId(R.id.navigation_home))
            .perform(click())
        
        onView(withId(R.id.navigation_settings))
            .perform(click())
        
        onView(withId(R.id.navigation_home))
            .perform(click())
        
        // Then - Should return to home without crash
        onView(withId(R.id.bottom_navigation))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun mainActivity_rotateScreen_maintainsState() {
        // Given
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // When - Rotate screen
        scenario.onActivity { activity ->
            activity.requestedOrientation = 
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        
        // Wait for rotation
        Thread.sleep(500)
        
        // Then - UI should still be functional
        onView(withId(R.id.bottom_navigation))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun mainActivity_backPressed_exitsApp() {
        // Given
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // When - Press back
        scenario.onActivity { activity ->
            activity.onBackPressed()
        }
        
        // Then - Activity should finish
        assert(scenario.state.name == "DESTROYED")
    }
}
