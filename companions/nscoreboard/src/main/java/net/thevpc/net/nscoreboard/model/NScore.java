package net.thevpc.net.nscoreboard.model;

public class NScore implements Cloneable {

    public int index;
    public String name;
    public String subName;
    public String disclosedName;
    public double score;
    public NColorApplier background;
    public NColorApplier foreground;
    public NColorApplier border;
    public boolean animating;
    public int position;
    public int lastPosition;
    public double positionMove;
    public boolean finished;

    public NScore(String name, String subName,String disclosedName, double score) {
        this.index = 0;
        this.name = name;
        this.subName = subName;
        this.score = score;
        this.disclosedName = disclosedName;
    }

    public NScore copy() {
        return clone();
    }

    public NScore clone() {
        try {
            return (NScore) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

}
