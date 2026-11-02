package com.fanofeno.gpstracker
import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
object AppLog {
 private const val NAME="gps_tracker.log"
 @Synchronized fun write(c:Context, level:String, msg:String) {
  val line="${SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).format(Date())} [$level] $msg"
  if(level=="ERROR") Log.e("GPSTracker",msg) else if(level=="WARN") Log.w("GPSTracker",msg) else Log.i("GPSTracker",msg)
  try { val f=File(c.filesDir,NAME); if(f.exists() && f.length()>1048576) { val old=File(c.filesDir,"$NAME.1"); old.delete(); f.renameTo(old) }; f.appendText(line+"\n") } catch(e:Exception) { Log.e("GPSTracker","log write failed",e) }
 }
 fun read(c:Context,n:Int=120):String=try { val f=File(c.filesDir,NAME); if(!f.exists()) "Tsy mbola misy log." else f.readLines().takeLast(n).joinToString("\n") } catch(e:Exception) { "Log read error: ${e.message}" }
 fun clear(c:Context) { try { File(c.filesDir,NAME).writeText("") } catch(_:Exception) {}; write(c,"INFO","Nofafana ny log taloha.") }
}