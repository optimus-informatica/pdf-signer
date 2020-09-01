package br.osasco.optimus.pdfsigner

import javafx.scene.control.Label

class AppConfigs {
    Label status = new Label()
    File configFile = new File(System.properties["user.home"] + "/config.groovy")

    def loadConfigs() {
        ConfigSlurper slurper = new ConfigSlurper()
        if (!configFile.exists()) {
            status.text = "Não foi possivel localizar o arquivo de configuração criando um novo."
            try {
                makeNewFile()
                status.text = "Novo arquivo de configuração criando em ${configFile.absolutePath}"
            }
            catch (IOException ex) {
                status.text = "Erro ao tentar criar o arquivo de configuração ${ex.message}"
            }
        }
        status.text = "Carregando configurações de ${configFile.absolutePath}"
        slurper.parse(configFile.toURL())
    }

    private void makeNewFile() {
        InputStream is = getClass().getResourceAsStream("/config.groovy")
        byte[] buffer = new byte[is.available()]
        is.read(buffer)
        OutputStream os = new FileOutputStream(configFile, false)
        os.write(buffer)
        os.close()
        is.close()
    }

}
