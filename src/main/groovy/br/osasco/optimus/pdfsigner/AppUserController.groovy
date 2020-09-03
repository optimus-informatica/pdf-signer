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

import javafx.concurrent.WorkerStateEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.layout.VBox
import javafx.stage.Stage

class AppUserController {
    private Stage stage
    private AppConfigs configs
    private ConfigObject config
    private File certificateFile, folderIn, folderOut
    private char[] pin
    private Boolean a1 = false, a3 = false, store = true
    private String alias, title


    @FXML
    VBox rootBox
    @FXML
    PasswordField fieldPassword
    @FXML
    ComboBox comboBoxAliases
    @FXML
    Label statusLabel, logsLabel
    @FXML
    ProgressIndicator progress
    @FXML
    Button buttonSign
    @FXML
    MenuItem menuSave

    void listStore() {
        String password = fieldPassword.text
        if (password.length() < 1 && !store) {
            statusLabel.text = "Por favor, informe a senha/pin do certificado."
            return
        }
        pin = password.toCharArray()

        if (a1) {
            if (certificateFile == null) {
                statusLabel.text = "Por favor, informe o arquivo de certificado e clique em salvar."
                return
            }
            if (!certificateFile.exists()) {
                statusLabel.text = "O arquivo de certificado informado não existe."
                return
            }
        }

        if (a3) {
            String library = config.pkcs11.library
            if (library == null) {
                statusLabel.text = "Biblioteca não definida, clique em configurar, e depois clique em salvar."
                return
            }
            File libraryFile = new File(library)
            if (!libraryFile.exists()) {
                statusLabel.text = "A biblioteca ${libraryFile.absolutePath} não existe."
                return
            }
        }

        CertificateListTask listTask = new CertificateListTask([config: config, pin: pin])
        statusLabel.textProperty().bind(listTask.messageProperty())
        listTask.onSucceeded = new EventHandler<WorkerStateEvent>() {
            @Override
            void handle(WorkerStateEvent event) {
                Map map = listTask.value
                List<String> aliases = map['aliases'] as List<String>
                String alias
                comboBoxAliases.items.clear()
                aliases.each {
                    comboBoxAliases.items.add(it)
                    if ((alias = config.signature.certificate.alias)) {
                        if (alias == it)
                            comboBoxAliases.selectionModel.selectedItem = alias
                        else
                            comboBoxAliases.selectionModel.selectedIndex = 0
                    }
                }
            }
        }
        Thread thread = new Thread(listTask)
        thread.start()
    }

    void initialize() {
        configs = new AppConfigs()
        //configs.status = statusLabel
        config = configs.loadConfigs()

        if (!config.signature.certificate.alias) {
            statusLabel.text = "Pelo amor de deus configura antes, ta de brincadeira neh!?"
        }

        listStore()
    }

    @FXML
    private void selectAlias() {
        if (config.signature.certificate.alias != comboBoxAliases.selectionModel.selectedItem) {
            menuSave.disable = false
        }
        config.signature.certificate.alias = comboBoxAliases.selectionModel.selectedItem
    }

    @FXML
    private void close() {
        System.exit(0)
    }

    @FXML
    private void about() {
        Parent root = FXMLLoader.load(getClass().getResource("/about.fxml"))
        Stage stage = new Stage()
        stage.icons.add(new Image(getClass().getResourceAsStream("/signature-solid.png")))
        stage.setTitle("PDFSigner - Sobre")
        Scene scene = new Scene(root)
        stage.scene = scene
        stage.show()
    }

    @FXML
    private void signer() {
        if (!config.signature.certificate.alias) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "ERRO", ButtonType.OK)
            alert.contentText = "Brincadeira, além de não configurar, ainda quer executar sem selecionar o certificado, pra apredner, vou fechar o programa."
            alert.showAndWait()
            System.exit(1)
        }

        String password = fieldPassword.text
        pin = password.toCharArray()
        PDFSignerTask task = new PDFSignerTask(pin: pin)
        progress.progress = ProgressIndicator.INDETERMINATE_PROGRESS
        progress.visible = true
        logsLabel.text = "Inciando..."
        logsLabel.visible = true
        logsLabel.textProperty().bind(task.messageProperty())
        progress.progressProperty().bind(task.progressProperty())
        MainApp.mainStage.titleProperty().bind(task.titleProperty())
        task.onSucceeded = new EventHandler<WorkerStateEvent>() {
            @Override
            void handle(WorkerStateEvent evt) {
                logsLabel.visible = false
                progress.visible = false

                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Processo concluído", ButtonType.FINISH)
                alert.contentText = "Todos os ${task.total} arquivos, foram assinados com sucesso!"
                alert.showAndWait()
            }
        }

        Thread thread = new Thread(task)
        thread.start()
    }

    @FXML
    private void saveConfigs() {
        FileWriter writer = new FileWriter(configs.configFile)
        config.writeTo(writer)
        writer.close()
    }
}
