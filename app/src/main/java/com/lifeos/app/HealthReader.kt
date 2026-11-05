package com.lifeos.app

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.records.metadata.DataOrigin
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HealthSnapshot(val steps: Long = 0, val activeKcal: Double = 0.0, val eatenKcal: Double = 0.0, val status: String = "Bağlantı bekleniyor")

class HealthReader(private val context: Context) {
    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class)
    )
    fun available(): Boolean = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    fun requiredPermissions() = permissions
    suspend fun readToday(): HealthSnapshot {
        if (!available()) return HealthSnapshot(status = "Health Connect bulunamadı")
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        if (!granted.containsAll(permissions)) return HealthSnapshot(status = "Sağlık izinleri verilmedi")
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val end = Instant.now()
        val range = TimeRangeFilter.between(start, end)
        return try {
            val steps = client.aggregate(androidx.health.connect.client.request.AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL), timeRangeFilter = range
            ))[StepsRecord.COUNT_TOTAL] ?: 0L
            val active = client.aggregate(androidx.health.connect.client.request.AggregateRequest(
                metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL), timeRangeFilter = range
            ))[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0
            val eaten = client.aggregate(androidx.health.connect.client.request.AggregateRequest(
                metrics = setOf(NutritionRecord.ENERGY_TOTAL), timeRangeFilter = range
            ))[NutritionRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
            HealthSnapshot(steps, active, eaten, "Health Connect bağlı")
        } catch (e: Exception) {
            HealthSnapshot(status = "Veri okunamadı: ${e.message ?: "izin veya kaynak kontrolü"}")
        }
    }
}
