package io.kyrixen.studio.project.fsave;

import java.util.Arrays;

import io.kyrixen.studio.Vars;
import io.kyrixen.studio.project.Project;

public class ProjectConvertor {

    public static ProjectBlueprint toBlueprint(Project project) {

        ProjectBlueprint pb = new ProjectBlueprint();
        pb.formatVersion = Vars.SAVE_FORMAT;

        pb.projectName = project.getName();
        pb.projectLocation = project.getLocation().toAbsolutePath().toString();

        pb.projectPackage = project.getPackage();

        pb.javaVersion = project.getJavaVersion();
        pb.libgdxVersion = project.getLibgdxVersion();

        pb.platforms = project.getPlatforms().toArray(new String[project.getPlatforms().size()]);

        return pb;

    }

    public static Project toProject(ProjectBlueprint pb) {

        if(pb.formatVersion != Vars.SAVE_FORMAT) throw new IllegalArgumentException("Invalid format version: " + pb.formatVersion);

        Project project = new Project()
                .setName(pb.projectName)
                .setLocation(pb.projectLocation)
                .setPackage(pb.projectPackage)
                .setJavaVersion(pb.javaVersion)
                .setLibgdxVersion(pb.libgdxVersion)
                .setPlatforms(Arrays.asList(pb.platforms));

        return project;

    }

}
