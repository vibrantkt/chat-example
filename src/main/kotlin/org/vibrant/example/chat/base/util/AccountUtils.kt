package org.vibrant.example.chat.base.util

import java.security.*
import java.security.KeyPair
import java.io.ObjectOutputStream
import java.io.ByteArrayOutputStream
import java.security.KeyPairGenerator
import java.io.ObjectInputStream
import java.io.ByteArrayInputStream






object AccountUtils {


    fun generateKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(1024)
        return kpg.genKeyPair()
    }

    fun signData(data: String, keyPair: KeyPair): ByteArray {
        val byteData = data.toByteArray(charset("UTF8"))
        val sig = Signature.getInstance("SHA1WithRSA")
        sig.initSign(keyPair.private)
        sig.update(byteData)
        val signatureBytes = sig.sign()
        sig.initVerify(keyPair.public)
        sig.update(byteData)
        sig.verify(signatureBytes)

        return signatureBytes
    }


    fun verifySignature(data: String, publicKey: PublicKey, signature: ByteArray): Boolean {
        val sig = Signature.getInstance("SHA1WithRSA")
        sig.initVerify(publicKey)
        sig.update(data.toByteArray(charset("UTF8")))
        return sig.verify(signature)
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


    fun serializeKeyPair(keyPair: KeyPair): ByteArray {
        val b = ByteArrayOutputStream()
        val o = ObjectOutputStream(b)
        o.writeObject(keyPair)
        return b.toByteArray()
    }


    fun deserializeKeyPair(byteArray: ByteArray): KeyPair{
        val bi = ByteArrayInputStream(byteArray)
        val oi = ObjectInputStream(bi)
        return oi.readObject() as KeyPair
    }
}