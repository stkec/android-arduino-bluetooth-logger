package com.arbl.arduinobluetooth.utils.bluetooth

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelProvider
import com.arbl.arduinobluetooth.ui.main.MainActivity
import com.arbl.arduinobluetooth.ui.main.SharedMainViewModel
import java.io.File
import java.io.FileWriter


class ForegroundService : Service() {
    private var data: PendingIntent? = null
    private var mCount = 0
    private var basePath: File? = null

    private lateinit var sharedMainViewModel: SharedMainViewModel
    private lateinit var bluetoothUtils : BluetoothUtils
    private val bluetoothAdapter : BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private var dataString : String = ""
    private var connectedDevice : String = ""
    private var backPressedTime : Long = 0


    private val handler = Handler(Looper.getMainLooper())
    private var reconnect: java.lang.Runnable = object : java.lang.Runnable {
        override fun run() {
            try {
                if (bluetoothUtils.status == 0) {
                    autoConnectATACMS()
                }
            } finally {
                handler.postDelayed(this, 3000)
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        sharedMainViewModel = ViewModelProvider(this)[SharedMainViewModel::class.java]
        bluetoothUtils = BluetoothUtils(handlerBluetooth)

        handler.postDelayed(reconnect, 3000)



        val input = intent.getStringExtra("inputExtra")
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )
        val notification = NotificationCompat.Builder(this, MainActivity.CHANNEL_ID)
            .setContentTitle("ATACMS Log")
            .setContentText(input)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)
        data = intent.getParcelableExtra("pendingIntent")
        basePath = getExternalFilesDir("recs")
        mCount = 0
        Thread {
            try {
                while (mCount < 1200) {
                    val resultIntent = Intent()
                    resultIntent.putExtra(FOREGROUND_MESSAGE, ++mCount)
//                    writeFile(
//                        """${Date().time / 1000} Increasing counter: $mCount
//"""
//                    )
                    data!!.send(this@ForegroundService, 200, resultIntent)
                    SystemClock.sleep(1000)
                }
            } catch (ignored: Exception) {
            }
        }.start()

        //stopSelf();
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun writeFile(data: String) {
        val file = File(basePath, "test")
        if (!file.exists()) {
            val mkdirs = file.mkdirs()
            if (!mkdirs) {
                Log.e("RECORDING", "Error creating SAVE BASE PATH")
            }
        }
        try {
            val counter_file = File(file, "counter.txt")
            val writer = FileWriter(counter_file, true)
            writer.append(data)
            writer.flush()
            writer.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val CHANNEL_ID = "ForegroundServiceChannel"
        const val FOREGROUND_MESSAGE = "ATACMS: "
    }
}