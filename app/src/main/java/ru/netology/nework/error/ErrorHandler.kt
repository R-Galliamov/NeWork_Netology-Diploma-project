package ru.netology.nework.error

import android.util.Log

class ErrorHandler {
    companion object {
        fun getApiErrorDescriptor(e: ApiError): String {
            return when (e.status) {
                400 -> "Oops! Something's not quite right with your request."
                401 -> "Hold on! You need proper authorization for this."
                403 -> "Sorry, but you're not allowed to access this."
                404 -> "Uh-oh! The thing you're looking for? Nowhere to be found."
                500 -> "Whoops! Our server seems to be having a bad day. Try again later."
                else -> "Hmm, something unexpected happened. Try again later"
            }

        }
    }
}