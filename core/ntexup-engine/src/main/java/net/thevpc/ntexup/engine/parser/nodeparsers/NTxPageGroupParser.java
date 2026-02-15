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
 * @author vpc
 */
public class NTxPageGroupParser extends NTxNodeParserBase {

    public NTxPageGroupParser() {
        super(true, NTxNodeType.PAGE_GROUP);
    }

    @Override
    public void compileNode(NTxNode node, NTxResolutionContext context) {

    }

}
