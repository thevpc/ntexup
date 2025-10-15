package net.thevpc.ntexup;

import net.thevpc.ntexup.cmdline.*;
import net.thevpc.ntexup.cmdline.options.Options;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.nuts.app.NApp;
import net.thevpc.nuts.app.NAppDefinition;
import net.thevpc.nuts.app.NAppRunner;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.core.NWorkspace;

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
        DefaultNTxEngine engine = new DefaultNTxEngine();
        new NTexupOptionsProcessor().process(options, engine);
    }


}
