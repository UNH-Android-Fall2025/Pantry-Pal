package com.unh.pantrypalonevo

import android.os.Parcel
import android.os.Parcelable

data class DetectedProduct(
    val name: String,
    val confidence: Float,
    var quantity: Int = 1,
    var approved: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readFloat(),
        parcel.readInt(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeFloat(confidence)
        parcel.writeInt(quantity)
        parcel.writeByte(if (approved) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<DetectedProduct> {
        override fun createFromParcel(parcel: Parcel): DetectedProduct {
            return DetectedProduct(parcel)
        }

        override fun newArray(size: Int): Array<DetectedProduct?> {
            return arrayOfNulls(size)
        }
    }
}
