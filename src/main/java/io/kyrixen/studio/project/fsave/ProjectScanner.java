package io.kyrixen.studio.project.fsave;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import dev.kyrixen.libs.logger.Logger;
import io.kyrixen.studio.project.Project;
import io.kyrixen.studio.Vars;

public class ProjectScanner {
 
    private static final Gson json = new Gson();


    @SuppressWarnings("unchecked")
    public static List<Project> getProjects() {

        List<Project> projects = new ArrayList<>();

        String projectsListPath = Vars.studioPath + "/.internal/projects.json";
        Path projectsList = Paths.get(projectsListPath);

        try {

            if(!Files.exists(projectsList)) return projects;

            String projectsData = Files.readString(projectsList);

            List<String> projectsPaths = json.fromJson(projectsData, ArrayList.class);
            for(String projectPath : projectsPaths) {

                if(!Files.exists(Paths.get(projectPath + "/.kstudio/project.json"))) continue;

                String projectData = Files.readString(Paths.get(projectPath + "/.kstudio/project.json"));
                ProjectBlueprint bp = json.fromJson(projectData, ProjectBlueprint.class);

                projects.add(ProjectConvertor.toProject(bp));

            }


        } catch (IOException e) { Logger.LOGGER.error("PROJECT", "Couldnt open projects file: " + e); }

        return projects;

    }

}
