package net.thevpc.ntexup.api.document.style;

import java.util.Objects;

public class NTxStyleMagnitude implements Comparable<NTxStyleMagnitude> {
    private NTxStyleRuleSelector selector;
    private int distance;
    private int index;
    private int score;
    private int classOrder;
    private int classDepth;

    public NTxStyleMagnitude(int distance, int index,NTxStyleRuleSelector selector) {
        this(distance, index, selector, 0, 0);
    }

    public NTxStyleMagnitude(int distance, int index, NTxStyleRuleSelector selector,
                             int classOrder, int classDepth) {
        this.distance = distance;
        this.index = index;
        this.selector = selector == null ? DefaultNTxNodeSelector.ofAny() : selector;
        this.classOrder = classOrder;
        this.classDepth = classDepth;
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

    public int getIndex() {
        return index;
    }

    @Override
    public int compareTo(NTxStyleMagnitude o) {
        if (this.distance != o.distance) {
            int u = Integer.compare(this.distance, o.distance);
            if (u != 0) {
                return u;
            }
        }
        if (this.classOrder != o.classOrder) {
            return -Integer.compare(this.classOrder, o.classOrder);
        }
        if (this.classDepth != o.classDepth) {
            return Integer.compare(this.classDepth, o.classDepth);
        }
        if (this.selector != o.selector) {
            int u = this.selector.compareTo(o.selector);
            if (u != 0) {
                return u;
            }
        }
        if (this.index != o.index) {
            int u = Integer.compare(this.index, o.index);
            if (u != 0) {
                //last index first!!
                return -u;
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
        return distance == that.distance && score == that.score&& index == that.index
                && classOrder == that.classOrder && classDepth == that.classDepth
                && Objects.equals(selector, that.selector);
    }

    @Override
    public int hashCode() {
        return Objects.hash(selector, distance, score, classOrder, classDepth);
    }

    @Override
    public String toString() {
        return "HStyleMagnitude{" +
                "selector=" + selector +
                ", distance=" + distance +
                ", index=" + index +
                ", support=" + score +
                '}';
    }
}
