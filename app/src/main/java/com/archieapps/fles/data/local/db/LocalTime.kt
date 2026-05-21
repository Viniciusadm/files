package com.archieapps.fles.data.local.db

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

fun nowMs(): Long = System.currentTimeMillis()

fun formatIso(ms: Long): String = synchronized(isoFormat) { isoFormat.format(Date(ms)) }

fun parseRemoteDate(value: String?): Long {
    if (value.isNullOrBlank()) return nowMs()
    val normalized = if (value.endsWith("Z")) value else value.replace(" ", "T").let {
        if (it.contains(".")) "${it.substringBefore(".")}.000Z" else "${it.substringBefore("+")}.000Z"
    }
    return runCatching {
        synchronized(isoFormat) { isoFormat.parse(normalized)?.time ?: nowMs() }
    }.getOrElse {
        runCatching { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(normalized)?.time ?: nowMs() }.getOrDefault(nowMs())
    }
}
