package com.example.blurdetect.api

import com.example.blurdetect.model.ImageRequest
import com.example.blurdetect.model.ImageRespon
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("predict/")
    fun uploadImage(@Part image: MultipartBody.Part):Call<ImageRespon>

    @Multipart
    @POST("deblur/")
    fun deblurImage(@Part image: MultipartBody.Part):Call<ImageRespon>
}