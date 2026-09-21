package net.thevpc.ntexup.engine.eval;

import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.engine.eval.git.NTxGitProvider;
import net.thevpc.ntexup.engine.eval.git.NTxGitProviderFactory;
import net.thevpc.nuts.app.NApplication;
import net.thevpc.nuts.artifact.NId;
import net.thevpc.nuts.core.NSession;
import net.thevpc.nuts.core.NStoreKey;
import net.thevpc.nuts.core.NWorkspace;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.mon.NChronometer;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NStringUtils;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper that resolves {@code github://user/repo/...} (and equivalent
 * {@code git@...} / {@code https://github.com/...}) paths by lazily cloning the
 * repository into the Nuts cache and rate-limiting pulls.
 *
 * <p>The actual clone/pull operations are delegated to a {@link NTxGitProvider}
 * selected by {@link NTxGitProviderFactory}: JGit by default, or the native
 * {@code git} executable when explicitly requested
 * ({@code engine.setEnv("git.provider", "system")} / {@code --git-provider system})
 * and available.</p>
 */
public class NTxGitHelper {
    /**
     * Engine env key that controls the git provider
     * ({@code "jgit"} default, {@code "system"} to prefer the native git executable).
     */
    public static final String CONFIG_GIT_PROVIDER = "git.provider";

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

    /**
     * Configure the git provider used by this process. Called by the engine when
     * {@code engine.setEnv(NTxGitHelper.CONFIG_GIT_PROVIDER, ...)} is invoked.
     *
     * @param provider requested provider ({@code "jgit"} or {@code "system"}), or {@code null}
     * @param messages logger (may be {@code null})
     */
    public static void configureGitProvider(String provider, NTxLogger messages) {
        NTxGitProviderFactory.configure(provider, messages);
    }

    /**
     * @return the active {@link NTxGitProvider}.
     */
    public static NTxGitProvider gitProvider(NTxLogger messages) {
        return NTxGitProviderFactory.getProvider(messages);
    }

    /**
     * Clone {@code url} into {@code targetRepositoryDirectory} using the active provider.
     *
     * @throws RuntimeException when the clone fails
     */
    public static void cloneGitRepository(String url, NPath targetRepositoryDirectory, NTxLogger messages) {
        gitProvider(messages).clone(url, targetRepositoryDirectory);
    }

    /**
     * Pull {@code repositoryDirectory} using the active provider.
     *
     * @throws RuntimeException when the pull fails
     */
    public static void pullGitRepository(NPath repositoryDirectory, NTxLogger messages) {
        gitProvider(messages).pull(repositoryDirectory);
    }

    public static NPath resolveGithubPath(String githubPath, NTxLogger messages) {
        NPath userConfHome;
        NPath appCacheFolder = NApplication.of().cacheFolder();
        if (appCacheFolder == null) {
            userConfHome = NPath.of(NStoreKey.ofCache(NId.of("net.thevpc.ntexup:ntexup"))).resolve("github");
        } else {
            userConfHome = appCacheFolder.resolve("ntexup/github");
        }
        if (githubPath.startsWith("github://")) {
            Pattern pattern = Pattern.compile("github://(?<user>[a-zA-Z0-9_-]+)/(?<repo>[a-zA-Z0-9_-]+)((/(?<path>.*))?)");
            Matcher matcher = pattern.matcher(githubPath);
            if (matcher.matches()) {
                String user = matcher.group("user");
                String repo = matcher.group("repo");
                String path = NStringUtils.strip(matcher.group("path"));
                cloneOrPull(userConfHome, user, repo, new String[]{
                        "git@github.com:" + user + "/" + repo + ".git",
                        "https://github.com/" + user + "/" + repo + ".git"
                }, messages);
                return userConfHome.resolve(user + "/" + repo + "/" + path);
            }
        } else if (githubPath.startsWith("git@")) {
            Pattern pattern = Pattern.compile("git@github.com:(?<user>[a-zA-Z0-9_-]+)/(?<repo>[a-zA-Z0-9_-]+).git((/(?<path>.*))?)");
            Matcher matcher = pattern.matcher(githubPath);
            if (matcher.matches()) {
                String user = matcher.group("user");
                String repo = matcher.group("repo");
                String path = NStringUtils.strip(matcher.group("path"));
                //always consider https because we assume it is a public repo
                cloneOrPull(userConfHome, user, repo, new String[]{
                        "git@github.com:" + user + "/" + repo + ".git",
                        "https://github.com/" + user + "/" + repo + ".git"
                }, messages);
                return userConfHome.resolve(user + "/" + repo + "/" + path);
            }
        } else if (githubPath.startsWith("https://github.com")) {
            // https://github.com/thevpc/ntexup-templates.git
            Pattern pattern = Pattern.compile("https://github.com/(?<user>[a-zA-Z0-9_-]+)/(?<repo>[a-zA-Z0-9_-]+).git((/(?<path>.*))?)");
            Matcher matcher = pattern.matcher(githubPath);
            if (matcher.matches()) {
                String user = matcher.group("user");
                String repo = matcher.group("repo");
                String path = NStringUtils.strip(matcher.group("path"));
                cloneOrPull(userConfHome, user, repo, new String[]{
                        "https://github.com/" + user + "/" + repo + ".git",
                        "git@github.com:" + user + "/" + repo + ".git",
                }, messages);
                return userConfHome.resolve(user + "/" + repo + "/" + path);
            }
        }
        throw new IllegalArgumentException("invalid github path : " + githubPath);
    }

    private static void cloneOrPull(NPath userConfHome, String user, String repo, String[] githubPaths, NTxLogger messages) {
        userConfHome.resolve(user).mkdirs();
        NPath localRepo = userConfHome.resolve(user).resolve(repo);
        boolean pulling = false;
        boolean succeeded = false;
        String errorMessage = null;
        NSession session = NSession.of();
        NChronometer c = NChronometer.of();
        try {
            if (localRepo.isDirectory()) {
                Instant now = Instant.now();
                Instant last = (Instant) NWorkspace.of().getProperty("resolveGithubPath.lastPull").orNull();
                if (last == null || now.toEpochMilli() - last.toEpochMilli() > (1000 * 60 * 5)) {
                    pulling = true;
                    messages.log(NMsg.ofC("pull repo at %s", localRepo));
                    pullGitRepository(localRepo, messages);
                } else {
                    NMsg message = NMsg.ofC("ignored pull repo %s to %s", NPath.of(githubPaths[0]), localRepo).asWarning();
                    if (messages != null) {
                        messages.log(message);
                    }
                    if (session.isTrace()) {
                        session.out().println(message);
                    }
                }
                NWorkspace.of().setProperty("resolveGithubPath.lastPull", now);
            } else {
                RuntimeException rex=null;
                for (int i = 0; i < githubPaths.length; i++) {
                    messages.log(NMsg.ofC("cloning repo %s to %s", githubPaths[i], userConfHome.resolve(user)));
                    rex=null;
                    try {
                        cloneGitRepository(githubPaths[i], localRepo, messages);
                        break;
                    }catch (RuntimeException ex) {
                        rex=ex;
                    }
                }
                if(rex!=null){
                    throw rex;
                }
            }
            succeeded = true;
        } catch (RuntimeException e) {
            errorMessage = e.getMessage();
            throw e;
        } finally {
            c.stop();
            if (!succeeded) {
                NMsg message = NMsg.ofC("took %s and failed to %s repo %s to %s : %s", c, pulling ? "pull" : "clone", NPath.of(githubPaths[0]), localRepo, errorMessage)
                        .asSevere();
                if (messages != null) {
                    messages.log(message);
                }
                if (session.isTrace()) {
                    session.out().println(message);
                }
            } else {
                NMsg message = NMsg.ofC("took %s to %s repo %s to %s", c, pulling ? "pull" : "clone", NPath.of(githubPaths[0]), localRepo).asWarning();
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