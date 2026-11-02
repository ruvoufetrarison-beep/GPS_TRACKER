package com.fanofeno.gpstracker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var status: TextView
    private lateinit var start: Button
    private lateinit var stop: Button
    private val prefs by lazy { getSharedPreferences("gps_session", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        updateUi()
    }

    private fun buildUi() {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 30, 28, 24)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        wrap.addView(TextView(this).apply {
            text = "GPS TRACKER"
            textSize = 27f
            setTextColor(0xff145b91.toInt())
            gravity = Gravity.CENTER
        }, lp())
        wrap.addView(TextView(this).apply {
            text = "Toerana GPS • isaky ny 5 minitra"
            textSize = 15f; gravity = Gravity.CENTER
        }, lp())
        email = EditText(this).apply { hint = "Adiresy mailaka"; inputType = 33 }
        password = EditText(this).apply { hint = "Teny miafina"; inputType = 129 }
        wrap.addView(email, lp())
        wrap.addView(password, lp())
        val login = Button(this).apply { text = "Hiditra" }
        val signup = Button(this).apply { text = "Hamorona kaonty" }
        wrap.addView(login, lp())
        wrap.addView(signup, lp())
        login.setOnClickListener { authenticate(false) }
        signup.setOnClickListener { authenticate(true) }

        status = TextView(this).apply {
            textSize = 14f; setPadding(0, 24, 0, 16)
            text = "Midira amin'ny kaontinao aloha."
        }
        wrap.addView(status, lp())
        start = Button(this).apply { text = "ALEFA NY GPS" }
        stop = Button(this).apply { text = "AJANONY NY GPS" }
        wrap.addView(start, lp())
        wrap.addView(stop, lp())
        start.setOnClickListener { requestAndStart() }
        stop.setOnClickListener {
            stopService(Intent(this, LocationService::class.java))
            status.text = "Najanona ny GPS. Tsy maka toerana intsony ny app."
            updateUi()
        }
        wrap.addView(TextView(this).apply {
            text = "Fanamarihana: mila GPS sy alalana Location. Hita foana ny notification rehefa mandeha ny fanarahana. Ny toerana dia alefa any amin'ny kaontinao Supabase."
            textSize = 12f; setPadding(0, 24, 0, 0)
        }, lp())
        setContentView(ScrollView(this).apply { addView(wrap) })
    }

    private fun lp() = LinearLayout.LayoutParams(-1, -2).apply { topMargin = 8; bottomMargin = 8 }

    private fun authenticate(signup: Boolean) {
        val e = email.text.toString().trim()
        val p = password.text.toString()
        if (e.isBlank() || p.length < 6) {
            status.text = "Ampidiro ny mailaka ary teny miafina 6 tarehintsoratra farafahakeliny."
            return
        }
        status.text = "Miandry valiny..."
        executor.execute {
            try {
                if (signup) {
                    val msg = Api.signUp(e, p)
                    runOnUiThread { status.text = msg }
                } else {
                    val session = Api.signIn(e, p)
                    prefs.edit()
                        .putString("access_token", session.getString("access_token"))
                        .putString("refresh_token", session.getString("refresh_token"))
                        .putString("user_id", session.getJSONObject("user").getString("id"))
                        .apply()
                    runOnUiThread {
                        status.text = "Tafiditra soa aman-tsara: $e"
                        updateUi()
                    }
                }
            } catch (ex: Exception) {
                runOnUiThread { status.text = "Hadisoana: ${ex.message}" }
            }
        }
    }

    private fun requestAndStart() {
        if (prefs.getString("access_token", null) == null) {
            status.text = "Midira aloha vao afaka manomboka ny GPS."
            return
        }
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= 33) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
            return
        }
        startServiceNow()
    }

    private fun startServiceNow() {
        val i = Intent(this, LocationService::class.java)
        ContextCompat.startForegroundService(this, i)
        status.text = "Nalefa ny GPS. Jereo ny notification raha te hijanona."
        updateUi()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults.any { it == PackageManager.PERMISSION_GRANTED } &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                startServiceNow()
            } else {
                status.text = "Tsy nomena ny alalana GPS. Omeo alalana Location ao amin'ny Settings."
            }
        }
    }

    private fun updateUi() {
        val loggedIn = prefs.getString("access_token", null) != null
        start.isEnabled = loggedIn
        stop.isEnabled = loggedIn
        if (loggedIn && status.text.isNullOrBlank()) status.text = "Efa tafiditra. Tsindrio ALEFA NY GPS rehefa vonona."
    }
}
