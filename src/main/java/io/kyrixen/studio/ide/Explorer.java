package io.kyrixen.studio.ide;

import java.io.File;

import io.kyrixen.studio.ide.editor.Editor;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

public class Explorer extends TreeView<File> {

    private final TreeItem<File> root;

    public Explorer(File projectDir, Editor editor) {
        
        this.root = createView(projectDir);
        this.setRoot(root);

        configureFiles();
        setupInteraction(editor);

    }


    private TreeItem<File> createView(File file) {
        
        TreeItem<File> item = new TreeItem<>(file);

        if(file.isDirectory()) {
            File[] children = file.listFiles(f -> !f.getName().startsWith(".") && !f.getName().equals("bin"));
            if (children != null) {
                for(File child : children) { item.getChildren().add(createView(child)); }
            }
        }

        return item;
    
    }

    private final void configureFiles() {

        this.setCellFactory(tc -> new TreeCell<>() {
            
            @Override
            protected void updateItem(File file, boolean empty) {
                
                super.updateItem(file, empty);

                if(empty || file == null) setText(null);
                else {
                    if(file.getName().isEmpty()) setText(file.getAbsolutePath());
                    else setText(file.getName());
                }

            }
        
        });

    }

    private void setupInteraction(Editor editor) {

        this.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if(newItem == null) return;
            File file = newItem.getValue();
            if(file.isFile()) editor.open(file, 1, 1);
        
        });

    }

}
