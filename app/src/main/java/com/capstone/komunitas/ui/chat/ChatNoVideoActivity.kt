package com.capstone.komunitas.ui.chat

import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.capstone.komunitas.R
import com.capstone.komunitas.data.db.entities.Chat
import com.capstone.komunitas.databinding.ActivityChatNoVideoBinding
import com.capstone.komunitas.engines.AudioRecord
import com.capstone.komunitas.util.*
import com.xwray.groupie.GroupieAdapter
import kotlinx.android.synthetic.main.activity_chat_no_video.*
import kotlinx.android.synthetic.main.activity_chat_no_video.messagesRecyclerView
import kotlinx.android.synthetic.main.activity_chat_no_video.progress_bar_chat_novideo
import kotlinx.android.synthetic.main.activity_chat_with_video.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.io.*
import java.lang.Exception


class ChatNoVideoActivity : AppCompatActivity(), ChatListener, KodeinAware {
    override val kodein by kodein()

    private val factory: ChatViewModelFactory by instance()
    private val audioRecorder:AudioRecord = AudioRecord()

    private lateinit var recorder: MediaRecorder
    private lateinit var fileOutput:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewModel binding
        val binding: ActivityChatNoVideoBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_chat_no_video)
        val viewModel = ViewModelProviders.of(this, factory).get(ChatViewModel::class.java)
        binding.viewmodel = viewModel

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
        messagesRecyclerView.scrollToPosition(groupAdapter.itemCount - 1)
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
        return
    }

    override fun onRecordPressed(isRecording: Boolean) {
        if(isRecording){
            btnrecord_chat_novid.setImageResource(R.drawable.ic_stop_white)
            startRecording()
        }else{
            btnrecord_chat_novid.setImageResource(R.drawable.ic_record_white)
            stopRecording()
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

    private fun startRecording(){
        recorder = MediaRecorder()
        fileOutput = Environment.getExternalStorageDirectory().absolutePath+"/AudioRecording.3gp"
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        recorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB)

        try {
            recorder.setOutputFile(fileOutput)
        }catch (e: Exception){
            Log.e(TAG_AUDIO, "startRecording: OutputFailed $e" )
        }

        try{
            recorder.prepare()
            recorder.start()
        }catch (e: Exception){
            e.printStackTrace()
            Log.e(TAG_AUDIO, "startRecording: Failed to start! $e" )
        }
    }

    private fun stopRecording(){

        try{
            recorder.stop()
            recorder.reset()
            recorder.release()
        }catch (e: Exception){
            e.printStackTrace()
            Log.e(TAG_AUDIO, "startRecording: Failed to stop! $e" )
        }

    }

    private fun readFile(){
        var inputStream: InputStream? = null
        try {
            inputStream = BufferedInputStream(FileInputStream(fileOutput))
        }catch (e: FileNotFoundException){
            e.printStackTrace()
        }

        finally {
            if (inputStream!=null){
                try {
                    inputStream.close()
                }catch (e: IOException){
                    e.printStackTrace()
                }
            }
        }
    }



    companion object{
        private const val TAG_AUDIO="AudioRecord"
    }
}