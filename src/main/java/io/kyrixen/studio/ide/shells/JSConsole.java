package io.kyrixen.studio.ide.shells;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;

import io.kyrixen.studio.Vars;
import javafx.application.Platform;
import javafx.scene.control.Tab;

public class JSConsole extends Tab {

    private final InlineCssTextArea outputArea = new InlineCssTextArea();
    private String style = "-fx-fill: #dddddd;";


    public JSConsole() {
        
        this.outputArea.setEditable(false);
        this.outputArea.setWrapText(true);
        this.outputArea.setFocusTraversable(false);

        VirtualizedScrollPane<InlineCssTextArea> scrollBar = new VirtualizedScrollPane<>(outputArea);
        this.setContent(scrollBar);

        this.setText("JS Console");
    
        outputArea.widthProperty().addListener(event -> this.moveToEnd());
        outputArea.heightProperty().addListener(event -> this.moveToEnd());

    }

    
    public void info(String group, String info) {
        writeMsg("λ|BLUE|λ[INFO] λ|CYAN|λ[" + group + "]λ|RESET|λ " + info + "λ|RESET|λ");
    }

    public void debug(String group, String debug) {
        if(!Vars.DEBUG) return;
        writeMsg("λ|BLUE|λ[DEBUG] λ|CYAN|λ[" + group + "]λ|GRAY|λ " + debug + "λ|RESET|λ");
    }

    public void warn(String group, String warn) {
        writeMsg("λ|BLUE|λ[WARN] λ|CYAN|λ[" + group + "]λ|YELLOW|λ " + warn + "λ|RESET|λ");
    }

    public void error(String group, String error) {
        writeMsg("λ|BLUE|λ[ERROR] λ|CYAN|λ[" + group + "]λ|RED|λ " + error + "λ|RESET|λ");
    }

    public void writeMsg(String msg) {
        printToLogger(msg);
        Platform.runLater(() -> this.parseText(msg + "\n"));
    }


    private void parseText(String text) {

        String[] textParts = text.split("λ", -1);
        
        for(String textPart : textParts) {
        
            if(textPart.startsWith("|") && textPart.endsWith("|")) { this.parseColor(textPart); continue; }
            else writeText(textPart);

        }

        this.moveToEnd();
    
    }

    private void writeText(String text) {

        if(text.isEmpty()) return;
        int start = outputArea.getLength();

        outputArea.appendText(text);
        outputArea.setStyle(start, outputArea.getLength(), style);

    }

    private void parseColor(String colorcode) {
        
        switch(colorcode) {

            case "|RESET|":
                style = "-fx-fill: #dddddd;";
                break;

            case "|RED|":
                style = "-fx-fill: #ff5555;";
                break;

            case "|GREEN|":
                style = "-fx-fill: #50fa7b;";
                break;

            case "|YELLOW|":
                style = "-fx-fill: #f1fa8c;";
                break;

            case "|BLUE|":
                style = "-fx-fill: #61afef;";
                break;

            case "|GRAY|":
                style = "-fx-fill: gray;";
                break;

            case "|CYAN|":
                style = "-fx-fill: #84ffff;";
                break;

        }
    
    }

    private void printToLogger(String msg) {

        String fixedMsg = msg.replace("\n", "");
        fixedMsg = convertInternalToANSI(fixedMsg);

        System.out.println(fixedMsg);

    }
    
    private String convertInternalToANSI(String text) {
        
        return text.replace("λ|RESET|λ", "\u001B[0m")
        .replace("λ|RED|λ", "\u001B[31m")
        .replace("λ|GREEN|λ", "\u001B[32m")
        .replace("λ|YELLOW|λ", "\u001B[33m")
        .replace("λ|BLUE|λ", "\u001B[34m")
        .replace("λ|GRAY|λ", "\u001B[90m")
        .replace("λ|CYAN|λ", "\u001B[96m");
    
    }


    public void moveToEnd() {
        outputArea.moveTo(outputArea.getLength());
        outputArea.requestFollowCaret();
    }

}