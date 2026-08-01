package io.kyrixen.studio.ide;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import dev.kyrixen.libs.logger.Logger;
import io.kyrixen.studio.Vars;
import io.kyrixen.studio.ide.editor.Editor;
import io.kyrixen.studio.ide.lsp.Bridge;
import io.kyrixen.studio.ide.lsp.JDT;
import io.kyrixen.studio.ide.shells.Console;
import io.kyrixen.studio.ide.shells.JSConsole;
import io.kyrixen.studio.ide.shells.Terminal;
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

    private final Editor editor;
    private final JSConsole jsConsole;

    private final Project project;
    private final JDT jdtls;
    private final Bridge bridge;
    

    public StudioIDE(Stage stage, Project project) {
        
        this.stage = stage;

        this.root = new BorderPane();
        this.scene = new Scene(root, 1280, 720);

        this.project = project;

        this.jdtls = new JDT(project);
        jdtls.launchJDT();

        int port = findWorkingPort();

        bridge = new Bridge(new InetSocketAddress("127.0.0.1", port), jdtls);
        bridge.start();
        
        this.jsConsole = new JSConsole();
        this.editor = new Editor(project, jsConsole, port);
        
        initializeStage();
        initializeLayout();

        stage.show();

    }


    private void initializeStage() {

        scene.getStylesheets().add(getClass().getResource("/ide_style.css").toExternalForm());

        stage.setTitle("Kyrixen Studio " + Vars.VERSION + " - " + project.getName());
        stage.getIcons().add(new Image("/icons/studio.png"));

        stage.setScene(scene);
        stage.setOnCloseRequest(event -> stopIDE());

        stage.setMinWidth(960);
        stage.setMinHeight(540);

    }

    private void initializeLayout() {

        root.setTop(createHeader());

        editor.setMinWidth(550);
        editor.setMinHeight(240);

        TabPane consoles = new TabPane(new Console(), jsConsole, new Terminal());
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
        Menu view = new Menu("View");
        Menu tools = new Menu("Tools");

        MenuBar menuBar = new MenuBar(file, edit, project, view, tools);
        header.getChildren().add(menuBar);


        HBox toolbar = new HBox(10);
        header.getChildren().add(toolbar);


        return header;

    }


    private int findWorkingPort() {
        
        try(ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch(IOException e) { Logger.LOGGER.error("IDE", "Failed to find a free port, defaulting to 8080: " + e); return 8080; }
    
    }


    private void stopIDE() {
    
        Logger.LOGGER.info("IDE", "Stopping IDE...");
        
        jdtls.stopJDT();
        bridge.shutdown();
        editor.stopServer();

    }

}
