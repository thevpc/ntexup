package net.thevpc.ntexup.debug;

import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.log.NLog;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.time.NChronometer;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

public abstract class NTxTsonPanel extends JPanel {
    private Supplier<NTxDocument> model;
    private JTextArea view;
    protected NTxEngine engine;
    protected String panelName;

    public NTxTsonPanel(String panelName,NTxEngine engine) {
        super(new BorderLayout());
        this.engine = engine;
        this.panelName = panelName;
        view = new JTextArea();
        add(new JScrollPane(view));
    }

    public abstract NElement createTson();

    public void updateContent() {
        NChronometer chronometer = NChronometer.startNow();
        NChronometer chronometer1 = null;
        NChronometer chronometer2 = null;
        NChronometer chronometer3 = null;
        String txt = "";
        try {
            chronometer1 = NChronometer.startNow();
            NElement e = createTson();
            chronometer1.stop();
            if (e != null) {
                chronometer2 = NChronometer.startNow();
                txt = e.toPrettyString();
                chronometer2.stop();
            }
        } catch (Exception ex) {
            txt = "ERROR EVALUATION TSON : " + ex.toString();
            NLog.of(NTxTsonPanel.class).log(NMsg.ofC("failed : %s", ex).asFinestFail(ex));
        }
        String finalTxt = txt;
        if (SwingUtilities.isEventDispatchThread()) {
            chronometer3 = NChronometer.startNow();
            view.setText(finalTxt);
            chronometer3.stop();
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    view.setText(finalTxt);
                }
            });
        }
        chronometer.stop();
        this.engine.log().log(NMsg.ofC("[%s] update content in %s (tson:%s , ser:%s , ui:%s)", panelName, chronometer, chronometer1, chronometer2, chronometer3));
    }
}
