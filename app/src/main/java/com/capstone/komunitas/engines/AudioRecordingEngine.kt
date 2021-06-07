package com.capstone.komunitas.engines

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import android.widget.ImageButton
import com.capstone.komunitas.R
import java.io.File

class AudioRecordingEngine(private val context: Context){
    private lateinit var recorder:MediaRecorder
    private lateinit var fileOutput:String

     fun startRecording(){
        recorder = MediaRecorder()
        fileOutput = context.filesDir.absolutePath+"/recording.3gp"
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

        try {
            recorder.setOutputFile(fileOutput)
        }catch (e:Exception){
            Log.e(TAG_AUDIO, "startRecording: OutputFailed $e" )
        }

        try{
            recorder.prepare()
            recorder.start()
        }catch (e:Exception){
            e.printStackTrace()
            Log.e(TAG_AUDIO, "startRecording: Failed to start! $e" )
        }
    }

     fun stopRecording(){
         try{
             if(recorder!=null){
                 recorder.stop()
                 recorder.reset()
                 recorder.release()
                 uploadFile()
             }
         }catch (e:Exception){
             e.printStackTrace()
             Log.e(TAG_AUDIO, "startRecording: Failed to stop! $e" )
         }
    }

    fun uploadFile():File{
        return File(fileOutput)
    }

    fun replayAudio(audioUrl: String, replayChat: ImageButton){
        val mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(audioUrl)
            prepare()
            setOnCompletionListener {
                replayChat.setImageResource(R.drawable.ic_play_softerblue)
            }
            start()
        }
    }

    fun playAudio(audioUrl: String){
        val mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(audioUrl)
            prepare()
            start()
        }
    }

    companion object{
        private const val TAG_AUDIO="AudioRecord"
    }
}