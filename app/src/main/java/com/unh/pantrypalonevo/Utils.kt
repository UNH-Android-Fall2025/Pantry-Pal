package com.unh.pantrypalonevo

class Utils {
    fun Double.format(digits: Int) = "%.${digits}f".format(this)
}