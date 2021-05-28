package com.capstone.komunitas.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.capstone.komunitas.R
import com.capstone.komunitas.data.db.entities.User
import com.capstone.komunitas.databinding.ActivityDaftarBinding
import com.capstone.komunitas.ui.home.HomeActivity
import com.capstone.komunitas.util.*
import kotlinx.android.synthetic.main.activity_daftar.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class DaftarActivity : AppCompatActivity(), AuthListener, KodeinAware {
    override val kodein by kodein()

    private val factory: AuthViewModelFactory by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewModel binding
        val binding: ActivityDaftarBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_daftar)
        val viewModel = ViewModelProviders.of(this, factory).get(AuthViewModel::class.java)
        binding.viewmodel = viewModel

        viewModel.authListener = this

        viewModel.getLoggetInUser().observe(this, Observer { user ->
            if (user != null) {
                Intent(this, HomeActivity::class.java).also {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(it)
                }
            }
        })
    }

    override fun onStarted() {
        progress_bar_daftar.show()
    }

    override fun onSuccess(user: User) {
        progress_bar_daftar.hide()
    }

    override fun onFailure(message: String) {
        progress_bar_daftar.hide()
        root_layout_daftar.snackbar(message)
    }
}