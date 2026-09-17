package net.thevpc.ntexup.extension.progress.skin;

import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.extension.progress.NTxProgressSkin;
import net.thevpc.nuts.ext.NServiceLoader;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry that discovers NTxProgressSkin implementations via SPI
 * and provides lookup by skin id.
 */
public class NTxProgressSkinRegistry {

    private static final NTxProgressSkinRegistry INSTANCE = new NTxProgressSkinRegistry();
    private final Map<String, NTxProgressSkin> skins = new LinkedHashMap<>();
    private boolean loaded = false;

    private NTxProgressSkinRegistry() {
    }

    public static NTxProgressSkinRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Load skins from the service loader. Safe to call multiple times — only loads once.
     */
    public synchronized void load() {
        if (loaded) return;
        loaded = true;
        for (NTxProgressSkin skin : NServiceLoader.of(NTxProgressSkin.class, null, NTxProgressSkinRegistry.class.getClassLoader()).loadAll(null)) {
            skins.put(NTxUtils.uid(skin.id()), skin);
        }
    }

    /**
     * Register a skin programmatically (useful for tests or built-in skins
     * that are in the same module and not discovered via SPI).
     */
    public void register(NTxProgressSkin skin) {
        load();
        skins.put(NTxUtils.uid(skin.id()), skin);
    }

    /**
     * Get a skin by id. Returns null if not found.
     */
    public NTxProgressSkin get(String skinId) {
        load();
        NTxProgressSkin skin = skins.get(NTxUtils.uid(skinId));
        if (skin == null) {
            // fallback to progressbar
            skin = skins.get(NTxUtils.uid("progressbar"));
        }
        return skin;
    }

    /**
     * Check if a skin with the given id is registered.
     */
    public boolean contains(String skinId) {
        load();
        return skins.containsKey(NTxUtils.uid(skinId));
    }
}
