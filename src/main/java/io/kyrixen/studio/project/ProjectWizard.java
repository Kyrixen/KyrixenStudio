package io.kyrixen.studio.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.lang.model.SourceVersion;

import io.kyrixen.studio.Vars;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ProjectWizard {
    
    private final Stage stage;
    private final BorderPane root;
    private final Scene scene;

    private Project project = null;


    private TextField projectNameF;
    private TextField projectPkgF;
    private Label projectPathF;

    private ComboBox<String> javaVerC;
    private ComboBox<String> libgdxVerC;

    private CheckBox desktop;
    private CheckBox html;
    private CheckBox android;

    private Label errorMsg;
    private Button createB;

    public ProjectWizard(Stage stage) {

        this.stage = stage;
        this.root = new BorderPane();
        this.scene = new Scene(root, 800, 600);

        initializeStage();
        initializeLayout();        

        updateValidation();

        stage.showAndWait();

    }


    private void initializeStage() {

        scene.getStylesheets().add(ProjectWizard.class.getResource("/wizard_style.css").toExternalForm());

        stage.getIcons().add(new Image("/icons/studio.png"));
        stage.setTitle("Kyrixen Studio Project Wizard");
        
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        
        stage.setMinWidth(800);
        stage.setMinHeight(600);

    }


    private void initializeLayout() {
    
        root.setPadding(new Insets(20));

        root.setTop(createHeader());
    
        VBox center = new VBox(10);
        center.getChildren().add(createProjectInfo());
        center.getChildren().add(createProjectVersions());
        center.getChildren().add(createProjectPlatforms());
        root.setCenter(center);
    
        root.setBottom(createOptions());
    
    }


    private Node createHeader() {

        VBox header = new VBox(5);

        Label title = new Label("Project Wizard");
        title.getStyleClass().add("title");

        Label subtitle = new Label("Kyrixen Studio Project Wizard");
        subtitle.getStyleClass().add("subtitle");

        header.getChildren().addAll(title, subtitle);

        return header;

    }

    private Node createProjectInfo() {

        VBox projectInfo = new VBox(10);

        Label projectLabel = new Label("New Project");
        projectLabel.getStyleClass().add("sectitle");
        projectInfo.getChildren().add(projectLabel);

        HBox projectName = new HBox(5);

        Label projectNameT = new Label("Project name: ");
        projectNameT.getStyleClass().add("label");
        projectName.getChildren().add(projectNameT);

        projectNameF = new TextField("NewGame");
        projectNameF.textProperty().addListener((obs, oldVal, newVal) -> updateValidation());
        projectName.getChildren().add(projectNameF);

        projectInfo.getChildren().add(projectName);


        HBox projectPkg = new HBox(5);

        Label projectPkgT = new Label("Project package: ");
        projectPkgT.getStyleClass().add("label");
        projectPkg.getChildren().add(projectPkgT);
        
        projectPkgF = new TextField("com.ilove.libgdx");
        projectPkgF.textProperty().addListener((obs, oldVal, newVal) -> updateValidation());
        projectPkg.getChildren().add(projectPkgF);

        projectInfo.getChildren().add(projectPkg);


        HBox projectPath = new HBox(5);

        Label projectPathT = new Label("Project directory: ");
        projectPathT.getStyleClass().add("label");
        projectPath.getChildren().add(projectPathT);
        
        projectPathF = new Label(Vars.studioPath);
        projectPath.getChildren().add(projectPathF);
        
        Button projectPathB = new Button("Search");
        projectPathB.setOnAction(e -> {

            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setInitialDirectory(new File(Vars.studioPath));

            File directory = chooser.showDialog(stage);
            if(directory != null) { projectPathF.setText(directory.getAbsolutePath()); updateValidation(); }

        });
        projectPath.getChildren().add(projectPathB);

        projectInfo.getChildren().add(projectPath);

        return projectInfo;

    }

    private Node createProjectVersions() {

        VBox versions = new VBox(10);


        HBox javaVer = new HBox(5);

        Label javaVerT = new Label("Java version: ");
        javaVerT.getStyleClass().add("label");
        javaVer.getChildren().add(javaVerT);

        javaVerC = new ComboBox<>();
        javaVerC.getItems().addAll("Java 8 (Legacy)", "Java 11 (Recommended)", "Java 17", "Java 21");
        javaVerC.getSelectionModel().select(1);
        javaVerC.valueProperty().addListener((obs, oldVal, newVal) -> updateValidation());
        javaVer.getChildren().add(javaVerC);

        versions.getChildren().add(javaVer);


        HBox libgdxVer = new HBox(5);

        Label libgdxVerT = new Label("LibGDX version: ");
        libgdxVerT.getStyleClass().add("label");
        libgdxVer.getChildren().add(libgdxVerT);

        libgdxVerC = new ComboBox<>();
        libgdxVerC.getItems().addAll("LibGDX 1.14.2 (Recommended)", "LibGDX 1.12.1", "LibGDX 1.11.0", "LibGDX 1.10.0 (Legacy)", "LibGDX 1.9.14 (Legacy)");
        libgdxVerC.getSelectionModel().selectFirst();
        libgdxVerC.valueProperty().addListener((obs, oldVal, newVal) -> updateValidation());
        libgdxVer.getChildren().add(libgdxVerC);

        versions.getChildren().add(libgdxVer);


        return versions;

    }

    private Node createProjectPlatforms() {

        VBox platforms = new VBox(10);


        desktop = new CheckBox("Desktop");
        desktop.getStyleClass().add("label");
        desktop.setSelected(true);
        desktop.selectedProperty().addListener((obs, oldVal, newVal) -> updateValidation());
        platforms.getChildren().add(desktop);

        html = new CheckBox("Web");
        html.getStyleClass().add("label");
        html.selectedProperty().addListener((obs, oldVal, newVal) -> updateValidation());
        platforms.getChildren().add(html);

        android = new CheckBox("Android");
        android.getStyleClass().add("label");
        android.selectedProperty().addListener((obs, oldVal, newVal) -> updateValidation());
        platforms.getChildren().add(android);


        List<CheckBox> cBoxes = platforms.getChildren().stream().filter(node -> node instanceof CheckBox).map(node -> (CheckBox) node).toList();
        for(CheckBox cBox : cBoxes) {
            cBox.selectedProperty().addListener((idk, oldVal, newVal) -> {
                if(!newVal) {
                    long checkedCount = cBoxes.stream().filter(CheckBox::isSelected).count();
                    if(checkedCount == 0) cBox.setSelected(true);
                }
            });
        }


        return platforms;

    }

    private Node createOptions() {
        
        HBox options = new HBox(20);

        errorMsg = new Label();
        errorMsg.getStyleClass().add("error");
        errorMsg.setVisible(false);
        options.getChildren().add(errorMsg);

        Button cancelB = new Button("Cancel");
        cancelB.setOnAction(event -> {
            stage.close();
        });
        options.getChildren().add(cancelB);

        createB = new Button("Create");
        createB.setOnAction(event -> {
            project = createProject();
            if(validate() != null) return;
            stage.close();
        });
        options.getChildren().add(createB);

        return options;

    }


    public Project getProject() {
        return project;
    }


    private Project createProject() {

        Project project = new Project();

        project.setName(projectNameF.getText());
        project.setPackage(projectPkgF.getText());
        project.setLocation(projectPathF.getText() + "/" + project.getName());

        project.setJavaVersion(Integer.parseInt(javaVerC.getValue().split(" ")[1]));
        project.setLibgdxVersion(libgdxVerC.getValue().split(" ")[1]);

        List<String> platforms = new ArrayList<>();

        if(desktop.isSelected()) platforms.add("Desktop");
        if(html.isSelected()) platforms.add("Web");
        if(android.isSelected()) platforms.add("Android");

        project.setPlatforms(platforms);

        return project;
        
    }


    private String validate() {

        List<String> platforms = project.getPlatforms();
        if(platforms.isEmpty()) return "Project must contain at least one Platform (tbh this should have not happen)";

        int currentJava = project.getJavaVersion();
        boolean isLegacy = isLegacy();
    
        int htmlMinJava = 11;
        int desktopMinJava = 8;
        int androidLegacyMinJava = 11;
        int androidMinJava = 8;

        int minJava = 0;

        if(isLegacy) {

            if(platforms.contains("Desktop")) minJava = desktopMinJava;
            if(platforms.contains("Android")) minJava = androidLegacyMinJava;
            if(platforms.contains("Web")) minJava = htmlMinJava;
        
        } else {

            if(platforms.contains("Desktop")) minJava = desktopMinJava;
            if(platforms.contains("Android")) minJava = androidMinJava;
            if(platforms.contains("Web")) minJava = htmlMinJava;

        }

        if(minJava > currentJava) return "Project Java version must be higher.";
        if(isLegacy && currentJava > 17) return "Project Java version for legacy LibGDX must be under Java 17.";
        if(isLegacy && platforms.contains("Android") && currentJava > 11) return "Project Java version for legacy LibGDX with Android must be Java 11.";


        if(!SourceVersion.isIdentifier(project.getName())) return "Project name can only contain A-Z, a-z, 0-9 and _.";
        if(SourceVersion.isKeyword(project.getName())) return "Project name cannot be a Java keyword.";


        String pkg = project.getPackage();
        if(pkg == null || pkg.isBlank()) return "Package must be filled.";

        String[] parts = pkg.split("\\.", -1);
        for(String part : parts) {
            if(part.isEmpty()) return "Package cannot contain space or dot at the end.";
            if(!SourceVersion.isIdentifier(part)) return "Package can only contain A-Z, a-z, 0-9 and _.";
            if(SourceVersion.isKeyword(part)) return "Package cannot contain a Java keyword.";
        }


        return null;

    }

    private void updateValidation() {

        project = createProject();

        String error = validate();
        if (error == null) {
            errorMsg.setVisible(false);
            createB.setDisable(false);
        } else {
            errorMsg.setText(error);
            errorMsg.setVisible(true);
            createB.setDisable(true);
        }

    }


    public boolean isLegacy() {
        String[] legacy = {"1.10.0", "1.9.14"};
        return Arrays.asList(legacy).contains(project.getLibgdxVersion());
    }

}
