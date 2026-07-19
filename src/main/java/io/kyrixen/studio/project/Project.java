package io.kyrixen.studio.project;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Project {

    private String name;
    private Path path;

    private String pkg;
    
    private int javaVer;
    private String libgdxVer;
    
    private List<String> platforms = new ArrayList<>();


    public Project() {

        this.name = "NewGame";
        this.path = Paths.get(System.getProperty("user.home") + "/kstudio/");

        this.pkg = "com.ilove.libgdx";

        this.javaVer = 17;
        this.libgdxVer = "1.14.2";
        this.platforms.add("lwjgl3");

    }


    public String getName() { return this.name; }
    public Path getLocation() { return this.path; }
    
    public String getPackage() { return this.pkg; }

    public int getJavaVersion() { return this.javaVer; }
    public String getLibgdxVersion() { return this.libgdxVer; }

    public List<String> getPlatforms() { return new ArrayList<>(platforms); }


    public Project setName(String name) {
        this.name = name;
        return this;
    }

    public Project setLocation(String path) {
        this.path = Paths.get(path);
        return this;
    }

    public Project setPackage(String pkg) {
        this.pkg = pkg;
        return this;
    }

    public Project setJavaVersion(int javaVer) {
        this.javaVer = javaVer;
        return this;
    }

    public Project setLibgdxVersion(String libgdxVer) {
        this.libgdxVer = libgdxVer;
        return this;
    }

    public Project setPlatforms(List<String> platforms) {
        this.platforms = platforms;
        return this;
    }


    @Override
    public String toString() {
        return "Project{ name=" + this.name + ", path=" + this.path + ", pkg=" + this.pkg + ", javaVer=" + this.javaVer + ", libgdxVer=" + this.libgdxVer + ", platforms=" + this.platforms.toString() + " }";
    }

}