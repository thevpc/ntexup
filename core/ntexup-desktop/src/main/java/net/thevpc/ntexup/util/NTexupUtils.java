package net.thevpc.ntexup.util;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

public class NTexupUtils {
    public static void runUiAsync(Runnable r) {
        if(r==null) {
            return;
        }
        SwingUtilities.invokeLater(r);
    }
    public static void runUiAndWait(Runnable r) {
        if(r==null) {
            return;
        }
        if(SwingUtilities.isEventDispatchThread()) {
            r.run();
        }else{
            try {
                SwingUtilities.invokeAndWait(r);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public static void runNonUiAsync(Runnable r) {
        if(r==null) {
            return;
        }
        if(SwingUtilities.isEventDispatchThread()) {
            new Thread(r).start();
        }else {
            SwingUtilities.invokeLater(r);
        }
    }

    public static void runNonUiAndWait(Runnable r) {
        if(r==null) {
            return;
        }
        if(SwingUtilities.isEventDispatchThread()) {
            new Thread(r).start();
        }else{
            r.run();
        }
    }
}
