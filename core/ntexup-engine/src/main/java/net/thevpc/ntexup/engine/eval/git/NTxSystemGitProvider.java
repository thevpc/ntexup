package net.thevpc.ntexup.engine.eval.git;

import net.thevpc.nuts.command.NExec;
import net.thevpc.nuts.io.NPath;

/**
 * Git provider that shells out to the native {@code git} executable via
 * Nuts {@link NExec}. Only used when explicitly requested
 * ({@code ntexup.git.provider=system} or {@code --git-provider system})
 * <i>and</i> a {@code git} executable is available.
 */
public class NTxSystemGitProvider implements NTxGitProvider {

    @Override
    public String name() {
        return NTxGitProviderFactory.PROVIDER_SYSTEM;
    }

    @Override
    public void clone(String url, NPath targetRepositoryDirectory) {
        NPath parent = targetRepositoryDirectory.parent();
        if (parent != null) {
            parent.mkdirs();
        }
        NExec.ofSystem("git", "clone", url, targetRepositoryDirectory.toString())
                .failFast(true)
                .run();
    }

    @Override
    public void pull(NPath repositoryDirectory) {
        NExec.ofSystem("git", "pull")
                .directory(repositoryDirectory)
                .failFast(true)
                .run();
    }
}