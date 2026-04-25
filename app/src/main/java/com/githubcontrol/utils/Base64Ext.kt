package com.githubcontrol.utils

import android.util.Base64

fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
fun String.fromBase64(): ByteArray = Base64.decode(this.replace("\n", ""), Base64.DEFAULT)
