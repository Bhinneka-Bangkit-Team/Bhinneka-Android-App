package com.capstone.komunitas

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler

class SplashscreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splashscreen)

        val handler = Handler()
        handler.postDelayed({
            val intent = Intent(this@SplashscreenActivity, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }
}