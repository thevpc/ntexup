/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.net.nscoreboard.engine;

import net.thevpc.net.nscoreboard.model.NScore;
import net.thevpc.net.nscoreboard.model.NScoreboard;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author vpc
 */
public class ScoreModelAnimator {

    private Timer tm = new Timer();
    private NScoreboard model = new NScoreboard();
    private NScoreboard currentModel = model;
    private boolean started = false;
    private boolean stopped = false;
    private double currentPos;
    private List<Consumer<NScoreboard>> listeners = new ArrayList<>();

    public ScoreModelAnimator() {

    }

    public void setModel(NScoreboard model) {
        this.model = model.reindex();
        this.currentModel = model;
        for (Consumer<NScoreboard> listener : listeners) {
            listener.accept(currentModel);
        }
    }

    public void addListener(Consumer<NScoreboard> r) {
        if (r != null) {
            listeners.add(r);
        }
    }

    private double computeStep() {
        int fps = model.getFps();
        if (fps <= 0) {
            fps = 60;
        }
        int durationSeconds = model.getDurationSeconds();
        if (durationSeconds <= 0) {
            durationSeconds = 1;
        }
        return 1.0 / fps / durationSeconds;
    }

    public void tic() {
        double step = computeStep();
        boolean finished = false;
        if (currentPos < 1) {
            currentPos += step;
        }else{
            finished=true;
        }
        NScoreboard cc = model.copy();
        for (NScore s : cc.scores()) {
            NScore u = Arrays.stream(currentModel.scores()).filter(x -> x.name.equals(s.name)).findFirst().orElse(null);
            if (u != null) {
                s.animating = u.animating;
                s.positionMove = u.positionMove;
                s.lastPosition = u.lastPosition;
                s.position = u.position;
            }
        }
        double sortSpeed = model.getSortSpeed();
        if(sortSpeed<=0){
            sortSpeed=1;
        }
        currentModel = cc.step(currentPos, step*sortSpeed*100);
        for (int i = 0; i < currentModel.scores().length; i++) {
            NScore score = currentModel.scores()[i];
            score.finished = finished;
        }

        for (Consumer<NScoreboard> listener : listeners) {
            listener.accept(currentModel);
        }
    }

    public void reset() {
        currentModel = model.reindex();
        currentPos = 0;
    }

    public void start() {
        if (!started) {
            started = true;
            int fps = model.getFps();
            if (fps <= 0) {
                fps = 200;
            }
            tm.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (started) {
                        if (!stopped) {
                            tic();
                        }
                    }
                }
            }, 0, 1000 / fps);
        }
        if (stopped) {
            stopped = false;
        }
    }

    public void stop() {
        stopped = true;
    }

    public void startStop() {
        if (!started) {
            start();
        } else if (!stopped) {
            stopped = true;
        } else {
            stopped = false;
        }
    }

    public boolean isStarted() {
        return started;
    }
}
