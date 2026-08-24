package net.thevpc.ntexup;

import net.thevpc.ntexup.cmdline.*;
import net.thevpc.ntexup.cmdline.options.Options;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.nuts.app.NApplication;
import net.thevpc.nuts.app.NApp;
import net.thevpc.nuts.app.NAppRun;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.core.NWorkspace;

/**
 * @author vpc
 */
@NApp
public class NTexupMain {

    public static void main(String[] args) {
        NApplication.builder(args).run();
    }

    @NAppRun
    public void run() {
        NWorkspace.of().share();
        Options options = new Options();
        NCmdLine cmdLine = NApplication.of().cmdLine();
        cmdLine.commandName("ntexup");
        new NTexupOptionsParser().parse(cmdLine, options);
        DefaultNTxEngine engine = new DefaultNTxEngine();
        new NTexupOptionsProcessor().process(options, engine);
    }


}
