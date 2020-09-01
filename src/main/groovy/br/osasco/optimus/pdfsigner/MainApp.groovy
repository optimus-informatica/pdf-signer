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
