package com.capstone.komunitas.ui.chat

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.capstone.komunitas.R
import com.capstone.komunitas.data.db.entities.Chat
import com.capstone.komunitas.databinding.ActivityChatNoVideoBinding
import com.capstone.komunitas.util.Coroutines
import com.capstone.komunitas.util.hide
import com.capstone.komunitas.util.show
import com.capstone.komunitas.util.toast
import com.google.mediapipe.components.PermissionHelper
import com.xwray.groupie.GroupieAdapter
import kotlinx.android.synthetic.main.activity_chat_no_video.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance


class ChatNoVideoActivity : AppCompatActivity(), ChatListener, KodeinAware {
    override val kodein by kodein()

    private val factory: ChatViewModelFactory by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewModel binding
        val binding: ActivityChatNoVideoBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_chat_no_video)
        val viewModel = ViewModelProviders.of(this, factory).get(ChatViewModel::class.java)
        binding.viewmodel = viewModel

        PermissionHelper.checkAndRequestAudioPermissions(this)
        viewModel.chatListener = this
        bindUI(viewModel)
        bindAppBar()
    }

//    override fun onResume() {
//        super.onResume()
//        KeyboardEventListener(this) { isOpen ->
//            if(isOpen){
//                layout_control_novid.hide()
//            }else{
//                layout_control_novid.show()
//            }
//        }
//    }

    fun bindAppBar() {
        // Back listener
        chat_no_vid_appbar.setNavigationOnClickListener {
            onBack()
        }

    }

    private fun bindUI(viewModel: ChatViewModel) = Coroutines.main {
        progress_bar_chat_novideo.show()
        viewModel.chats.await().observe(this, Observer {
            progress_bar_chat_novideo.hide()
            initRecyclerView(it, viewModel)
        })
    }

    private fun initRecyclerView(chatItem: List<Chat>, viewModel: ChatViewModel) {
        val groupAdapter = GroupieAdapter()
        groupAdapter.spanSizeLookup.apply {  }

        if(chatItem.isEmpty()){
            layout_chat_novid_nodata.show()
            messages_rv_chat_novid.hide()
        }else{
            layout_chat_novid_nodata.hide()
            messages_rv_chat_novid.show()
            chatItem.forEach {
                if (it.isSpeaker == 1) {
                    groupAdapter.add(ChatReceivedItem(it))
                } else {
                    groupAdapter.add(ChatSentItem(it, viewModel))
                }
            }

            messages_rv_chat_novid.apply {
                layoutManager = LinearLayoutManager(context)
                setHasFixedSize(true)
                adapter = groupAdapter
            }
            messages_rv_chat_novid.scrollToPosition(groupAdapter.itemCount - 1)
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

    override fun onBack() {
        this.finish()
    }

    override fun onChangeLens() {
        return
    }

    override fun onRecordPressed(isRecording: Boolean) {
        if(isRecording){
            btnrecord_chat_novid.setImageResource(R.drawable.ic_stop_white)

        }else{
            btnrecord_chat_novid.setImageResource(R.drawable.ic_record_white)

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            1 -> {
                if(grantResults.size > 0){
                    var permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    var permissionToStore = grantResults[1] == PackageManager.PERMISSION_GRANTED

                }
            }
        }
    }

    companion object{
        private const val TAG_AUDIO="AudioRecord"
    }
}