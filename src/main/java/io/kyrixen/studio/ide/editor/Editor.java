package io.kyrixen.studio.ide.editor;

import java.io.File;
import java.util.List;

import io.kyrixen.studio.project.Project;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

public class Editor extends BorderPane {

    private final TabPane tabs;
    private final MonacoEditor monaco;


    public Editor(Project project) {
        
        this.tabs = new TabPane();
        this.monaco = new MonacoEditor(project, this);

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

    public void isDirty(String file, boolean dirty) {

        File f = new File(file);

        EditorTab tab = findTab(f);
        if(tab == null) return;

        if(dirty) tab.setText(f.getName() + " *");
        else tab.setText(f.getName());

    }


    private EditorTab findTab(File file) {

        List<EditorTab> editorTabs = tabs.getTabs().stream().filter(tab -> tab instanceof EditorTab).map(editorTab -> (EditorTab) editorTab).toList();
        for(EditorTab eTab : editorTabs) { if(eTab.getFile().toPath().equals(file.toPath())) return eTab; }

        return null;

    }


    public void stopServer() {
        monaco.stop();
    }

}
