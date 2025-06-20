// EXE201/app/src/main/java/com/android/birdlens/data/model/HotspotAnalysisModels.kt
package com.android.birdlens.data.model

import com.google.gson.annotations.SerializedName

data class VisitingTimesAnalysis(
    @SerializedName("monthly_activity")
    val monthlyActivity: List<MonthlyStat>,
    @SerializedName("hourly_activity")
    val hourlyActivity: List<HourlyStat>
)

data class MonthlyStat(
    @SerializedName("month")
    val month: String, // e.g., "January"
    @SerializedName("relative_frequency")
    val relativeFrequency: Double // A score from 0.0 to 1.0
)

data class HourlyStat(
    @SerializedName("hour")
    val hour: Int, // 0-23
    @SerializedName("relative_frequency")
    val relativeFrequency: Double // A score from 0.0 to 1.0
)