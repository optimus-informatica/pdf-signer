package br.osasco.optimus.pdfsigner

import org.bouncycastle.jce.provider.BouncyCastleProvider
import sun.security.mscapi.SunMSCAPI
import sun.security.pkcs11.SunPKCS11

import java.security.KeyStore
import java.security.Provider
import java.security.Security
import java.security.cert.X509Certificate

class StoreUtils {
    ConfigObject config
    char[] pin
    Provider provider


    StoreUtils(Map map) {
        config = map['config'] as ConfigObject
        pin = map['pin'] as char[]
    }

    KeyStore startStore() {
        switch (config.signature.certificate.type) {
            case "a1":
                a1Start()
                break
            case "a3":
                a3Start()
                break
            default:
                win32Start()
        }
    }

    InputStream a3ConfigData() throws Exception {
        String library = config.pkcs11.library
        Integer slot = config.pkcs11.slot
        String name = config.pkcs11.name ?: 'SmartCard'
        String strConfig = slot == null ? "name=${name}\nlibrary=${library}" : "name=${name}\nlibrary=${library}\nslot=${slot}"
        new ByteArrayInputStream(strConfig.bytes)
    }

    KeyStore a3Start() throws Exception {
        provider = new SunPKCS11(a3ConfigData())
        Security.addProvider(provider)
        KeyStore keyStore = KeyStore.getInstance("PKCS11")
        keyStore.load(null, pin)
        return keyStore
    }

    KeyStore a1Start() throws Exception {
        provider = new BouncyCastleProvider()
        Security.addProvider(provider)
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        File certFile = new File(config.signature.certificate.filename)
        keyStore.load(new FileInputStream(certFile), pin)
        return keyStore
    }

    KeyStore win32Start() throws Exception {
        provider = new SunMSCAPI()
        Security.addProvider(provider)
        KeyStore keyStore = KeyStore.getInstance("Windows-MY", provider)
        keyStore.load(null)
        return keyStore
    }

    static Map<String, String> readDNInfos(X509Certificate certificate) {
        Map<String, String> map = new HashMap()
        println(certificate.subjectX500Principal.name)
        certificate.subjectX500Principal.name.split(',').each {
            String[] pair = it.split('=')
            if (pair.length == 2) {
                map.put(pair[0], pair[1])
            }
        }
        return map
    }
}
