package com.applicaster.cleeng.network.error

import com.applicaster.cleeng.network.ServiceGenerator
import retrofit2.Response
import java.io.IOException

class ErrorUtil {
    companion object {
        fun handleError(response: Response<*>): WebServiceError {
            val error: ResponseError = parseError(response)
            var webError: WebServiceError = WebServiceError.DEFAULT
            if (response.code() == 500 && error.code != null) {
                webError = when {
                    error.code.toInt() == 10 -> WebServiceError.NO_USER_EXISTS
                    error.code.toInt() == 13 -> WebServiceError.USER_ALREADY_EXISTS
                    error.code.toInt() == 15 -> WebServiceError.INVALID_CREDENTIALS
                    else -> WebServiceError.DEFAULT
                }
            }
            return webError
        }

        private fun parseError(response: Response<*>): ResponseError {
            val converter = ServiceGenerator.retrofit.responseBodyConverter<ResponseError>(
                ResponseError::class.java, arrayOfNulls<Annotation>(0)
            )

            val error: ResponseError

            try {
                error = response.errorBody()?.let {
                    converter.convert(it)
                } ?: ResponseError("", "")
            } catch (e: IOException) {
                return ResponseError("", "")
            }
            return error
        }
    }
}