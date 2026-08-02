package io.kyrixen.studio.ide.editor;

import javafx.scene.control.Tab;

public class JdtTab extends Tab {

    private final String jdtUri;


    public JdtTab(String jdtUri) {
        
        this.jdtUri = jdtUri;

        String className = jdtUri.substring(0, jdtUri.indexOf('?'));
        setText(className.substring(className.lastIndexOf('/') + 1));
    
    }


    public String getJdtUri() { return jdtUri; }

}
