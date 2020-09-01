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

import javafx.concurrent.Task
import org.bouncycastle.asn1.x509.KeyPurposeId

import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateParsingException
import java.security.cert.X509Certificate

class CertificateListTask extends Task<Map> {
    ConfigObject config
    char[] pin

    CertificateListTask(Map map) {
        config = map['config'] as ConfigObject
        pin = map['pin'] as char[]
    }

    @Override
    protected Map call() throws Exception {
        List<String> aliasesList = new ArrayList<>()
        List<X509Certificate> certificateList = new ArrayList<>()
        try {
            StoreUtils storeUtils = new StoreUtils([config: config, pin: pin])
            KeyStore keyStore = storeUtils.startStore()
            updateMessage("Carregando lista de identidades.")
            Enumeration aliases = keyStore.aliases()
            int i = 0
            while (aliases.hasMoreElements()) {
                def alias = aliases.nextElement()
                println("verificando o alias: ${alias}")
                X509Certificate certificate = keyStore.getCertificate(alias) as X509Certificate
                Certificate[] chain = keyStore.getCertificateChain(alias)
                try {
                    (chain[0] as X509Certificate).checkValidity()
                    checkCertificateUsage(chain[0])
                    aliasesList.add(alias)
                    certificateList.add(certificate)
                    i++
                }
                catch (Exception ex) {
                    println("INFO: ${ex.message}")
                    updateMessage(ex.localizedMessage)
                }
            }
            keyStore.finalize()
            updateMessage("Total de identidades encontradas: ${i}")
        }
        catch (Exception ex) {
            updateMessage(ex.localizedMessage)
        }
        return [aliases: aliasesList, certificates: certificateList]
    }


    /**
     * Log if the certificate is not valid for signature usage. Doing this
     * anyway results in Adobe Reader failing to validate the PDF.
     *
     * @param x509Certificate
     * @throws java.security.cert.CertificateParsingException
     */
    void checkCertificateUsage(X509Certificate x509Certificate) throws CertificateParsingException {
        // Check whether signer certificate is "valid for usage"
        // https://stackoverflow.com/a/52765021/535646
        // https://www.adobe.com/devnet-docs/acrobatetk/tools/DigSig/changes.html#id1
        boolean[] keyUsage = x509Certificate.getKeyUsage()
        if (keyUsage != null && !keyUsage[0] && !keyUsage[1]) {
            // (unclear what "signTransaction" is)
            // https://tools.ietf.org/html/rfc5280#section-4.2.1.3
            println("Certificate key usage does not include " +
                    "digitalSignature nor nonRepudiation")
        }
        List<String> extendedKeyUsage = x509Certificate.getExtendedKeyUsage()
        if (extendedKeyUsage != null &&
                !extendedKeyUsage.contains(KeyPurposeId.id_kp_emailProtection.toString()) &&
                !extendedKeyUsage.contains(KeyPurposeId.id_kp_codeSigning.toString()) &&
                !extendedKeyUsage.contains(KeyPurposeId.anyExtendedKeyUsage.toString()) &&
                !extendedKeyUsage.contains("1.2.840.113583.1.1.5") &&
                // not mentioned in Adobe document, but tolerated in practice
                !extendedKeyUsage.contains("1.3.6.1.4.1.311.10.3.12")) {
            println("Certificate extended key usage does not include " +
                    "emailProtection, nor codeSigning, nor anyExtendedKeyUsage, " +
                    "nor 'Adobe Authentic Documents Trust'")
        }
    }
}
