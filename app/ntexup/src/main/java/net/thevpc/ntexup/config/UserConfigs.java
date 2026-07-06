package net.thevpc.ntexup.config;

import net.thevpc.nuts.util.NStringUtils;

import java.util.*;

public class UserConfigs implements Cloneable {
    private String main;
    private UserConfig[] users;

    public String getMain() {
        return main;
    }

    public UserConfigs setMain(String main) {
        this.main = main;
        return this;
    }

    public UserConfig[] getUsers() {
        return users;
    }

    public UserConfigs setUsers(UserConfig[] users) {
        this.users = users;
        return this;
    }

    public UserConfig findUser(String id) {
        validate();
        id=NStringUtils.firstNonBlank(NStringUtils.strip(id), "default");
        for (UserConfig user : users) {
            if(Objects.equals(user.getId(),id)){
                return user;
            }
        }
        return null;
    }

    public UserConfigs updateUser(UserConfig user) {
        validate();
        if (user != null) {
            user.validate();
            for (UserConfig c : this.getUsers()) {
                if (Objects.equals(c.getId(), user.getId())) {
                    c.mergeAlways(user);
                    return this;
                }
            }
            List<UserConfig> old = new ArrayList<>(Arrays.asList(this.users));
            old.add(user);
            this.users = old.toArray(new UserConfig[old.size()]);
        }
        return this;
    }

    public UserConfigs mergeNonBlankOtherUser(UserConfig user) {
        validate();
        if (user != null) {
            user.validate();
            for (UserConfig c : this.getUsers()) {
                if (Objects.equals(c.getId(), user.getId())) {
                    c.mergeNonBlankOther(user);
                    return this;
                }
            }
            List<UserConfig> old = new ArrayList<>(Arrays.asList(this.users));
            old.add(user);
            this.users = old.toArray(new UserConfig[old.size()]);
        }
        return this;
    }

    public UserConfigs mergeBlankSelfUser(UserConfig user) {
        validate();
        if (user != null) {
            user.validate();
            for (UserConfig c : this.getUsers()) {
                if (Objects.equals(c.getId(), user.getId())) {
                    c.mergeBlankSelf(user);
                    return this;
                }
            }
            List<UserConfig> old = new ArrayList<>(Arrays.asList(this.users));
            old.add(user);
            this.users = old.toArray(new UserConfig[old.size()]);
        }
        return this;
    }

    public UserConfigs validate() {
        this.setMain(NStringUtils.stripToNull(this.getMain()));
        if (this.getUsers() == null) {
            this.setUsers(new UserConfig[0]);
        }
        Map<String, UserConfig> all = new LinkedHashMap<>();
        if (this.getUsers() != null) {
            for (UserConfig u : this.getUsers()) {
                u = (u == null ? new UserConfig() : u).validate();
                UserConfig old = all.get(u.getId());
                if (old != null) {
                    old.mergeBlankSelf(u);
                } else {
                    all.put(u.getId(), u);
                }
            }
        }
        this.setUsers(all.values().toArray(new UserConfig[0]));
        return this;
    }

}
