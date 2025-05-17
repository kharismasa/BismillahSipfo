package com.example.bismillahsipfo.data.network

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @Headers("Content-Type: application/json")
    @POST("{endpoint}")
    suspend fun createTransaction(
        @Path("endpoint") url: String,
        @Header("Authorization") authHeader: String,
        @Body requestBody: RequestBody
    ): Response<ResponseBody>
}