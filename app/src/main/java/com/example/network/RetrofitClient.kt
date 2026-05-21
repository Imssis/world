package com.example.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://api.modrinth.com/"
    
    val apiService: ModrinthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ModrinthApiService::class.java)
    }

    val mojangApiService: MojangApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://launchermeta.mojang.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MojangApiService::class.java)
    }
}
