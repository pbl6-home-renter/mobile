package com.rentify.app.core.ui.extension

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun Long.toVnd(): String {
    val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = '.'
    }
    val formatter = DecimalFormat("#,##0", symbols)
    return "${formatter.format(this)} đ"
}

fun Double.toVnd(): String {
    val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = '.'
    }
    val formatter = DecimalFormat("#,##0", symbols)
    return "${formatter.format(Math.round(this))} đ"
}

fun String.toDisplayDate(): String {
    if (isBlank()) return this
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return try {
        when (val temporal = parseIsoTemporal(this)) {
            is ZonedDateTime -> temporal.format(dateFormatter)
            is OffsetDateTime -> temporal.format(dateFormatter)
            is LocalDateTime -> temporal.format(dateFormatter)
            is LocalDate -> temporal.format(dateFormatter)
            else -> this
        }
    } catch (_: Exception) {
        this
    }
}

fun String.toDisplayDateTime(): String {
    if (isBlank()) return this
    val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    return try {
        when (val temporal = parseIsoTemporal(this)) {
            is ZonedDateTime -> temporal.format(dateTimeFormatter)
            is OffsetDateTime -> temporal.format(dateTimeFormatter)
            is LocalDateTime -> temporal.format(dateTimeFormatter)
            is LocalDate -> temporal.atStartOfDay().format(dateTimeFormatter)
            else -> this
        }
    } catch (_: Exception) {
        this
    }
}

fun String.toDisplayMonth(): String {
    if (isBlank()) return this
    val monthFormatter = DateTimeFormatter.ofPattern("MM/yyyy")
    return try {
        when (val temporal = parseIsoTemporal(this)) {
            is ZonedDateTime -> temporal.format(monthFormatter)
            is OffsetDateTime -> temporal.format(monthFormatter)
            is LocalDateTime -> temporal.format(monthFormatter)
            is LocalDate -> temporal.format(monthFormatter)
            is YearMonth -> temporal.format(monthFormatter)
            else -> this
        }
    } catch (_: Exception) {
        this
    }
}

private fun parseIsoTemporal(input: String): Any {
    val trimmed = input.trim()
    return try {
        ZonedDateTime.parse(trimmed)
    } catch (_: Exception) {
        try {
            OffsetDateTime.parse(trimmed)
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(trimmed)
            } catch (_: Exception) {
                try {
                    LocalDate.parse(trimmed)
                } catch (_: Exception) {
                    YearMonth.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM"))
                }
            }
        }
    }
}
