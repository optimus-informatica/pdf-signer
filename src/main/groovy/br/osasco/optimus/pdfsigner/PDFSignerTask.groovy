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
import javafx.scene.control.ProgressIndicator

import java.security.KeyStore
import java.security.PrivateKey
import java.security.Provider
import java.security.cert.Certificate

class PDFSignerTask extends Task<File> {
    private List<File> inFiles, outFiles
    private int total = 0
    private int processed = 0
    char[] pin
    private String title = MainApp.mainStage.title

    PDFSignerTask(Map map) {
        pin = map['pin'] as char[]
    }

    int getTotal() {
        return total
    }

    int getProcessed() {
        return processed
    }

    private void listFiles(File folderIn, String folderOut, String subDir) {
        folderIn.listFiles()?.each {
            updateMessage("Lendo diretório ${it.absolutePath}")
            if (it.isFile() && it.name.toLowerCase().endsWith(".pdf")) {
                inFiles.add(it)
                String outFilenae = "${folderOut}${subDir}${it.name}"
                outFiles.add(new File(outFilenae))
            } else if (it.isDirectory()) {
                listFiles(it, folderOut, "${subDir}${it.name}/")
            }
        }
    }

    private setUpdateProgress(double current) {
        double count = (double) inFiles.size()
        double progress = (current / count) * 100
        updateProgress(progress, 100.0)
    }

    @Override
    protected File call() throws Exception {
        updateTitle("${title} - Processando pastas.")
        AppConfigs appConfigs = new AppConfigs()
        inFiles = new ArrayList<>()
        outFiles = new ArrayList<>()
        double current = 1.0

        try {
            updateMessage("Carregando configurações")
            updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, 100.0)
            ConfigObject config = appConfigs.loadConfigs()
            String alias, reason, location

            // Recupera as informações do arquivo de configuração
            if (!(alias = config.signature.certificate.alias)) {
                updateMessage("Não foi possivel resolver o alias")
                return null
            }
            reason = config.signature.reason
            location = config.signature.location
            File folderIn = new File(config.signature.folder.input)
            String folderOutName = config.signature.folder.output

            // Inicia o Store
            updateMessage("Iniciando o Store")
            StoreUtils utils = new StoreUtils([config: config, pin: pin])
            KeyStore keyStore = utils.startStore()
            Provider provider = utils.provider ?: keyStore.provider

            // Pega a chave privada do certificado
            PrivateKey key = keyStore.getKey(alias, pin) as PrivateKey

            // Pega a cadeia de validação do certificado
            Certificate[] chain = keyStore.getCertificateChain(alias)

            // Recupera as informações do SubjectDN
            Map dn = StoreUtils.readDNInfos(keyStore.getCertificate(alias))

            // Inicia a classe de assinatura de documento
            updateMessage("Iniciando o processo de assinatura")
            PDFSigner signer = new PDFSigner(config: config, keyStore: keyStore, provider: provider, reason: reason, location: location, dn: dn)

            // Lista o diretório de entrada
            updateMessage("Obtendo lista de arquivos a serem assinados.")
            listFiles(folderIn, folderOutName, '/')

            updateMessage("Foram encontrados um total de ${inFiles.size()} arquivos")
            updateProgress(0, 100)


            // Lê a lista de arquivos
            total = inFiles.size()
            for (int i = 0; i < total; i++) {
                File inFile = inFiles[i]
                File outFile = outFiles[i]

                // Verifica se existe a pasta de destino, se não existir cria
                if (!outFile.parentFile.exists()) {
                    updateMessage("Criando diretório de destino ${outFile.absolutePath}")
                    outFile.parentFile.mkdirs()
                }

                // Preparando documento para a assinatura
                updateMessage("Preparando documento para a assinatura.")
                Map prepared = signer.prepare(inFile: inFile, outFile: outFile, privateKey: key, chain: chain)

                // Assina o documento
                updateMessage("Assinando o documento ${inFile.absolutePath}")
                signer.signer(prepared)

                updateMessage("Documento ${inFile.absolutePath}")

                setUpdateProgress(current)
                current++
                processed++
                updateTitle("${title} assinando ${processed} de ${total}")
            }
        }
        catch (Exception ex) {
            println(ex)
            updateMessage(ex.localizedMessage)
        }
        updateTitle("${title} - Todos os documentos foram processados.")

        return null
    }
}
