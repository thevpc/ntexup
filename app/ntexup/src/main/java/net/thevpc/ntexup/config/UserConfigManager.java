package net.thevpc.ntexup.config;

import net.thevpc.nuts.artifact.NId;
import net.thevpc.nuts.elem.NElementParser;
import net.thevpc.nuts.elem.NElementWriter;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.platform.NStoreType;

public class UserConfigManager {
    private NPath userConfigFile;

    public UserConfigManager() {
        this.userConfigFile = NPath.ofIdStore(NId.of("net.thevpc.nuts:nuts"), NStoreType.CONF).resolve("user-config.tson");
    }


    public UserConfigs validate(UserConfigs config) {
        return (config == null ? new UserConfigs() : config).validate();
    }

    public UserConfig validate(UserConfig config) {
        return (config == null ? new UserConfig() : config).validate();
    }

    public UserConfigs updateUser(UserConfigs config, UserConfig user) {
        if (user != null) {
            validate(user);
            validate(config);
            config.updateUser(user);
        }
        return config;
    }

    public void saveUserConfig(UserConfig user) {
        UserConfigs userConfigs = loadUserConfigs();
        updateUser(userConfigs, user);
        saveUserConfigs(userConfigs);
        userConfigFile.mkParentDirs();
        NElementWriter.ofTson().write(user,userConfigFile.mkParentDirs());
    }

    public void saveUserConfigs(UserConfigs config) {
        config=validate(config);
        NElementWriter.ofTson().write(config,userConfigFile.mkParentDirs());
    }

    public UserConfig loadUserConfig(String id) {
        return loadUserConfigs().findUser(id);
    }

    public UserConfigs loadUserConfigs() {
        UserConfigs config = null;
        if (userConfigFile.isRegularFile()) {
            config = NElementParser.ofTson().parse(userConfigFile, UserConfigs.class);
        }
        config=validate(config);
        return config;
    }

    private int compare(Object a, Object b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null && b != null) {
            return -1;
        }
        if (a != null && b == null) {
            return 1;
        }
        if (a instanceof Comparable) {
            return ((Comparable) a).compareTo(b);
        }
        throw new IllegalArgumentException("invalid comparison");
    }
}
