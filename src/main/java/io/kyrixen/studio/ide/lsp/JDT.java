package io.kyrixen.studio.ide.lsp;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dev.kyrixen.libs.logger.Logger;
import io.kyrixen.studio.Vars;
import io.kyrixen.studio.project.Project;

public class JDT {

    private Project project;
    private Process jdt;


    public JDT(Project project) {
        this.project = project;
        setupJDT();
    }


    private void setupJDT() {

        try {
                
            Path jdtPath = Paths.get(Vars.studioPath).resolve(".internal/jdtls");
            if(Files.exists(jdtPath)) return;

            Files.createDirectories(jdtPath);

            try (InputStream in = getClass().getResourceAsStream("/jdtls.zip")) {
                if(in == null) throw new IOException("jdtls.zip not found");
                Files.copy(in, jdtPath.resolve("jdtls.zip"));
            }
            
            unzip(jdtPath.resolve("jdtls.zip"), jdtPath.toAbsolutePath().toString());
            Files.deleteIfExists(jdtPath.resolve("jdtls.zip"));
            
        } catch(IOException e) { Logger.LOGGER.error("JDTLS", "Failed to setup JDT: " + e); }

    }

    public void launchJDT() {

        try {

            Path jdtPath = Paths.get(Vars.studioPath).resolve(".internal/jdtls");

            String maxHeap = "512M";

            String jdtJar = "plugins/org.eclipse.equinox.launcher_1.6.900.v20240613-2009.jar";
            
            String config = getLaunchConfig();
            if(config == null) throw new IllegalStateException("Unknown OS");
            
            Files.createDirectories(Paths.get(Vars.studioPath).resolve(".internal").resolve("jdt-workspaces").resolve(project.getName()));
            String projectDir = Paths.get(Vars.studioPath).resolve(".internal").resolve("jdt-workspaces").resolve(project.getName()).toAbsolutePath().toString();

            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "-Xmx" + maxHeap,
                    "-Declipse.application=org.eclipse.jdt.ls.core.id1",
                    "-Declipse.product=org.eclipse.jdt.ls.core.product",
                    "-Dosgi.bundles.defaultStartLevel=4",
                    "-Dlog.protocol=true",
                    "-Dlog.level=ALL",
                    "-jar", jdtPath.resolve(jdtJar).toAbsolutePath().toString(),
                    "-configuration", jdtPath.resolve(config).toAbsolutePath().toString(),
                    "-data", projectDir
            );
            
            jdt = pb.start();

            
            Thread stderr = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(jdt.getErrorStream()))) {
                    reader.lines().forEach(line -> Logger.LOGGER.error("JDTLS", line));
                } catch (IOException dontcare) {}
            }, "JDTLS-STDERR");

            stderr.setDaemon(true);
            stderr.start();

        } catch(IOException e) { Logger.LOGGER.error("JDTLS", "Failed to start JDT LS server: " + e); }
        if(jdt == null) throw new ExceptionInInitializerError("JDT LS process failed");

    }

    public void stopJDT() {
 
        try {

            if(jdt != null) {
                jdt.destroy();
                if(!jdt.waitFor(3, TimeUnit.SECONDS)) jdt.destroyForcibly();
            }

        } catch (InterruptedException e) { Logger.LOGGER.error("JDTLS", "Failed to stop JDTLS: " + e); }

    }

    public InputStream getInputStream() { return jdt.getInputStream(); }

    public InputStream getErrorStream() { return jdt.getErrorStream(); }
    public OutputStream getOutputStream() { return jdt.getOutputStream(); }


    private static void unzip(Path zip, String targetFolder) {

        try(ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {

            ZipEntry entry = zis.getNextEntry();
            while(entry != null) {

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

    private String getLaunchConfig() {

        String os = System.getProperty("os.name").toLowerCase();

        if(os.contains("win")) return "config_win";
        if(os.contains("linux")) return "config_linux";
        if(os.contains("mac")) return "config_mac";

        return null;

    }

}
