/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.net.nscoreboard.engine;

import net.thevpc.net.nscoreboard.engine.board.HorizontalScoreBoard;
import net.thevpc.net.nscoreboard.model.NScoreboard;
import net.thevpc.net.nscoreboard.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 *
 * @author vpc
 */
public class ScoreBoardPanelHeader extends JPanel {

    public static final String DEFAULT_TITLE_FONT_NAME = "Linux Biolinum Keyboard O";
    public HorizontalScoreBoard board;
    public JLabel titleLabel;
    public JLabel iconLabel;

    public ScoreBoardPanelHeader(HorizontalScoreBoard board) {
        super(new BorderLayout());
        setLayout(new GridBagLayout());
        this.board=board;
        NScoreboard model = board.getModel();
        titleLabel = new JLabel(model.getTitle());
        titleLabel.setForeground(Color.BLACK);
        iconLabel = new JLabel();
        GridBagConstraints g = new GridBagConstraints();
        g.gridx=0;
        g.gridy=0;
        g.fill=GridBagConstraints.BOTH;
        g.ipady=20;
        add(iconLabel, g);
        g = new GridBagConstraints();
        g.anchor=GridBagConstraints.CENTER;
        g.gridx=1;
        g.gridy=0;
        g.weightx=2;
        g.fill=GridBagConstraints.BOTH;
        g.ipady=20;
        add(titleLabel,g);
        addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                prepareAgain();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                prepareAgain();
            }

            @Override
            public void componentShown(ComponentEvent e) {
                prepareAgain();
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                prepareAgain();
            }
        });
        prepareAgain();
    }

    private void prepareAgain() {
        Dimension s = getSize();
        titleLabel.setFont(Utils.prepareFont(DEFAULT_TITLE_FONT_NAME, 0, s.getHeight() / 3).deriveFont(Font.BOLD));
        titleLabel.setText(board.getModel().getTitle());
        NScoreboard model = board.getModel();
        if(model.getIcon()!=null){
            iconLabel.setIcon(new ImageIcon(model.getIcon()));
        }else{
            iconLabel.setIcon(null);
        }

    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }


}
