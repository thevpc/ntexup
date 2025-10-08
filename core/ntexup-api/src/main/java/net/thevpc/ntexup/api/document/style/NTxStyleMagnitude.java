package net.thevpc.ntexup.api.document.style;

import java.util.Objects;

public class NTxStyleMagnitude implements Comparable<NTxStyleMagnitude> {
    private NTxStyleRuleSelector selector;
    private int distance;
    private int score;

    public NTxStyleMagnitude(int distance, NTxStyleRuleSelector selector) {
        this.distance = distance;
        this.selector = selector == null ? DefaultNTxNodeSelector.ofAny() : selector;
    }

    public NTxStyleRuleSelector getSelector() {
        return selector;
    }

    public int getScore() {
        return score;
    }

    public int getDistance() {
        return distance;
    }

    @Override
    public int compareTo(NTxStyleMagnitude o) {
        if (this.selector != o.selector) {
            int u = this.selector.compareTo(o.selector);
            if (u != 0) {
                return u;
            }
        }
        if (this.distance != o.distance) {
            int u = Integer.compare(this.distance, o.distance);
            if (u != 0) {
                return u;
            }
        }
        if (this.score != o.score) {
            // bigger is first!
            int u = -Integer.compare(this.score, o.score);
            if (u != 0) {
                return u;
            }
            return u;
        }

        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NTxStyleMagnitude that = (NTxStyleMagnitude) o;
        return distance == that.distance && score == that.score && Objects.equals(selector, that.selector);
    }

    @Override
    public int hashCode() {
        return Objects.hash(selector, distance, score);
    }

    @Override
    public String toString() {
        return "HStyleMagnitude{" +
                "selector=" + selector +
                ", distance=" + distance +
                ", support=" + score +
                '}';
    }
}
