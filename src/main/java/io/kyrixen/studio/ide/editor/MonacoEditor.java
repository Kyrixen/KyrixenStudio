package io.kyrixen.studio.ide.editor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import io.kyrixen.studio.ide.shells.JSConsole;
import javafx.concurrent.Worker;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public class MonacoEditor extends BorderPane {

    private final WebView webView = new WebView();
    private final WebEngine webEngine = webView.getEngine();

    private boolean ready = false;
    private final List<File> waitFiles;


    public MonacoEditor() {
        
        this.waitFiles = new ArrayList<>();

        this.setCenter(webView);
        webEngine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {

            if(state == Worker.State.SUCCEEDED) {

                JSObject window = (JSObject) webEngine.executeScript("window");

                window.setMember("consoleBridge", new JSConsole());
                window.setMember("studio", this);
                webEngine.executeScript("if(window.monacoEditorReady) window.studio.editorReady();");

            }

        });

        webEngine.load(getClass().getResource("/monaco/index.html").toExternalForm());

    }

    public void editorReady() {

        if(ready) return;
        ready = true;
        if(!waitFiles.isEmpty()) {
            for(File waitFile : waitFiles) { open(waitFile); }
            waitFiles.clear();
        }

    }


    public void open(File file) {

        if(!ready) { waitFiles.add(file); return; }

        webEngine.executeScript("""
            window.openFile("%s");
        """.formatted(file.getAbsolutePath().replace("\\", "\\\\")));

    }

    public String readFile(String path) {
        try {
            return Files.readString(Paths.get(path));
        } catch (IOException e) { e.printStackTrace(); return ""; }
    }

}    
