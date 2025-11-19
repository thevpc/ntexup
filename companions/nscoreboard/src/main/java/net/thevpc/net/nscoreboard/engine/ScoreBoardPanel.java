/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.net.nscoreboard.engine;

import net.thevpc.net.nscoreboard.engine.board.HorizontalScoreBoard;
import net.thevpc.net.nscoreboard.model.NScoreboard;
import net.thevpc.net.nscoreboard.util.Utils;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;

/**
 *
 * @author vpc
 */
public class ScoreBoardPanel extends JPanel {

    public static final String DEFAULT_TITLE_FONT_NAME = "Linux Biolinum Keyboard O";
    public HorizontalScoreBoard board;
    public ScoreModelAnimator animator;

    public ScoreBoardPanel() {
        super(new BorderLayout());
        board = new HorizontalScoreBoard();
        ScoreBoardPanelHeader comp = new ScoreBoardPanelHeader(board);
        add(comp, BorderLayout.NORTH);
        add(board, BorderLayout.CENTER);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isLeftMouseButton(e)) {
                    animator.startStop();
                }else if(SwingUtilities.isRightMouseButton(e)) {
                    animator.reset();
                }
            }

        });

        animator = new ScoreModelAnimator();
        animator.addListener(m -> {
            board.setModel(m);
            board.setStarted(animator.isStarted());
            repaint();
        });
//        setBackground(new Color(0x008c7a));

    }
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        super.paintComponent(g);
    }

    public void setModel(NScoreboard NScoreboard) {
        board.setModel(NScoreboard);
        animator.setModel(NScoreboard);
    }

}
