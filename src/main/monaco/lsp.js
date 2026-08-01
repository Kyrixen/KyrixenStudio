let socket;
let initialized;

let nextId = 1;
const pending = new Map();


export function startLSP(port) {

    if(initialized) return initialized;
    initialized = new Promise((resolve, reject) => {

        socket = new WebSocket("ws://127.0.0.1:" + port);

        socket.onopen = async () => {

            window.consoleBridge.info("Connected to JDT bridge");

            try {

                const response = await request("initialize", {
                        
                    processId: null,
                    rootUri: window.studio.getProjectUri(),
                    capabilities: {}
                    
                });

                notifyNow("initialized", {});
                window.consoleBridge.info(JSON.stringify(response));
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

            window.consoleBridge.debug("[JDT -> JS] " + event.data);

        }
        socket.onclose = () => { window.consoleBridge.warn("JDT bridge disconnected"); socket = null; initialized = null; };
        socket.onerror = (e) => { window.consoleBridge.error("WebSocket error: " + e); initialized = null; reject(e); };

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


async function notify(method, params) {
    await startLSP();
    notifyNow(method, params);
}

function notifyNow(method, params) {
    if(!socket || socket.readyState !== WebSocket.OPEN) { window.consoleBridge.error("LSP socket is not connected."); return; }
    socket.send(JSON.stringify({jsonrpc: "2.0", method, params}));
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