package net.thevpc.ntexup.cmdline.options;

import net.thevpc.nuts.util.NIllegalArgumentException;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Options {
    public List<ActionOptions> actionOptions = new ArrayList<>();
    public Map<String,String> vars=new LinkedHashMap<>();

    public <T extends ActionOptions> T get(Class<T> cls){
        for (ActionOptions actionOption : actionOptions) {
            if(actionOption.getClass().equals(cls)) {
                return (T) actionOption;
            }
        }
        return null;
    }

    public boolean contains(Action cls){
        for (ActionOptions actionOption : actionOptions) {
            if(actionOption.action==cls) {
                return true;
            }
        }
        return false;
    }

    public <T extends ActionOptions> T getOrCreate(Class<T> cls){
        for (ActionOptions actionOption : actionOptions) {
            if(actionOption.getClass().equals(cls)) {
                return (T) actionOption;
            }
        }
        T t = null;
        try {
            t = cls.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        actionOptions.add(t);
        return t;
    }


}
