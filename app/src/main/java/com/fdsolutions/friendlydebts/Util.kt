package com.fdsolutions.friendlydebts

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.round

/**
 * General utilities.
 */

@SuppressLint("ConstantLocale")
val sdfDB = SimpleDateFormat("yy/MM/dd", Locale.getDefault())

@SuppressLint("ConstantLocale")
val sdfApp = SimpleDateFormat("dd.MM.yy", Locale.getDefault())


/**
 * @param date String: date in App format
 * @return String: date in DB format
 */
fun dateAppToDB(date: String): String {
    return sdfDB.format(sdfApp.parse(date)!!)
}

/**
 * @param date String: date in DB format
 * @return String: date in App format
 */
fun dateDBToApp(date: String): String {
    return sdfApp.format(sdfDB.parse(date)!!)
}

/**
 * Cut off all decimals after the second of a Double.
 */
fun round2Decimals(double: Double): Double {
    return round(double * 100.0) / 100.0
}
