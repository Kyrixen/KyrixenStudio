package io.kyrixen.studio.ide.shells;

import java.io.PrintStream;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;

import dev.kyrixen.libs.logger.Logger;
import javafx.scene.control.Tab;

public class Console extends Tab {

    private final PrintStream output;
    private final ConsoleOutputStream consoleOutput;
    private final InlineCssTextArea outputArea = new InlineCssTextArea();
    

    public Console() {

        this.outputArea.setEditable(false);
        this.consoleOutput = new ConsoleOutputStream(outputArea);
        
        this.output = new PrintStream(consoleOutput, true);

        VirtualizedScrollPane<InlineCssTextArea> scrollBar = new VirtualizedScrollPane<>(outputArea);
        this.setContent(scrollBar);
        this.setText("Console");

        outputArea.widthProperty().addListener(event -> consoleOutput.moveToEnd());
        outputArea.heightProperty().addListener(event -> consoleOutput.moveToEnd());
        
        Logger.LOGGER.addOutput(output);
        this.setOnClosed(event -> Logger.LOGGER.removeOutput(output));

    }

}
