package com.papa.fr.football.common.extensions

import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.ImageView

fun ImageView.setImageBase64(base64: String?) {
    if (base64.isNullOrBlank()) {
        setImageDrawable(null)
        return
    }

    val decodedBytes = runCatching { Base64.decode(base64, Base64.DEFAULT) }.getOrNull()
    if (decodedBytes == null) {
        setImageDrawable(null)
        return
    }

    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    if (bitmap != null) {
        setImageBitmap(bitmap)
    } else {
        setImageDrawable(null)
    }
}
