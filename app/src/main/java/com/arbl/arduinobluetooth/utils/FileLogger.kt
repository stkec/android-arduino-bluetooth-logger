package com.arbl.arduinobluetooth.utils

import android.os.Environment
import java.io.File
import java.util.*

class FileLogger
{
    public fun logData(data: String) {
        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString()
        val dir = File("$root/ATACMSLog")

        if (!dir.exists()) {
            dir.mkdir()
        }

        val file = File(dir, getLogFilename())
        if (!file.exists()) {
            file.createNewFile()
        }

        file.appendText(formatLogLine(data));
    }

    private fun formatLogLine(data: String): String {
        val calendar = Calendar.getInstance()

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val min = calendar.get(Calendar.MINUTE)
        val sec = calendar.get(Calendar.SECOND)

        return "$hour:$min.$sec: $data";
    }

    private fun getLogFilename(): String {
        val calendar = Calendar.getInstance()

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        return "flightLog_" + "$year" + "_$month" + "_$day";
    }
}