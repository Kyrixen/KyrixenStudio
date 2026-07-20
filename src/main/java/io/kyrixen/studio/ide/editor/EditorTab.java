package io.kyrixen.studio.ide.editor;

import java.io.File;

import javafx.scene.control.Tab;

public class EditorTab extends Tab {

    private final File file;


    public EditorTab(File file) {
        this.file = file;
        setText(file.getName());
    }


    public File getFile() { return file; }

}
