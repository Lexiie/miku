package com.miku.agent

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.LocationServices
import java.time.Instant
import java.util.Locale
import java.util.TimeZone

class AutomationExecutor(private val context: Context) {
    companion object {
        const val NOTIFICATION_CHANNEL_ID = "miku_default"
        private const val PERMISSION_REQUEST_CODE = 1101
    }

    init {
        ensureNotificationChannel()
    }

    fun execute(action: AndroidAction, onUpdate: ((String) -> Unit)? = null): String {
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
                "GET_LOCATION" -> getLocation(onUpdate)
                else -> "Unknown action: ${action.type}"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Miku Automation",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications triggered by Miku automations"
        }
        manager.createNotificationChannel(channel)
    }

    private fun requestPermission(vararg permissions: String): Boolean {
        val denied = permissions.filter {
            ActivityCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (denied.isEmpty()) {
            return true
        }

        val activity = context as? Activity ?: return false
        ActivityCompat.requestPermissions(activity, denied.toTypedArray(), PERMISSION_REQUEST_CODE)
        return false
    }

    private fun launchIntent(intent: Intent) {
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun intParam(params: Map<String, Any>, key: String, defaultValue: Int): Int {
        val value = params[key]
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: defaultValue
            else -> defaultValue
        }
    }

    private fun longParam(params: Map<String, Any>, key: String): Long? {
        val value = params[key]
        return when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        }
    }

    private fun stringParam(params: Map<String, Any>, key: String): String? {
        return params[key] as? String
    }

    private fun resolveInstalledPackage(appName: String): String? {
        val pm = context.packageManager
        val query = appName.trim().lowercase(Locale.getDefault())

        val directPackage = pm.getLaunchIntentForPackage(appName)?.component?.packageName
        if (directPackage != null) {
            return directPackage
        }

        val installed = pm.getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS)
        return installed.firstOrNull { appInfo ->
            val label = pm.getApplicationLabel(appInfo).toString().lowercase(Locale.getDefault())
            label.contains(query) || appInfo.packageName.lowercase(Locale.getDefault()).contains(query)
        }?.packageName
    }

    private fun setAlarm(params: Map<String, Any>): String {
        val hour = intParam(params, "hour", 7).coerceIn(0, 23)
        val minute = intParam(params, "minute", 0).coerceIn(0, 59)
        val label = stringParam(params, "label") ?: "Alarm"

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        }
        launchIntent(intent)
        return "✅ Alarm set for ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    }

    private fun setTimer(params: Map<String, Any>): String {
        val durationMs = longParam(params, "duration") ?: 300000L
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, (durationMs / 1000L).toInt())
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        }
        launchIntent(intent)
        return "✅ Timer set"
    }

    private fun addCalendar(params: Map<String, Any>): String {
        if (!requestPermission(Manifest.permission.WRITE_CALENDAR)) {
            return "🔐 Calendar permission requested"
        }

        val title = stringParam(params, "title") ?: "Event"
        val startTime = stringParam(params, "start")?.let { Instant.parse(it).toEpochMilli() }
            ?: System.currentTimeMillis() + 86400000L
        val endTime = stringParam(params, "end")?.let { Instant.parse(it).toEpochMilli() }
            ?: startTime + 3600000L

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
        if (!requestPermission(Manifest.permission.READ_CALENDAR)) {
            return "🔐 Calendar permission requested"
        }

        val dayOffset = if ((stringParam(params, "date") ?: "today").equals("tomorrow", ignoreCase = true)) 1 else 0
        val start = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, dayOffset)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = start + 24L * 60L * 60L * 1000L

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, start)
        ContentUris.appendId(builder, end)

        val cursor = context.contentResolver.query(
            builder.build(),
            arrayOf(CalendarContract.Instances.TITLE),
            null,
            null,
            CalendarContract.Instances.BEGIN + " ASC"
        )

        val events = mutableListOf<String>()
        cursor?.use {
            while (it.moveToNext()) {
                events.add(it.getString(0))
            }
        }

        return if (events.isEmpty()) {
            "No events found"
        } else {
            "Events: ${events.joinToString(", ")}"
        }
    }

    private fun sendSms(params: Map<String, Any>): String {
        if (!requestPermission(Manifest.permission.SEND_SMS)) {
            return "🔐 SMS permission requested"
        }

        val phone = stringParam(params, "phone") ?: return "❌ Phone number missing"
        val message = stringParam(params, "message") ?: return "❌ Message missing"
        val smsManager = context.getSystemService(SmsManager::class.java)
        smsManager.sendTextMessage(phone, null, message, null, null)
        return "✅ SMS sent"
    }

    private fun makeCall(params: Map<String, Any>): String {
        if (!requestPermission(Manifest.permission.CALL_PHONE)) {
            return "🔐 Call permission requested"
        }

        val phone = stringParam(params, "phone") ?: stringParam(params, "target")
            ?: return "❌ Contact missing"

        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone"))
        launchIntent(intent)
        return "✅ Calling"
    }

    private fun openApp(params: Map<String, Any>): String {
        val appName = stringParam(params, "appName") ?: return "❌ App name missing"
        val packageName = resolveInstalledPackage(appName) ?: return "❌ App not found"
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return "❌ App not found"
        launchIntent(intent)
        return "✅ App opened"
    }

    private fun uninstallApp(params: Map<String, Any>): String {
        val appName = stringParam(params, "appName") ?: return "❌ App name missing"
        val packageName = resolveInstalledPackage(appName) ?: return "❌ App not found"
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
        launchIntent(intent)
        return "✅ Uninstall initiated"
    }

    private fun toggleWifi(params: Map<String, Any>): String {
        val enable = params["enable"] as? Boolean ?: true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            launchIntent(Intent(Settings.Panel.ACTION_WIFI))
            return "✅ WiFi settings opened"
        }

        @Suppress("DEPRECATION")
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        wifiManager.isWifiEnabled = enable
        return "✅ WiFi ${if (enable) "enabled" else "disabled"}"
    }

    private fun toggleBluetooth(params: Map<String, Any>): String {
        if (!requestPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            return "🔐 Bluetooth permission requested"
        }

        val enable = params["enable"] as? Boolean ?: true
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter ?: return "❌ Bluetooth unavailable"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launchIntent(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            return "✅ Bluetooth settings opened"
        }

        @Suppress("DEPRECATION")
        if (enable) bluetoothAdapter.enable() else bluetoothAdapter.disable()
        return "✅ Bluetooth ${if (enable) "enabled" else "disabled"}"
    }

    private fun setBrightness(params: Map<String, Any>): String {
        if (!Settings.System.canWrite(context)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
            launchIntent(intent)
            return "🔐 Write settings permission requested"
        }

        val level = intParam(params, "level", 50).coerceIn(0, 100)
        val brightness = (level * 255 / 100).coerceIn(0, 255)
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
        return "✅ Brightness set to $level%"
    }

    private fun toggleFlashlight(params: Map<String, Any>): String {
        if (!requestPermission(Manifest.permission.CAMERA)) {
            return "🔐 Camera permission requested"
        }

        val enable = params["enable"] as? Boolean ?: true
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return "❌ Flashlight unavailable"
        cameraManager.setTorchMode(cameraId, enable)
        return "✅ Flashlight ${if (enable) "on" else "off"}"
    }

    private fun setRingerMode(params: Map<String, Any>): String {
        val mode = stringParam(params, "mode") ?: "normal"
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.ringerMode = when (mode.lowercase(Locale.getDefault())) {
            "silent" -> AudioManager.RINGER_MODE_SILENT
            "vibrate" -> AudioManager.RINGER_MODE_VIBRATE
            else -> AudioManager.RINGER_MODE_NORMAL
        }
        return "✅ Ringer mode: $mode"
    }

    private fun setVolume(params: Map<String, Any>): String {
        val level = intParam(params, "level", 50).coerceIn(0, 100)
        val streamType = when ((stringParam(params, "stream") ?: "music").lowercase(Locale.getDefault())) {
            "alarm" -> AudioManager.STREAM_ALARM
            "ring" -> AudioManager.STREAM_RING
            else -> AudioManager.STREAM_MUSIC
        }

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(streamType)
        val volume = (level * maxVolume / 100).coerceIn(0, maxVolume)
        audioManager.setStreamVolume(streamType, volume, 0)
        return "✅ Volume set to $level%"
    }

    private fun sendNotification(params: Map<String, Any>): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !requestPermission(Manifest.permission.POST_NOTIFICATIONS)) {
            return "🔐 Notification permission requested"
        }

        val title = stringParam(params, "title") ?: "Notification"
        val body = stringParam(params, "body") ?: ""

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
        return "✅ Notification sent"
    }

    private fun setReminder(params: Map<String, Any>): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                launchIntent(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                return "🔐 Exact alarm permission requested"
            }
        }

        val message = stringParam(params, "message") ?: "Reminder"
        val triggerAt = stringParam(params, "triggerAt")?.let { Instant.parse(it).toEpochMilli() }
            ?: (System.currentTimeMillis() + (longParam(params, "delayMs") ?: 30L * 60L * 1000L))

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TITLE, "Reminder")
            putExtra(ReminderReceiver.EXTRA_BODY, message)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            triggerAt.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        return "✅ Reminder scheduled"
    }

    private fun getLocation(onUpdate: ((String) -> Unit)?): String {
        if (!requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            return "🔐 Location permission requested"
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                val message = if (location != null) {
                    "Lat ${location.latitude}, Lng ${location.longitude}"
                } else {
                    "Location unavailable"
                }
                onUpdate?.invoke(message)
            }
            .addOnFailureListener { error ->
                onUpdate?.invoke("Location error: ${error.message}")
            }

        return "✅ Getting location..."
    }
}
