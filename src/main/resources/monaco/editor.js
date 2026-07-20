let editor;
const models = new Map();


require(["vs/editor/editor.main"], async () => {

    const response = await fetch("themes/kyrixen-dark.json");
    const theme = await response.json();
    monaco.editor.defineTheme("kyrixen-dark", theme);

    const container = document.getElementById("container");
    editor = monaco.editor.create(container, {
        
        automaticLayout: true,
        theme: "kyrixen-dark",

        smoothScrolling: true,
        cursorSmoothCaretAnimation: "on",
        mouseWheelScrollSensitivity: 0.05,

        scrollBeyondLastLine: false,

        renderWhitespace: "selection",

        tabSize: 4,

        fontLigatures: true
    
    });

    console.log("Studio editor loading");

    if(window.studio) window.studio.editorReady();
    else console.error("Studio bridge not ready");
    
});


window.openFile = function(path) {

    const text = window.studio.readFile(path);
    window.consoleBridge.debug("File content: " + text);

    let model = models.get(path);
    if(!model) {
        model = monaco.editor.createModel(text, getLanguage(path), monaco.Uri.file(path));
        models.set(path, model);
    }

    window.consoleBridge.debug("Model: " + model);

    editor.setModel(model);
    editor.focus();

};


function getLanguage(name) {

    window.consoleBridge.debug("Prefix: " + name.split(".")[1]);

    if(name.endsWith(".java")) return "java";
    if(name.endsWith(".json")) return "json";
    if(name.endsWith(".gradle")) return "groovy";
    if(name.endsWith(".md")) return "markdown";

    return "plaintext";

}