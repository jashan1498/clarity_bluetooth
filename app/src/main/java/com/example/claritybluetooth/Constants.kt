package com.example.claritybluetooth

public class Constants {
    companion object {
        const val SOS: String = "SOS"

        const val EOS: String = "EOS"

        const val STATE_NONE = 0 // we're doing nothing

        const val STATE_LISTEN = 1 // now listening for incoming connections

        const val STATE_CONNECTING = 2 // now initiating an outgoing connection

        const val STATE_CONNECTED = 3 // now connected to a remote device

        const val STATE_CONNECTION_FAILED = -1 // now connected to a remote device

        const val STATE_LISTEN_TEXT = "Listening"

        const val STATE_CONNECTING_TEXT = "Connecting"

        const val STATE_CONNECTED_TEXT = "Connected"

        const val STATE_NONE_TEXT = ""

        const val STATE_CONNECTION_FAILED_TEXT = "Connection Failed"

        const val STATE_DATA_LOST = -100

        const val STATE_DATA_COMPLETE = 202


    }
}