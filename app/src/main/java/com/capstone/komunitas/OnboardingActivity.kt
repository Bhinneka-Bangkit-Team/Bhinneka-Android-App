package com.capstone.komunitas

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.capstone.komunitas.ui.auth.DaftarActivity
import kotlinx.android.synthetic.main.activity_onboarding.*

class OnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager.adapter = ViewPagerAdapter(supportFragmentManager)

        btn_skip.setOnClickListener {
            Intent(this@OnboardingActivity, DaftarActivity::class.java).also {
                startActivity(it)
            }
        }
    }
}