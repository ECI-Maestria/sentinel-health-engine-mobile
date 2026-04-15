package com.example.tesisv3.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tesisv3.data.AppDatabase
import com.example.tesisv3.data.NotificationEntity
import kotlin.math.abs
import java.util.UUID
import com.example.tesisv3.*
import com.example.tesisv3.ui.*

class MedicationReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val name = inputData.getString("name") ?: "Medication"
        val amount = inputData.getString("amount") ?: ""
        val schedule = inputData.getString("schedule") ?: ""
        val type = inputData.getString("type") ?: "Pill"
        val medId = inputData.getString("med_id") ?: ""

        val channelId = "medication_reminders"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Medication Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val actionText = when (type) {
            "Injection" -> "Aplicar inyección"
            "Pill" -> "Tomar pastilla"
            else -> "Tomar medicamento"
        }
        val intent = Intent(applicationContext, CareActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            abs(medId.hashCode()),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$actionText: $name")
            .setContentText("$amount • $schedule")
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = if (medId.isBlank()) {
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        } else {
            abs(medId.hashCode())
        }
        manager.notify(notificationId, notification)

        val dao = AppDatabase.getInstance(applicationContext).notificationDao()
        dao.insert(
            NotificationEntity(
                id = UUID.randomUUID().toString(),
                title = "$actionText: $name",
                body = "$amount • $schedule",
                createdAt = System.currentTimeMillis()
            )
        )
        return Result.success()
    }
}
