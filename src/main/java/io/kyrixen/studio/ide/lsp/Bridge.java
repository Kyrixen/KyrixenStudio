package io.kyrixen.studio.ide.lsp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import dev.kyrixen.libs.logger.Logger;

public class Bridge extends WebSocketServer {
    
    private WebSocket monaco;
    private final JDT jdtls;

    public Bridge(InetSocketAddress address, JDT jdtls) {
        super(address);
        this.jdtls = jdtls;
    }
    

    @Override
    public void onStart() {
        Logger.LOGGER.info("LSP", "Starting WebSocket server");
        startInputListening();
    }


    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Logger.LOGGER.info("LSP", "Monaco connected");
        monaco = conn;
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Logger.LOGGER.debug("LSP", "Monaco disconnected");
        monaco = null;
    }

    @Override
    public void onError(WebSocket conn, Exception e) {
        Logger.LOGGER.error("LSP", "WebSocket error" + e);
    }


    @Override
    public void onMessage(WebSocket conn, String msg) {
    
        try {
        
            Logger.LOGGER.debug("LSP -> JDTLS", msg);
            writeLspMessage(jdtls.getOutputStream(), msg);
    
        } catch(IOException e) { Logger.LOGGER.error("LSP", "Failed to read Monaco message: " + e); }
    
    }

    public void shutdown() {
        try { this.stop(); } catch(InterruptedException e) { Logger.LOGGER.error("LSP", "Failed to stop LSP: " + e); }
    }

    private void startInputListening() {

        Thread stdout = new Thread(() -> {

            try {

                InputStream in = jdtls.getInputStream();

                while(true) {

                    String json = readLspMessage(in);
                    if(json == null) break;

                    Logger.LOGGER.debug("JDTLS -> LSP", json);
                    if(monaco != null) monaco.send(json);
                
                }

            } catch(IOException e) { Logger.LOGGER.error("LSP", "Error while listening: " + e.toString()); }

        }, "JDTLS-STDOUT");

        stdout.setDaemon(true);
        stdout.start();
    
    }

    private void writeLspMessage(OutputStream out, String json) throws IOException {
        
        byte[] body = json.getBytes(StandardCharsets.UTF_8);

        out.write(("Content-Length: " + body.length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        out.write(body);
        out.flush();
    
    }


    private String readLspMessage(InputStream in) throws IOException {

        int contentLength = -1;
        while(true) {

            String line = readLine(in);
            if(line == null) return null;

            if(line.isEmpty()) break;
            if(line.startsWith("Content-Length:")) contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
            
        }

        if(contentLength < 0) throw new IOException("Missing Content-Length header");
        
        byte[] body = in.readNBytes(contentLength);
        if(body.length != contentLength) throw new IOException("Unexpected end of stream");

        return new String(body, StandardCharsets.UTF_8);
    
    }
 
    private String readLine(InputStream in) throws IOException {

        StringBuilder line = new StringBuilder();
        while(true) {

            int b = in.read();
            if(b == -1) {
                if(line.length() == 0) return null;
                else return line.toString();
            }

            if(b == '\r') {
                int next = in.read();
                if(next != '\n') throw new IOException("Malformed LSP header");
                return line.toString();
            }

            line.append((char) b);
        
        }
    
    }

}