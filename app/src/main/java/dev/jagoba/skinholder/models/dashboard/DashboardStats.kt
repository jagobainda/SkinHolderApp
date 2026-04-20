package dev.jagoba.skinholder.models.dashboard

import com.google.gson.annotations.SerializedName

data class ConnectionStats(
    val status: String,
    val ping: Long,
    val uptime: Long
) {
    companion object {
        const val STATUS_ACTIVE = "active"
        const val STATUS_INACTIVE = "inactive"
    }
}

data class LastRegistryStats(
    val totalSteam: Double,
    val totalGamepay: Double,
    val totalCsfloat: Double
)

data class LatencyStats(
    val steam: Long,
    val gamerpay: Long,
    val csfloat: Long
)

data class VarianceStats(
    @SerializedName("weeklyVariancePercentSteam")
    val weeklyVariancePercentSteam: Double = 0.0,
    @SerializedName("monthlyVariancePercentSteam")
    val monthlyVariancePercentSteam: Double = 0.0,
    @SerializedName("yearlyVariancePercentSteam")
    val yearlyVariancePercentSteam: Double = 0.0,
    @SerializedName("weeklyVariancePercentGamerPay")
    val weeklyVariancePercentGamerPay: Double = 0.0,
    @SerializedName("monthlyVariancePercentGamerPay")
    val monthlyVariancePercentGamerPay: Double = 0.0,
    @SerializedName("yearlyVariancePercentGamerPay")
    val yearlyVariancePercentGamerPay: Double = 0.0,
    @SerializedName("weeklyVariancePercentCSFloat")
    val weeklyVariancePercentCSFloat: Double = 0.0,
    @SerializedName("monthlyVariancePercentCSFloat")
    val monthlyVariancePercentCSFloat: Double = 0.0,
    @SerializedName("yearlyVariancePercentCSFloat")
    val yearlyVariancePercentCSFloat: Double = 0.0
) {
    companion object {
        const val NA_VALUE = -101.0
    }
}

data class DashboardStats(
    val connection: ConnectionStats,
    val lastRegistry: LastRegistryStats?,
    val latency: LatencyStats,
    val variance: VarianceStats
)
