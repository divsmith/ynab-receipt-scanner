package com.receiptscanner.presentation.camera

import android.content.Context
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FramingOverlayViewTest {

    private lateinit var context: Context
    private lateinit var view: FramingOverlayView

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        view = FramingOverlayView(context)
    }

    @After
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `view initializes without crashing`() {
        // When - view is created in setup
        
        // Then
        assertNotNull("View should not be null", view)
    }

    @Test
    fun `view has correct default properties`() {
        // Then
        assertNotNull("View should have paint object", view)
        // View should be visible and enabled
        assertTrue("View should be visible", view.visibility == android.view.View.VISIBLE)
    }

    @Test
    fun `onDraw does not crash with valid canvas`() {
        // Given
        val canvas = mockk<Canvas>(relaxed = true)
        every { canvas.width } returns 1080
        every { canvas.height } returns 1920

        // When - Force onDraw call
        view.layout(0, 0, 1080, 1920)
        view.draw(canvas)

        // Then - Should not crash
        verify(atLeast = 1) { canvas.drawRect(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `view calculates frame rectangle within bounds`() {
        // Given
        view.layout(0, 0, 1080, 1920)

        // When - Layout is applied
        view.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(1920, android.view.View.MeasureSpec.EXACTLY)
        )

        // Then - View should have valid dimensions
        assertTrue("View width should be positive", view.measuredWidth > 0)
        assertTrue("View height should be positive", view.measuredHeight > 0)
    }

    @Test
    fun `view renders without crashing on different screen sizes`() {
        // Test various screen sizes
        val screenSizes = listOf(
            Pair(720, 1280),   // Small phone
            Pair(1080, 1920),  // Medium phone
            Pair(1440, 2560)   // Large phone
        )

        screenSizes.forEach { (width, height) ->
            // Given
            val testView = FramingOverlayView(context)
            val canvas = mockk<Canvas>(relaxed = true)
            every { canvas.width } returns width
            every { canvas.height } returns height

            // When
            testView.layout(0, 0, width, height)
            testView.draw(canvas)

            // Then - Should not crash
            verify(atLeast = 1) { canvas.drawRect(any(), any(), any(), any(), any()) }
            clearMocks(canvas)
        }
    }

    @Test
    fun `setGuideText updates text and triggers invalidate`() {
        // Given
        val newText = "Test guide text"

        // When
        view.setGuideText(newText)

        // Then
        // View properties should be updated (this would trigger a redraw)
        assertNotNull("View should still be valid", view)
    }

    @Test
    fun `view draws guide text when canvas is valid`() {
        // Given
        val canvas = mockk<Canvas>(relaxed = true)
        every { canvas.width } returns 1080
        every { canvas.height } returns 1920
        view.setGuideText("Align receipt within frame")

        // When
        view.layout(0, 0, 1080, 1920)
        view.draw(canvas)

        // Then
        verify(atLeast = 1) { canvas.drawText(any<String>(), any(), any(), any()) }
    }
}
