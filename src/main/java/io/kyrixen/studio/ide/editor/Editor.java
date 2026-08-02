package io.kyrixen.studio.ide.editor;

import java.io.File;
import java.util.List;

import io.kyrixen.studio.ide.shells.JSConsole;
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


    public Editor(Project project, JSConsole jsConsole, int port) {
        
        this.tabs = new TabPane();
        this.monaco = new MonacoEditor(project, this, jsConsole, port);

        setupPlaceholder();

        this.setTop(tabs);
        updateCenter();

        tabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if(newTab instanceof FileTab fTab) monaco.open(fTab.getFile(), 1, 1);
            if(newTab instanceof JdtTab jTab) monaco.openDec(jTab.getJdtUri(), 1, 1);
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


    public void open(File file, int line, int column) {

        FileTab tab = findTab(file);

        if(tab == null) {
            tab = new FileTab(file);
            tabs.getTabs().add(tab);
        }

        tabs.getSelectionModel().select(tab);

        monaco.open(file, line, column);
        tab.setOnClosed(event -> monaco.close(file));

    }

    public void openDecompiled(String jdtUri, int line, int column) {

        JdtTab tab = findTab(jdtUri);

        if(tab == null) {
            tab = new JdtTab(jdtUri);
            tabs.getTabs().add(tab);
        }

        tabs.getSelectionModel().select(tab);

        monaco.openDec(jdtUri, line, column);
        tab.setOnClosed(event -> monaco.closeDec(jdtUri));

    }


    public void isDirty(String file, boolean dirty) {

        File f = new File(file);

        FileTab tab = findTab(f);
        if(tab == null) return;

        if(dirty) tab.setText(f.getName() + " *");
        else tab.setText(f.getName());

    }


    private FileTab findTab(File file) {

        List<FileTab> fileTabs = tabs.getTabs().stream().filter(tab -> tab instanceof FileTab).map(fileTab -> (FileTab) fileTab).toList();
        for(FileTab fTab : fileTabs) { if(fTab.getFile().toPath().equals(file.toPath())) return fTab; }

        return null;

    }
    
    private JdtTab findTab(String jdtUri) {

        List<JdtTab> jdtTabs = tabs.getTabs().stream().filter(tab -> tab instanceof JdtTab).map(jdtTab -> (JdtTab) jdtTab).toList();
        for(JdtTab jTab : jdtTabs) { if(jTab.getJdtUri().equals(jdtUri)) return jTab; }

        return null;

    }


    public void stopServer() {
        monaco.stop();
    }

}
