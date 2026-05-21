package com.example.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class SearchResponse(val hits: List<ModHit>)
data class ModHit(
    @SerializedName("project_id") val project_id: String,
    val title: String,
    val description: String,
    val author: String,
    val downloads: Int,
    @SerializedName("icon_url") val icon_url: String
)

data class ProjectDetails(
    val categories: List<String>,
    val versions: List<String>
)

data class ModVersion(
    val files: List<ModFile>
)

data class ModFile(
    val url: String,
    val primary: Boolean
)

interface ModrinthApiService {
    @GET("v2/search")
    suspend fun searchMods(@Query("query") query: String): SearchResponse

    @GET("v2/project/{id}")
    suspend fun getProjectDetails(@Path("id") id: String): ProjectDetails

    @GET("v2/project/{id}/version")
    suspend fun getProjectVersions(@Path("id") id: String): List<ModVersion>
}
