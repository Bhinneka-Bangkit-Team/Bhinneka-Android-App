package com.capstone.komunitas

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        btn_login2.setOnClickListener {
            Intent(this@LoginActivity, MainActivity::class.java).also {
                startActivity(it)
            }
        }

        btn_daftar2.setOnClickListener {
            Intent(this@LoginActivity, DaftarActivity::class.java).also {
                startActivity(it)
            }
        }
    }
}