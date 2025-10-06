package net.thevpc.ntexup.cmdline;

import net.thevpc.nuts.util.NIllegalArgumentException;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NMsg;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Options {
    public List<NPath> paths = new ArrayList<>();
    public Action action = Action.OPEN;
    public OutputFormat outputFormat = OutputFormat.PDF;
    public NPath output;
    public boolean documentation;
    public boolean dump;
    public String templateUrl;
    public boolean showLogs;
    public boolean reopen;
    public boolean terminalMode;
    public boolean guiMode;
    public Map<String,String> vars=new LinkedHashMap<>();

    public Options setTerminalMode(boolean terminalMode) {
        if(guiMode && terminalMode) {
            throw new NIllegalArgumentException(NMsg.ofC("cannot mix terminal mode and gui mode"));
        }
        this.terminalMode = terminalMode;
        return this;
    }

    public Options setGuiMode(boolean guiMode) {
        if(guiMode && terminalMode) {
            throw new NIllegalArgumentException(NMsg.ofC("cannot mix terminal mode and gui mode"));
        }
        this.guiMode = guiMode;
        return this;
    }
}
