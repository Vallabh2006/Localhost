package com.localhost.tunnel

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.security.auth.x500.X500Principal

object SelfSignedCertHelper {
    private const val ALIAS = "localhost_proxy_cert"

    fun getOrCreateSSLContext(): SSLContext {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            if (!keyStore.containsAlias(ALIAS)) {
                val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
                val spec = KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY or KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setCertificateSubject(X500Principal("CN=localhost, O=Localhost Mobile Server, OU=Tunnel"))
                    .setCertificateSerialNumber(BigInteger.valueOf(System.currentTimeMillis()))
                    .setCertificateNotBefore(Date(System.currentTimeMillis() - 86400000L))
                    .setCertificateNotAfter(Date(System.currentTimeMillis() + 31536000000L * 5))
                    .setKeySize(2048)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                    .build()
                kpg.initialize(spec)
                kpg.generateKeyPair()
            }

            val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            kmf.init(keyStore, null)

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(kmf.keyManagers, null, SecureRandom())
            return sslContext
        } catch (_: Exception) {
            return SSLContext.getDefault()
        }
    }

    fun createCert(keyPair: KeyPair): X509Certificate {
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)
        val cert = ks.getCertificate(ALIAS)
        if (cert is X509Certificate) {
            return cert
        }
        throw IllegalStateException("Failed to load X509Certificate from AndroidKeyStore")
    }
}
