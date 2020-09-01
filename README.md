# pdf-signer
Assinador de documentos PDF
Aplicação para assinar documentos PDF em lote.

Tipos de certitificados disponivel:

* A1 PFX, CRT e PEM
* A3 Token e SmartCard
* Win32 Store

Protocolos A3
* PKCS#11
* SunMSCAPI

#### Assinar documento PDF
```
import br.osasco.optimus.pdfsigner.StoreUtils
import br.osasco.optimus.pdfsigner.PDFSigner

import java.security.KeyStore
import java.security.PrivateKey
import java.security.Provider
import java.security.cert.Certificate

package org.exemple

class MainClass {
    void main(String[] args) {
        // Configuração do objeto
        ConfigObject config = new ConfigObject()
        config.signature.certificate.type = "a1" // Tipos disponiveis a1, a3 e em windows win32
        config.signature.certificate.filename = '/home/usuario/certificado.crt'
        config.signature.certificate.alias = "1"
        config.signature.reason = "Motivo"
        config.signature.location = "Osasco/SP"

        // Senha do certificado
        String password = "123456"
        char[] pin = password.toCharArray()

        // Inicia a classe para carregar o Store
        StoreUtils utils = new StoreUtils(config: config, pin: pin)
        KeyStore keyStore = utils.startStore() // Inicia o Store
        Provider provider = utils.provider
        
        // Informações do certificado
        PrivateKey key = (PrivateKey) keyStore.getKey(alias, pin)
        Certificate[] chain = keyStore.getCertificateChain(alias)
        Map<String, String> dn = StoreUtils.readDNInfos((Certificate) chain[0])
        
        // Instancia a classe de assinatura do PDF
        // Se pretende fazer um loop para assinar varios documentos, faça abaixo dessa instancia
        PDFSigner signer = new PDFSigner(config: config, keyStore: keyStore, provider: provider, reason: reason, location: location, dn: dn)

        File fileIn = new File("/home/user/documento.pdf") // Arquivo de entrada
        File fileOut = new File("/home/user/documento-assinado.pdf") // Arquivo de saida (será criado um novo)
        Map prepared = signer.prepare(inFile: fileIn, outFile: fileOut, privateKey: key, chain: chain) // prepara o documento
        // Assina o documento
        signer.signer(prepared)
    }
}
```

#### Para assinar um documento com PKCS#11

```
    // TODO O CODIGO

    config.signature.certificate.type = 'a3'
    config.pkcs11.library = '/usr/lib/lib-pkcs11.so' // ou .dll
    // Na maioria dos casos, o nome do dispositivo é irrelevante
    // Mas por via das duvidas, consulte informação do token e pegue o nome dele
    config.pkcs11.name = 'SmartCard'
    config.pkcs11.slot = null // null ou numerico de 0 a 9 use keytool para mais informações

    // TODO RESTO DO CODIGO
```

#### Recomendções
É fortemente recomendo você usar, Thread para assinar os documentos, pois a inicialização do Store costuma lever um tempo indeterminado, e pode acarretar em travas no programa.

#### Licence
* OpenGL v3.0 https://www.gnu.org/licenses
