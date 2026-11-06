package net.thevpc.ntexup.extension.scoreboard.model;

import java.awt.Color;

/**
 * One competitor/row of a scoreboard.
 *
 * <p>The model is intentionally tiny and framework free: it can be built from
 * inline TSON, CSV text or an external TSON/JSON/YAML/CSV file.</p>
 */
public class ScoreEntry {

    private String name = "";
    private String subName = "";
    private String disclosedName = "";
    private double score;
    private Color color;
    private String unit = "";
    private int index;

    public String getName() {
        return name;
    }

    public ScoreEntry setName(String name) {
        this.name = name == null ? "" : name;
        return this;
    }

    public String getSubName() {
        return subName;
    }

    public ScoreEntry setSubName(String subName) {
        this.subName = subName == null ? "" : subName;
        return this;
    }

    public String getDisclosedName() {
        return disclosedName;
    }

    public ScoreEntry setDisclosedName(String disclosedName) {
        this.disclosedName = disclosedName == null ? "" : disclosedName;
        return this;
    }

    public double getScore() {
        return score;
    }

    public ScoreEntry setScore(double score) {
        this.score = score;
        return this;
    }

    public Color getColor() {
        return color;
    }

    public ScoreEntry setColor(Color color) {
        this.color = color;
        return this;
    }

    public String getUnit() {
        return unit;
    }

    public ScoreEntry setUnit(String unit) {
        this.unit = unit == null ? "" : unit;
        return this;
    }

    public int getIndex() {
        return index;
    }

    public ScoreEntry setIndex(int index) {
        this.index = index;
        return this;
    }

    /**
     * Stable identity used by the animation cache: names may repeat, positions
     * may change, but the input index never does.
     */
    public String key() {
        return index + "\u0000" + name;
    }

    @Override
    public String toString() {
        return "ScoreEntry{"
                + "name='" + name + '\''
                + ", subName='" + subName + '\''
                + ", disclosedName='" + disclosedName + '\''
                + ", score=" + score
                + ", index=" + index
                + '}';
    }
}
