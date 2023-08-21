package ru.netology.nework.view

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.android.exoplayer2.ui.PlayerView
import ru.netology.nework.R

fun ImageView.loadAvatar(url: String, vararg transforms: BitmapTransformation = emptyArray()) =
    Glide.with(this)
        .load(url)
        .timeout(10_000)
        .error(R.drawable.user_icon)
        .transform(*transforms)
        .into(this)

fun ImageView.loadCircleCropAvatar(
    url: String,
    vararg transforms: BitmapTransformation = emptyArray()
) = loadAvatar(url, CircleCrop(), *transforms)

fun ImageView.loadImageAttachment(url: String) =
    Glide.with(this)
        .load(url)
        .timeout(10_000)
        .error(R.drawable.error_icon)
        .into(this)