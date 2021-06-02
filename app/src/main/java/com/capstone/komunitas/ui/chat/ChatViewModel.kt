package com.capstone.komunitas.ui.chat

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.capstone.komunitas.data.repositories.ChatRepository
import com.capstone.komunitas.engines.AudioRecord
import com.capstone.komunitas.engines.TextToSpeechEngine
import com.capstone.komunitas.util.ApiException
import com.capstone.komunitas.util.Coroutines
import com.capstone.komunitas.util.NoInternetException
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.InputStream
import kotlin.coroutines.coroutineContext

class ChatViewModel(
    private val repository: ChatRepository,
    private val textToSpeechEngine: TextToSpeechEngine,
    @SuppressLint("StaticFieldLeak") private val context: Context
) : ViewModel() {
    var chatListener: ChatListener? = null
    var newMessageText: String? = null
    var isRecording: Boolean = false
    var lensFacing = CameraSelector.LENS_FACING_BACK
    val audioRecord = AudioRecord(context)

    val chats by lazyDeferred {
        repository.getChat()
    }

    fun changeLens(){
        lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
       // chatListener?.onChangeLens(lensFacing)
    }

    fun<T> lazyDeferred(block: suspend CoroutineScope.() -> T): Lazy<Deferred<T>>{
        return lazy {
            GlobalScope.async(start = CoroutineStart.LAZY) {
                block.invoke(this)
            }
        }
    }

    fun onRecordPressed(){
        isRecording = !isRecording
        chatListener?.onRecordPressed(isRecording)
        if (isRecording){
            audioRecord.startRecording()
        }else{
            audioRecord.stopRecording()
            Log.d("audioRecord", "onRecordPressed: "+audioRecord.uploadFile())
            val requestBody = RequestBody.create(MediaType.parse("audio/*"),audioRecord.uploadFile())
            val lang = RequestBody.create(MediaType.parse("text/plain"),"id-ID")
            val body = MultipartBody.Part.createFormData("file",audioRecord.uploadFile().name,requestBody)
            sendAudio(body,lang)

        }
    }

    fun speechChat(text: String?){
        textToSpeechEngine.textToSpeech(text!!)
    }

    fun sendMessagePressed() {
        chatListener?.onSendStarted()
        // Username or password is empty
        if (newMessageText.isNullOrEmpty()) {
            chatListener?.onSendFailure("Pesan tidak boleh kosong")
            return
        }

        // Call api via kotlin coroutines
        Coroutines.main {
            try {
                val chatResponse = repository.sendChat(newMessageText!!, 0)
                chatResponse?.let {
                    if (it.data!!.size > 0) {
                        speechChat(newMessageText!!)
                        repository.saveChat(it.data)
                        chatListener?.onSendSuccess("Berhasil mengambil pesan")
                        newMessageText = null
                        return@main
                    }
                }
                chatListener?.onSendFailure("Terjadi kesalahan")
            } catch (e: ApiException) {
                chatListener?.onSendFailure(e.message!!)
            } catch (e: NoInternetException) {
                chatListener?.onSendFailure(e.message!!)
            }
        }
    }

    fun sendAudio(body:MultipartBody.Part,lang:RequestBody){
        chatListener?.onSendStarted()
        Coroutines.main {
            try {
                val responseTTS = repository.sendAudio(body,lang)
                Log.d("AudioRecord", "sendAudio: ${responseTTS.message}" )
                responseTTS?.let {

                    if (it.statusCode?.equals(200) == true){
                        chatListener?.onSendSuccess("Berhasil mengambil audio pesan ${it.data}")
                        newMessageText = it.data
                        sendMessagePressed()
                        return@main
                    }
                }
                chatListener?.onSendFailure("Terjadi kesalahan")
            }catch (e: ApiException){
                Log.e("AudioRecord", "sendAudio: $e" )
            }catch (e:NoInternetException){
                Log.e("AudioRecord", "sendAudio: $e" )
            }
        }
    }
     fun downloadAudio(text: String?){
        Coroutines.main {
            try {
                val responseSTT = repository.downloadAudio("test")
                Log.d("AudioRecord", "sendAudio: ${responseSTT.message}" )
                responseSTT?.let {
                    if (it.statusCode?.equals(200) == true){
                        audioRecord.saveIntoAudio(it)
                        chatListener?.onSendSuccess("Berhasil mengambil audio pesan : ${it.message}")
                    }
                }
            }catch (e:ApiException){
                Log.e("AudioRecord", "downloadAudio: $e" )
            }catch (e:NoInternetException){
                Log.e("AudioRecord", "downloadAudio: $e" )
            }
        }
    }


}