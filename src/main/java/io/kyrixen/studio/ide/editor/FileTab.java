package io.kyrixen.studio.ide.editor;

import java.io.File;

import javafx.scene.control.Tab;

public class FileTab extends Tab {

    private final File file;


    public FileTab(File file) {
        this.file = file;
        setText(file.getName());
    }


    public File getFile() { return file; }

}
