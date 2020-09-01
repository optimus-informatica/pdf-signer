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

import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.StampingProperties
import com.itextpdf.signatures.*

import java.security.KeyStore
import java.security.PrivateKey
import java.security.Provider
import java.security.cert.Certificate

class PDFSigner {
    ConfigObject config
    String reason, location
    File outFile, inFile
    KeyStore keyStore
    Provider provider
    InputStream inputStream
    Map<String, String> dn

    private OutputStream outStream
    private PdfReader reader
    private PdfSigner signer

    PDFSigner(Map map) {
        config = map['config'] as ConfigObject
        keyStore = map['keyStore'] as KeyStore
        provider = map['provider'] as Provider
        dn = map['dn'] as Map<String, String>
        reason = config.signature.reason
        location = config.signature.location
    }

    /**
     * Prepara o documento para ser assinado
     * @param map
     * @return
     * @throws Exception
     */
    Map prepare(Map map) throws Exception {
        PrivateKey key = map['privateKey'] as PrivateKey
        Certificate[] chain = map['chain'] as Certificate[]

        if (map['inputStream']) {
            inputStream = map['inputStream'] as InputStream
        } else {
            inputStream = new FileInputStream(map['inFile'] as File)
        }
        outFile = map['outFile'] as File
        init()

        IExternalSignature pks = new PrivateKeySignature(key, DigestAlgorithms.SHA256, provider.getName())
        IExternalDigest digest = new ProviderDigest(provider.getName())
        return [privateKeySignature: pks, providerDigest: digest, chain: chain]
    }

    /**
     * Assina o documento preparado
     * @param map
     * @return
     * @throws Exception
     */
    Map signer(Map map) throws Exception {
        IExternalSignature pks = map['privateKeySignature'] as IExternalSignature
        IExternalDigest digest = map['providerDigest'] as IExternalDigest
        Certificate[] chain = map['chain'] as Certificate[]

        signer.signDetached(digest, pks, chain, null, null, null, 0, PdfSigner.CryptoStandard.CMS)
        signer.finalize()
        reader.close()
        inputStream.close()
        outStream.close()
        return [reader: reader, inputStream: inputStream, outputStream: outStream]
    }

    private void openOutFile() throws IOException {
        outStream = new FileOutputStream(outFile)
    }

    private void init() throws Exception {
        reader = new PdfReader(inputStream)
        openOutFile()
        signer = new PdfSigner(reader, outStream, new StampingProperties())
        signer.setFieldName("assinatura")
        setAppearance()
    }

    /**
     * Gera a geometria da assinatura
     * @return Rectangle
     */
    private Rectangle signatureGeometry() {
        PdfDocument document = signer.document

        // Altura e Largura da ultima pagina do documento.
        float widthPage = document.lastPage.pageSize.width
        float heightPage = document.lastPage.pageSize.height

        // Altura e Largura da caixa da assinatura
        float width = widthPage / 1.5f
        float height = heightPage / 20f

        // Posição X e Y da assinatura *NOTE positionY 0 = BOTTOM
        float y = height / 2
        float x = (widthPage - width) / 2f

        // Retorna um novo Rectangle com as dimensões e posição da assinatura
        new Rectangle(x, y, width, height)
    }

    /**
     * Seta a aparecia da assinatura
     * @return PdfSignatureAppearance
     */
    private PdfSignatureAppearance setAppearance() {
        PdfSignatureAppearance appearance = signer.signatureAppearance

        // Pega o index da ultima pagina do documento
        int page = signer.document.getPageNumber(signer.document.lastPage)

        appearance.reason = reason                          // Seta o motivo da assinatura
        appearance.location = location                      // Seta o local da assinatura
        appearance.setLayer2Text(getStrSignature())         // Escreve a assinatura
        appearance.setLayer2FontColor(ColorConstants.RED)   // Seta a cor do texto da assinatura RED
        appearance.reuseAppearance = false
        appearance.pageRect = signatureGeometry()           // Seta a geometria da assinatura
        appearance.pageNumber = page                        // Define a pagina onde vai ser escrita a assinatura

        return appearance
    }

    private String getStrSignature() {
        Date date = new Date()
        String dateStr = date.format("dd/MM/yyyy HH:mm:ss z")
        new String("Assinado por: ${dn['CN']} | Data: ${dateStr}\nMotivo: ${reason} | Local: ${location}")
    }


}
