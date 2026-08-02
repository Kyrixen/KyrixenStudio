import * as monaco from "monaco-editor";

import editorWorker from "monaco-editor/editor/editor.worker?worker";
import jsonWorker from "monaco-editor/language/json/json.worker?worker";
import { startLSP, didOpen, didClose, didChange, didSave, hover, definition, classFileContents } from "./lsp";

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
monaco.languages.registerHoverProvider("java", {

    async provideHover(model, position) {

        try {

            const result = await hover(model.uri.toString(), position.lineNumber - 1, position.column - 1);
            if(!result || !result.contents) return null;

            let values;
            if(Array.isArray(result.contents)) values = result.contents;
            else values = [result.contents];
            
            const contents = [];
            for(const value of values) {

                if(typeof value === "string") contents.push({ value });
                else if(value.language) contents.push({value: `\`\`\`${value.language}\n${value.value}\n\`\`\``});
                else if(value.value) contents.push({value: value.value});

            }

            return { contents };
        
        } catch(e) { window.consoleBridge.error("HOVER", e); return null; }

    }

});
monaco.languages.registerDefinitionProvider("java", {

    async provideDefinition(model, position) {

        try {

            const response = await definition(model.uri.toString(), position.lineNumber - 1, position.column - 1);
            
            const locations = Array.isArray(response) ? response : (response ? [response] : []);
            if(locations.length === 0) return null;
            
            const location = normalizeDefinitionLocation(locations[0]);
            if(!location) return null;

            window.studio.openFile(location.uri, location.range.start.line + 1, location.range.start.character + 1);

            return null;

        } catch(e) { window.consoleBridge.error("DEFINITION", e); return null; }

    }

});


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
editor.onDidChangeModel(() => {
    const model = editor.getModel();
    editor.updateOptions({readOnly: model?.uri.scheme === "jdt"});
});


export async function setupLSP(port) {
    await startLSP(port);
}
window.setupLSP = setupLSP;


window.openFile = async function(path, line, column) {

    if(!editor) { window.consoleBridge.warn("EDITOR", "Editor not created yet"); return; }

    let text;
    let uri;
    if(path.startsWith("jdt://")) { text = await classFileContents(path); uri = monaco.Uri.parse(path); }
    else { text = window.studio.readFile(path); uri = monaco.Uri.file(path); }


    let model = models.get(path);
    if(!model) {

        model = monaco.editor.createModel(text, getLanguage(path), uri);
    
        model.onDidChangeContent(() => {
            if(model.uri.scheme === "jdt") return;
            window.studio.markSaved(model.uri.fsPath, false);
        });

        models.set(path, model);

        if(model.getLanguageId() === "java") {
            
            jdtFileVer.set(model.uri.toString(), 1);

            model.onDidChangeContent(() => {
                if(!openedJdt.has(model.uri.toString())) return;
                if(model.uri.scheme === "jdt") return;
                const version = (jdtFileVer.get(model.uri.toString()) ?? 1) + 1;
                jdtFileVer.set(model.uri.toString(), version);
                didChange(model.uri.toString(), version, model.getValue());
                window.consoleBridge.INFO("CHANGE", "didChange v" + jdtFileVer.get(model.uri.toString()) + ": " + model.uri.toString());
            });
        }
    
    }

    if(model.getLanguageId() === "java" && model.uri.scheme !== "jdt" && !openedJdt.has(model.uri.toString())) {

        openedJdt.add(model.uri.toString());

        didOpen(model.uri.toString(), text);
        window.consoleBridge.info("OPEN", "didOpen: " + model.uri.toString());

    }

    window.consoleBridge.debug("MODEL", "Model: " + model);

    editor.setModel(model);

    editor.setPosition({lineNumber: line, column: column});
    editor.revealPositionInCenter({lineNumber: line, column: column});
    
    editor.focus();

};

export function requestSave() {

    if(!editor) return;
    
    const model = editor.getModel();
    if(!model) return;
    
    if (model.uri.scheme === "jdt") return;

    window.studio.saveFile(model.uri.fsPath, model.getValue());
    window.studio.markSaved(model.uri.fsPath, true);

    if(model.getLanguageId() === "java") { didSave(model.uri.toString()); window.consoleBridge.info("SAVE", "didSave: " + model.uri.toString()); }

}
window.requestSave = requestSave;


export function closeFile(path) {

    const model = models.get(path);
    if(!model) return;

    if(model.getLanguageId() === "java" && model.uri.scheme !== "jdt") {
        didClose(model.uri.toString());
        jdtFileVer.delete(model.uri.toString());
        openedJdt.delete(model.uri.toString());
        window.consoleBridge.info("CLOSE", "didClose: " + model.uri.toString());
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

    window.consoleBridge.info("LANGUAGE", "Extension: " + (name.includes(".") ? name.substring(name.lastIndexOf('.') + 1) : ""));
    
    if(name.startsWith("jdt://")) return "java";
    if(name.endsWith(".java")) return "java";
    if(name.endsWith(".json")) return "json";
    if(name.endsWith(".gradle")) return "groovy";
    if(name.endsWith(".md")) return "markdown";

    return "plaintext";

}


function normalizeDefinitionLocation(location) {

    if(!location) return null;

    const uri = location.uri ?? location.targetUri;
    const range = location.range ?? location.targetSelectionRange ?? location.targetRange;
    if(!uri || !range?.start) { window.consoleBridge.warn("DEFINITION", "Unsupported location: " + JSON.stringify(location)); return null; }

    return { uri, range };

}


function notifyReady() {
    if(window.studio) window.studio.editorDone();
    else setTimeout(notifyReady, 10);
}
notifyReady();
