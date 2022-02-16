package com.qompliance.compliancechecker

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.kotlin.logger
import org.erdtman.jcs.JsonCanonicalizer
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import javax.crypto.Cipher

/**
 * Class for generating a signature over the canonicalized JSON representation of an object. Canonicalizes the JSON
 * according to RFC8785 (https://datatracker.ietf.org/doc/html/rfc8785). The UTF-8 byte array of the canonicalized JSON
 * is then hashed using SHA-256. Finally, this hash is encrypted using an RSA-2048 private key. Note that the keys are
 * generated in-memory and live within this object only for demonstration purposes, and the public key is logged. A real
 * implementation should change this and use proper key management, for example by using something like Java KeyStore.
 */
object Signature {

    private val logger = logger()
    private val keyPair: KeyPair
    private val cipher: Cipher

    init {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        keyPair = keyPairGenerator.genKeyPair()
        cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.private)
        logger.info("PUBLIC KEY HEX (${keyPair.public.format}): ${keyPair.public.encoded.toHexString()}")
    }

    fun generate(value: Any): ByteArray {
        val utf8Encoded = parseAndCanonicalizeJson(value)
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(utf8Encoded)
        logger.info("RESULT HASH: ${hash.toHexString()}")
        return cipher.doFinal(hash)
    }

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

    private fun parseAndCanonicalizeJson(value: Any): ByteArray {
        val objectWriter = ObjectMapper().writer().withDefaultPrettyPrinter()
        val json = objectWriter.writeValueAsString(value)
        val canonicalizer = JsonCanonicalizer(json)
        return canonicalizer.encodedUTF8
    }

}