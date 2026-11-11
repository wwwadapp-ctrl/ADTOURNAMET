package com.example.core.i18n

import java.util.Locale

// Numbers and money that follow the selected app language.
// Bengali -> Bengali digits (৳১৪৫.০০), English -> English digits (৳145.00). Display only.
// Usage in a composable: val strings = LocalAppStrings.current; Text(strings.taka(balance))

fun AppStrings.digits(input: String): String =
    if (this === BengaliStrings) {
        input.map { if (it in '0'..'9') '০' + (it - '0') else it }.joinToString("")
    } else {
        input
    }

/** 145.5 -> "৳১৪৫.৫০" (Bengali) or "৳145.50" (English) */
fun AppStrings.taka(amount: Double): String =
    "৳" + digits(String.format(Locale.US, "%.2f", amount))

/** 14550 paisa -> "৳১৪৫.৫০" / "৳145.50" */
fun AppStrings.takaFromMinor(minorUnits: Long): String = taka(minorUnits / 100.0)

/** 145.0 -> "৳১৪৫" / "৳145" (no decimals, for big headline numbers) */
fun AppStrings.takaWhole(amount: Double): String =
    "৳" + digits(String.format(Locale.US, "%.0f", amount))

/** 12 -> "১২" / "12" */
fun AppStrings.number(value: Long): String = digits(value.toString())

/** 12 -> "১২" / "12" */
fun AppStrings.number(value: Int): String = digits(value.toString())
