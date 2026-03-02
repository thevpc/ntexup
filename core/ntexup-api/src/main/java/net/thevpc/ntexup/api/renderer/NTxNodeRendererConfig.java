package net.thevpc.ntexup.api.renderer;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxBounds3D;
import net.thevpc.nuts.util.NAssert;

import java.util.HashMap;
import java.util.Map;

public class NTxNodeRendererConfig {
    private double width;
    private double height;
    private long startTime;
    private boolean useCache;
    private NTxBounds2D realBounds2D;
    private NTxBounds3D realBounds3D;
    private Map<String, Object> capabilities;

    public NTxNodeRendererConfig() {
    }

    public NTxNodeRendererConfig(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public NTxBounds2D getRealBounds2D() {
        return realBounds2D;
    }

    public void setRealBounds2D(NTxBounds2D realBounds2D) {
        this.realBounds2D = realBounds2D;
    }

    public NTxBounds3D getRealBounds3D() {
        return realBounds3D;
    }

    public void setRealBounds3D(NTxBounds3D realBounds3D) {
        this.realBounds3D = realBounds3D;
    }

    public boolean isUseCache() {
        return useCache;
    }

    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public double getWidth() {
        return width;
    }

    public NTxNodeRendererConfig setWidth(double width) {
        this.width = width;
        return this;
    }

    public double getHeight() {
        return height;
    }

    public NTxNodeRendererConfig setHeight(double height) {
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
        return setCapability(NTxRendererContext.CAPABILITY_ANIMATE, value);
    }

    public NTxNodeRendererConfig withPrint(boolean value) {
        return setCapability(NTxRendererContext.CAPABILITY_PRINT, value);
    }
}
