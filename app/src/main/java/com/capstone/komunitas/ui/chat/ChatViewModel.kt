package com.capstone.komunitas.ui.chat

import android.content.Context
import android.util.Log
import android.widget.ImageButton
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import com.capstone.komunitas.R
import com.capstone.komunitas.data.repositories.ChatRepository
import com.capstone.komunitas.engines.AudioRecordingEngine
import com.capstone.komunitas.engines.TextToSpeechEngine
import com.capstone.komunitas.util.ApiException
import com.capstone.komunitas.util.Coroutines
import com.capstone.komunitas.util.NoInternetException
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody

class ChatViewModel(
    private val repository: ChatRepository,
    private val textToSpeechEngine: TextToSpeechEngine,
    context: Context
) : ViewModel() {
    var chatListener: ChatListener? = null
    var newMessageText: ObservableField<String> = ObservableField<String>("")
    var audioMessageText: String? = null
    var isRecording: Boolean = false
    val audioRecord = AudioRecordingEngine(context.applicationContext)

    val chats by lazyDeferred {
        repository.getChat()
    }

    fun changeLens() {
        chatListener?.onChangeLens()
    }

    fun <T> lazyDeferred(block: suspend CoroutineScope.() -> T): Lazy<Deferred<T>> {
        return lazy {
            GlobalScope.async(start = CoroutineStart.LAZY) {
                block.invoke(this)
            }
        }
    }

    fun onRecordPressed() {
        isRecording = !isRecording
        chatListener?.onRecordPressed(isRecording)
        if (isRecording) {
            audioRecord.startRecording()
        } else {
            audioRecord.stopRecording()
            Log.d("audioRecord", "onRecordPressed: " + audioRecord.uploadFile())
            val requestBody =
                RequestBody.create(MediaType.parse("audio/*"), audioRecord.uploadFile())
            val lang = RequestBody.create(MediaType.parse("text/plain"), "id-ID")
            val body = MultipartBody.Part.createFormData(
                "file",
                audioRecord.uploadFile().name,
                requestBody
            )
            sendAudio(body, lang)
        }
    }

    fun speechChat(text: String?) {
        textToSpeechEngine.textToSpeech(text!!)
    }

    fun sendMessagePressed() {
        chatListener?.onSendStarted()
        // Username or password is empty
        if (newMessageText.get()!!.isEmpty()) {
            chatListener?.onSendFailure("Pesan tidak boleh kosong")
            return
        }
        sendMessage(newMessageText.get()!!, 0)
        newMessageText.set("")
    }

    fun sendMessage(messageText: String, isSpeaker: Int) {
        if (messageText.isEmpty()) {
            return
        }
        // Call api via kotlin coroutines
        Coroutines.main {
            try {
                val chatResponse = repository.sendChat(messageText, isSpeaker)
                chatResponse?.let {
                    if (it.data!!.size > 0) {
                        if (isSpeaker == 0) {
                            downloadAudio(messageText)
                        }
                        repository.saveChat(it.data)
                        chatListener?.onSendSuccess("Berhasil mengambil pesan")
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

    fun sendAudio(body: MultipartBody.Part, lang: RequestBody) {
        chatListener?.onSendStarted()
        Coroutines.main {
            try {
                val responseTTS = repository.sendAudio(body, lang)
                Log.d("AudioRecord", "sendAudio: ${responseTTS.message}")
                responseTTS?.let {
                    if (it.statusCode?.equals(200) == true) {
                        chatListener?.onSendSuccess("Berhasil mengambil audio pesan ${it.data}")
                        if (it.data!!.length > 0) {
                            sendMessage(it.data!!, 1)
                        }
                        return@main
                    }
                }
                chatListener?.onSendFailure("Terjadi kesalahan")
            } catch (e: ApiException) {
                Log.e("AudioRecord", "sendAudio: $e")
            } catch (e: NoInternetException) {
                Log.e("AudioRecord", "sendAudio: $e")
            }
        }
    }

    fun listenAudio(text: String?, replayChat: ImageButton) {
        chatListener?.onGetStarted()
        replayChat.setImageResource(R.drawable.ic_pause_softerblue)
        Coroutines.main {
            try {
                val responseSTT = repository.downloadAudio(text!!)
                Log.d("AudioRecord", "sendAudio: ${responseSTT.message}")
                responseSTT?.let {
                    if (it.statusCode?.equals(200) == true) {
                        Log.d("AudioRecord", "it.data: ${it.data}")
                        audioRecord.replayAudio(it.data!!, replayChat)
                        chatListener?.onSendSuccess("Berhasil mengambil audio pesan : ${it.message}")
                    }
                }
            } catch (e: ApiException) {
                Log.e("AudioRecord", "downloadAudio: $e")
            } catch (e: NoInternetException) {
                Log.e("AudioRecord", "downloadAudio: $e")
            }
        }
    }

    fun downloadAudio(text: String?) {
        Log.d("AUDIO:", text!!)
        chatListener?.onGetStarted()
        Coroutines.main {
            try {
                val responseSTT = repository.downloadAudio(text)
                Log.d("AudioRecord", "sendAudio: ${responseSTT.message}")
                responseSTT?.let {
                    if (it.statusCode?.equals(200) == true) {
                        Log.d("AudioRecord", "it.data: ${it.data}")
                        audioRecord.playAudio(it.data!!)
                        chatListener?.onSendSuccess("Berhasil mengambil audio pesan : ${it.message}")
                    }
                }
            } catch (e: ApiException) {
                Log.e("AudioRecord", "downloadAudio: $e")
            } catch (e: NoInternetException) {
                Log.e("AudioRecord", "downloadAudio: $e")
            }
        }
    }
}