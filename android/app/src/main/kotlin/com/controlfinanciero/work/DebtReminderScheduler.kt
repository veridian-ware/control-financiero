package com.controlfinanciero.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Programación del recordatorio diario de vencimientos (cuotas/deudas) + canal de notificación. */
object DebtReminderScheduler {

    const val CHANNEL_ID = "debt_reminders"
    private const val WORK_NAME = "debt_reminders_daily"

    /**
     * Encola el chequeo diario. Idempotente (KEEP): si ya está programado no lo pisa.
     * La primera ejecución corre apenas haya red; las siguientes, cada ~24 h.
     */
    fun schedule(context: Context) {
        ensureChannel(context)
        val request = PeriodicWorkRequestBuilder<DebtReminderWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
        )
    }

    /** Crea el canal de notificación si no existe (minSdk 26, siempre disponible). */
    fun ensureChannel(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recordatorios de vencimientos",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Avisos de cuotas y deudas vencidas o por vencer" }
            mgr.createNotificationChannel(channel)
        }
    }
}
