package com.example.network

import com.google.gson.annotations.SerializedName

data class MojangVersionManifest(
    @SerializedName("latest") val latest: LatestVersions,
    @SerializedName("versions") val versions: List<MojangVersion>
)

data class LatestVersions(
    @SerializedName("release") val release: String,
    @SerializedName("snapshot") val snapshot: String
)

data class MojangVersion(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("url") val url: String,
    @SerializedName("time") val time: String,
    @SerializedName("releaseTime") val releaseTime: String
)

data class VersionDetail(
    @SerializedName("id") val id: String,
    @SerializedName("libraries") val libraries: List<Library>,
    @SerializedName("mainClass") val mainClass: String,
    @SerializedName("assetIndex") val assetIndex: AssetIndex,
    @SerializedName("assets") val assets: String,
    @SerializedName("downloads") val downloads: Downloads
)

data class Downloads(
    @SerializedName("client") val client: DownloadInfo
)

data class DownloadInfo(
    @SerializedName("url") val url: String,
    @SerializedName("size") val size: Long,
    @SerializedName("sha1") val sha1: String
)

data class AssetIndex(
    @SerializedName("id") val id: String,
    @SerializedName("sha1") val sha1: String,
    @SerializedName("size") val size: Long,
    @SerializedName("url") val url: String
)

data class Library(
    @SerializedName("name") val name: String,
    @SerializedName("downloads") val downloads: LibraryDownloads?
)

data class LibraryDownloads(
    @SerializedName("artifact") val artifact: DownloadInfo?
)
