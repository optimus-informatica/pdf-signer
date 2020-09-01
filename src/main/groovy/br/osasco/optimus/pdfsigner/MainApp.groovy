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

import com.sun.javafx.application.HostServicesDelegate
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage

class MainApp extends Application {
    static HostServicesDelegate hostServicesDelegate

    @Override
    void start(Stage stage) throws Exception {
        hostServicesDelegate = HostServicesDelegate.getInstance(this)
        String version = Package.getPackage(this.class).implementationVersion ?: '0.0.0-DEBUG'
        String title = Package.getPackage(this.class).implementationTitle ?: 'Optimus - PDFSigner'
        String appForm = "/app-config.fxml"

        ConfigObject config
        try {
            AppConfigs configs = new AppConfigs()
            config = configs.loadConfigs()
            appForm = "/app-" + (config.app.mode == "user" ? "user" : "config") + ".fxml"
        }
        catch (Exception ex) {
            println(ex)
        }

        Parent root = FXMLLoader.load(getClass().getResource(appForm))
        Scene scene = new Scene(root)
        stage.scene = scene
        stage.title = "${title} v${version}"
        stage.icons.add(new Image(getClass().getResourceAsStream("/signature-solid.png")))
        stage.show()
    }

    static void main(String[] args) {
        launch(MainApp.class)
    }
}
