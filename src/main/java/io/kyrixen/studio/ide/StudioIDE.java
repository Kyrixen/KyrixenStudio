package io.kyrixen.studio.ide;

import java.io.File;

import io.kyrixen.studio.Vars;
import io.kyrixen.studio.ide.editor.Editor;
import io.kyrixen.studio.project.Project;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class StudioIDE {
 
    private final Stage stage;
    private final Scene scene;
    private final BorderPane root;

    private final Project project;


    public StudioIDE(Stage stage, Project project) {
        
        this.stage = stage;

        this.root = new BorderPane();
        this.scene = new Scene(root, 1280, 720);

        this.project = project;

        initializeStage();
        initializeLayout();

        stage.show();

    }


    private void initializeStage() {

        scene.getStylesheets().add(getClass().getResource("/ide_style.css").toExternalForm());

        stage.setTitle("Kyrixen Studio " + Vars.VERSION + " - " + project.getName());
        stage.getIcons().add(new Image("/icons/studio.png"));

        stage.setScene(scene);
        
        stage.setMinWidth(960);
        stage.setMinHeight(540);

    }

    private void initializeLayout() {

        root.setTop(createHeader());

        BorderPane editor = new Editor();
        editor.setMinWidth(550);
        editor.setMinHeight(240);

        TabPane consoles = new TabPane(new Console(), new Terminal());
        consoles.setMinHeight(140);
        consoles.setMinWidth(300);

        TreeView<File> explorer = new Explorer(project.getLocation().toFile(), (Editor) editor);
        explorer.setMinWidth(180);


        SplitPane vertical = new SplitPane(editor, consoles);
        vertical.setOrientation(Orientation.VERTICAL);
        vertical.setDividerPositions(0.70);


        SplitPane horizontal = new SplitPane(explorer, vertical);
        horizontal.setDividerPositions(0.25);

        root.setCenter(horizontal);

    }


    private Node createHeader() {

        VBox header = new VBox(5);       


        Menu file = new Menu("File");
        Menu edit = new Menu("Edit");
        Menu project = new Menu("Project");
        Menu tools = new Menu("Tools");

        MenuBar menuBar = new MenuBar(file, edit, project, tools);
        header.getChildren().add(menuBar);


        HBox toolbar = new HBox(10);
        header.getChildren().add(toolbar);


        return header;

    }

}
