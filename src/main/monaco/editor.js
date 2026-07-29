import * as monaco from "monaco-editor";

import editorWorker from "monaco-editor/editor/editor.worker?worker";
import jsonWorker from "monaco-editor/language/json/json.worker?worker";
import { startLSP, didOpen, didClose, didChange, didSave } from "./lsp";

self.MonacoEnvironment = {
    getWorker(_, label) {
        switch(label) {
            case "json": return new jsonWorker();
            default: return new editorWorker();
        }

    }
};


let editor;
const models = new Map();
const openedJdt = new Set();
const jdtFileVer = new Map();


const response = await fetch("./themes/kyrixen-dark.json");
const theme = await response.json();

monaco.editor.defineTheme("kyrixen-dark", theme);
monaco.languages.register({id: "java"});

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

export async function setupLSP() {
    await startLSP();
}
window.setupLSP = setupLSP;


window.openFile = function(path) {

    if(!editor) { window.consoleBridge.warn("Editor not created yet"); return; }

    const text = window.studio.readFile(path);

    let model = models.get(path);
    if(!model) {
    
        model = monaco.editor.createModel(text, getLanguage(path), monaco.Uri.file(path));
    
        model.onDidChangeContent(() => {
            window.studio.markSaved(model.uri.fsPath, false);
        });

        models.set(path, model);

        if(model.getLanguageId() === "java") {
            
            jdtFileVer.set(model.uri.toString(), 1);

            model.onDidChangeContent(() => {
                const version = (jdtFileVer.get(model.uri.toString()) ?? 1) + 1;
                jdtFileVer.set(model.uri.toString(), version);
                didChange(model.uri.toString(), version, model.getValue());
                window.consoleBridge.debug("didChange v" + jdtFileVer.get(model.uri.toString()) + ": " + model.uri.toString());
            });
        }
    
    }

    if(model.getLanguageId() === "java" && !openedJdt.has(model.uri.toString())) {

        openedJdt.add(model.uri.toString());

        didOpen(model.uri.toString(), text);
        window.consoleBridge.debug("didOpen: " + model.uri.toString());

    }

    window.consoleBridge.debug("Model: " + model);

    editor.setModel(model);
    editor.focus();

};

export function requestSave() {

    if(!editor) return;
    
    const model = editor.getModel();
    if(!model) return;

    window.studio.saveFile(model.uri.fsPath, model.getValue());
    window.studio.markSaved(model.uri.fsPath, true);

    if(model.getLanguageId() === "java") { didSave(); window.consoleBridge.debug("didSave: " + model.uri.toString()); }

}
window.requestSave = requestSave;


export function closeFile(path) {

    const model = models.get(path);
    if(!model) return;

    if(model.getLanguageId() === "java") {
        didClose(model.uri.toString());
        jdtFileVer.delete(model.uri.toString());
        openedJdt.delete(model.uri.toString());
        window.consoleBridge.debug("didClose: " + model.uri.toString());
    }

    model.dispose();

    models.delete(path);

};
window.closeFile = closeFile;


editor.addAction({
    
    id: "save-file",
    label: "Save File",
    keybindings: [monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyS],
    run: () => { requestSave(); }

});


function getLanguage(name) {

    window.consoleBridge.debug("Prefix: " + name.split(".")[1]);

    if(name.endsWith(".java")) return "java";
    if(name.endsWith(".json")) return "json";
    if(name.endsWith(".gradle")) return "groovy";
    if(name.endsWith(".md")) return "markdown";

    return "plaintext";

}


function notifyReady() {
    if(window.studio) window.studio.editorDone();
    else setTimeout(notifyReady, 10);
}
notifyReady();
