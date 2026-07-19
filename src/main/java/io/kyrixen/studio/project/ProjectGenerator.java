package io.kyrixen.studio.project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dev.kyrixen.libs.logger.Logger;

public class ProjectGenerator {

    public static void generate(Project project) {

        try {
                
            copyAndExtract(project.getLocation().toAbsolutePath().toString());
            replacePlaceholders(project);
            replacePackages(project);
            renameMain(project);
            setupSettings(project);
            deleteUnusedModules(project);

            Path gradlew = project.getLocation().resolve("gradlew");
            gradlew.toFile().setExecutable(true, false);

        } catch (IOException e) { Logger.LOGGER.error("GENERATOR", "Unable to generate project"); }

    }


    private static void copyAndExtract(String dirPath) throws IOException {

        try (InputStream in = ProjectGenerator.class.getResourceAsStream("/template.zip")) {
            if (in == null) throw new IOException("template.zip not found");
            Files.copy(in, Paths.get(dirPath, "template.zip"));
        }
        
        unzip(Paths.get(dirPath + "/template.zip"), dirPath);
        Files.deleteIfExists(Paths.get(dirPath + "/template.zip"));

    }
    

    private static void replacePlaceholders(Project project) throws IOException {
     
        Files.walk(project.getLocation()).filter(Files::isRegularFile).forEach(path -> {
                try {

                    String name = path.getFileName().toString();
                    if (name.endsWith(".jar") || name.endsWith(".png") || name.endsWith(".icns") || name.endsWith(".ico")) return;

                    String content = Files.readString(path);

                    content = content
                        .replace("${PROJECT_NAME}", project.getName())
                        .replace("${PACKAGE}", project.getPackage())
                        .replace("${JAVA_VERSION}", String.valueOf(project.getJavaVersion()))
                        .replace("${LIBGDX_VERSION}", String.valueOf(project.getLibgdxVersion()));

                    Files.writeString(path, content);

                } catch (IOException e) { throw new UncheckedIOException("Failed: " + path, e); }
        });
        
    }


    private static void replacePackages(Project project) throws IOException {

        Path replacePackage = Paths.get("com/gdx/game");
        Path replacementPackage = Paths.get(project.getPackage().replace('.', '/'));

        String[] modules = {"core", "lwjgl3", "android", "teavm"};

        for(String module : modules) {

            Path javaRoot = project.getLocation().resolve(module).resolve("src/main/java");
            Path oldPath = javaRoot.resolve(replacePackage);

            if(!Files.exists(oldPath)) continue;

            Path newPath = javaRoot.resolve(replacementPackage);

            Files.createDirectories(newPath.getParent());
            Files.move(oldPath, newPath);

            Path dir = oldPath.getParent();
            while(dir != null && !dir.equals(javaRoot)) {
                try {
                    Files.delete(dir);
                    dir = dir.getParent();
                } catch(DirectoryNotEmptyException e) { break; }
            }

        }

    }


    private static void renameMain(Project project) throws IOException {

        Path javaRoot = project.getLocation().resolve("core").resolve("src/main/java");
        Path mainPackage = Paths.get(project.getPackage().replace('.', '/'));
        Path mainPath = javaRoot.resolve(mainPackage);
        
        Files.move(mainPath.resolve(("GdxGame.java")), mainPath.resolve(project.getName() + ".java"));
    
    }


    private static void setupSettings(Project project) throws IOException {
    
        Path gradleSettings = project.getLocation().resolve("settings.gradle");

        if(project.getPlatforms().contains("Desktop")) include("lwjgl3", gradleSettings);
        if(project.getPlatforms().contains("Web")) include("teavm", gradleSettings);
        if(project.getPlatforms().contains("Android")) include("android", gradleSettings);
    
    }


    private static void deleteUnusedModules(Project project) throws IOException {
        
        Path gradleProps = project.getLocation().resolve("gradle.properties");
        Path buildGradle = project.getLocation().resolve("build.gradle");

        replaceLine(gradleProps, "ANDROID", project.getPlatforms().contains("Android"));
        replaceLine(gradleProps, "TEAVM", project.getPlatforms().contains("Web"));
        replaceLine(gradleProps, "LWJGL3", project.getPlatforms().contains("Desktop"));

        replaceLine(buildGradle, "ANDROID", project.getPlatforms().contains("Android"));
        replaceLine(buildGradle, "TEAVM", project.getPlatforms().contains("Web"));
        replaceLine(buildGradle, "LWJGL3", project.getPlatforms().contains("Desktop"));
    
        deleteIfMissing(project, "Desktop", "lwjgl3");
        deleteIfMissing(project, "Web", "teavm");
        if(deleteIfMissing(project, "Android", "android")) Files.deleteIfExists(project.getLocation().resolve("local.properties"));
    
    }


    private static void unzip(Path zip, String targetFolder) {

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {

            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {

                String output = targetFolder + "/" + entry.getName();

                if(entry.isDirectory()) Files.createDirectories(Paths.get(output));
                else {

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();

                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zis.read(buffer)) != -1) { bos.write(buffer, 0, len); }

                    byte[] bytes = bos.toByteArray();
                    Files.write(Paths.get(output), bytes);
                
                }

                zis.closeEntry();
                entry = zis.getNextEntry();

            }

        } catch(IOException e) { Logger.LOGGER.error("ZIPPER", "Failed to unzip to " + targetFolder + ": " + e); }
    
    }

    private static boolean deleteIfMissing(Project project, String platform, String module) throws IOException {

        if(project.getPlatforms().contains(platform)) return false;

        Path path = project.getLocation().resolve(module);
        if(!Files.exists(path)) return false;

        Files.walk(path).sorted(Comparator.reverseOrder()).forEach(p -> {
            try {
                Files.delete(p);
            } catch(IOException e) { throw new UncheckedIOException(e); }
        });

        return true;
        
    }


    private static void replaceLine(Path file, String marker, boolean keep) throws IOException {

        String text = Files.readString(file);

        String start = "/*" + marker + "_LINE_START|";
        String end = "|" + marker + "_LINE_END*/";

        while(text.contains(start)) {

            int startIndex = text.indexOf(start);
            int endIndex = text.indexOf(end, startIndex);

            String fullMarker = text.substring(startIndex, endIndex + end.length());
            String content = text.substring(startIndex + start.length(), endIndex);

            if(keep) text = text.replace(fullMarker, content);
            else text = text.replace(fullMarker, "");
        
        }

        Files.writeString(file, text);

    }

    private static void include(String module, Path gradleSettings) throws IOException {
        Files.writeString(gradleSettings, "include '" + module + "'" + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

}