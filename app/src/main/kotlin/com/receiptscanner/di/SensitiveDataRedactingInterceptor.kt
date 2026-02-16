package com.receiptscanner.di

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import timber.log.Timber
import org.json.JSONObject

/**
 * OkHttp interceptor that redacts sensitive data from request/response logs
 * to prevent tokens, secrets, and credentials from appearing in debug logs.
 */
class SensitiveDataRedactingInterceptor : Interceptor {

    private val sensitiveHeaders = setOf(
        "authorization",
        "x-api-key",
        "api-key"
    )

    private val sensitiveBodyFields = setOf(
        "access_token",
        "refresh_token",
        "client_secret",
        "client_id",
        "password",
        "api_key"
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Log request with redacted headers
        Timber.d("---> ${request.method} ${request.url}")
        request.headers.forEach { (name, value) ->
            if (sensitiveHeaders.contains(name.lowercase())) {
                Timber.d("$name: [REDACTED]")
            } else {
                Timber.d("$name: $value")
            }
        }

        // Log request body if present (with redaction)
        request.body?.let { body ->
            val buffer = Buffer()
            body.writeTo(buffer)
            val bodyString = buffer.readUtf8()
            
            if (bodyString.isNotEmpty()) {
                val redactedBody = redactSensitiveFields(bodyString)
                Timber.d("Request Body: $redactedBody")
            }
        }

        // Execute request
        val startTime = System.currentTimeMillis()
        val response = chain.proceed(request)
        val duration = System.currentTimeMillis() - startTime

        // Log response with redacted body
        Timber.d("<--- ${response.code} ${response.request.url} (${duration}ms)")
        response.headers.forEach { (name, value) ->
            if (sensitiveHeaders.contains(name.lowercase())) {
                Timber.d("$name: [REDACTED]")
            } else {
                Timber.d("$name: $value")
            }
        }

        // Read and redact response body
        val responseBody = response.body
        if (responseBody != null) {
            val source = responseBody.source()
            source.request(Long.MAX_VALUE)
            val buffer = source.buffer

            val bodyString = buffer.clone().readUtf8()
            if (bodyString.isNotEmpty()) {
                val redactedBody = redactSensitiveFields(bodyString)
                Timber.d("Response Body: $redactedBody")
            }
        }

        return response
    }

    /**
     * Redacts sensitive fields from JSON body strings
     */
    private fun redactSensitiveFields(body: String): String {
        return try {
            val json = JSONObject(body)
            val redacted = JSONObject()
            
            json.keys().forEach { key ->
                val value = json.get(key)
                if (sensitiveBodyFields.contains(key.lowercase())) {
                    redacted.put(key, "[REDACTED]")
                } else {
                    redacted.put(key, value)
                }
            }
            
            redacted.toString(2)
        } catch (e: Exception) {
            // Not JSON or parse error - redact with regex
            var redacted = body
            sensitiveBodyFields.forEach { field ->
                // Match patterns like "field_name": "value" or field_name=value
                redacted = redacted.replace(
                    Regex("""($field)["']?\s*[:=]\s*["']?([^"',&\s}]+)["']?""", RegexOption.IGNORE_CASE)
                ) { matchResult ->
                    "${matchResult.groupValues[1]}=[REDACTED]"
                }
            }
            redacted
        }
    }
}
