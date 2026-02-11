/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.api.eval;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.NOptional;

/**
 * @author vpc
 */
public interface NTxObjectEvalContext {
    NElement eval(NElement elementExpr);
}
