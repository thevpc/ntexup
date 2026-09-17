package net.thevpc.ntexup.engine.parser.resources;

import net.thevpc.ntexup.api.source.NTxSource;

import java.util.Objects;

public abstract class NTxSourceWithState implements NTxSource {
    protected Object state;

    @Override
    public void save() {
        state = state();
    }

    protected abstract Object state();

    @Override
    public boolean changed() {
        if (state == null) {
            state = state();
            return false;
        }
        Object ns = state();
        return !Objects.equals(ns, state);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NTxSourceWithState that = (NTxSourceWithState) o;
        return Objects.equals(state, that.state);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(state);
    }

    @Override
    public String toString() {
        return "DefaultHResource{" +
                "state=" + state +
                '}';
    }

    public String shortName() {
        return toString();
    }

}
