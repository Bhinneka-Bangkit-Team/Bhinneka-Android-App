package com.capstone.komunitas.engines

import android.media.MediaRecorder
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.*
import java.lang.Exception

class AudioRecord{
    private lateinit var recorder:MediaRecorder
    private lateinit var fileOutput:String

     fun startRecording(){
        recorder = MediaRecorder()
        fileOutput = Environment.getExternalStorageDirectory().absolutePath+"/recording.3gp"
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        recorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB)

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

     fun readFile(){
        var inputStream: InputStream? = null
        try {
            inputStream = BufferedInputStream(FileInputStream(fileOutput))
        }catch (e:FileNotFoundException){
            e.printStackTrace()
        }

        finally {
            if (inputStream!=null){
                try {
                    inputStream.close()
                }catch (e:IOException){
                    e.printStackTrace()
                }
            }
        }
    }

    fun uploadFile():File{
        return File(fileOutput)
    }

    companion object{
        private const val TAG_AUDIO="AudioRecord"
    }

}