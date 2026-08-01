package io.kyrixen.studio.ide.shells;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.fxmisc.richtext.InlineCssTextArea;

import javafx.application.Platform;

public class ConsoleOutputStream extends OutputStream {

    private final InlineCssTextArea textArea;
    private String style = "-fx-fill: #dddddd;";


    public ConsoleOutputStream(InlineCssTextArea textArea) {

        this.textArea = textArea;
        this.textArea.setWrapText(true);
        this.textArea.setFocusTraversable(false);

    }


    @Override
    public void write(int b) {
        write(new byte[] { (byte) b }, 0, 1);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        String text = new String(b, off, len, StandardCharsets.UTF_8);
        Platform.runLater(() -> this.parseText(text));
    }


    private void parseText(String text) {

        while(!text.isEmpty()) {

            int esc = text.indexOf("\u001B[");
            if(esc == -1) { this.writeText(text); break; }
            if(esc > 0) this.writeText(text.substring(0, esc));

            int end = text.indexOf("m", esc);
            if(end == -1) break;

            this.switchColor(text, esc, end);
            text = text.substring(end + 1);
        
        }

        this.moveToEnd();
    
    }

    private void writeText(String text) {

        if(text.isEmpty()) return;
        int start = textArea.getLength();

        textArea.appendText(text);
        textArea.setStyle(start, textArea.getLength(), style);

    }

    private void switchColor(String text, int esc, int end) {
        
        switch (text.substring(esc, end + 1)) {

            case "\u001B[0m":
                style = "-fx-fill: #dddddd;";
                break;

            case "\u001B[31m":
                style = "-fx-fill: #ff5555;";
                break;

            case "\u001B[32m":
                style = "-fx-fill: #50fa7b;";
                break;

            case "\u001B[33m":
                style = "-fx-fill: #f1fa8c;";
                break;

            case "\u001B[34m":
                style = "-fx-fill: #61afef;";
                break;

            case "\u001B[90m":
                style = "-fx-fill: gray;";
                break;

            case "\u001B[96m":
                style = "-fx-fill: #84ffff;";
                break;

        }
    
    }


    public void moveToEnd() {
        textArea.moveTo(textArea.getLength());
        textArea.requestFollowCaret();
    }

}