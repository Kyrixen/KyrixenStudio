package io.kyrixen.studio;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import dev.kyrixen.libs.logger.Logger;
import io.kyrixen.studio.project.Project;
import io.kyrixen.studio.project.ProjectGenerator;
import io.kyrixen.studio.project.ProjectGeneratorLegacy;
import io.kyrixen.studio.project.ProjectWizard;
import io.kyrixen.studio.project.fsave.ProjectFile;
import io.kyrixen.studio.project.fsave.ProjectScanner;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class EntryLauncher {
    
    private final Stage stage;
    private final BorderPane root;

    private final Scene scene;

    private Project currentProject = null;
    private Button openP;

    public EntryLauncher(Stage stage) {
        
        this.stage = stage;

        root = new BorderPane();
        scene = new Scene(root, 1000, 700);

        initializeStage();
        initializeLayout();

        stage.show();
    
    }

    private void initializeStage() {

        scene.getStylesheets().add(getClass().getResource("/launcher_style.css").toExternalForm());

        stage.setTitle("Kyrixen Studio");
        stage.getIcons().add(new Image("/icons/studio.png"));

        stage.setScene(scene);
        
        stage.setMinWidth(900);
        stage.setMinHeight(650);

    }


    private void initializeLayout() {
    
        root.setPadding(new Insets(20));

        root.setTop(createHeader());

        Node projects = createProjects();
        BorderPane.setMargin(projects, new Insets(20, 0, 20, 0));
        root.setCenter(projects);
        
        root.setBottom(createOptions());

    }


    private Node createHeader() {

        VBox header = new VBox(5);

        Label title = new Label("Kyrixen Studio " + Vars.VERSION);
        title.getStyleClass().add("title");

        Label subtitle = new Label("Open-source LibGDX IDE");
        subtitle.getStyleClass().add("subtitle");

        header.getChildren().addAll(title, subtitle);

        return header;

    }

    private Node createProjects() {

        VBox projects = new VBox(4);
        
        Label title = new Label("Recent Projects");
        title.getStyleClass().add("sectitle");
        projects.getChildren().add(title);

        ListView<Project> list = new ListView<>();
        
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Project item, boolean empty) {
                
                super.updateItem(item, empty);

                if(empty || item == null) setText(null);
                else setText(item.getName());
                
            }
        });

        list.getItems().addAll(ProjectScanner.getProjects());
        list.setOnMouseClicked(event -> updateList(event));

        projects.getChildren().add(list);

        return projects;

    }

    @SuppressWarnings("unchecked")
    private void updateList(MouseEvent event) {

        ListView<Project> list = (ListView<Project>) event.getSource();
        currentProject = list.getSelectionModel().getSelectedItem();

        Logger.LOGGER.info("LAUNCHER", "Selected: " + currentProject);

        openP.setDisable(currentProject == null);

        if(event.getClickCount() == 2 && currentProject != null) openProject();

    }


    private Node createOptions() {

        HBox options = new HBox(10);

        Button newP = new Button("New Project");
        openP = new Button("Open Project");
        Button openF = new Button("Open Folder");


        newP.setOnAction(event -> newProject());
        openP.setOnAction(event -> openProject());
        openP.setDisable(currentProject == null);
        openF.setOnAction(event -> openFolder());


        options.getChildren().addAll(newP, openP, openF);
        options.setAlignment(Pos.CENTER);

        return options;

    }
    

    private void newProject() {

        ProjectWizard projectWizard = new ProjectWizard(new Stage());
        Project project = projectWizard.getProject();

        if(project == null) return;

        Logger.LOGGER.info("LAUNCHER", "Saving...");
        ProjectFile.save(project);
        
        if(projectWizard.isLegacy()) ProjectGeneratorLegacy.generate(project);
        else ProjectGenerator.generate(project);
        
        currentProject = project;
        openProject();

    }

    private void openProject() {
        if(currentProject == null) return;
        Logger.LOGGER.info("LAUNCHER", "Opening project...");
    }
    
    private void openFolder() {
        
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setInitialDirectory(new File(Vars.studioPath));

        File directory = chooser.showDialog(stage);
        if(directory == null) return;
        if(!Files.exists(Paths.get(directory.toPath().toAbsolutePath().toString() + "/.kstudio/project.json"))) return;
    
        Logger.LOGGER.info("LAUNCHER", "Opening folder...");

    }
    
}
