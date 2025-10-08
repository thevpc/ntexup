package net.thevpc.ntexup.engine.eval;

import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.nuts.app.NApp;
import net.thevpc.nuts.artifact.NId;
import net.thevpc.nuts.command.NExecCmd;
import net.thevpc.nuts.core.NSession;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.platform.NStoreType;
import net.thevpc.nuts.spi.NScopeType;
import net.thevpc.nuts.time.NChronometer;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NStringUtils;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NTxGitHelper {
    public static boolean isGithubFolder(String sp) {
        return
                //  github://thevpc/ntexup-templates/myFolder
                sp.startsWith("github://")
                        // git@github.com:thevpc/ntexup-templates.git/myFolder
                        || sp.startsWith("git@")
                        // https://github.com/thevpc/ntexup-templates.git/myFolder
                        || sp.startsWith("https://github.com/")
                ;
    }

    public static NPath resolveGithubPath(String githubPath, NTxLogger messages) {
        NPath userConfHome;
        NPath appCacheFolder = NApp.of().getCacheFolder();
        if (appCacheFolder == null) {
            userConfHome = NPath.ofIdStore(NId.of("net.thevpc.ntexup:ntexup"), NStoreType.CACHE).resolve("github");
        } else {
            userConfHome = appCacheFolder.resolve("ntexup/github");
        }
        if (githubPath.startsWith("github://")) {
            Pattern pattern = Pattern.compile("github://(?<user>[a-zA-Z0-9_-]+)/(?<repo>[a-zA-Z0-9_-]+)((/(?<path>.*))?)");
            Matcher matcher = pattern.matcher(githubPath);
            if (matcher.matches()) {
                String user = matcher.group("user");
                String repo = matcher.group("repo");
                String path = NStringUtils.trim(matcher.group("path"));
                cloneOrPull(userConfHome, user, repo, "git@github.com:" + user + "/" + repo + ".git", messages);
                return userConfHome.resolve(user + "/" + repo + "/" + path);
            }
        } else if (githubPath.startsWith("git@")) {
            Pattern pattern = Pattern.compile("git@github.com:(?<user>[a-zA-Z0-9_-]+)/(?<repo>[a-zA-Z0-9_-]+).git((/(?<path>.*))?)");
            Matcher matcher = pattern.matcher(githubPath);
            if (matcher.matches()) {
                String user = matcher.group("user");
                String repo = matcher.group("repo");
                String path = NStringUtils.trim(matcher.group("path"));
                cloneOrPull(userConfHome, user, repo, "git@github.com:" + user + "/" + repo + ".git", messages);
                return userConfHome.resolve(user + "/" + repo + "/" + path);
            }
        } else if (githubPath.startsWith("https://github.com")) {
            // https://github.com/thevpc/ntexup-templates.git
            Pattern pattern = Pattern.compile("https://github.com/(?<user>[a-zA-Z0-9_-]+)/(?<repo>[a-zA-Z0-9_-]+).git((/(?<path>.*))?)");
            Matcher matcher = pattern.matcher(githubPath);
            if (matcher.matches()) {
                String user = matcher.group("user");
                String repo = matcher.group("repo");
                String path = NStringUtils.trim(matcher.group("path"));
                cloneOrPull(userConfHome, user, repo, "https://github.com/" + user + "/" + repo + ".git", messages);
                return userConfHome.resolve(user + "/" + repo + "/" + path);
            }
        }
        throw new IllegalArgumentException("invalid github path : " + githubPath);
    }

    private static void cloneOrPull(NPath userConfHome, String user, String repo, String githubPath, NTxLogger messages) {
        userConfHome.resolve(user).mkdirs();
        NPath localRepo = userConfHome.resolve(user).resolve(repo);
        boolean pulling = false;
        boolean succeeded = false;
        String errorMessage = null;
        NSession session = NSession.of();
        NChronometer c = NChronometer.startNow();
        try {
            if (localRepo.isDirectory()) {
                Instant now = Instant.now();
                Instant last = (Instant) NApp.of().getProperty("resolveGithubPath.lastPull",NScopeType.WORKSPACE).orNull();
                if (last == null || now.toEpochMilli() - last.toEpochMilli() > (1000 * 60 * 5)) {
                    pulling = true;
                    NExecCmd.of()
                            .addCommand("git", "pull")
                            .setDirectory(localRepo)
                            .failFast()
                            .run();
                } else {
                    NMsg message = NMsg.ofC("ignored pull repo %s to %s", NPath.of(githubPath), localRepo).asWarning();
                    if (messages != null) {
                        messages.log(message);
                    }
                    if (session.isTrace()) {
                        session.out().println(message);
                    }
                }
                NApp.of().setProperty("resolveGithubPath.lastPull", NScopeType.WORKSPACE, now);
            } else {
                NExecCmd.of()
                        .addCommand("git", "clone", githubPath)
                        .setDirectory(userConfHome.resolve(user))
                        .failFast()
                        .run();
            }
            succeeded = true;
        } catch (RuntimeException e) {
            errorMessage = e.getMessage();
            throw e;
        } finally {
            c.stop();
            if (!succeeded) {
                NMsg message = NMsg.ofC("took %s and failed to %s repo %s to %s : %s", c, pulling ? "pull" : "clone", NPath.of(githubPath), localRepo, errorMessage)
                        .asSevere();
                if (messages != null) {
                    messages.log(message);
                }
                if (session.isTrace()) {
                    session.out().println(message);
                }
            } else {
                NMsg message = NMsg.ofC("took %s to %s repo %s to %s", c, pulling ? "pull" : "clone", NPath.of(githubPath), localRepo).asWarning();
                if (messages != null) {
                    messages.log(message);
                }
                if (session.isTrace()) {
                    session.out().println(message);
                }
            }
        }
    }
}
