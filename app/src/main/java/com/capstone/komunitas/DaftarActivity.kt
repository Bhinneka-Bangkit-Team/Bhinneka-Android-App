package com.capstone.komunitas

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_daftar.*

class DaftarActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daftar)

        btn_daftar1.setOnClickListener {
            Intent(this@DaftarActivity, MainActivity::class.java).also {
                startActivity(it)
            }
        }

        btn_login1.setOnClickListener {
            Intent(this@DaftarActivity, LoginActivity::class.java).also {
                startActivity(it)
            }
        }
    }
}