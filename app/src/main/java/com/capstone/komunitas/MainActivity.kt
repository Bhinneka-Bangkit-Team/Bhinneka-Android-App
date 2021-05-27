package com.capstone.komunitas

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.viewpager.widget.ViewPager
import com.capstone.komunitas.views.camera_detection.DetectionObjectActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val isyarat_btn = findViewById(R.id.isyarat_btn) as Button
        isyarat_btn.setOnClickListener {
            val intent = Intent(this,DetectionObjectActivity::class.java)
            startActivity(intent)
        }
    }
}