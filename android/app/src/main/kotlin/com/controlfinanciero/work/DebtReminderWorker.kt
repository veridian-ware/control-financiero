package com.controlfinanciero.work

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.controlfinanciero.MainActivity
import com.controlfinanciero.data.api.RetrofitClient
import com.controlfinanciero.data.auth.SessionManager
import com.controlfinanciero.data.models.Debt
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.util.Locale

/**
 * Chequea los vencimientos de cuotas/deudas (`/api/debts`) y, si hay vencidas o próximas,
 * dispara una notificación local. Se programa con [DebtReminderScheduler].
 */
class DebtReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Cargar el token persistido sincroniza AuthTokenProvider (lo lee el interceptor).
            SessionManager(applicationContext).tokenFlow.first()
                ?: return Result.success() // sin sesión: nada que recordar
            val resp = RetrofitClient.api.getDebts()
            val summary = resp.data ?: return Result.retry()
            val overdue = summary.debts.filter { it.dueStatus == "vencido" }
            val dueSoon = summary.debts.filter { it.dueStatus == "proximo" }
            if (overdue.isNotEmpty() || dueSoon.isNotEmpty()) {
                notify(applicationContext, overdue, dueSoon)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    @SuppressLint("MissingPermission") // se chequea areNotificationsEnabled() antes de notificar
    private fun notify(context: Context, overdue: List<Debt>, dueSoon: List<Debt>) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val fmt = NumberFormat.getCurrencyInstance(Locale("es", "AR"))
        val title = when {
            overdue.isNotEmpty() && dueSoon.isNotEmpty() ->
                "${overdue.size} vencida${plural(overdue.size)} · ${dueSoon.size} por vencer"
            overdue.isNotEmpty() ->
                "${overdue.size} cuota${plural(overdue.size)} vencida${plural(overdue.size)}"
            else ->
                "${dueSoon.size} vencimiento${plural(dueSoon.size)} próximo${plural(dueSoon.size)}"
        }
        val lines = (
            overdue.map { "⚠ ${itemText(it, fmt)} · venció ${prettyDate(it.dueDate)}" } +
                dueSoon.map { "• ${itemText(it, fmt)} · vence ${prettyDate(it.dueDate)}" }
            ).take(6)
        val bigText = lines.joinToString("\n")

        DebtReminderScheduler.ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val contentIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, DebtReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(lines.firstOrNull() ?: "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
        } catch (e: SecurityException) {
            // Sin permiso POST_NOTIFICATIONS (Android 13+): se ignora silenciosamente.
        }
    }

    /** "Philco (cuota 4/6) $155.703" — para deuda en cuotas muestra la próxima a pagar. */
    private fun itemText(d: Debt, fmt: NumberFormat): String {
        val cuota = d.totalInstallments?.let { " (cuota ${d.paidInstallments + 1}/$it)" } ?: ""
        return "${d.description}$cuota ${fmt.format(d.installmentAmount)}"
    }

    private fun plural(n: Int): String = if (n == 1) "" else "s"

    /** "2026-08-10" -> "10/08". */
    private fun prettyDate(iso: String?): String =
        if (iso != null && iso.length >= 10) "${iso.substring(8, 10)}/${iso.substring(5, 7)}" else "—"

    companion object {
        private const val NOTIF_ID = 4201
    }
}
