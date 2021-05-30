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
import com.capstone.komunitas.databinding.ActivityChatWithVideoBinding
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

        // ViewModel binding
        val binding: ActivityChatWithVideoBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_chat_with_video)
        val viewModel = ViewModelProviders.of(this, factory).get(ChatViewModel::class.java)
        binding.viewmodel = viewModel

        viewModel.chatListener = this
        viewModel.lensFacing = CameraSelector.LENS_FACING_BACK

        bindUI(viewModel)
        bindCamera(viewModel.lensFacing)
        bindAppBar(viewModel)
    }

//    override fun onResume() {
//        super.onResume()
//        KeyboardEventListener(this) { isOpen ->
//            if(isOpen){
//                layout_control_withvid.hide()
//            }else{
//                layout_control_withvid.show()
//            }
//        }
//    }

    fun bindAppBar(viewModel: ChatViewModel){
        // Back listener
        chat_with_vid_appbar.setNavigationOnClickListener {
            onBack()
        }
        // Menu listener
        chat_with_vid_appbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.flip_camera -> {
                    viewModel.changeLens()
                    false
                }
                else -> false
            }
        }
    }

    fun bindCamera(lensFacing: Int){
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
            bindPreview(cameraProvider, lensFacing)
        }, ContextCompat.getMainExecutor(this))
    }

    fun bindPreview(cameraProvider : ProcessCameraProvider, lensFacing: Int) {
        val preview : Preview = Preview.Builder()
            .build()

        val cameraSelector : CameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
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
        messagesRecyclerView.scrollToPosition(groupAdapter.itemCount -1)
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

    override fun onBack() {
        this.finish()
    }

    override fun onChangeLens(lensFacing: Int) {
        bindCamera(lensFacing)
    }

    override fun onRecordPressed(isRecording: Boolean) {
        if(isRecording){
            btnrecord_chat_withvid.setImageResource(R.drawable.ic_stop_white)
        }else{
            btnrecord_chat_withvid.setImageResource(R.drawable.ic_record_white)
        }
    }
}