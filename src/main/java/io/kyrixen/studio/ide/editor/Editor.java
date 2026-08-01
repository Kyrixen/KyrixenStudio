package io.kyrixen.studio.ide.editor;

import java.io.File;
import java.util.List;

import io.kyrixen.studio.project.Project;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class Editor extends BorderPane {

    private final TabPane tabs;
    private final MonacoEditor monaco;
    private final StackPane bg_placeholder = new StackPane();


    public Editor(Project project, int port) {
        
        this.tabs = new TabPane();
        this.monaco = new MonacoEditor(project, this, port);

        setupPlaceholder();

        this.setTop(tabs);
        updateCenter();

        tabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if(newTab instanceof EditorTab tab) monaco.open(tab.getFile());
        });

        tabs.getTabs().addListener((ListChangeListener<Tab>) change -> updateCenter());
        
    }

    private void updateCenter() {
        if(tabs.getTabs().isEmpty()) setCenter(bg_placeholder);
        else setCenter(monaco);
    }

    private void setupPlaceholder() {

        ImageView img_bg = new ImageView(new Image("/icons/smiley_bg.png"));
        img_bg.setFitWidth(280);
        img_bg.setPreserveRatio(true);

        Label message = new Label("Open a file to start tinkering!");

        VBox background = new VBox(15, img_bg, message);
        background.setAlignment(Pos.CENTER);

        bg_placeholder.getChildren().add(background);

    }


    public void open(File file) {

        EditorTab tab = findTab(file);

        if(tab == null) {
            tab = new EditorTab(file);
            tabs.getTabs().add(tab);
        }

        tabs.getSelectionModel().select(tab);

        monaco.open(file);

        tab.setOnClosed(event -> monaco.close(file));

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
