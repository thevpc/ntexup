package net.thevpc.ntexup.engine.eval.git;

import net.thevpc.nuts.io.NPath;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure-Java git provider backed by Eclipse JGit. Requires no external
 * {@code git} executable.
 */
public class NTxJGitProvider implements NTxGitProvider {

    @Override
    public String name() {
        return NTxGitProviderFactory.PROVIDER_JGIT;
    }

    @Override
    public void clone(String url, NPath targetRepositoryDirectory) {
        NPath parent = targetRepositoryDirectory.parent();
        if (parent != null) {
            parent.mkdirs();
        }
        try (Git git = Git.cloneRepository()
                .setURI(url)
                .setDirectory(targetRepositoryDirectory.toFile().get())
                .call()) {
            // nothing to do, cloned repository is on disk
        } catch (GitAPIException e) {
            deleteRecursively(targetRepositoryDirectory);
            throw new RuntimeException(
                    "unable to clone repository " + url + " to " + targetRepositoryDirectory + " : " + e.getMessage(),
                    e);
        }
    }

    @Override
    public void pull(NPath repositoryDirectory) {
        try (Git git = Git.open(repositoryDirectory.toFile().get())) {
            normalizeRemoteToHttps(git);
            git.pull().call();
        } catch (GitAPIException | IOException e) {
            throw new RuntimeException(
                    "unable to pull repository " + repositoryDirectory + " : " + e.getMessage(),
                    e);
        }
    }

    private static void normalizeRemoteToHttps(Git git) throws IOException {
        StoredConfig cfg = git.getRepository().getConfig();
        String origin = cfg.getString("remote", "origin", "url");
        String https = toHttps(origin);
        if (https != null) {
            cfg.setString("remote", "origin", "url", https);
            cfg.save();
        }
    }

    /**
     * Rewrites an scp-like remote url ({@code user@host:path}) into its
     * {@code https://host/path} equivalent. JGit has no SSH transport, so a
     * cached repository previously cloned by the native {@code git} with a
     * {@code git@host:...} origin cannot be pulled otherwise.
     *
     * @return {@code null} when the url is not scp-like (nothing to rewrite)
     */
    static String toHttps(String url) {
        if (url == null) {
            return null;
        }
        Matcher m = SCP_REMOTE_PATTERN.matcher(url);
        if (!m.matches()) {
            return null;
        }
        return "https://" + m.group(2) + "/" + m.group(3);
    }

    private static final Pattern SCP_REMOTE_PATTERN =
            Pattern.compile("^([^@/]+)@([^:]+):(.+)$");

    private static void deleteRecursively(NPath p) {
        if (p == null) {
            return;
        }
        try {
            Path path = p.toFile().get().toPath();
            if (!Files.exists(path)) {
                return;
            }
            try (var walk = Files.walk(path)) {
                walk.sorted(Comparator.reverseOrder()).forEach(x -> {
                    try {
                        Files.deleteIfExists(x);
                    } catch (IOException ignored) {
                        // best effort cleanup so the next clone attempt starts fresh
                    }
                });
            }
        } catch (IOException ignored) {
            // best effort cleanup so the next clone attempt starts fresh
        }
    }
}