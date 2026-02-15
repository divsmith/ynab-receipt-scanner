package com.ynab.receiptscanner.core.util

/**
 * A generic wrapper for handling success and error states in operations
 * Used throughout the app for consistent error handling
 */
sealed class Result<out T> {
    
    /**
     * Successful operation with data
     */
    data class Success<out T>(val data: T) : Result<T>()
    
    /**
     * Failed operation with error details
     */
    data class Error(
        val exception: Throwable,
        val message: String = exception.message ?: "Unknown error"
    ) : Result<Nothing>()
    
    /**
     * Loading state
     */
    object Loading : Result<Nothing>()

    /**
     * Returns true if this is a Success result
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Returns true if this is an Error result
     */
    val isError: Boolean
        get() = this is Error

    /**
     * Returns true if this is a Loading result
     */
    val isLoading: Boolean
        get() = this is Loading

    /**
     * Returns the data if Success, null otherwise
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * Returns the data if Success, throws exception if Error, returns null if Loading
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
        is Loading -> throw IllegalStateException("Result is still loading")
    }
}

/**
 * Extension function to map Success data, preserving Error and Loading states
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
    is Result.Loading -> this
}

/**
 * Extension function to handle result with callbacks
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) {
        action(data)
    }
    return this
}

/**
 * Extension function to handle errors
 */
inline fun <T> Result<T>.onError(action: (Throwable) -> Unit): Result<T> {
    if (this is Result.Error) {
        action(exception)
    }
    return this
}

/**
 * Extension function to handle loading state
 */
inline fun <T> Result<T>.onLoading(action: () -> Unit): Result<T> {
    if (this is Result.Loading) {
        action()
    }
    return this
}
