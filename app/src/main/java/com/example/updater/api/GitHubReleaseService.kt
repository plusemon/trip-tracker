package com.example.updater.api

import com.example.updater.model.GitHubRelease
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface GitHubReleaseService {

    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Accept") accept: String = "application/vnd.github.v3+json",
        @Header("User-Agent") userAgent: String = "TripSplitBD-Android-App"
    ): Response<GitHubRelease>
}
