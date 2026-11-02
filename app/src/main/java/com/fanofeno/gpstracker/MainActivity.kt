package com.fanofeno.gpstracker
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class MainActivity:Activity(){
 private val executor=Executors.newSingleThreadExecutor()
 private lateinit var email:EditText
 private lateinit var password:EditText
 private lateinit var status:TextView
 private lateinit var logs:TextView
 private lateinit var start:Button
 private lateinit var stop:Button
 private val prefs by lazy{getSharedPreferences("gps_session",MODE_PRIVATE)}
 private val navy=Color.rgb(13,31,56)
 private val blue=Color.rgb(27,111,210)
 private fun shape(color:Int,r:Float=16f)=GradientDrawable().apply{setColor(color);cornerRadius=r}
 private fun label(s:String,size:Float,color:Int=navy,bold:Boolean=false)=TextView(this).apply{text=s;textSize=size;setTextColor(color);if(bold)setTypeface(typeface,Typeface.BOLD)}
 private fun btn(s:String,color:Int=blue)=Button(this).apply{text=s;isAllCaps=false;textSize=14f;setTextColor(Color.WHITE);background=shape(color)}
 private fun add(box:LinearLayout,v:View,top:Int=8){box.addView(v,LinearLayout.LayoutParams(-1,-2).apply{topMargin=top;bottomMargin=4})}
 private fun card()=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(16,16,16,16);background=shape(Color.WHITE,18f);elevation=3f}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);AppLog.write(this,"INFO","Nosokafana ny application.");drawScreen()}
 private fun drawScreen(){
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(18,20,18,28);setBackgroundColor(Color.rgb(242,246,251))}
  val header=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(16,20,16,20);background=shape(navy,20f)}
  header.addView(ImageView(this).apply{setImageResource(R.drawable.ic_gps_tracker);layoutParams=LinearLayout.LayoutParams(62,62).apply{rightMargin=14}})
  val brand=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
  brand.addView(label("GPS TRACKER",23f,Color.WHITE,true))
  brand.addView(label("SECURE LOCATION  •  LIVE SYNC",10f,Color.rgb(174,204,239),true))
  header.addView(brand);add(root,header,0)
  val auth=card();add(auth,label("KAONTY SUPABASE",15f,navy,true))
  email=EditText(this).apply{hint="Adiresy mailaka";inputType=33;setSingleLine();background=shape(Color.rgb(244,247,251),10f);setPadding(12,8,12,8)}
  password=EditText(this).apply{hint="Teny miafina";inputType=129;setSingleLine();background=shape(Color.rgb(244,247,251),10f);setPadding(12,8,12,8)}
  add(auth,email);add(auth,password)
  val authRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
  authRow.addView(btn("Hiditra"),LinearLayout.LayoutParams(0,-2,1f).apply{rightMargin=5})
  authRow.addView(btn("Hamorona kaonty",Color.rgb(63,82,108)),LinearLayout.LayoutParams(0,-2,1f).apply{leftMargin=5})
  add(auth,authRow)
  (authRow.getChildAt(0) as Button).setOnClickListener{auth(false)}
  (authRow.getChildAt(1) as Button).setOnClickListener{auth(true)}
  add(root,auth)
  val gps=card();add(gps,label("FANARAHANA TOERANA",15f,navy,true))
  status=label("Midira amin'ny kaontinao aloha.",13f,Color.DKGRAY)
  status.setPadding(12,14,12,14);status.background=shape(Color.rgb(237,243,250),10f);add(gps,status)
  start=btn("ALEFA NY GPS",Color.rgb(19,145,99));stop=btn("AJANONY NY GPS",Color.rgb(190,57,57))
  add(gps,start);add(gps,stop)
  val test=btn("HITSAPA NY FIFANDRAISANA SUPABASE",Color.rgb(58,78,106));add(gps,test)
  start.setOnClickListener{requestStart()}
  stop.setOnClickListener{stopService(Intent(this,LocationService::class.java));status.text="Najanona ny GPS.";AppLog.write(this,"INFO","Najanona tamin'ny bokotra ny GPS.");updateState();refreshLogs()}
  test.setOnClickListener{status.text="Mitsapa Supabase...";executor.execute{val r=Api.testConnection(this);runOnUiThread{status.text=r.message;refreshLogs()}}}
  add(root,gps)
  val logCard=card()
  val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
  row.addView(label("DIAGNOSTIC LOG",15f,navy,true),LinearLayout.LayoutParams(0,-2,1f))
  val reload=btn("HAVAOZY",Color.rgb(58,78,106));row.addView(reload)
  add(logCard,row)
  logs=label("Miandry log...",10f,Color.rgb(48,61,78));logs.typeface=Typeface.MONOSPACE;logs.setPadding(10,10,10,10);logs.background=shape(Color.rgb(239,243,248),10f);add(logCard,logs)
  reload.setOnClickListener{refreshLogs()}
  val actions=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
  val share=btn("ZARAO NY LOG");val clear=btn("FAFAO",Color.GRAY)
  actions.addView(share,LinearLayout.LayoutParams(0,-2,1f).apply{rightMargin=4});actions.addView(clear,LinearLayout.LayoutParams(0,-2,1f).apply{leftMargin=4})
  add(logCard,actions)
  share.setOnClickListener{val i=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_SUBJECT,"GPS Tracker diagnostic log");putExtra(Intent.EXTRA_TEXT,AppLog.read(this@MainActivity,300))};startActivity(Intent.createChooser(i,"Zarao ny log"))}
  clear.setOnClickListener{AppLog.clear(this);refreshLogs()}
  add(root,logCard)
  add(root,label("GPS Tracker • Supabase • 5 minitra • Offline queue",10f,Color.GRAY),16)
  setContentView(ScrollView(this).apply{addView(root)})
  updateState();refreshLogs()
 }
 private fun auth(signup:Boolean){
  val e=email.text.toString().trim();val p=password.text.toString()
  if(e.isBlank()||p.length<6){status.text="Ampidiro mailaka sy teny miafina 6 tarehintsoratra farafahakeliny.";return}
  status.text="Miandry valiny..."
  executor.execute{try{
   if(signup){val m=Api.signUp(this,e,p);runOnUiThread{status.text=m;refreshLogs()}}
   else{val s=Api.signIn(this,e,p);prefs.edit().putString("access_token",s.getString("access_token")).putString("refresh_token",s.getString("refresh_token")).putString("user_id",s.getJSONObject("user").getString("id")).apply();runOnUiThread{status.text="Tafiditra soa aman-tsara: $e";updateState();refreshLogs()}}
  }catch(x:Exception){AppLog.write(this,"ERROR","Auth error: ${x.message}");runOnUiThread{status.text="Hadisoana: ${x.message}";refreshLogs()}}}
 }
 private fun requestStart(){
  if(prefs.getString("access_token",null)==null){status.text="Midira aloha.";return}
  val perms=mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)
  if(Build.VERSION.SDK_INT>=33)perms.add(Manifest.permission.POST_NOTIFICATIONS)
  val missing=perms.filter{ContextCompat.checkSelfPermission(this,it)!=PackageManager.PERMISSION_GRANTED}
  if(missing.isNotEmpty()){ActivityCompat.requestPermissions(this,missing.toTypedArray(),100);return}
  startGps()
 }
 private fun startGps(){try{ContextCompat.startForegroundService(this,Intent(this,LocationService::class.java));status.text="Nangataka hanomboka GPS. Jereo notification sy log.";AppLog.write(this,"INFO","Start GPS requested.");refreshLogs()}catch(e:Exception){status.text="Tsy afaka nanomboka GPS: ${e.message}";AppLog.write(this,"ERROR","Start service: ${e.message}");refreshLogs()}}
 override fun onRequestPermissionsResult(code:Int,perms:Array<out String>,results:IntArray){super.onRequestPermissionsResult(code,perms,results);if(code==100){val fine=ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;val coarse=ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;if(fine||coarse)startGps()else{status.text="Tsy nomena alalana Location.";AppLog.write(this,"ERROR","Location permission denied.");refreshLogs()}}}
 private fun updateState(){val yes=prefs.getString("access_token",null)!=null;start.isEnabled=yes;stop.isEnabled=yes}
 private fun refreshLogs(){if(::logs.isInitialized)logs.text=AppLog.read(this,35)}
 override fun onDestroy(){executor.shutdown();super.onDestroy()}
}