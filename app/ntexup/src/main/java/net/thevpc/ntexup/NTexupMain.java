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
        cmdLine.setCommandName("ntexup");
        new NTexupOptionsParser().parse(cmdLine, options);
        if(options.terminalMode){
            new NTxTerminalProcessor().runTerminal(options);
        }else{
            new NTxViewerProcessor().runViewer(options);
        }
    }


}
