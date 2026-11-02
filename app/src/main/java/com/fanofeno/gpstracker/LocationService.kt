package com.fanofeno.gpstracker
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
class LocationService:Service(),LocationListener{
 private lateinit var manager:LocationManager
 private lateinit var queue:QueueDb
 private val intervalMs=5*60*1000L
 private val channelId="gps_tracking_channel"
 private var registered=false
 override fun onCreate(){
  super.onCreate();queue=QueueDb(this);manager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
  createChannel();startForeground(71,notification("Manomana GPS..."))
  try{
   val fine=ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
   val coarse=ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
   if(!fine&&!coarse){AppLog.write(this,"ERROR","Tsy nomena alalana Location.");stopSelf();return}
   var count=0
   listOf(LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER).forEach{provider->
    try{if(manager.isProviderEnabled(provider)){manager.requestLocationUpdates(provider,intervalMs,0f,this);manager.getLastKnownLocation(provider)?.let{onLocationChanged(it)};count++;AppLog.write(this,"INFO","Provider $provider natomboka.")}}
    catch(e:SecurityException){AppLog.write(this,"WARN","Provider $provider: ${e.message}")}
   }
   registered=count>0
   if(!registered){AppLog.write(this,"ERROR","Tsy misy provider GPS/Network mandeha.");updateNotification("Tsy misy GPS provider mandeha.")}
   else{AppLog.write(this,"INFO","GPS service mandeha; interval 5 minitra.");updateNotification("Miandry toerana GPS...")}
  }catch(e:Exception){AppLog.write(this,"ERROR","Service start: ${e.message}");updateNotification("Hadisoana GPS: ${e.message}");stopSelf()}
 }
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int)=START_STICKY
 override fun onLocationChanged(location:Location){
  try{
   val p=getSharedPreferences("gps_session",MODE_PRIVATE)
   val uid=p.getString("user_id",null)?:run{AppLog.write(this,"ERROR","Tsy misy user_id. Midira indray.");return}
   val did=p.getString("device_id",null)?:UUID.randomUUID().toString().also{p.edit().putString("device_id",it).apply()}
   val iso=SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.US).apply{timeZone=java.util.TimeZone.getTimeZone("UTC")}.format(Date(location.time))
   val point=JSONObject().put("user_id",uid).put("device_id",did).put("latitude",location.latitude).put("longitude",location.longitude)
    .put("accuracy_m",if(location.hasAccuracy())location.accuracy else JSONObject.NULL)
    .put("altitude_m",if(location.hasAltitude())location.altitude else JSONObject.NULL)
    .put("speed_mps",if(location.hasSpeed())location.speed else JSONObject.NULL)
    .put("bearing_deg",if(location.hasBearing())location.bearing else JSONObject.NULL).put("captured_at",iso)
   AppLog.write(this,"INFO","Toerana voaray lat=${location.latitude}, lon=${location.longitude}, accuracy=${location.accuracy}m.")
   queue.add(point);flushQueue()
   updateNotification("GPS mandeha • ${queue.count()} miandry • ${SimpleDateFormat("HH:mm:ss",Locale.US).format(Date())}")
  }catch(e:Exception){AppLog.write(this,"ERROR","Location callback: ${e.message}");updateNotification("Hadisoana: ${e.message}")}
 }
 private fun flushQueue(){for((id,point) in queue.pending()){if(Api.upload(this,point))queue.delete(id)else break};AppLog.write(this,"INFO","Queue mbola miandry: ${queue.count()}")}
 private fun updateNotification(msg:String){(getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(71,notification(msg))}
 private fun notification(msg:String):Notification{
  val pending=PendingIntent.getBroadcast(this,72,Intent(this,StopReceiver::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  return NotificationCompat.Builder(this,channelId).setSmallIcon(R.drawable.ic_gps_tracker).setContentTitle("GPS Tracker")
   .setContentText(msg).setStyle(NotificationCompat.BigTextStyle().bigText(msg)).setOngoing(true)
   .addAction(android.R.drawable.ic_media_pause,"Ajanony",pending).build()
 }
 private fun createChannel(){if(Build.VERSION.SDK_INT>=26){val ch=NotificationChannel(channelId,"GPS tracking",NotificationManager.IMPORTANCE_LOW);(getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)}}
 override fun onDestroy(){try{if(registered)manager.removeUpdates(this)}catch(_:Exception){};AppLog.write(this,"WARN","GPS service najanona.");super.onDestroy()}
 override fun onBind(intent:Intent?):IBinder?=null
}