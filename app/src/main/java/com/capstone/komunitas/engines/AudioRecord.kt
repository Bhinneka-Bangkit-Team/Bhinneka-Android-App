package com.capstone.komunitas.engines

import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import okhttp3.ResponseBody
import java.io.*

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

    fun saveIntoAudio(responseBody: ResponseBody):Boolean{
        try {
            var finalFile  =File(Environment.getExternalStorageDirectory().absolutePath+"/recording_download.3gp")
            var inputStream:InputStream? = null
            var outputStream:OutputStream? =null

            try {
                val fileReader = ByteArray(1024)
                var fileSize = responseBody.contentLength()
                var fileSizeDownloaded = 0

                 inputStream = responseBody.byteStream()
                outputStream = FileOutputStream(finalFile)
                while (true){
                    var read = inputStream.read(fileReader)
                    if (read == -1){
                        break
                    }
                    outputStream.write(fileReader,0,read)
                    fileSizeDownloaded += read

                    Log.d(TAG_AUDIO, "File Download: " + fileSizeDownloaded + " of " + fileSize)
                }
                outputStream.flush()
                return true
            }catch (e:IOException){
                Log.e(TAG_AUDIO, "File Download: $e")
               return false
            }finally {
                if (inputStream != null){
                    inputStream.close()
                }

                if (outputStream!=null){
                    outputStream.close()
                }
            }
        }catch (e:IOException){
                return false
        }
    }

    companion object{
        private const val TAG_AUDIO="AudioRecord"
    }

}