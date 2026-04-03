package com.miku.agent

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.CalendarContract
import android.provider.Settings
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.LocationServices
import java.util.*

class AutomationExecutor(private val context: Context) {

    fun execute(action: AndroidAction): String {
        return try {
            when (action.type) {
                "SET_ALARM" -> setAlarm(action.params)
                "SET_TIMER" -> setTimer(action.params)
                "ADD_CALENDAR" -> addCalendar(action.params)
                "GET_EVENTS" -> getEvents(action.params)
                "SEND_SMS" -> sendSms(action.params)
                "MAKE_CALL" -> makeCall(action.params)
                "OPEN_APP" -> openApp(action.params)
                "UNINSTALL_APP" -> uninstallApp(action.params)
                "TOGGLE_WIFI" -> toggleWifi(action.params)
                "TOGGLE_BLUETOOTH" -> toggleBluetooth(action.params)
                "SET_BRIGHTNESS" -> setBrightness(action.params)
                "TOGGLE_FLASHLIGHT" -> toggleFlashlight(action.params)
                "SET_RINGER_MODE" -> setRingerMode(action.params)
                "SET_VOLUME" -> setVolume(action.params)
                "SEND_NOTIFICATION" -> sendNotification(action.params)
                "SET_REMINDER" -> setReminder(action.params)
                "GET_LOCATION" -> getLocation(action.params)
                else -> "Unknown action: ${action.type}"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun setAlarm(params: Map<String, Any>): String {
        val hour = (params["hour"] as? Double)?.toInt() ?: 7
        val minute = (params["minute"] as? Double)?.toInt() ?: 0
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour)
            putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute)
            putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true)
        }
        context.startActivity(intent)
        return "✅ Alarm set"
    }

    private fun setTimer(params: Map<String, Any>): String {
        val duration = (params["duration"] as? Double)?.toInt() ?: 300000
        
        val intent = Intent(android.provider.AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(android.provider.AlarmClock.EXTRA_LENGTH, duration / 1000)
            putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true)
        }
        context.startActivity(intent)
        return "✅ Timer set"
    }

    private fun addCalendar(params: Map<String, Any>): String {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return "❌ Calendar permission required"
        }
        
        val title = params["title"] as? String ?: "Event"
        val startTime = System.currentTimeMillis() + 86400000
        val endTime = startTime + 3600000
        
        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startTime)
            put(CalendarContract.Events.DTEND, endTime)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.CALENDAR_ID, 1)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
        
        context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        return "✅ Event added"
    }

    private fun getEvents(params: Map<String, Any>): String {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return "❌ Calendar permission required"
        }
        
        val cursor = context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(CalendarContract.Events.TITLE),
            null, null, null
        )
        
        val events = mutableListOf<String>()
        cursor?.use {
            while (it.moveToNext()) {
                events.add(it.getString(0))
            }
        }
        
        return if (events.isEmpty()) "No events found" else "Events: ${events.joinToString(", ")}"
    }

    private fun sendSms(params: Map<String, Any>): String {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return "❌ SMS permission required"
        }
        
        val phone = params["phone"] as? String ?: return "❌ Phone number missing"
        val message = params["message"] as? String ?: return "❌ Message missing"
        
        val smsManager = context.getSystemService(SmsManager::class.java)
        smsManager.sendTextMessage(phone, null, message, null, null)
        return "✅ SMS sent"
    }

    private fun makeCall(params: Map<String, Any>): String {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return "❌ Call permission required"
        }
        
        val phone = params["phone"] as? String ?: return "❌ Phone number missing"
        
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone"))
        context.startActivity(intent)
        return "✅ Calling"
    }

    private fun openApp(params: Map<String, Any>): String {
        val appName = params["appName"] as? String ?: return "❌ App name missing"
        
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(appName) ?: return "❌ App not found"
        
        context.startActivity(intent)
        return "✅ App opened"
    }

    private fun uninstallApp(params: Map<String, Any>): String {
        val packageName = params["packageName"] as? String ?: return "❌ Package name missing"
        
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
        context.startActivity(intent)
        return "✅ Uninstall initiated"
    }

    private fun toggleWifi(params: Map<String, Any>): String {
        val enable = params["enable"] as? Boolean ?: true
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val intent = Intent(Settings.Panel.ACTION_WIFI)
            context.startActivity(intent)
            return "✅ WiFi settings opened"
        }
        
        @Suppress("DEPRECATION")
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.isWifiEnabled = enable
        return "✅ WiFi ${if (enable) "enabled" else "disabled"}"
    }

    private fun toggleBluetooth(params: Map<String, Any>): String {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return "❌ Bluetooth permission required"
        }
        
        val enable = params["enable"] as? Boolean ?: true
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        
        if (enable) bluetoothAdapter?.enable() else bluetoothAdapter?.disable()
        return "✅ Bluetooth ${if (enable) "enabled" else "disabled"}"
    }

    private fun setBrightness(params: Map<String, Any>): String {
        if (!Settings.System.canWrite(context)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
            context.startActivity(intent)
            return "❌ Write settings permission required"
        }
        
        val level = (params["level"] as? Double)?.toInt() ?: 50
        val brightness = (level * 255 / 100).coerceIn(0, 255)
        
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
        return "✅ Brightness set to $level%"
    }

    private fun toggleFlashlight(params: Map<String, Any>): String {
        val enable = params["enable"] as? Boolean ?: true
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0]
        
        cameraManager.setTorchMode(cameraId, enable)
        return "✅ Flashlight ${if (enable) "on" else "off"}"
    }

    private fun setRingerMode(params: Map<String, Any>): String {
        val mode = params["mode"] as? String ?: "normal"
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        audioManager.ringerMode = when (mode) {
            "silent" -> AudioManager.RINGER_MODE_SILENT
            "vibrate" -> AudioManager.RINGER_MODE_VIBRATE
            else -> AudioManager.RINGER_MODE_NORMAL
        }
        return "✅ Ringer mode: $mode"
    }

    private fun setVolume(params: Map<String, Any>): String {
        val level = (params["level"] as? Double)?.toInt() ?: 50
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volume = (level * maxVolume / 100).coerceIn(0, maxVolume)
        
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
        return "✅ Volume set to $level%"
    }

    private fun sendNotification(params: Map<String, Any>): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return "❌ Notification permission required"
            }
        }
        
        val title = params["title"] as? String ?: "Notification"
        val body = params["body"] as? String ?: ""
        
        val notification = NotificationCompat.Builder(context, "default")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
        return "✅ Notification sent"
    }

    private fun setReminder(params: Map<String, Any>): String {
        return sendNotification(mapOf(
            "title" to "Reminder",
            "body" to (params["message"] as? String ?: "Reminder")
        ))
    }

    private fun getLocation(params: Map<String, Any>): String {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return "❌ Location permission required"
        }
        
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                // Location will be shown in callback
            }
        }
        return "✅ Getting location..."
    }
}
