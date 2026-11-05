/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.engine.parser.nodeparsers;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.engine.parser.NTxNodeParserBase;

/**
 * Parses the transparent {@code fragment} container. The compiler inlines it
 * into the parent scope, so parsing itself has nothing extra to do.
 *
 * @author vpc
 */
public class NTxFragmentParser extends NTxNodeParserBase {

    public NTxFragmentParser() {
        super(true, NTxNodeType.FRAGMENT);
    }

    @Override
    public void compileNode(NTxNode node, NTxResolutionContext context) {

    }
}
