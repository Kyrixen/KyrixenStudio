let socket;
let initialized;

let nextId = 1;
const pending = new Map();


export function startLSP(port) {

    if(initialized) return initialized;
    initialized = new Promise((resolve, reject) => {

        socket = new WebSocket("ws://127.0.0.1:" + port);

        socket.onopen = async () => {

            window.consoleBridge.info("LSP", "Connected to JDT bridge");

            try {

                const response = await request("initialize", {
                    
                    processId: null,
                    rootUri: window.studio.getProjectUri(),

                    capabilities: {
                        
                        workspace: {
                            configuration: true,
                            workspaceFolders: true
                        },

                        textDocument: {
                            
                            hover: {contentFormat: ["markdown", "plaintext"]},

                            definition: {linkSupport: true},
                            declaration: {linkSupport: true},
                            typeDefinition: {linkSupport: true},
                            implementation: {linkSupport: true}
                        
                        }
                    
                    },

                    initializationOptions: {

                        workspaceFolders: [window.studio.getProjectUri()],
                        settings: {java: {}},
                        extendedClientCapabilities: {classFileContentsSupport: true}

                    },

                    workspaceFolders: [{
                        uri: window.studio.getProjectUri(),
                        name: window.studio.getProjectName()
                    }]

                });

                notifyNow("initialized", {});
                window.consoleBridge.debug("REQUEST", JSON.stringify(response));
                resolve(response);

            } catch(e) { initialized = null; reject(e); }

        };

        socket.onmessage = (event) => {

            const msg = JSON.parse(event.data);
            if(msg.id && pending.has(msg.id)) {

                const pendingRequest = pending.get(msg.id);
                pending.delete(msg.id);

                if(msg.error) pendingRequest.reject(msg.error);
                else pendingRequest.resolve(msg.result);

                return;
            
            }

            if(msg.id && msg.method) { handleServerRequest(msg); return; }

            if(msg.method) { handleNotification(msg); }


            window.consoleBridge.debug("NOTIFICATION", event.data);

        }
        socket.onclose = () => { window.consoleBridge.warn("LSP", "JDT bridge disconnected"); socket = null; initialized = null; };
        socket.onerror = (e) => { window.consoleBridge.error("LSP", "WebSocket error: " + e); initialized = null; reject(e); };

    });

    return initialized;

}


export function request(method, params) {

    if(!socket || socket.readyState !== WebSocket.OPEN) return Promise.reject(new Error("LSP socket is not connected."));

    return new Promise((resolve, reject) => {

        const id = nextId++;

        pending.set(id, {resolve, reject});
        socket.send(JSON.stringify({jsonrpc: "2.0", id, method, params}));

    });

}

export async function classFileContents(uri) {
    await startLSP();
    return request("java/classFileContents", {textDocument: {uri}});
}


async function notify(method, params) {
    notifyNow(method, params);
}

function notifyNow(method, params) {
    if(!socket || socket.readyState !== WebSocket.OPEN) { window.consoleBridge.error("LSP", "LSP socket is not connected."); return; }
    socket.send(JSON.stringify({jsonrpc: "2.0", method, params}));
}

async function handleServerRequest(msg) {
    
    window.consoleBridge.debug("REQUEST",  msg.method + "\n" + JSON.stringify(msg.params, null, 2));
    
    switch(msg.method) {

        case "workspace/configuration":
            socket.send(JSON.stringify({jsonrpc: "2.0", id: msg.id, result: msg.params.items.map(() => ({}))}));
            break;

        default:
            socket.send(JSON.stringify({jsonrpc:"2.0", id: msg.id, result:null}));
    
    }

}

function handleNotification(msg) {
    window.consoleBridge.debug("NOTIFICATION", JSON.stringify(msg));
}


export function didOpen(path, text) {

    notify("textDocument/didOpen", {
        
        textDocument: {

            uri: path,
            languageId: "java",
            version: 1,
            text: text

        }
    
    });

}

export function didClose(uri) {

    notify("textDocument/didClose", {
    
        textDocument: {
            uri
        }
    
    });

}

export function didChange(uri, version, text) {

    notify("textDocument/didChange", {
 
        textDocument: {
            uri,
            version
        },
 
        contentChanges: [{ text }]
    
    });

}

export function didSave(uri) {

    notify("textDocument/didSave", {
 
        textDocument: {
            uri
        }
 
    });

}

export async function hover(uri, line, character) {

    await startLSP();

    return request("textDocument/hover", {

        textDocument: {
            uri
        },

        position: {
            line,
            character
        }

    });

}

export async function definition(uri, line, character) {

    await startLSP();

    return request("textDocument/definition", {

        textDocument: {
            uri
        },

        position: {
            line,
            character
        }

    });

}
