package com.fanofeno.gpstracker

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Bundle
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class LocationService : Service(), LocationListener {
    private lateinit var manager: LocationManager
    private lateinit var queue: QueueDb
    private val intervalMs = 5 * 60 * 1000L
    private val channelId = "gps_tracking_channel"

    override fun onCreate() {
        super.onCreate()
        queue = QueueDb(this)
        manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createChannel()
        startForeground(71, notification("Miasa ny GPS — fanavaozana isaky ny 5 minitra"))
        try {
            val provider = if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
            manager.requestLocationUpdates(provider, intervalMs, 0f, this)
            manager.getLastKnownLocation(provider)?.let { onLocationChanged(it) }
        } catch (_: SecurityException) {
            stopSelf()
        } catch (_: Exception) {
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onLocationChanged(location: Location) {
        val prefs = getSharedPreferences("gps_session", MODE_PRIVATE)
        val userId = prefs.getString("user_id", null) ?: return
        val deviceId = prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("device_id", it).apply()
        }
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(Date(location.time))
        val point = JSONObject()
            .put("user_id", userId)
            .put("device_id", deviceId)
            .put("latitude", location.latitude)
            .put("longitude", location.longitude)
            .put("accuracy_m", if (location.hasAccuracy()) location.accuracy else JSONObject.NULL)
            .put("altitude_m", if (location.hasAltitude()) location.altitude else JSONObject.NULL)
            .put("speed_mps", if (location.hasSpeed()) location.speed else JSONObject.NULL)
            .put("bearing_deg", if (location.hasBearing()) location.bearing else JSONObject.NULL)
            .put("captured_at", iso)
        queue.add(point)
        flushQueue()
        updateNotification()
    }

    private fun flushQueue() {
        for ((id, point) in queue.pending()) {
            if (Api.upload(this, point)) queue.delete(id) else break
        }
    }

    private fun updateNotification() {
        val n = notification("GPS mandeha • ${queue.count()} toerana mbola miandry fandefasana")
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(71, n)
    }

    private fun notification(message: String): Notification {
        val stop = Intent(this, StopReceiver::class.java)
        val pending = PendingIntent.getBroadcast(this, 72, stop,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("GPS Tracker")
            .setContentText(message)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Ajanony", pending)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(channelId, "GPS tracking", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        try { manager.removeUpdates(this) } catch (_: Exception) {}
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    @Deprecated("Deprecated in Android")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
