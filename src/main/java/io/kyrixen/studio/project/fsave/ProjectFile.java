package io.kyrixen.studio.project.fsave;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import dev.kyrixen.libs.logger.Logger;
import io.kyrixen.studio.Vars;
import io.kyrixen.studio.project.Project;

public class ProjectFile {
    
    private static final Gson json = new Gson();


    @SuppressWarnings("unchecked")
    public static void save(Project project) {

        Path savePath = Paths.get(project.getLocation().toAbsolutePath().toString() + "/.kstudio/");
        
        ProjectBlueprint projectBlueprint = ProjectConvertor.toBlueprint(project);

        try {

            Files.createDirectories(savePath);

            String projectData = json.toJson(projectBlueprint);
            FileWriter fileWriter = new FileWriter(savePath + "/project.json");
            fileWriter.write(projectData);
            fileWriter.close();

            String projectsListPath = Vars.studioPath + "/.internal/projects.json";
            Path projectsList = Paths.get(projectsListPath);

            Files.createDirectories(projectsList.getParent());
            if (!Files.exists(projectsList)) { Files.writeString(projectsList, "[]"); }

            List<String> projects = json.fromJson(Files.readString(projectsList), ArrayList.class);

            if (projects == null) projects = new ArrayList<>();
            String path = project.getLocation().toAbsolutePath().toString();

            if (!projects.contains(path)) {
                projects.add(path);
                Files.writeString(projectsList, json.toJson(projects));
            }

        } catch (IOException e) { Logger.LOGGER.error("PROJECT", "Couldnt save project file: " + e); }

    }
    
    public static Project load(String projectDir) {
        
        ProjectBlueprint projectBlueprint = null;
        try {
            
            String fileData;
    
            byte[] bytes = Files.readAllBytes(Paths.get(projectDir + "/.kstudio/project.json"));
            fileData = new String(bytes);

            projectBlueprint = json.fromJson(fileData, ProjectBlueprint.class);

        } catch (IOException e) { Logger.LOGGER.error("PROJECT", "Couldnt load project file: " + e); }
        

        return ProjectConvertor.toProject(projectBlueprint);

    }

}
