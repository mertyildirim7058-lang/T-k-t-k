package com.example.backtap

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator

class TapDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    
    private var lastTapTime: Long = 0
    private val Z_AXIS_THRESHOLD = 15.0f // Telefon modeline göre bu hassasiyeti ayarlayabilirsin
    private val DOUBLE_TAP_TIMEOUT = 500L 
    private val COOLDOWN_TIMEOUT = 1000L // Tetiklendikten sonra beklenen süre
    private var isSensorRegistered = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> unregisterSensor()
                Intent.ACTION_SCREEN_ON -> registerSensor()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        
        registerSensor()

        // Ekran açılıp kapanmasını dinlemek için
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)
    }

    private fun registerSensor() {
        if (!isSensorRegistered && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
            isSensorRegistered = true
        }
    }

    private fun unregisterSensor() {
        if (isSensorRegistered) {
            sensorManager.unregisterListener(this)
            isSensorRegistered = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            val zAxis = event.values[2]

            if (zAxis > Z_AXIS_THRESHOLD || zAxis < -Z_AXIS_THRESHOLD) {
                val currentTime = System.currentTimeMillis()
                
                if (currentTime - lastTapTime < DOUBLE_TAP_TIMEOUT && (currentTime - lastTapTime > 100)) {
                    onDoubleTapDetected()
                    lastTapTime = currentTime + COOLDOWN_TIMEOUT // Spam'i engelle
                } else if (currentTime > lastTapTime) {
                    lastTapTime = currentTime
                }
            }
        }
    }

    private fun onDoubleTapDetected() {
        vibratePhone()
        ActionManager.executeAction(this)
    }

    private fun vibratePhone() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(50)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundService() {
        val channelId = "TapServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Back Tap Servisi", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("Back Tap Aktif")
                .setContentText("Kestirmeler dinleniyor...")
                .setSmallIcon(android.R.drawable.ic_menu_preferences)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("Back Tap Aktif")
                .setContentText("Kestirmeler dinleniyor...")
                .setSmallIcon(android.R.drawable.ic_menu_preferences)
                .build()
        }
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterSensor()
        unregisterReceiver(screenReceiver)
    }
}

