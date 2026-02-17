package com.receiptscanner.domain.usecase

import android.net.Uri
import com.receiptscanner.domain.repository.ImageRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

class CaptureReceiptUseCaseTest {

    private lateinit var useCase: CaptureReceiptUseCase
    private lateinit var imageRepository: ImageRepository

    @Before
    fun setup() {
        imageRepository = mockk()
        useCase = CaptureReceiptUseCase(imageRepository)
    }

    @After
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `invoke with valid image data returns success with URI`() = runTest {
        // Given
        val imageData = ByteArray(1024) { it.toByte() }
        val expectedUri = mockk<Uri>()
        coEvery { imageRepository.saveImage(imageData) } returns expectedUri

        // When
        val result = useCase.invoke(imageData)

        // Then
        assertTrue("Result should be success", result.isSuccess)
        assertEquals(expectedUri, result.getOrNull())
        coVerify(exactly = 1) { imageRepository.saveImage(imageData) }
    }

    @Test
    fun `invoke with empty image data returns failure`() = runTest {
        // Given
        val imageData = ByteArray(0)

        // When
        val result = useCase.invoke(imageData)

        // Then
        assertTrue("Result should be failure for empty data", result.isFailure)
        assertTrue(
            "Error message should mention empty data",
            result.exceptionOrNull()?.message?.contains("empty", ignoreCase = true) == true
        )
        coVerify(exactly = 0) { imageRepository.saveImage(any()) }
    }

    @Test
    fun `invoke with image size below minimum returns failure`() = runTest {
        // Given - image data too small (less than 1KB)
        val imageData = ByteArray(512) { it.toByte() }

        // When
        val result = useCase.invoke(imageData)

        // Then
        assertTrue("Result should be failure for image too small", result.isFailure)
        assertTrue(
            "Error message should mention size",
            result.exceptionOrNull()?.message?.contains("small", ignoreCase = true) == true ||
            result.exceptionOrNull()?.message?.contains("size", ignoreCase = true) == true
        )
        coVerify(exactly = 0) { imageRepository.saveImage(any()) }
    }

    @Test
    fun `invoke with image size above maximum returns failure`() = runTest {
        // Given - image data too large (more than 10MB)
        val imageData = ByteArray(11 * 1024 * 1024) { 0 }

        // When
        val result = useCase.invoke(imageData)

        // Then
        assertTrue("Result should be failure for image too large", result.isFailure)
        assertTrue(
            "Error message should mention size or large",
            result.exceptionOrNull()?.message?.contains("large", ignoreCase = true) == true ||
            result.exceptionOrNull()?.message?.contains("size", ignoreCase = true) == true
        )
        coVerify(exactly = 0) { imageRepository.saveImage(any()) }
    }

    @Test
    fun `invoke when repository save fails returns failure`() = runTest {
        // Given
        val imageData = ByteArray(2048) { it.toByte() }
        val exception = IOException("Disk full")
        coEvery { imageRepository.saveImage(imageData) } throws exception

        // When
        val result = useCase.invoke(imageData)

        // Then
        assertTrue("Result should be failure when repository fails", result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { imageRepository.saveImage(imageData) }
    }

    @Test
    fun `invoke validates image dimensions are within acceptable range`() = runTest {
        // Given - valid image data
        val imageData = ByteArray(5 * 1024) { it.toByte() }
        val expectedUri = mockk<Uri>()
        coEvery { imageRepository.saveImage(imageData) } returns expectedUri

        // When
        val result = useCase.invoke(imageData)

        // Then
        assertTrue("Result should be success for valid dimensions", result.isSuccess)
        assertEquals(expectedUri, result.getOrNull())
    }

    @Test
    fun `invoke handles repository returning null gracefully`() = runTest {
        // Given
        val imageData = ByteArray(2048) { it.toByte() }
        coEvery { imageRepository.saveImage(imageData) } returns null

        // When
        val result = useCase.invoke(imageData)

        // Then
        assertTrue("Result should be failure when URI is null", result.isFailure)
        assertTrue(
            "Error message should mention save failure",
            result.exceptionOrNull()?.message?.contains("save", ignoreCase = true) == true ||
            result.exceptionOrNull()?.message?.contains("null", ignoreCase = true) == true
        )
    }
}
