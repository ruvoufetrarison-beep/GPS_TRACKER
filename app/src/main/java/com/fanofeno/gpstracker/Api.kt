package com.fanofeno.gpstracker
import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
object Api {
 data class Result(val ok:Boolean,val message:String)
 private fun request(path:String,method:String,body:String?=null,token:String?=null,headers:Map<String,String> = emptyMap()):Pair<Int,String>{
  val c=URL(BuildConfig.SUPABASE_URL+path).openConnection() as HttpURLConnection
  try {
   c.requestMethod=method;c.connectTimeout=15000;c.readTimeout=20000
   c.setRequestProperty("apikey",BuildConfig.SUPABASE_KEY);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Accept","application/json")
   if(token!=null)c.setRequestProperty("Authorization","Bearer $token")
   headers.forEach{(k,v)->c.setRequestProperty(k,v)}
   if(body!=null){c.doOutput=true;c.outputStream.use{it.write(body.toByteArray(Charsets.UTF_8))}}
   val code=c.responseCode;val stream=if(code in 200..299)c.inputStream else c.errorStream
   return code to (stream?.bufferedReader()?.use{it.readText()} ?: "")
  } finally { c.disconnect() }
 }
 fun signUp(ctx:Context,email:String,password:String):String{
  AppLog.write(ctx,"INFO","Manomboka fisoratana anarana.")
  val(code,body)=request("/auth/v1/signup","POST",JSONObject().put("email",email).put("password",password).toString())
  if(code !in 200..299){AppLog.write(ctx,"ERROR","Signup HTTP $code: $body");throw Exception(error(body,"Tsy nahomby ny fisoratana anarana"))}
  AppLog.write(ctx,"INFO","Signup voaray HTTP $code.")
  return "Vita ny fangatahana. Jereo ny mailaka raha mila fanamarinana."
 }
 fun signIn(ctx:Context,email:String,password:String):JSONObject{
  AppLog.write(ctx,"INFO","Manandrana miditra Supabase Auth.")
  val(code,body)=request("/auth/v1/token?grant_type=password","POST",JSONObject().put("email",email).put("password",password).toString())
  if(code !in 200..299){AppLog.write(ctx,"ERROR","Login HTTP $code: $body");throw Exception(error(body,"Tsy nety ny fidirana"))}
  AppLog.write(ctx,"INFO","Login Supabase OK HTTP $code.");return JSONObject(body)
 }
 private fun refresh(token:String):JSONObject{
  val(code,body)=request("/auth/v1/token?grant_type=refresh_token","POST",JSONObject().put("refresh_token",token).toString())
  if(code !in 200..299)throw Exception(error(body,"Session refresh failed"))
  return JSONObject(body)
 }
 fun testConnection(ctx:Context):Result=try{
  val p=ctx.getSharedPreferences("gps_session",Context.MODE_PRIVATE)
  var access=p.getString("access_token",null)?:return Result(false,"Tsy mbola tafiditra amin'ny kaonty.")
  AppLog.write(ctx,"INFO","Mitsapa ny fifandraisana Supabase...")
  var response=request("/auth/v1/user","GET",token=access)
  if(response.first==401){
   val rt=p.getString("refresh_token",null)?:return Result(false,"Tapitra ny session. Midira indray.")
   val s=refresh(rt);access=s.getString("access_token")
   p.edit().putString("access_token",access).putString("refresh_token",s.optString("refresh_token",rt)).apply()
   response=request("/auth/v1/user","GET",token=access)
  }
  if(response.first in 200..299){AppLog.write(ctx,"INFO","Supabase Auth OK.");Result(true,"Mifandray tsara amin'ny Supabase.")}
  else {AppLog.write(ctx,"ERROR","Test HTTP ${response.first}: ${response.second}");Result(false,"HTTP ${response.first}: ${error(response.second,"Tsy nahomby")}") }
 }catch(e:Exception){AppLog.write(ctx,"ERROR","Supabase test: ${e.message}");Result(false,"Tsy afaka nifandray: ${e.message}")}
 fun upload(ctx:Context,point:JSONObject):Boolean=try{
  val p=ctx.getSharedPreferences("gps_session",Context.MODE_PRIVATE)
  var access=p.getString("access_token",null)?:run{AppLog.write(ctx,"ERROR","Tsy misy access token.");return false}
  val rt=p.getString("refresh_token",null)
  fun send(t:String)=request("/rest/v1/gps_locations","POST",point.toString(),t,mapOf("Prefer" to "return=minimal","Content-Profile" to "public"))
  var result=send(access)
  if(result.first==401 && rt!=null){
   AppLog.write(ctx,"WARN","Manavao session lany daty.")
   val s=refresh(rt);access=s.getString("access_token")
   p.edit().putString("access_token",access).putString("refresh_token",s.optString("refresh_token",rt)).apply()
   result=send(access)
  }
  if(result.first in 200..299){AppLog.write(ctx,"INFO","GPS nalefa Supabase HTTP ${result.first}.");true}
  else{AppLog.write(ctx,"ERROR","Upload HTTP ${result.first}: ${result.second}");false}
 }catch(e:Exception){AppLog.write(ctx,"ERROR","Upload exception ${e.javaClass.simpleName}: ${e.message}");false}
 private fun error(body:String,fallback:String):String=try{val j=JSONObject(body);j.optString("msg",j.optString("message",j.optString("error_description",fallback)))}catch(_:Exception){if(body.isBlank())fallback else body}
}