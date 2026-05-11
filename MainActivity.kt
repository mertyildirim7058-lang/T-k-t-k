package com.example.backtap

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvSelectedApp: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvSelectedApp = findViewById(R.id.tvSelectedApp)
        updateSelectedAppText()

        findViewById<Button>(R.id.btnStartService).setOnClickListener {
            val serviceIntent = Intent(this, TapDetectionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Toast.makeText(this, "Servis Başlatıldı", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnSelectApp).setOnClickListener {
            showAppPicker()
        }

        findViewById<Button>(R.id.btnBattery).setOnClickListener {
            requestBatteryOptimization()
        }
    }

    private fun updateSelectedAppText() {
        val targetPackage = ActionManager.getTargetPackage(this)
        tvSelectedApp.text = if (targetPackage != null) {
            try {
                val appName = packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(targetPackage, 0)
                ).toString()
                "Seçili Uygulama: $appName"
            } catch (e: PackageManager.NameNotFoundException) {
                "Seçili Uygulama: Paketi Bulunamadı"
            }
        } else {
            "Seçili Uygulama: Yok"
        }
    }

    private fun showAppPicker() {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        
        val appNames = resolveInfos.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        val appPackages = resolveInfos.map { it.activityInfo.packageName }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Bir Uygulama Seçin")
            .setItems(appNames) { _, which ->
                val selectedPackage = appPackages[which]
                ActionManager.saveTargetPackage(this, selectedPackage)
                updateSelectedAppText()
            }
            .show()
    }

    private fun requestBatteryOptimization() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val packageName = packageName
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } else {
                Toast.makeText(this, "Zaten pil optimizasyonu kapatılmış.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

