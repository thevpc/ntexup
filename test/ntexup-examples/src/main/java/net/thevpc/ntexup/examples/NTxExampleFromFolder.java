/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.examples;

import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.io.NPath;

/**
 * @author vpc
 */
public class NTxExampleFromFolder {

    public static void main(String[] args) {
        Nuts.require();
        NTxEngine e = new DefaultNTxEngine();
        NPath file = NPath.of("C:\\Users\\ibtih\\IdeaProjects\\ntexup\\test\\ndoc-examples\\src\\ndoc\\ibtihel").toAbsolute().normalize();
        e.newScreenRenderer().get().renderPath(file);
    }
}
