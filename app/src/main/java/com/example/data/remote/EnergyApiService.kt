package com.example.data.remote

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface EnergyApiService {
    @GET
    suspend fun getLatest(
        @Url url: String,
        @Query("action") action: String = "latest"
    ): RemoteLatestResponse

    @GET
    suspend fun getHistory(
        @Url url: String,
        @Query("action") action: String = "history",
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): List<RemotePowerRecord>

    @GET
    suspend fun getSummary(
        @Url url: String,
        @Query("action") action: String = "summary"
    ): RemoteSummaryResponse

    @GET
    suspend fun getAnalytics(
        @Url url: String,
        @Query("action") action: String = "analytics"
    ): RemoteAnalyticsResponse

    @GET
    suspend fun getDaily(
        @Url url: String,
        @Query("action") action: String = "daily"
    ): List<RemoteDailyEnergy>

    @GET
    suspend fun getMonthly(
        @Url url: String,
        @Query("action") action: String = "monthly"
    ): List<RemoteMonthlyEnergy>
}
