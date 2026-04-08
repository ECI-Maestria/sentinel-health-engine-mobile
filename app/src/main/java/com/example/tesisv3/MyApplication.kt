package com.example.tesisv3

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.color.DynamicColors
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import org.conscrypt.Conscrypt
import java.security.Security
import java.util.Calendar
import java.util.concurrent.TimeUnit

class  MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Security.insertProviderAt(Conscrypt.newProvider(), 1)
        try {
            ProviderInstaller.installIfNeeded(this)
        } catch (_: GooglePlayServicesRepairableException) {
        } catch (_: GooglePlayServicesNotAvailableException) {
        }
        DynamicColors.applyToActivitiesIfAvailable(this)
        AppContextHolder.init(this)
        DeviceRegistrationManager.registerIfNeeded(this)
        scheduleDailyReset()
    }

    private fun scheduleDailyReset() {
        val now = Calendar.getInstance()
        val nextMidnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val initialDelay = nextMidnight.timeInMillis - now.timeInMillis
        val request = PeriodicWorkRequestBuilder<DailyResetWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_medication_reset",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
