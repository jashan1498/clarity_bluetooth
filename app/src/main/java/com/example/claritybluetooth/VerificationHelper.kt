package com.example.claritybluetooth

import com.example.claritybluetooth.Constants.Companion.STATE_DATA_COMPLETE
import com.example.claritybluetooth.Constants.Companion.STATE_DATA_LOST

class VerificationHelper {

    companion object {
        fun verifyInfo(array: ByteArray, bytes: Int): Int {
            val startCheck = Constants.SOS.toByteArray()
            val endCheck = Constants.EOS.toByteArray()

            for (x in 0..2) {
                if (array[x] != startCheck[x])
                    return STATE_DATA_LOST
                if (array[bytes - 1 - x] != endCheck[endCheck.size - 1 - x])
                    return STATE_DATA_LOST
            }
            return STATE_DATA_COMPLETE
        }


        fun getByteArray(string: String): ByteArray = "${Constants.SOS}${string}${Constants.EOS}".trim().toByteArray()
    }

}