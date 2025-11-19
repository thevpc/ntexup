/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.net.nscoreboard.model;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

/**
 * @author vpc
 */
public class NScoreboard implements Cloneable{

    private NScore[] scores;
    private int fps;
    private int durationSeconds;
    private double sortSpeed;
    private double max;
    private String title;
    private Image icon;
    private String nameFontName;
    private String subNameFontName;
    private String scoreFontName;
    private String subScoreFontName;

    public NScoreboard setScores(NScore... scores) {
        this.scores = scores;
        return this;
    }

    public double getSortSpeed() {
        return sortSpeed;
    }

    public NScoreboard setSortSpeed(double sortSpeed) {
        this.sortSpeed = sortSpeed;
        return this;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public NScoreboard setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
        return this;
    }

    public int getFps() {
        return fps;
    }

    public NScoreboard setFps(int fps) {
        this.fps = fps;
        return this;
    }

    public String getScoreFontName() {
        return scoreFontName;
    }

    public NScoreboard setScoreFontName(String scoreFontName) {
        this.scoreFontName = scoreFontName;
        return this;
    }

    public String getSubScoreFontName() {
        return subScoreFontName;
    }

    public NScoreboard setSubScoreFontName(String subScoreFontName) {
        this.subScoreFontName = subScoreFontName;
        return this;
    }

    public String getNameFontName() {
        return nameFontName;
    }

    public NScoreboard setNameFontName(String nameFontName) {
        this.nameFontName = nameFontName;
        return this;
    }

    public String getSubNameFontName() {
        return subNameFontName;
    }

    public NScoreboard setSubNameFontName(String subNameFontName) {
        this.subNameFontName = subNameFontName;
        return this;
    }

    public NScoreboard(NScore... scores) {
        this.scores = scores;
    }

    public String getTitle() {
        return title;
    }

    public NScoreboard setTitle(String title) {
        this.title = title;
        return this;
    }

    public Image getIcon() {
        return icon;
    }

    public NScoreboard setIcon(URL url) {
        if(url!=null) {
            try {
                Image img = ImageIO.read(url);

                Image scaled = img.getScaledInstance(200, 200, Image.SCALE_SMOOTH);

                setIcon(scaled);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    public NScoreboard setIcon(Image icon) {
        this.icon = icon;
        return this;
    }

    public NScore[] scores() {
        return scores;
    }

    public NScoreboard setMax(double max) {
        this.max = max;
        return this;
    }

    public double max() {
        double max = this.max;
        for (NScore score : this.scores) {
            max = Math.max(score.score, max);
        }
        return max;
    }

    public NScoreboard sort() {
        final NScoreboard c = copy();
        Arrays.sort(c.scores, (a, b) -> {
            int d = Double.compare(b.score, a.score);
            if (d != 0) {
                return d;
            }
            return Integer.compare(a.index, b.index);
        });
        for (int i = 0; i < c.scores.length; i++) {
            NScore score = c.scores[i];
            if (i != score.position) {
                if(score.animating){
                    score.position = i;
                }else {
                    score.lastPosition = score.position;
                    score.position = i;
                    score.positionMove = score.lastPosition;
                    score.animating = true;
                }
            }
        }
        return c;
    }

    public NScoreboard normalize() {
        final NScoreboard c = copy();
        double max = 0;
        for (NScore score : c.scores) {
            max += score.score;
        }
        for (NScore score : scores) {
            score.score = (max == 0 || score.score == 0) ? 0 : score.score / max;
        }
        return c;
    }

    public NScoreboard step(double globalPosition, double singleStep) {
        if (globalPosition <= 0) {
            globalPosition = 0;
        }
        if (globalPosition >= 1) {
            globalPosition = 1;
        }
        final double max = max();
        if (max <= 0) {
            return this;
        }
        double v = max * globalPosition;
        NScoreboard c = copy().setMax(max);
        for (int i = 0; i < c.scores.length; i++) {
            NScore score = c.scores[i];
            if (score.score >= v) {
                score.score = v;
            }
        }
        c = c.sort();
        for (int i = 0; i < c.scores.length; i++) {
            NScore score = c.scores[i];
            if (score.animating) {
                double delta = score.position - score.positionMove;
                double dx = singleStep;
                if (Math.abs(delta) > dx) {
                    delta = delta > 0 ? dx : -dx;
                    double ovm = score.positionMove;
                    score.positionMove = score.positionMove + delta;
                }else{
                    score.positionMove = score.position;
                    score.lastPosition = score.position;
                    score.animating = false;
                }
            }
        }
        return c;
    }

    public NScoreboard copy() {
        NScoreboard m = null;
        try {
            m = (NScoreboard) clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        m.max=max;
        m.scores=Arrays.stream(scores).map(x -> x.copy()).toArray(NScore[]::new);
        m.title=title;
        m.icon=icon;
        return m;
    }

    public NScoreboard withColors(int... colors) {
        NScore[] c = Arrays.stream(scores).map(x -> x.copy()).toArray(NScore[]::new);
        for (int i = 0; i < c.length; i++) {
            int index = i;
            if (index > colors.length) {
                index = index % colors.length;
            } else {
                index = (int) (1.0 * index / c.length * colors.length);
            }
            Color bg = new Color(colors[index]);
            c[i].background = NColorApplier.ofGradient(bg,bg.darker());
            c[i].border = NColorApplier.of(bg.darker());
//            c[i].foreground = NColorApplier.of(new Color(255 - bg.getRed(), 255 - bg.getGreen(), 255 - bg.getBlue()));
            c[i].foreground = (cc->{
                    int brightness = (bg.getRed() * 299 + bg.getGreen() * 587 + bg.getBlue() * 114) / 1000;
                    cc.graphics().setColor((brightness < 128) ? Color.WHITE : Color.BLACK);
            });
        }
        NScoreboard copy = copy();
        copy.scores=c;
        return copy;
    }

    public NScoreboard reindex() {
        NScore[] c = Arrays.stream(scores).map(x -> x.copy()).toArray(NScore[]::new);
        for (int i = 0; i < c.length; i++) {
            c[i].index = i;
            c[i].position = i;
            c[i].lastPosition = i;
            c[i].animating = false;
            c[i].positionMove = c[i].position;
        }
        NScoreboard copy = copy();
        copy.scores=c;
        return copy;
    }

    public NScoreboard reindexByName() {
        NScore[] c = Arrays.stream(scores).map(x -> x.copy()).toArray(NScore[]::new);
        Arrays.sort(c, (a, b) -> {
            return a.name.compareTo(b.name);
        });
        for (int i = 0; i < c.length; i++) {
            c[i].index = i;
        }
        NScoreboard copy = copy();
        copy.scores=c;
        return copy;
    }
}
