// app/src/main/java/com/android/birdlens/data/local/LocalHotspot.kt
package com.android.birdlens.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.android.birdlens.data.model.ebird.EbirdNearbyHotspot

@Entity(tableName = "hotspots")
data class LocalHotspot(
    @PrimaryKey @ColumnInfo(name = "loc_id") val locId: String,
    @ColumnInfo(name = "loc_name") val locName: String,
    @ColumnInfo(name = "country_code") val countryCode: String,
    @ColumnInfo(name = "subnational1_code") val subnational1Code: String?,
    @ColumnInfo(name = "subnational2_code") val subnational2Code: String?,
    @ColumnInfo(name = "latitude") val lat: Double,
    @ColumnInfo(name = "longitude") val lng: Double,
    @ColumnInfo(name = "latest_obs_dt") val latestObsDt: String?,
    @ColumnInfo(name = "num_species_all_time") val numSpeciesAllTime: Int?,
    @ColumnInfo(name = "last_updated_timestamp") val lastUpdatedTimestamp: Long // For cache expiry
)

fun LocalHotspot.toEbirdNearbyHotspot(): EbirdNearbyHotspot {
    return EbirdNearbyHotspot(
        locId = this.locId,
        locName = this.locName,
        countryCode = this.countryCode,
        subnational1Code = this.subnational1Code,
        subnational2Code = this.subnational2Code,
        lat = this.lat,
        lng = this.lng,
        latestObsDt = this.latestObsDt,
        numSpeciesAllTime = this.numSpeciesAllTime
    )
}

fun EbirdNearbyHotspot.toLocalHotspot(timestamp: Long): LocalHotspot {
    return LocalHotspot(
        locId = this.locId,
        locName = this.locName,
        countryCode = this.countryCode,
        subnational1Code = this.subnational1Code,
        subnational2Code = this.subnational2Code,
        lat = this.lat,
        lng = this.lng,
        latestObsDt = this.latestObsDt,
        numSpeciesAllTime = this.numSpeciesAllTime,
        lastUpdatedTimestamp = timestamp
    )
}