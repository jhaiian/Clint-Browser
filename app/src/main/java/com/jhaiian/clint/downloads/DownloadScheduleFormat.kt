package com.jhaiian.clint.downloads

import android.content.Context
import java.util.Date

internal fun formatScheduledDateTime(context: Context, millis: Long): String {
    val date = Date(millis)
    val datePart = android.text.format.DateFormat.getMediumDateFormat(context).format(date)
    val timePart = android.text.format.DateFormat.getTimeFormat(context).format(date)
    return "$datePart, $timePart"
}
