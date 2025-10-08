package net.thevpc.ntexup.engine.base.nodes.text;

import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.renderer.text.NTxTextRendererFlavorParseContext;
import net.thevpc.ntexup.api.renderer.text.NTxTextToken;
import net.thevpc.ntexup.api.renderer.text.NTxTextTokenFlavored;
import net.thevpc.nuts.util.NExceptions;
import net.thevpc.nuts.internal.util.NReservedSimpleCharQueue;
import net.thevpc.nuts.util.NAssert;
import net.thevpc.nuts.text.NMsg;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

class MyNTxTextRendererFlavorParseContext implements NTxTextRendererFlavorParseContext {
    private final NTxNodeRendererContext rendererContext;
    private final NReservedSimpleCharQueue cq;

    public MyNTxTextRendererFlavorParseContext(NTxNodeRendererContext rendererContext, NReservedSimpleCharQueue cq) {
        this.rendererContext = rendererContext;
        this.cq = cq;
    }

    @Override
    public NTxNodeRendererContext rendererContext() {
        return rendererContext;
    }

    @Override
    public boolean hasNext() {
        return cq.hasNext();
    }

    @Override
    public int length() {
        return cq.length();
    }

    @Override
    public char peek() {
        return cq.peek();
    }

    @Override
    public String peek(int count) {
        return cq.peek(count);
    }

    @Override
    public String read(int count) {
        return cq.read(count);
    }

    @Override
    public void skip(int count) {
        cq.skip(count);
    }

    @Override
    public char read() {
        return cq.read();
    }

    @Override
    public char readAt(int index) {
        return cq.readAt(index);
    }

    @Override
    public List<NTxTextToken> parseDefault(String[] flavorIds, String[] customStartStops, Function<String,String> converter) {
        NAssert.requireNonNull(flavorIds, "flavorIds");
        if(customStartStops!=null && customStartStops.length%2!=0){
            throw NExceptions.ofSafeAssertException(NMsg.ofC("customStartStops shoud be a series of start/stop tokens, hence it must be even"));
        }

        String stop=null;
        for (String flavorId : flavorIds) {
            if (cq.peek(flavorId.length()+3).equals("[["+flavorId+":")) {
                cq.read(flavorId.length()+3);
                stop="]]";
                break;
            }
        }
        if(stop==null && customStartStops!=null) {
            for (int i = 0; i < customStartStops.length; i+=2) {
                String start = customStartStops[i];
                if (cq.peek(start.length()).equals(start)) {
                    cq.read(start.length());
                    stop= customStartStops[i+1];
                    break;
                }
            }
        }
        if(stop==null){
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int endLength = stop.length();
        while (hasNext()) {
            String u = peek(endLength +1);
            if (u.equals("\\" + stop)) {
                sb.append(read(endLength +1));
            } else if (peek(endLength).equals(stop)) {
                read(endLength);
                if(converter!=null) {
                    return Arrays.asList(
                            new NTxTextTokenFlavored(flavorIds[0], converter.apply(sb.toString().trim()))
                    );
                }
                return Arrays.asList(
                        new NTxTextTokenFlavored(flavorIds[0], sb.toString().trim())
                );
            } else {
                char c = read();
                sb.append(c);
            }
        }
        if(converter!=null) {
            return Arrays.asList(
                    new NTxTextTokenFlavored(flavorIds[0], converter.apply(sb.toString().trim()))
            );
        }
        return Arrays.asList(
                new NTxTextTokenFlavored(flavorIds[0], sb.toString().trim())
        );
    }
}
