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
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Stage
import org.apache.commons.lang3.SystemUtils
import sun.security.pkcs11.SunPKCS11

import java.security.cert.X509Certificate

class AppConfigController {
    private AppConfigs configs
    private ConfigObject config
    private File certificateFile, folderIn, folderOut
    private char[] pin
    private List<String> aliases = new ArrayList<>()
    private List<X509Certificate> certificates = new ArrayList<>()

    ToggleGroup certTypes = new ToggleGroup()

    @FXML
    private TextField fieldCertificate, fieldFolderIn, fieldFolderOut

    @FXML
    private PasswordField fieldPassword

    @FXML
    private Label status, logs

    @FXML
    private ProgressIndicator progress

    @FXML
    private ComboBox cbCertificate

    @FXML
    private RadioButton a1, a3, win32

    @FXML
    private void signer(ActionEvent event) {
        String password = fieldPassword.text
        pin = password.toCharArray()
        PDFSignerTask task = new PDFSignerTask(pin: pin)
        progress.progress = ProgressIndicator.INDETERMINATE_PROGRESS
        progress.visible = true
        logs.text = ""
        logs.visible = true
        logs.textProperty().bind(task.messageProperty())
        progress.progressProperty().bind(task.progressProperty())
        task.onSucceeded = new EventHandler<WorkerStateEvent>() {
            @Override
            void handle(WorkerStateEvent evt) {
                logs.visible = false
                progress.visible = false
            }
        }
        Thread thread = new Thread(task)
        thread.start()
    }

    @FXML
    private void openAbout(ActionEvent event) {
        Parent root = FXMLLoader.load(getClass().getResource("/about.fxml"))
        Stage stage = new Stage()
        stage.icons.add(new Image(getClass().getResourceAsStream("/signature-solid.png")))
        stage.setTitle("PDFSigner - Sobre")
        Scene scene = new Scene(root)
        stage.scene = scene
        stage.show()
    }

    @FXML
    private void closeApp() {
        System.exit(0)
    }

    @FXML
    private void signerTest(ActionEvent event) {
        String password = fieldPassword.text
        pin = password.toCharArray()
        TestSignerTask task = new TestSignerTask(pin: pin)
        status.textProperty().bind(task.messageProperty())
        task.onSucceeded = new EventHandler<WorkerStateEvent>() {
            @Override
            void handle(WorkerStateEvent evt) {
                File pdf = task.value
                if (pdf) {
                    Runtime.runtime.exec("rundll32 url.dll,FileProtocolHandler ${pdf.absolutePath}")
                }
            }
        }
        Thread thread = new Thread(task)
        thread.start()
    }

    @FXML
    private void loadCertificates() {
        String password = fieldPassword.text
        if (password.length() < 1 && !win32) {
            status.text = "Por favor, informe a senha/pin do certificado."
            return
        }
        pin = password.toCharArray()

        if (a1.selected) {
            if (certificateFile == null) {
                status.text = "Por favor, informe o arquivo de certificado e clique em salvar."
                return
            }
            if (!certificateFile.exists()) {
                status.text = "O arquivo de certificado informado não existe."
                return
            }
        }

        if (a3.selected) {
            String library = config.pkcs11.library
            if (library == null) {
                status.text = "Biblioteca não definida, clique em configurar, e depois clique em salvar."
                return
            }
            File libraryFile = new File(library)
            if (!libraryFile.exists()) {
                status.text = "A biblioteca ${libraryFile.absolutePath} não existe."
                return
            }
        }

        CertificateListTask listTask = new CertificateListTask([config: config, pin: pin])
        status.textProperty().bind(listTask.messageProperty())
        listTask.onSucceeded = new LoadCertificateOnSucceded()
        Thread thread = new Thread(listTask)
        thread.start()
    }

    @FXML
    private void configure(ActionEvent event) {
        String[] libraries = config.pkcs11.libraries
        A3Utils a3Utils = new A3Utils()
        a3Utils.libraries = libraries
        status.textProperty().bind(a3Utils.messageProperty())
        a3Utils.onSucceeded = new A3OnSucceeded()
        Thread thread = new Thread(a3Utils)
        thread.start()
    }

    @FXML
    private void selectCertificateFile(ActionEvent event) {
        FileChooser chooser = new FileChooser()
        chooser.initialDirectory = new File(System.properties["user.home"] as String)
        if (certificateFile) {
            chooser.initialDirectory = certificateFile.parentFile
        }
        FileChooser.ExtensionFilter[] filters = [new FileChooser.ExtensionFilter("Arquivos de certificado", "*.pfx", "*.crt", "*.pem")]
        chooser.extensionFilters.addAll(filters)
        File file = chooser.showOpenDialog(((Button) event.source).scene.window)
        if (file) {
            fieldCertificate.text = file.absolutePath
            config.signature.certificate.filename = file.absolutePath.replace('\\', '/')
        }
    }

    @FXML
    private void saveConfigs(ActionEvent event) {
        config.signature.certificate.alias = cbCertificate.selectionModel.selectedItem
        config.signature.certificate.type = (certTypes.selectedToggle as RadioButton).id
        config.writeTo(new FileWriter(configs.configFile))
    }

    @FXML
    private void selectFolderIn(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser()
        chooser.initialDirectory = new File(System.properties["user.home"])
        File file = chooser.showDialog(((Button) event.source).scene.window)
        if (file) {
            fieldFolderIn.text = file.absolutePath
            config.signature.folder.input = file.absolutePath.replace('\\', '/')
        }
    }

    @FXML
    private void selectFolderOut(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser()
        chooser.initialDirectory = new File(System.properties["user.home"])
        File file = chooser.showDialog(((Button) event.source).scene.window)
        if (file) {
            fieldFolderOut.text = file.absolutePath
            config.signature.folder.output = file.absolutePath.replace('\\', '/')
        }
    }

    private void loadConfigs() {
        String certificateFilename, folderInName, folderOutName
        if ((certificateFilename = config.signature.certificate.filename)) {
            certificateFile = new File(certificateFilename)
            fieldCertificate.text = certificateFile.absolutePath
        }

        if ((folderInName = config.signature.folder.input)) {
            folderIn = new File(folderInName)
            fieldFolderIn.text = folderIn.absolutePath
        }

        if ((folderOutName = config.signature.folder.input)) {
            folderOut = new File(folderOutName)
            fieldFolderOut.text = folderOut.absolutePath
        }

        a1.selected = config.signature.certificate.type == 'a1'
        a3.selected = config.signature.certificate.type == 'a3'
        win32.selected = config.signature.certificate.type == 'win32'

    }

    private class A3OnSucceeded implements EventHandler<WorkerStateEvent> {

        @Override
        void handle(WorkerStateEvent event) {
            A3Utils a3Utils = event.target as A3Utils
            Map data = a3Utils.value
            if (data) {
                config.pkcs11.library = data['library']
                config.pkcs11.slot = data['slot']
                (data['provider'] as SunPKCS11)?.finalize()
            }
        }
    }

    private class LoadCertificateOnSucceded implements EventHandler<WorkerStateEvent> {

        @Override
        void handle(WorkerStateEvent event) {
            CertificateListTask listTask = event.target as CertificateListTask
            Map map = listTask.value
            aliases = map['aliases'] as List<String>
            certificates = map['certificates'] as List<X509Certificate>

            updateComboBox()
        }
    }

    private void updateComboBox() {
        String alias
        cbCertificate.items.clear()
        aliases.each {
            cbCertificate.items.add(it)
            if ((alias = config.signature.certificate.alias)) {
                cbCertificate.selectionModel.selectedItem = alias
            }
        }
    }

    /**
     * Função executado ao inicializar o controller
     */
    void initialize() {
        configs = new AppConfigs()
        configs.status = status
        config = configs.loadConfigs()

        a1.toggleGroup = certTypes
        a3.toggleGroup = certTypes
        win32.toggleGroup = certTypes

        if (!SystemUtils.IS_OS_WINDOWS) {
            win32.visible = false
        }
        loadConfigs()
        loadCertificates()
    }

}
