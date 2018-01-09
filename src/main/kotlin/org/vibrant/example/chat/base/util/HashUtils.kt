package org.vibrant.example.chat.base.util

import java.security.KeyPair
import java.security.MessageDigest
import java.security.Signature

/**
 * Hashing Utils
 * @from Sam Clarke <www.samclarke.com>
 * @license MIT
 */
object HashUtils {

    private val HEX_CHARS = "0123456789ABCDEF".toCharArray()



//    fun sha512(input: String) = hashString("SHA-512", input)
//
//    fun sha256(input: String) = hashString("SHA-256", input)
//
//    fun sha1(input: String) = hashString("SHA-1", input)



    fun sha512(input: ByteArray) = hashBytes("SHA-512", input)

    fun sha256(input: ByteArray) = hashBytes("SHA-256", input)

    fun sha1(input: ByteArray) = hashBytes("SHA-1", input)


    /**
     * Supported algorithms on Android:
     *
     * Algorithm	Supported API Levels
     * MD5          1+
     * SHA-1	    1+
     * SHA-224	    1-8,22+
     * SHA-256	    1+
     * SHA-384	    1+
     * SHA-512	    1+
     */
//    private fun hashString(_type: String, input: String): String {
//        val HEX_CHARS = "0123456789ABCDEF"
//        val bytes = MessageDigest
//                .getInstance(_type)
//                .digest(input.toByteArray())
//        val result = StringBuilder(bytes.size * 2)
//
//        bytes.forEach {
//            val i = it.toInt()
//            result.append(HEX_CHARS[i shr 4 and 0x0f])
//            result.append(HEX_CHARS[i and 0x0f])
//        }
//
//        return result.toString()
//    }
//
    private fun hashBytes(type: String, input: ByteArray): ByteArray {
        return MessageDigest
                .getInstance(type)
                .digest(input)
    }


    fun signData(data: ByteArray, keyPair: KeyPair): ByteArray {
        val sig = Signature.getInstance("SHA1WithRSA")
        sig.initSign(keyPair.private)
        sig.update(data)
        val signatureBytes = sig.sign()
        sig.initVerify(keyPair.public)
        sig.update(data)
        sig.verify(signatureBytes)
        return signatureBytes
    }

    fun bytesToHex(bytes: ByteArray): String {
        val result = StringBuffer()

        bytes.forEach {
            val octet = it.toInt()
            val firstIndex = (octet and 0xF0).ushr(4)
            val secondIndex = octet and 0x0F
            result.append(HEX_CHARS[firstIndex])
            result.append(HEX_CHARS[secondIndex])
        }

        return result.toString()
    }

//    fun hexToBytes(hex: String): ByteArray {
//
//        val result = ByteArray(hex.length / 2)
//
//        for (i in 0 until hex.length step 2) {
//            val firstIndex = HEX_CHARS.indexOf(hex[i])
//            val secondIndex = HEX_CHARS.indexOf(hex[i + 1])
//
//            val octet = firstIndex.shl(4).or(secondIndex)
//            result[i.shr(1)] = octet.toByte()
//        }
//
//        return result
//    }
}