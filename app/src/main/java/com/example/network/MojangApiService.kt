package com.example.network

import retrofit2.http.GET
import retrofit2.http.Url

interface MojangApiService {
    @GET("v1/packages/json/version_manifest_v2.json")
    suspend fun getVersionManifest(): MojangVersionManifest

    @GET
    suspend fun getVersionDetail(@Url url: String): VersionDetail
}
