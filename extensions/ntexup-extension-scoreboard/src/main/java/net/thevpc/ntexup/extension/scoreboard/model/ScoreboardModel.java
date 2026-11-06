package net.thevpc.ntexup.extension.scoreboard.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A parsed scoreboard: metadata plus the (still unsorted) list of entries.
 */
public class ScoreboardModel {

    private String title;
    private String subtitle;
    private final List<ScoreEntry> entries = new ArrayList<>();

    public String getTitle() {
        return title;
    }

    public ScoreboardModel setTitle(String title) {
        this.title = title;
        return this;
    }

    public ScoreboardModel setTitleIfAbsent(String title) {
        if (this.title == null || this.title.isEmpty()) {
            this.title = title;
        }
        return this;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public ScoreboardModel setSubtitle(String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    public ScoreboardModel setSubtitleIfAbsent(String subtitle) {
        if (this.subtitle == null || this.subtitle.isEmpty()) {
            this.subtitle = subtitle;
        }
        return this;
    }

    public List<ScoreEntry> getEntries() {
        return entries;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public double maxScore() {
        double max = 0;
        for (ScoreEntry entry : entries) {
            if (entry.getScore() > max) {
                max = entry.getScore();
            }
        }
        return max;
    }

    /**
     * Assigns the immutable input index to each entry. Must be called once,
     * after parsing, before rendering.
     */
    public ScoreboardModel reindex() {
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setIndex(i);
        }
        return this;
    }

    /**
     * Final ranking used by static renders and as the target of the animation:
     * descending score, input order as the tie breaker.
     */
    public List<ScoreEntry> sorted() {
        List<ScoreEntry> copy = new ArrayList<>(entries);
        copy.sort(Comparator
                .comparingDouble(ScoreEntry::getScore).reversed()
                .thenComparingInt(ScoreEntry::getIndex));
        return copy;
    }
}
