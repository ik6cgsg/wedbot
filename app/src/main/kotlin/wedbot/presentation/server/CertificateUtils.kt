package wedbot.presentation.server

import wedbot.SystemProperties
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore

object CertificateUtils {
    private const val keyStorePath = "res/keystore.jks"
    val keyStorePassword = SystemProperties.keystorePassword.toCharArray()
    const val keyAlias = "wedbot"
    const val certPath = "res/pub.pem"

    val keyStoreFile: File
        get() = File(keyStorePath).let { file ->
            if (file.exists() || file.isAbsolute) {
                file
            } else {
                File(".", keyStorePath).absoluteFile
            }
        }

    val certPathFile: File
        get() = File(certPath).let { file -> 
            if (file.exists() || file.isAbsolute) {
                file
            } else {
                File(".", certPath).absoluteFile
            }
        }

    val keyStore: KeyStore
        get() = KeyStore.getInstance("JKS").apply {
            FileInputStream(keyStoreFile).use {
                load(it, keyStorePassword)
            }
        }
}
