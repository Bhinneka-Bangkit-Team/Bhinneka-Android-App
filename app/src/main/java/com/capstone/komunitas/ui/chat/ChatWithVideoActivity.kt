package com.capstone.komunitas.ui.chat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.capstone.komunitas.R
import com.capstone.komunitas.data.db.entities.Chat
import com.capstone.komunitas.databinding.ActivityChatNoVideoBinding
import com.capstone.komunitas.util.*
import com.google.common.util.concurrent.ListenableFuture
import com.xwray.groupie.GroupieAdapter
import kotlinx.android.synthetic.main.activity_chat_no_video.*
import kotlinx.android.synthetic.main.activity_chat_no_video.messagesRecyclerView
import kotlinx.android.synthetic.main.activity_chat_no_video.progress_bar_chat_novideo
import kotlinx.android.synthetic.main.activity_chat_with_video.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class ChatWithVideoActivity : AppCompatActivity(), ChatListener, KodeinAware {

    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>

    override val kodein by kodein()

    private val factory: ChatViewModelFactory by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))

        // ViewModel binding
        val binding: ActivityChatNoVideoBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_chat_with_video)
        val viewModel = ViewModelProviders.of(this, factory).get(ChatViewModel::class.java)
        binding.viewmodel = viewModel

        viewModel.chatListener = this
        bindUI(viewModel)
    }

    fun bindPreview(cameraProvider : ProcessCameraProvider) {
        var preview : Preview = Preview.Builder()
            .build()

        var cameraSelector : CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.setSurfaceProvider(previewView.getSurfaceProvider())

        var camera = cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview)
    }

    private fun bindUI(viewModel: ChatViewModel) = Coroutines.main {
        progress_bar_chat_novideo.show()
        viewModel.chats.await().observe(this, Observer {
            progress_bar_chat_novideo.hide()
            initRecyclerView(it)
        })
    }

    private fun initRecyclerView(chatItem: List<Chat>) {
        val groupAdapter = GroupieAdapter()

        chatItem.forEach {
            if (it.isSpeaker == 1) {
                groupAdapter.add(ChatReceivedItem(it))
            } else {
                groupAdapter.add(ChatSentItem(it))
            }
        }

        messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            adapter = groupAdapter
        }
    }

    override fun onGetStarted() {
        toast("Started")
    }

    override fun onGetSuccess(chats: List<Chat>) {
        toast("Success")
    }

    override fun onGetFailure(message: String) {
        toast(message)
    }

    override fun onSendStarted() {
        toast("Started")
    }

    override fun onSendSuccess(message: String) {
        toast(message)
    }

    override fun onSendFailure(message: String) {
        toast(message)
    }
}