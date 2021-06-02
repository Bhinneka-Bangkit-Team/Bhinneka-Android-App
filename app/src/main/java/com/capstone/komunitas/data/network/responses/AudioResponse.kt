package com.capstone.komunitas.data.network.responses

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

class AudioResponse (
	val statusCode: Int? = null,
	val message: String? = null,
	val data: String? = null,
	val error: String? = null
)