//package net.thevpc.ntexup.cmdline;
//
//import net.thevpc.nuts.core.NSession;
//import net.thevpc.nuts.cmdline.NCmdLine;
//import net.thevpc.nuts.io.NPath;
//
//public class NTxCmdLineParser {
//    public void parse(Options options,NCmdLine cmdLine){
//        while (!cmdLine.isEmpty()) {
//            cmdLine.matcher()
//                    .with("--reopen").matchTrueFlag(a -> options.reopen = true)
//                    .with("--build-repo").matchEntry(a -> {
//                        options.action = Action.BUILD_REPO;
//                        options.buildRepoPath = NPath.of(a.stringValue());
//                    })
//                    .with("--list-templates").matchFlag(a -> options.action = net.thevpc.ntexup.cmdline.Action.LIST_TEMPLATES)
//                    .with("--dump").matchFlag(a -> options.action = Action.DUMP)
//                    .with("--documentation").matchFlag(a -> options.action = Action.DOC)
//                    .with("--output").matchEntry(a -> options.output = NPath.of(a.stringValue()))
//                    .with("--open").matchEntry(a -> options.paths.add(NPath.of(a.stringValue())))
//                    .with("--pdf").matchEntry(a -> options.outputFormat=OutputFormat.PDF)
//                    .with("--new").matchTrueFlag(a -> options.action = Action.NEW)
//                    .with("--view").matchTrueFlag(a -> options.requireViewer())
//                    .with("--view-log").matchTrueFlag(a -> options.requireViewer().showLogs = true)
//                    .withNonOption().matchAny(a -> options.paths.add(NPath.of(a.image())))
//                    .requireWithDefault();
//        }
//        if (!options.viewer && !options.console) {
//            options.viewer = NSession.of().isGui();
//            options.console = !options.viewer;
//        } else if (options.viewer && options.console) {
//            options.viewer = NSession.of().isGui();
//            options.console = !options.viewer;
//        }
//    }
//}
