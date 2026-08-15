package com.jhaiian.clint.downloads

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.fragment.app.FragmentManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import java.util.Calendar
import java.util.TimeZone

/**
 * Shows the same MaterialDatePicker/MaterialTimePicker Fragment dialogs used by the "Schedule
 * This Download" row in [DownloadRequestDialog] and [DownloadManualDialog], reporting the picked
 * value back via callback. [MaterialDatePicker] works in UTC regardless of device timezone, so
 * the picked day is read back through a UTC calendar rather than used directly.
 */
internal fun showScheduleDatePicker(fragmentManager: FragmentManager, currentMillis: Long, onPicked: (year: Int, month: Int, dayOfMonth: Int) -> Unit) {
    val calendar = Calendar.getInstance().apply { timeInMillis = currentMillis }
    val utcSelection = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
    val picker = MaterialDatePicker.Builder.datePicker()
        .setTitleText(R.string.download_schedule_date_picker_title)
        .setSelection(utcSelection)
        .build()
    picker.addOnPositiveButtonClickListener { selectedUtcMillis ->
        val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = selectedUtcMillis }
        onPicked(picked.get(Calendar.YEAR), picked.get(Calendar.MONTH), picked.get(Calendar.DAY_OF_MONTH))
    }
    picker.show(fragmentManager, "download_schedule_date_picker")
}

internal fun showScheduleTimePicker(context: Context, fragmentManager: FragmentManager, currentMillis: Long, onPicked: (hour: Int, minute: Int) -> Unit) {
    val calendar = Calendar.getInstance().apply { timeInMillis = currentMillis }
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    val picker = MaterialTimePicker.Builder()
        .setTimeFormat(if (is24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
        .setHour(calendar.get(Calendar.HOUR_OF_DAY))
        .setMinute(calendar.get(Calendar.MINUTE))
        .setTitleText(R.string.download_schedule_time_picker_title)
        .build()
    picker.addOnPositiveButtonClickListener { onPicked(picker.hour, picker.minute) }
    picker.show(fragmentManager, "download_schedule_time_picker")
}

/**
 * True when, on API 31+, the user hasn't yet granted the "Alarms & reminders" permission needed
 * for alarms to fire at an exact time rather than an OS-deferred approximation. Without it,
 * "Schedule This Download" would silently start late by anywhere from a few minutes to much
 * longer, so callers should show [exactAlarmPermissionDialogConfig]'s rationale up front, the
 * same way the battery-optimization rationale in [com.jhaiian.clint.browser.delegates] does.
 */
internal fun needsExactAlarmPermissionRationale(context: Context): Boolean =
    !DownloadCustomScheduleMonitor.canScheduleExact(context)

/** Config for the caller's own `ConfirmDialogConfig`/`ConfirmDialogHost` state (both
 *  [DownloadRequestDialog] and [DownloadManualDialog] already have one for their other blocking
 *  dialogs) — shown when [needsExactAlarmPermissionRationale] is true. */
internal fun exactAlarmPermissionDialogConfig(context: Context): ConfirmDialogConfig = ConfirmDialogConfig(
    title = context.getString(R.string.download_schedule_exact_alarm_title),
    message = context.getString(R.string.download_schedule_exact_alarm_message, context.getString(R.string.app_name)),
    positiveLabel = context.getString(R.string.action_allow),
    onPositive = {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    },
    negativeLabel = context.getString(R.string.action_not_now)
)
