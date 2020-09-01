package br.osasco.optimus.pdfsigner

import javafx.concurrent.Task
import sun.security.pkcs11.SunPKCS11

import java.security.KeyStore
import java.security.Provider
import java.security.Security

class A3Utils extends Task<Map> {
    String[] libraries

    @Override
    protected Map call() throws Exception {
        updateMessage("Iniciando varredura por bibliotecas instaladas no sistema.")
        File lib
        for (String library in libraries) {
            lib = new File(library)
            updateMessage("Verificando ${library}")
            if (lib.exists()) {
                updateMessage("${library} existe, tentao realizar comunicação com o dispositivo.")
                try {
                    Map data
                    if ((data = checkProviderAndProtocol(library))) {
                        updateMessage("${library} comunicação realizada com sucesso. Não se esqueça da salvar as configurações.")
                        updateValue(data)
                        return data
                    }
                }
                catch (Exception ex) {
                    updateMessage(ex.message)
                }
            } else {
                updateMessage("${library} não econtrada no sistema.")
            }
        }
        updateMessage("Nenhuma biblioteca ou dispositivo compativel foi encontrado no sistema.")
        return null
    }

    private Map checkProviderAndProtocol(String library) {
        Map data
        if ((data = checkWithSlot(library))) {
            return data
        }
        checkWithoutSlot(library)
    }

    private Map checkWithSlot(String library) {
        KeyStore keyStore
        for (int i = 0; i < 10; i++) {
            try {
                String config = "name=SmartCard\nlibrary=${library}\nslot=${i}"
                InputStream data = new ByteArrayInputStream(config.bytes)
                Provider provider = new SunPKCS11(data)
                if ((keyStore = checkProtocol(provider))) {
                    return [provider: provider, keyStore: keyStore, library: library, slot: i]
                }
            }
            catch (Exception ex) {
                updateMessage(ex.message)
            }
        }
        return null
    }

    private Map checkWithoutSlot(String library) {
        KeyStore keyStore
        try {
            String config = "name=SmartCard\nlibrary=${library}"
            InputStream data = new ByteArrayInputStream(config.bytes)
            Provider provider = new SunPKCS11(data)
            if ((keyStore = checkProtocol(provider))) {
                return [provider: provider, keyStore: keyStore, library: library, slot: null]
            }
        }
        catch (Exception ex) {
            updateMessage(ex.message)
        }
        return null
    }

    private KeyStore checkProtocol(Provider provider) {
        try {
            Security.addProvider(provider)
            KeyStore keyStore = KeyStore.getInstance("PKCS11", provider)
            return keyStore
        }
        catch (Exception ex) {
            updateMessage(ex.message)
        }
        return null
    }
}
