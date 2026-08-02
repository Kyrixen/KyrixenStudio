package io.kyrixen.studio.ide.editor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dev.kyrixen.libs.logger.Logger;
import io.kyrixen.studio.Vars;
import io.kyrixen.studio.ide.shells.JSConsole;
import io.kyrixen.studio.project.Project;
import io.kyrixen.studio.project.ProjectGenerator;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import com.sun.net.httpserver.HttpServer;

public class MonacoEditor extends BorderPane {

    private final WebView webView = new WebView();
    private final WebEngine webEngine = webView.getEngine();
    private HttpServer server;

    private final int editorPort;

    private final Project project;
    private final Editor editor;

    private boolean editorInit = false;
    private final List<File> waitFiles;

    private final Path monacoPath = Paths.get(Vars.studioPath).resolve(".internal/monaco");


    public MonacoEditor(Project project, Editor editor, JSConsole jsConsole, int port) {
        
        this.project = project;
        this.editor = editor;

        this.editorPort = port;

        this.waitFiles = new ArrayList<>();

        this.setCenter(webView);
        webEngine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {

            if(state == Worker.State.SUCCEEDED) {

                JSObject window = (JSObject) webEngine.executeScript("window");

                window.setMember("consoleBridge", jsConsole);
                window.setMember("studio", this);
            
            }

        });

        String monacoUrl = setupMonaco();
        if(monacoUrl != null) webEngine.load(monacoUrl);

    }

    public void editorDone() {

        if(editorInit) return;
        editorInit = true;

        webEngine.executeScript("window.setupLSP(" + editorPort + ");");

        if(!waitFiles.isEmpty()) {
            for(File waitFile : waitFiles) { open(waitFile, 1, 1); }
            waitFiles.clear();
        }

    }

    public String getProjectUri() {
        return project.getLocation().toUri().toString();
    }

    public String getProjectName() {
        return project.getName();
    }


    public void open(File file, int line, int column) {

        if(!editorInit) { waitFiles.add(file); return; }
        
        Platform.runLater(() -> {
            webEngine.executeScript("""
                window.openFile("%s", %d, %d);
                """.formatted(file.getAbsolutePath().replace("\\", "\\\\"), line, column));
        });

    }
    
    public void openDec(String jdtUri, int line, int column) {

        if(!editorInit) return;
        
        Platform.runLater(() -> {
            webEngine.executeScript("""
                window.openFile("%s", %d, %d);
                """.formatted(jdtUri.replace("\\", "\\\\"), line, column));
        });

    }


    public void openFile(String path, int line, int column) {

        if(path.startsWith("jdt://")) { editor.openDecompiled(path, line, column); return; }

        if(path.startsWith("file://")) {
            
            try {
                editor.open(Paths.get(URI.create(path)).toFile(), line, column);
            } catch (AbstractMethodError e) { e.printStackTrace(); }
            
            return;
        }

        editor.open(Paths.get(path).toFile(), line, column);
    
    }

    public String readFile(String path) {
        try {
            return Files.readString(Paths.get(path));
        } catch(IOException e) { e.printStackTrace(); return ""; }
    }

    public void saveFile(String path, String content) {
        
        try {
            Files.writeString(Paths.get(path), content);
        } catch(IOException e) { e.printStackTrace(); return; }

    }


    public void markSaved(String file, boolean saved) {
        editor.isDirty(file, !saved);
    }


    private String setupMonaco() {

        try {

            refreshMonaco();

            int port = startMonacoServer();
            return "http://" + "127.0.0.1" + ":" + port + "/index.html";

        } catch(IOException e) { Logger.LOGGER.error("EDITOR", "Couldnt setup Monaco: " + e); return null; }
        
    }

    private void refreshMonaco() {

        try {

            if(Files.exists(monacoPath)) {
                
                try (Stream<Path> paths = Files.walk(monacoPath)) {
                    for(Path path : paths.sorted(Comparator.reverseOrder()).toList()) { Files.deleteIfExists(path); }
                }
            
            }

            Files.createDirectories(monacoPath);

            Files.createDirectories(monacoPath);

            try(InputStream in = ProjectGenerator.class.getResourceAsStream("/monaco.zip")) {
                if(in == null) throw new IOException("monaco.zip not found");
                Files.copy(in, Paths.get(monacoPath.toAbsolutePath().toString(), "monaco.zip"));
            }
            
        } catch(IOException e) { Logger.LOGGER.error("EDITOR", "Couldnt copy monaco files: " + e); }
        

        unzip(monacoPath.resolve("monaco.zip"), monacoPath.toAbsolutePath().toString());
        try { Files.deleteIfExists(monacoPath.resolve("monaco.zip")); } catch(IOException e) { Logger.LOGGER.error("EDITOR", "Couldnt delete monaco.zip: " + e); }


        boolean corrupted = false;
        if(!Files.isRegularFile(monacoPath.resolve("index.html"))) corrupted = true;
        if(!Files.isRegularFile(monacoPath.resolve("themes/style.css"))) corrupted = true;
        if(!Files.isRegularFile(monacoPath.resolve("themes/kyrixen-dark.json"))) corrupted = true;

        if(!Files.isDirectory(monacoPath.resolve("assets"))) corrupted = true;


        if(corrupted) Logger.LOGGER.error("EDITOR", "monaco.zip did not contain a complete bundle");

    }


    private int startMonacoServer() throws IOException {

        if(server != null) return server.getAddress().getPort();

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {

            try {

                String request = exchange.getRequestURI().getPath();
                if(request.equals("/")) request = "/index.html";

                Path requested = monacoPath.resolve(request.substring(1)).normalize();
                if(!requested.startsWith(monacoPath) || !Files.exists(requested)) { exchange.sendResponseHeaders(404, -1); return; }

                String type = Files.probeContentType(requested);
                if(type == null) {
                    if(requested.toString().endsWith(".js")) type = "application/javascript";
                    else if(requested.toString().endsWith(".css")) type = "text/css";
                    else if(requested.toString().endsWith(".html")) type = "text/html";
                    else if(requested.toString().endsWith(".json")) type = "application/json";
                    else type = "application/octet-stream";
                }

                exchange.getResponseHeaders().set("Content-Type", type);

                byte[] bytes = Files.readAllBytes(requested);
                exchange.sendResponseHeaders(200, bytes.length);

                try(OutputStream out = exchange.getResponseBody()) { out.write(bytes); }

            } catch(IOException e) {
                Logger.LOGGER.error("EDITOR", "Failed to serve Monaco file: " + e);
                exchange.sendResponseHeaders(500, -1);
            } finally { exchange.close(); }

        });

        server.start();

        int port = server.getAddress().getPort();
        Logger.LOGGER.info("EDITOR", "Monaco server started on port " + port);

        return port;
    
    }

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


    public void close(File file) {

        if(!editorInit) return;

        webEngine.executeScript("""
            window.closeFile("%s");
        """.formatted(file.getAbsolutePath().replace("\\", "\\\\")));

    }

    public void closeDec(String jdtUri) {

        if(!editorInit) return;

        webEngine.executeScript("""
            window.closeFile("%s");
        """.formatted(jdtUri));
    
    }


    public void stop() {
        if(server != null) server.stop(0);   
    }

}    
