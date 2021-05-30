package com.capstone.komunitas.ui.onboarding

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.capstone.komunitas.R
import com.capstone.komunitas.ui.auth.DaftarActivity
import com.capstone.komunitas.ui.auth.LoginActivity
import kotlinx.android.synthetic.main.activity_onboarding.*

class OnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager.adapter = ViewPagerAdapter(supportFragmentManager)

        btn_skip.setOnClickListener {
            Intent(this@OnboardingActivity, LoginActivity::class.java).also {
                startActivity(it)
            }
        }
    }
}