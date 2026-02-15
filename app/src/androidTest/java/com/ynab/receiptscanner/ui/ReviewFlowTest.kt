package com.ynab.receiptscanner.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
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
 * UI tests for Review flow
 * Tests receipt review, editing, and submission
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ReviewFlowTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun reviewScreen_displaysExtractedFields() {
        // Given - Navigate to camera and capture
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.navigation_camera)).perform(click())
        onView(withId(R.id.button_capture)).perform(click())
        
        // Wait for OCR
        Thread.sleep(3000)
        
        // Then - Review screen should show extracted data
        onView(withId(R.id.edit_payee))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.edit_amount))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.edit_date))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun editPayee_updatesValue() {
        // Given - On review screen
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.navigation_camera)).perform(click())
        onView(withId(R.id.button_capture)).perform(click())
        Thread.sleep(3000)
        
        // When - Edit payee
        onView(withId(R.id.edit_payee))
            .perform(clearText(), typeText("New Store Name"), closeSoftKeyboard())
        
        // Then - Value should be updated
        onView(withId(R.id.edit_payee))
            .check(matches(withText("New Store Name")))
    }
    
    @Test
    fun editAmount_updatesValue() {
        // Given - On review screen
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.navigation_camera)).perform(click())
        onView(withId(R.id.button_capture)).perform(click())
        Thread.sleep(3000)
        
        // When - Edit amount
        onView(withId(R.id.edit_amount))
            .perform(clearText(), typeText("29.99"), closeSoftKeyboard())
        
        // Then - Value should be updated
        onView(withId(R.id.edit_amount))
            .check(matches(withText("29.99")))
    }
    
    @Test
    fun selectAccount_opensAccountPicker() {
        // Given - On review screen
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.navigation_camera)).perform(click())
        onView(withId(R.id.button_capture)).perform(click())
        Thread.sleep(3000)
        
        // When - Click account selector
        onView(withId(R.id.button_select_account))
            .perform(click())
        
        // Then - Account picker dialog should appear
        onView(withText(R.string.select_account))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun selectCategory_opensCategoryPicker() {
        // Given - On review screen
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.navigation_camera)).perform(click())
        onView(withId(R.id.button_capture)).perform(click())
        Thread.sleep(3000)
        
        // When - Click category selector
        onView(withId(R.id.button_select_category))
            .perform(click())
        
        // Then - Category picker dialog should appear
        onView(withText(R.string.select_category))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun submitButton_withValidData_createsTransaction() {
        // Given - On review screen with data
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.navigation_camera)).perform(click())
        onView(withId(R.id.button_capture)).perform(click())
        Thread.sleep(3000)
        
        // Ensure required fields are filled
        onView(withId(R.id.edit_payee))
            .perform(clearText(), typeText("Test Store"), closeSoftKeyboard())
        
        // When - Submit transaction
        onView(withId(R.id.button_submit))
            .perform(scrollTo(), click())
        
        // Then - Should show success and navigate
        Thread.sleep(1000)
        onView(withId(R.id.bottom_navigation))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun submitButton_withoutAccount_showsError() {
        // Given - On review screen
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.navigation_camera)).perform(click())
        onView(withId(R.id.button_capture)).perform(click())
        Thread.sleep(3000)
        
        // When - Try to submit without selecting account
        onView(withId(R.id.button_submit))
            .perform(scrollTo(), click())
        
        // Then - Should show error
        onView(withText(R.string.error_account_required))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun reviewScreen_hasReceiptImage() {
        // Given - Captured a receipt
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.navigation_camera)).perform(click())
        onView(withId(R.id.button_capture)).perform(click())
        Thread.sleep(3000)
        
        // Then - Should display receipt image
        onView(withId(R.id.image_receipt))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun backButton_fromReview_discardsReceipt() {
        // Given - On review screen
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.navigation_camera)).perform(click())
        onView(withId(R.id.button_capture)).perform(click())
        Thread.sleep(3000)
        
        // When - Press back
        onView(isRoot()).perform(pressBack())
        
        // Then - Should return to camera (or show confirmation)
        onView(withId(R.id.camera_preview))
            .check(matches(isDisplayed()))
    }
}
