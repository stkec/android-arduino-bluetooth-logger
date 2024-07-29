package com.arbl.arduinobluetooth.utils.bluetooth

import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelProvider
import com.arbl.arduinobluetooth.R
import com.arbl.arduinobluetooth.ui.main.MainActivity
import com.arbl.arduinobluetooth.ui.main.SharedMainViewModel
import com.arbl.arduinobluetooth.utils.FileLogger
import com.arbl.arduinobluetooth.utils.constant.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Thread.sleep
import java.util.*


class ForegroundService : Service() {
    private val TAG: String = "FService"
    private var fileLogger: FileLogger = FileLogger()
    private var data: PendingIntent? = null

    //    private lateinit var sharedMainViewModel: SharedMainViewModel
    private lateinit var bluetoothUtils: BluetoothUtils
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private var dataString: String = ""
    private var connectedDevice: String = ""

    private var wakeLock: PowerManager.WakeLock? = null

    private val handler = Handler(Looper.getMainLooper())
    private var reconnect: java.lang.Runnable = object : java.lang.Runnable {
        override fun run() {
            try {
                if (bluetoothUtils.status == 0) {
                    autoConnectATACMS()
                }
            } finally {
//                handler.postDelayed(this, 3000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int):Int {
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LocationService::lock").apply {
                    acquire(10*1000L)  // 10 seconds
                }
            }

        createNotificationChannel()

//        val notificationIntent = Intent(this, MainActivity::class.java)
//        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
//        val notification = NotificationCompat.Builder(this, MainActivity.CHANNEL_ID)
//            .setContentTitle("ATACMS Log")
//            .setContentIntent(pendingIntent)
//            .build()
        startForeground(2, getNotification())

//        startForeground(1, notification);


        Log.d("TAG", "onStartCommand")
        sleep(1000)
        Log.d("TAG", "onStartCommand2")
//        sharedMainViewModel = ViewModelProvider(this)[SharedMainViewModel::class.java]
        bluetoothUtils = BluetoothUtils(handlerBluetooth)

        handler.postDelayed(reconnect, 3000)


//        val input = intent.getStringExtra("inputExtra")
//        createNotificationChannel()
//        val notificationIntent = Intent(this, MainActivity::class.java)
//        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
//        val notification = NotificationCompat.Builder(this, MainActivity.CHANNEL_ID)
//            .setContentTitle("ATACMS Log")
//            .setContentText(input)
//            .setContentIntent(pendingIntent)
//            .build()
//        startForeground(1, notification)
//
//        data = intent.getParcelableExtra("pendingIntent")
//        basePath = getExternalFilesDir("recs")
//        mCount = 0
//        Thread {
//            try {
//                while (mCount < 1200) {
//                    val resultIntent = Intent()
//                    resultIntent.putExtra(FOREGROUND_MESSAGE, ++mCount)
////                    writeFile(
////                        """${Date().time / 1000} Increasing counter: $mCount
////"""
////                    )
//                    data!!.send(this@ForegroundService, 200, resultIntent)
//                    SystemClock.sleep(1000)
//                }
//            } catch (ignored: Exception) {
//            }
//        }.start()

        //stopSelf();
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "############ Build Notification");
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun getNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Service")
            .setContentText("Getting location updates")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return builder.build()
    }

    @SuppressLint("MissingPermission")
    private fun autoConnectATACMS() {
        if (/*!isBluetoothPermissionNotGranted()
            && */bluetoothAdapter.isEnabled
            && !bluetoothUtils.isConnect
        ) {
            val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
            if (pairedDevices.isNotEmpty()) {
                for (device in pairedDevices) {
                    if (device.name.trim() == "HC-05") {
                        bluetoothUtils.connect(bluetoothAdapter.getRemoteDevice(device.address))
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "#######     DESTROY")
        super.onDestroy()
        bluetoothUtils.stop()
        wakeLock?.release()
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "#######     Bind")
        return null
    }

    private val handlerBluetooth = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            Log.d(TAG, "#######  Handler MSG What:  " + msg.what)
            Log.d(TAG, "#######  Handler MSG ARG:  " + msg.arg1)
            when (msg.what) {
                Constants.messageStateChanged -> when (msg.arg1) {
                    Constants.stateNone -> {
                        var a = 7
                        Log.d(TAG, "#######  Handler:  None")
//                        sharedMainViewModel.setState(false)
//                        binding.appBarLayout.tvSubtitle.text = getString(R.string.stringNC)
                    }

                    Constants.stateListen -> {
                        Log.d(TAG, "#######  Handler:  Listen")
                        var a = 7
//                        sharedMainViewModel.setState(false)
//                        binding.appBarLayout.tvSubtitle.text = getString(R.string.stringNC)
                    }

                    Constants.stateConnecting -> {
                        Log.d(TAG, "#######  Handler:  Connecting")
                        var a = 7
//                        sharedMainViewModel.setState(false)
//                        binding.appBarLayout.tvSubtitle.text = getString(R.string.stringCTI)
                    }

                    Constants.stateConnected -> {
                        Log.d(TAG, "#######  Handler:  Connected")
                        var a = 7
//                        sharedMainViewModel.setState(true)
//                        val newText = this@MainActivity.resources.getString(R.string.stringCTD, connectedDevice)
//                        binding.appBarLayout.tvSubtitle.text = newText
                    }
                }

                Constants.messageRead -> {
                    val buffer = msg.obj as ByteArray
                    val inputBuffer = String(buffer, 0, msg.arg1)
                    dataString += inputBuffer
                    CoroutineScope(Dispatchers.Default).launch {
                        delay(293)
                        if (dataString.isNotEmpty()) {
                            Log.d(TAG, "#######     DATA:    " + dataString)
                            fileLogger.logData(dataString)

//                            val resultIntent = Intent()
//                            resultIntent.putExtra(FOREGROUND_MESSAGE, dataString)
//                            data!!.send(this@ForegroundService, 200, resultIntent)

                            dataString = ""
                        }
                    }
                }

                Constants.messageDeviceName -> {
                    connectedDevice = msg.data.getString(Constants.messageString)!!
                }

                Constants.messageToast -> {
                    val message = msg.data.getString(Constants.messageString);
                    Log.d(TAG, "#######  Handler:  Toast: " + message)

                    if (message == "Disconnect" || message == "Unable to connect with device") {
                        Log.d(TAG, "#######  Handler:  Reconnecting")
                        handler.postDelayed(reconnect, 3000)
                    }
//                    val msgToast = msg.data.getString(Constants.messageString)
//                    Toast.makeText(this@MainActivity, msgToast, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "ForegroundServiceChannel"
        const val FOREGROUND_MESSAGE = "ATACMSMessage"
    }
}