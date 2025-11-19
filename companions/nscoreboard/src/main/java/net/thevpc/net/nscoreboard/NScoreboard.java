/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package net.thevpc.net.nscoreboard;

import net.thevpc.net.nscoreboard.engine.NScoreboardFrame;
import net.thevpc.net.nscoreboard.model.NScore;
import net.thevpc.net.nscoreboard.util.Colors;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NLiteral;
import net.thevpc.nuts.util.NStringUtils;

import java.util.List;

/**
 * @author vpc
 */
public class NScoreboard {

    public static void main(String[] args) {
        Nuts.require();
        List<String> lines = NStringUtils.split(NPath.of(NScoreboard.class.getResource("/names.csv")).readString(),"\n",true,true);

        new NScoreboardFrame(new net.thevpc.net.nscoreboard.model.NScoreboard()
                .setTitle("<html>ENISo MIDNIGHT AI <br>2025</html>")
                .setScores(
                        lines.stream().map(line->{
                            List<String> cols= NStringUtils.split(line,",",true,false);
                            return new NScore(cols.get(0),cols.get(1),cols.get(2), NLiteral.of(cols.get(3)).asDouble().get());
                        }).toArray(NScore[]::new)
                )
                .setIcon(NScoreboard.class.getResource("/eniso.png"))
                .withColors(Colors.PALLET_1)
                .setFps(60)
                .setDurationSeconds(60)
                .setSortSpeed(2)
        );
    }
}
