package com.capstone.komunitas.ui.chat

import android.media.MediaParser
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.lifecycle.ViewModel
import com.capstone.komunitas.data.repositories.ChatRepository
import com.capstone.komunitas.engines.AudioRecord
import com.capstone.komunitas.engines.TextToSpeechEngine
import com.capstone.komunitas.util.ApiException
import com.capstone.komunitas.util.Coroutines
import com.capstone.komunitas.util.NoInternetException
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.RequestBody

class ChatViewModel(
    private val repository: ChatRepository,
    private val textToSpeechEngine: TextToSpeechEngine
) : ViewModel() {
    var chatListener: ChatListener? = null
    var newMessageText: String? = null
    var isRecording: Boolean = false
    var lensFacing = CameraSelector.LENS_FACING_BACK
    val audioRecord = AudioRecord()

    val chats by lazyDeferred {
        repository.getChat()
    }

    fun changeLens(){
        lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        chatListener?.onChangeLens(lensFacing)
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
            sendAudio(
                RequestBody.create(MediaType.parse("audio/*"),audioRecord.uploadFile())
            )
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

    fun sendAudio(requestBody: RequestBody){
        Coroutines.main {
            try {
                val responseTTS = repository.sendAudio("id-ID",requestBody)
                Log.d("AudioRecord", "sendAudio: $responseTTS" )
//                responseTTS?.let {
//                    if (it.data?.length!! > 0){
////                        speechChat(newMessageText!!)
////                        repository.saveChat(it.data)
//                        chatListener?.onSendSuccess("Berhasil mengambil audio pesan")
//                        newMessageText = null
//                        return@main
//                    }
//                }
            }catch (e: ApiException){
                Log.e("AudioRecord", "sendAudio: $e" )
            }catch (e:NoInternetException){
                Log.e("AudioRecord", "sendAudio: $e" )
            }
        }
    }
}