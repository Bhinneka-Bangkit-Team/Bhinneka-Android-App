package com.capstone.komunitas.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.capstone.komunitas.R
import com.capstone.komunitas.databinding.ActivitySplashscreenBinding
import com.capstone.komunitas.ui.home.HomeActivity
import com.capstone.komunitas.ui.onboarding.OnboardingActivity
import com.capstone.komunitas.util.hide
import com.capstone.komunitas.util.show
import kotlinx.android.synthetic.main.activity_splashscreen.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class SplashscreenActivity : AppCompatActivity(), KodeinAware {
    override val kodein by kodein()

    private val factory: SplashScreenViewModelFactory by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewModel binding
        val binding: ActivitySplashscreenBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_splashscreen)
        val viewModel = ViewModelProviders.of(this, factory).get(SplashScreenViewModel::class.java)
        binding.viewmodel = viewModel

        progress_bar_splash.show()
        viewModel.getLoggetInUser().observe(this, Observer { user ->
            // Check logged in user
            if (user != null) {
                progress_bar_splash.hide()
                Intent(this, HomeActivity::class.java).also {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(it)
                }
            }else{
                progress_bar_splash.hide()
                Intent(this, OnboardingActivity::class.java).also {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(it)
                }
            }
        })
        setContentView(R.layout.activity_splashscreen)
    }
}