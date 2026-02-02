package net.thevpc.ntexup.api.renderer;

import net.thevpc.nuts.util.NAssert;

import java.util.HashMap;
import java.util.Map;

public class NTxNodeRendererConfig {
    private int width;
    private int height;
    private Map<String, Object> capabilities;

    public NTxNodeRendererConfig() {
    }
    public NTxNodeRendererConfig(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public NTxNodeRendererConfig setWidth(int width) {
        this.width = width;
        return this;
    }

    public int getHeight() {
        return height;
    }

    public NTxNodeRendererConfig setHeight(int height) {
        this.height = height;
        return this;
    }

    public Map<String, Object> getCapabilities() {
        return capabilities;
    }

    public NTxNodeRendererConfig setCapabilities(Map<String, Object> capabilities) {
        this.capabilities = capabilities;
        return this;
    }

    public NTxNodeRendererConfig setCapability(String name, Object value) {
        NAssert.requireNamedNonBlank(name, "name");
        if (this.capabilities == null) {
            this.capabilities = new HashMap<>();
        }
        if (value == null) {
            this.capabilities.remove(name);
        } else {
            this.capabilities.put(name, value);
        }
        return this;
    }

    public NTxNodeRendererConfig withAnimate(boolean value) {
        return setCapability(NTxNodeRendererContext.CAPABILITY_ANIMATE, value);
    }

    public NTxNodeRendererConfig withPrint(boolean value) {
        return setCapability(NTxNodeRendererContext.CAPABILITY_PRINT, value);
    }
}
