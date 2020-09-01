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

import java.security.KeyStore
import java.security.PrivateKey
import java.security.Provider
import java.security.cert.Certificate

class TestSignerTask extends Task<File> {
    char[] pin

    def PDFSignerTask(Map map) {
        pin = map['pin'] as char[]
    }

    @Override
    protected File call() throws Exception {
        AppConfigs appConfigs = new AppConfigs()

        try {
            updateMessage("Carregando configurações")
            ConfigObject config = appConfigs.loadConfigs()
            String alias, reason, location

            // Recupera as informações do arquivo de configuração
            if (!(alias = config.signature.certificate.alias)) {
                updateMessage("Não foi possivel resolver o alias")
                return null
            }
            reason = config.signature.reason
            location = config.signature.location

            // Inicia o Store
            updateMessage("Iniciando o Store")
            StoreUtils utils = new StoreUtils([config: config, pin: pin])
            KeyStore keyStore = utils.startStore()
            Provider provider = utils.provider ?: keyStore.provider

            // Pega a chave privada do certificado
            PrivateKey key = (PrivateKey) keyStore.getKey(alias, pin)

            // Pega a cadeia de validação do certificado
            Certificate[] chain = keyStore.getCertificateChain(alias)

            // Recupera as informações do SubjectDN
            Map dn = StoreUtils.readDNInfos((Certificate) chain[0])

            // Inicia a classe de assinatura de documento
            updateMessage("Iniciando o processo de assinatura")
            PDFSigner signer = new PDFSigner(config: config, keyStore: keyStore, provider: provider, reason: reason, location: location, dn: dn)


            // Cria um arquivo temporário e abre para escrita
            updateMessage("Criando arquivo temporário")
            File outputFile = File.createTempFile("signed-", ".pdf")
            outputFile.deleteOnExit()
            updateMessage("Abrindo arquivo temporário ${outputFile.absolutePath}")


            // Preparando documento para a assinatura
            updateMessage("Preparando documento para a assinatura.")
            Map prepared = signer.prepare(inputStream: getClass().getResourceAsStream("/test.pdf"), outFile: outputFile, privateKey: key, chain: chain)

            // Assina o documento
            updateMessage("Assinando o documento")
            signer.signer(prepared)
            updateMessage("Documento assinado com sucesso em ${outputFile.absolutePath}")
            updateMessage("Fechando os arquvios...")
            updateMessage("Abrindo o documento ${outputFile.absolutePath}")
            sleep(1000)
            return outputFile

        }
        catch (Exception ex) {
            updateMessage(ex.localizedMessage)
        }

        return null
    }
}
