package com.capstone.komunitas.ui.home

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.capstone.komunitas.R
import com.capstone.komunitas.databinding.ActivityHomeBinding
import com.capstone.komunitas.util.toast
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class HomeActivity : AppCompatActivity(), HomeListener, KodeinAware {
    override val kodein by kodein()

    private val factory: HomeViewModelFactory by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewModel binding
        val binding: ActivityHomeBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_home)
        val viewModel = ViewModelProviders.of(this, factory).get(HomeViewModel::class.java)
        binding.viewmodel = viewModel

        viewModel.homeListener = this
    }

    override fun onStarted() {
        toast("Started")
    }

    override fun onSuccess(message: String) {
        toast("Success")
    }

    override fun onFailure(message: String) {
        toast("Failed")
    }
}