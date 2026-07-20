package io.kyrixen.studio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import dev.kyrixen.libs.logger.Logger;
import javafx.application.Application;
import javafx.stage.Stage;

public class Studio extends Application {

    @Override
    public void start(Stage stage) {
        setupStudio();
        new EntryLauncher(stage);
    }


    public static void main(String[] args) {
        launch(args);
    }


    private void setupStudio() {

        Logger.LOGGER.setDebug(true);

        try {
            Files.createDirectories(Paths.get(Vars.studioPath + "/.internal/"));
        } catch (IOException e) { throw new IllegalStateException("Couldnt create project file: " + e); }

    }

}
