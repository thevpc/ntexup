/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.examples;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.io.NPath;

/**
 * @author vpc
 */
public class NTxExampleFromFile2 {

    public static void main(String[] args) {
        Nuts.openWorkspace().share();
        NTxEngine e = new DefaultNTxEngine();
        NPath file = NPath.of("src/ntexup/test1.ndoc").toAbsolute().normalize();
        NTxCompiledDocument doc = e.loadDocument(file);
        System.out.println(e.toElement(doc.document(), false));
    }
}
