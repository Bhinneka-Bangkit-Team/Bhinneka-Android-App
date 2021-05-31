package com.capstone.komunitas.data.network

import android.util.Log
import com.capstone.komunitas.util.ApiException
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response
import java.lang.StringBuilder

abstract class SafeApiRequest {
    suspend fun<T: Any> apiRequest(call: suspend() -> Response<T>) : T{
        val response = call.invoke()

        if(response.isSuccessful){
            return response.body()!!
        }else{
            val error = response.errorBody()?.string()
            val message = StringBuilder()
            error?.let{
                try{
                    message.append(JSONObject(it).get("message"))
                    Log.e("SafeApiRequest", "apiRequest: $it" )
                    message.append(JSONObject(it).get("\n"))
                }catch(e: JSONException){
                    Log.e("SafeApiRequest", "apiRequest: $e" )

                }

            }
            message.append("Error Code: ${response.code()}")
            throw ApiException(message.toString())
        }
    }
}