/*
 *     Copyright © 2020 Optimus Informatica <http://optimus.osasco.br>
 *
 *     This file is part of PDFSigner.
 *
 *     PDFSigner is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PDFSigner is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Foobar.  If not, see <https://www.gnu.org/licenses/>
 *
 *     Este arquivo é parte do programa PDFSigner
 *
 *     PDFSigner é um software livre; você pode redistribuí-lo e/ou
 *     modificá-lo dentro dos termos da Licença Pública Geral GNU como
 *     publicada pela Free Software Foundation (FSF); na versão 3 da
 *     Licença, ou (a seu critério) qualquer versão posterior.
 *
 *     Este programa é distribuído na esperança de que possa ser útil,
 *     mas SEM NENHUMA GARANTIA; sem uma garantia implícita de ADEQUAÇÃO
 *     a qualquer MERCADO ou APLICAÇÃO EM PARTICULAR. Veja a
 *     Licença Pública Geral GNU para maiores detalhes.
 *
 *     Você deve ter recebido uma cópia da Licença Pública Geral GNU junto
 *     com este programa, Se não, veja <http://www.gnu.org/licenses/>.
 */

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
        certificate.subjectX500Principal.name.split(',').each {
            String[] pair = it.split('=')
            if (pair.length == 2) {
                map.put(pair[0], pair[1])
            }
        }
        return map
    }
}
