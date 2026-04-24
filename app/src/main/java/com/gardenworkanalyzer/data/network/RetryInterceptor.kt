package com.gardenworkanalyzer.data.network

import com.gardenworkanalyzer.domain.model.MAX_RETRY_ATTEMPTS
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class RetryInterceptor : Interceptor {

    companion object {
        private const val INITIAL_BACKOFF_MS = 1000L
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var lastException: IOException? = null

        for (attempt in 0..MAX_RETRY_ATTEMPTS) {
            try {
                val response = chain.proceed(request)
                if (response.isSuccessful || !isRetryable(response.code) || attempt == MAX_RETRY_ATTEMPTS) {
                    return response
                }
                response.close()
            } catch (e: IOException) {
                lastException = e
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    throw e
                }
            }

            val backoffMs = INITIAL_BACKOFF_MS * (1L shl attempt)
            Thread.sleep(backoffMs)
        }

        throw lastException ?: IOException("Retry failed")
    }

    private fun isRetryable(code: Int): Boolean = code in 500..599
}
