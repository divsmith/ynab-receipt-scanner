package com.ynab.receiptscanner.core.util

import android.content.Context
import android.widget.Toast
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

/**
 * Common extension functions used throughout the app
 */

// String extensions
/**
 * Checks if string is a valid email address
 */
fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

/**
 * Capitalizes first letter of each word
 */
fun String.toTitleCase(): String {
    return split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        }
    }
}

/**
 * Removes all whitespace from string
 */
fun String.removeWhitespace(): String {
    return replace("\\s".toRegex(), "")
}

// Date extensions
/**
 * Formats date to MM/dd/yyyy
 */
fun Date.toFormattedString(pattern: String = "MM/dd/yyyy"): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(this)
}

/**
 * Formats date to ISO 8601 format for YNAB API
 */
fun Date.toIso8601(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(this)
}

// Number extensions
/**
 * Formats number as currency with proper locale and symbol
 */
fun Double.toCurrencyString(currencyCode: String = "USD"): String {
    val format = NumberFormat.getCurrencyInstance()
    format.currency = Currency.getInstance(currencyCode)
    return format.format(this)
}

/**
 * Converts dollars to milliunits (YNAB uses milliunits: 1 dollar = 1000 milliunits)
 */
fun Double.toMilliunits(): Long {
    return (this * 1000).toLong()
}

/**
 * Converts milliunits to dollars
 */
fun Long.toDecimal(): Double {
    return this / 1000.0
}

// Context extensions
/**
 * Shows a short toast message
 */
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

/**
 * Shows a long toast message
 */
fun Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

// Collection extensions
/**
 * Returns a list with the item added if condition is true
 */
fun <T> List<T>.addIf(condition: Boolean, item: T): List<T> {
    return if (condition) this + item else this
}

/**
 * Returns the list if not empty, null otherwise
 */
fun <T> List<T>.takeIfNotEmpty(): List<T>? {
    return if (isNotEmpty()) this else null
}
