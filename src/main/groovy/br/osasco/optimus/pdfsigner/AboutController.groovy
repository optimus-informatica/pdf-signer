package br.osasco.optimus.pdfsigner


import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.Hyperlink
import javafx.scene.control.Label

class AboutController {
    @FXML
    Label labelVersion

    @FXML
    private void openLink(ActionEvent event) {
        Hyperlink hyperlink = event.target as Hyperlink
        MainApp.hostServicesDelegate.showDocument(hyperlink.text)
    }

    void initialize() {
        String version = Package.getPackage(this.class).implementationVersion ?: '0.0.0-DEBUG'
        labelVersion.text = "Versão: ${version}"
    }
}
