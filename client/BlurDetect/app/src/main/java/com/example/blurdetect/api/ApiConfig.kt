package com.example.blurdetect.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiConfig {
    private const val BASE_URL = "http://192.168.24.97:8000";
    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS) // Thời gian timeout kết nối
        .readTimeout(30, TimeUnit.SECONDS)    // Thời gian timeout đọc dữ liệu
        .writeTimeout(30, TimeUnit.SECONDS)   // Thời gian timeout ghi dữ liệu
        .build()
    private val builder = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())

    val retrofit = builder.build()
//    val apiService: ApiService = retrofit.create(ApiService::class.java)
}