package net.thevpc.ntexup.examples;

import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.io.NPath;

public class TestCompareAntennas {
    public static void main(String[] args) {
        Nuts.openWorkspace().share();
        NTxEngine engine = new DefaultNTxEngine();

        NPath baseDir = NPath.of(".").toAbsolute().normalize();
        NPath lineFile = baseDir.resolve("src/ntexup/examples/mw/transmission-line/compare-transmission-line.ntx");
        if (!lineFile.exists()) {
            lineFile = baseDir.resolve("test/ntexup-examples/src/ntexup/examples/mw/transmission-line/compare-transmission-line.ntx");
        }
        System.out.println("Compiling and running " + lineFile + "...");
        NTxCompiledDocument doc1 = engine.loadDocument(lineFile);
        System.out.println("Doc 1 loaded with " + doc1.pages().size() + " pages!");

        // Render to trigger simulation solvers
        net.thevpc.ntexup.api.renderer.NTxDocumentStreamRenderer r1 = engine.newPdfRenderer().get();
        NPath out1 = lineFile.parent().resolve("compare-transmission-line.pdf");
        r1.setOutput(out1);
        r1.render(doc1);
        System.out.println("Rendered Stage 1 to " + out1);
        printComparison("Stage 1: Transmission Line", doc1);

        // Test Stage 2: antenna-patch
        NPath patchFile = baseDir.resolve("src/ntexup/examples/mw/antenna-patch/compare-patch.ntx");
        if (!patchFile.exists()) {
            patchFile = baseDir.resolve("test/ntexup-examples/src/ntexup/examples/mw/antenna-patch/compare-patch.ntx");
        }
        System.out.println("Compiling and running " + patchFile + "...");
        NTxCompiledDocument doc2 = engine.loadDocument(patchFile);
        System.out.println("Doc 2 loaded with " + doc2.pages().size() + " pages!");

        net.thevpc.ntexup.api.renderer.NTxDocumentStreamRenderer r2 = engine.newPdfRenderer().get();
        NPath out2 = patchFile.parent().resolve("compare-patch.pdf");
        r2.setOutput(out2);
        r2.render(doc2);
        System.out.println("Rendered Stage 2 to " + out2);
        printComparison("Stage 2: Inset-Feed Patch Antenna", doc2);
    }

    private static void printComparison(String title, NTxCompiledDocument doc) {
        System.out.println("\n=======================================================");
        System.out.println("  " + title);
        System.out.println("=======================================================");
        try {
            net.thevpc.nuts.elem.NElement hElem = doc.getGlobalObject("hresult.allS11").get().toElement();
            net.thevpc.nuts.elem.NElement oElem = doc.getGlobalObject("oemsresult.allS11").get().toElement();

            net.thevpc.nuts.elem.NArrayElement xArr = hElem.asObject().get().get("x").get().asArray().get();
            net.thevpc.nuts.elem.NArrayElement hyArr = hElem.asObject().get().get("y").get().asArray().get();
            net.thevpc.nuts.elem.NArrayElement oyArr = oElem.asObject().get().get("y").get().asArray().get();

            System.out.printf("%-12s | %-12s | %-12s | %-12s | %-15s%n",
                    "Freq (GHz)", "|S11| MoM", "|S11| OEMS", "Abs Error", "Rel Error (%)");
            System.out.println("-----------------------------------------------------------------------");

            double sumRelErr = 0;
            int count = xArr.size();
            for (int i = 0; i < count; i++) {
                double f = net.thevpc.ntexup.api.eval.NTxValue.of(xArr.get(i).get()).asDouble().get() / 1e9;
                double ym = net.thevpc.ntexup.api.eval.NTxValue.of(hyArr.get(i).get()).asDouble().get();
                double yo = net.thevpc.ntexup.api.eval.NTxValue.of(oyArr.get(i).get()).asDouble().get();
                double absErr = Math.abs(ym - yo);
                double relErr = (absErr / yo) * 100.0;
                sumRelErr += relErr;
                System.out.printf("%-12.3f | %-12.4f | %-12.4f | %-12.4f | %-15.2f%%%n",
                        f, ym, yo, absErr, relErr);
            }
            System.out.println("-----------------------------------------------------------------------");
            System.out.printf("Average Relative Error: %.2f%%%n%n", sumRelErr / count);
        } catch (Exception ex) {
            System.out.println("Error extracting comparison data: " + ex);
            ex.printStackTrace();
        }
    }
}
