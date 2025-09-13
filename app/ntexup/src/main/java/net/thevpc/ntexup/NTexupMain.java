package net.thevpc.ntexup;

import net.thevpc.ntexup.cmdline.*;
import net.thevpc.nuts.*;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.util.NOptional;

/**
 * @author vpc
 */
@NAppDefinition
public class NTexupMain {

    public static void main(String[] args) {
        NApp.builder(args).run();
    }

    @NAppRunner
    public void run() {
        NWorkspace.of().share();
        Options options = new Options();
        NCmdLine cmdLine = NApp.of().getCmdLine();
        while (!cmdLine.isEmpty()) {
            NArg p = cmdLine.peek().get();
            if (p.isOption()) {
                if (p.key().equals("--gui")) {
                    NSession.of().setGui(true);
                    cmdLine.next();
                    new NTxViewerProcessor().runViewer(cmdLine, options);
                    return;
                } else if (p.key().equals("--view")) {
                    cmdLine.next();
                    new NTxViewerProcessor().runViewer(cmdLine, options);
                    return;
                } else if (NSession.of().configureFirst(cmdLine)) {
                    // okkay
                } else {
                    new NTxTerminalProcessor().runTerminal(cmdLine, options);
                    return;
                }
            } else {
                new NTxTerminalProcessor().runTerminal(cmdLine, options);
                return;
            }
        }
        new NTxTerminalProcessor().runTerminal(cmdLine, options);
    }


}
