package com.jhaiian.clint.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Formats [millis] as a time for today ("3:41 PM"), "Yesterday, 3:41 PM", a date this year
 * ("Jul 28, 3:41 PM"), or a full date otherwise. Used by every list row that shows a visit/added
 * timestamp (history, bookmarks) — previously duplicated verbatim in each screen's own file.
 */
fun formatRelativeTimestamp(millis: Long): String {
    val itemCal = Calendar.getInstance().apply { timeInMillis = millis }
    val nowCal = Calendar.getInstance()

    val isSameDay = itemCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
        itemCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = itemCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
        itemCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)

    val timePart = SimpleDateFormat("h:mm a", Locale.getDefault()).format(itemCal.time)

    return when {
        isSameDay -> timePart
        isYesterday -> "Yesterday, $timePart"
        itemCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) ->
            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(itemCal.time)
        else ->
            SimpleDateFormat("MMM d yyyy, h:mm a", Locale.getDefault()).format(itemCal.time)
    }
}
