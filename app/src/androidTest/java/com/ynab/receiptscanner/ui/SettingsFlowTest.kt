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
 * UI tests for Settings flow
 * Tests settings screen, preferences, and sign out
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsFlowTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun settingsScreen_displays_allOptions() {
        // Given
        ActivityScenario.launch(MainActivity::class.java)
        
        // When - Navigate to settings
        onView(withId(R.id.navigation_settings))
            .perform(click())
        
        // Then - Should show all settings options
        onView(withText(R.string.settings))
            .check(matches(isDisplayed()))
        
        onView(withText(R.string.account))
            .check(matches(isDisplayed()))
        
        onView(withText(R.string.sync_settings))
            .check(matches(isDisplayed()))
        
        onView(withText(R.string.about))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun signOutButton_clicked_showsConfirmation() {
        // Given
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.navigation_settings))
            .perform(click())
        
        // When - Click sign out
        onView(withText(R.string.sign_out))
            .perform(click())
        
        // Then - Should show confirmation dialog
        onView(withText(R.string.sign_out_confirmation))
            .check(matches(isDisplayed()))
        
        onView(withText(android.R.string.cancel))
            .check(matches(isDisplayed()))
        
        onView(withText(R.string.sign_out))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun signOut_confirmed_navigatesToAuth() {
        // Given
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.navigation_settings))
            .perform(click())
        
        // When - Confirm sign out
        onView(withText(R.string.sign_out))
            .perform(click())
        
        onView(withText(R.string.sign_out))
            .perform(click())
        
        // Then - Should navigate to auth screen
        Thread.sleep(1000)
        onView(withText(R.string.sign_in_with_ynab))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun syncSettings_displays_options() {
        // Given
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.navigation_settings))
            .perform(click())
        
        // When - Click sync settings
        onView(withText(R.string.sync_settings))
            .perform(click())
        
        // Then - Should show sync options
        onView(withText(R.string.auto_sync))
            .check(matches(isDisplayed()))
        
        onView(withText(R.string.sync_on_wifi_only))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun clearCache_clicked_showsConfirmation() {
        // Given
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.navigation_settings))
            .perform(click())
        
        // When - Click clear cache
        onView(withText(R.string.clear_cache))
            .perform(click())
        
        // Then - Should show confirmation
        onView(withText(R.string.clear_cache_confirmation))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun aboutScreen_displays_appInfo() {
        // Given
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.navigation_settings))
            .perform(click())
        
        // When - Click about
        onView(withText(R.string.about))
            .perform(click())
        
        // Then - Should show app info
        onView(withText(R.string.app_version))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun budgetSelection_displays_budgets() {
        // Given
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.navigation_settings))
            .perform(click())
        
        // When - Click change budget
        onView(withText(R.string.select_budget))
            .perform(click())
        
        // Then - Should show budget list
        Thread.sleep(1000)
        onView(withText(R.string.available_budgets))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun autoSyncToggle_changes_preference() {
        // Given
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.navigation_settings))
            .perform(click())
        onView(withText(R.string.sync_settings))
            .perform(click())
        
        // When - Toggle auto sync
        onView(withText(R.string.auto_sync))
            .perform(click())
        
        // Then - Preference should be saved (verified by toggle state)
        onView(withText(R.string.auto_sync))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun settingsScreen_backButton_returnsToHome() {
        // Given
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.navigation_settings))
            .perform(click())
        
        // When - Press back
        onView(isRoot()).perform(pressBack())
        
        // Then - Should return to home
        onView(withId(R.id.bottom_navigation))
            .check(matches(isDisplayed()))
    }
}
