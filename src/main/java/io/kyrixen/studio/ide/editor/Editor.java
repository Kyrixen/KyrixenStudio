package io.kyrixen.studio.ide.editor;

import java.io.File;
import java.util.List;

import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

public class Editor extends BorderPane {

    private final TabPane tabs = new TabPane();
    private final MonacoEditor monaco = new MonacoEditor();


    public Editor() {
        
        this.setTop(tabs);
        this.setCenter(monaco);

        tabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if(newTab instanceof EditorTab tab) monaco.open(tab.getFile());
        });

    }

    public void open(File file) {

        EditorTab tab = findTab(file);

        if(tab == null) {
            tab = new EditorTab(file);
            tabs.getTabs().add(tab);
        }

        tabs.getSelectionModel().select(tab);

        monaco.open(file);

    }


    private EditorTab findTab(File file) {

        List<EditorTab> editorTabs = tabs.getTabs().stream().filter(tab -> tab instanceof EditorTab).map(editorTab -> (EditorTab) editorTab).toList();
        for(EditorTab eTab : editorTabs) { if(eTab.getFile().toPath().equals(file.toPath())) return eTab; }

        return null;

    }

}
