package org.app.maxplayerpro.sharedlogic.data

import org.w3c.files.File
import kotlin.js.Promise

/**
 * Browser APIs for picking folders and remembering them. Every helper resolves instead of
 * rejecting, so missing features (older browsers, private windows, Firefox, Safari) degrade to
 * "not available" rather than errors.
 */
internal object WebFolderAccess {
    /** Folder handles that can be stored and re-opened later: Chromium-based browsers. */
    val canRememberFolders: Boolean by lazy { supportsHandles() }
}

@JsFun("""() => typeof window.showDirectoryPicker === 'function' && typeof window.indexedDB !== 'undefined' &&
    typeof FileSystemHandle !== 'undefined' && typeof FileSystemHandle.prototype.queryPermission === 'function'""")
private external fun supportsHandles(): Boolean

@JsFun("() => 'webkitdirectory' in document.createElement('input')")
internal external fun supportsDirectoryInput(): Boolean

/** Opens the folder picker; resolves to the handle, or null when cancelled or refused. */
@JsFun("() => window.showDirectoryPicker({ id: 'maxplayer-library', mode: 'read' }).catch(() => null)")
internal external fun pickHandle(): Promise<JsAny?>

@JsFun("(h) => h.name")
internal external fun handleName(handle: JsAny): String

/** "granted", "prompt" or "denied". */
@JsFun("(h) => h.queryPermission({ mode: 'read' }).then(String).catch(() => 'denied')")
internal external fun queryPermission(handle: JsAny): Promise<JsString>

/** Shows the browser's permission prompt; needs a recent user click. */
@JsFun("(h) => h.requestPermission({ mode: 'read' }).then(String).catch(() => 'denied')")
internal external fun requestPermission(handle: JsAny): Promise<JsString>

@JsFun("(a, b) => a.isSameEntry(b).catch(() => false)")
internal external fun isSameEntry(a: JsAny, b: JsAny): Promise<JsBoolean>

/**
 * Walks [handle] and every subfolder (skipping hidden ones). Resolves to `{ entries: [{file, path}] }`
 * or `{ error: 'NotFoundError' | 'NotAllowedError' | ... }` when the folder is gone or locked.
 */
@JsFun("""(h) => (async () => {
    const entries = [];
    async function walk(dir, prefix) {
        for await (const [name, child] of dir.entries()) {
            if (name.startsWith('.')) continue;
            try {
                if (child.kind === 'file') entries.push({ file: await child.getFile(), path: prefix + name });
                else await walk(child, prefix + name + '/');
            } catch (e) { /* unreadable file or subfolder: skip it */ }
        }
    }
    try { await walk(h, h.name + '/'); return { entries }; }
    catch (e) { return { error: (e && e.name) || 'Error' }; }
})()""")
internal external fun walkHandle(handle: JsAny): Promise<JsAny>

@JsFun("(r) => r.error || ''")
internal external fun walkError(result: JsAny): String

@JsFun("(r) => r.entries ? r.entries.length : 0")
internal external fun walkCount(result: JsAny): Int

@JsFun("(r, i) => r.entries[i].file")
internal external fun walkFile(result: JsAny, index: Int): File

@JsFun("(r, i) => r.entries[i].path")
internal external fun walkPath(result: JsAny, index: Int): String

// ---- remembered folders (IndexedDB) ----

private const val DB_OPEN = """const open = () => new Promise((ok) => {
    try {
        const req = indexedDB.open('maxplayer-library', 1);
        req.onupgradeneeded = () => req.result.createObjectStore('folders', { keyPath: 'id' });
        req.onsuccess = () => ok(req.result);
        req.onerror = () => ok(null);
        req.onblocked = () => ok(null);
    } catch (e) { ok(null); }
});"""

/** Every remembered folder as `[{id, name, handle}]`; empty when storage is unavailable. */
@JsFun("""() => (async () => { $DB_OPEN
    const db = await open(); if (!db) return [];
    return await new Promise((ok) => {
        try { const req = db.transaction('folders').objectStore('folders').getAll(); req.onsuccess = () => ok(req.result || []); req.onerror = () => ok([]); }
        catch (e) { ok([]); }
    });
})()""")
internal external fun loadRememberedFolders(): Promise<JsArray<JsAny>>

@JsFun("(a, i) => a[i].id")
internal external fun rememberedId(all: JsArray<JsAny>, index: Int): String

@JsFun("(a, i) => a[i].name")
internal external fun rememberedName(all: JsArray<JsAny>, index: Int): String

@JsFun("(a, i) => a[i].handle")
internal external fun rememberedHandle(all: JsArray<JsAny>, index: Int): JsAny

/** Stores a folder handle; resolves to false when storage is unavailable (private window, quota). */
@JsFun("""(id, name, handle) => (async () => { $DB_OPEN
    const db = await open(); if (!db) return false;
    return await new Promise((ok) => {
        try { const tx = db.transaction('folders', 'readwrite'); tx.objectStore('folders').put({ id, name, handle }); tx.oncomplete = () => ok(true); tx.onerror = () => ok(false); }
        catch (e) { ok(false); }
    });
})()""")
internal external fun rememberFolder(id: String, name: String, handle: JsAny): Promise<JsBoolean>

@JsFun("""(id) => (async () => { $DB_OPEN
    const db = await open(); if (!db) return false;
    return await new Promise((ok) => {
        try { const tx = db.transaction('folders', 'readwrite'); tx.objectStore('folders').delete(id); tx.oncomplete = () => ok(true); tx.onerror = () => ok(false); }
        catch (e) { ok(false); }
    });
})()""")
internal external fun forgetFolder(id: String): Promise<JsBoolean>

// ---- folder names for browsers without handles (localStorage) ----

@JsFun("() => { try { return localStorage.getItem('maxplayer.library.names') || ''; } catch (e) { return ''; } }")
internal external fun loadFolderNames(): String

@JsFun("(v) => { try { localStorage.setItem('maxplayer.library.names', v); } catch (e) {} }")
internal external fun saveFolderNames(value: String)

@JsFun("() => 'f' + Date.now().toString(36) + Math.random().toString(36).slice(2, 8)")
internal external fun newFolderId(): String
