package com.applicaster.cleeng.network

import retrofit2.Response
import java.io.IOException

class ErrorUtil {
    companion object {
        fun parseError(response: Response<*>): ResponseError {
            val converter = ServiceGenerator.retrofit.responseBodyConverter<ResponseError>(ResponseError::class.java, arrayOfNulls<Annotation>(0))

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