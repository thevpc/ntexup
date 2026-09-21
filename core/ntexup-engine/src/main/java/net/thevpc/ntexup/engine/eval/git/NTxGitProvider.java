package net.thevpc.ntexup.engine.eval.git;

import net.thevpc.nuts.io.NPath;

/**
 * Strategy used to clone and/or pull git repositories referenced from
 * {@code github://}, {@code git@...} and {@code https://github.com/...} paths.
 *
 * Two implementations are provided in the engine:
 * <ul>
 *     <li>{@link NTxJGitProvider} — pure-Java implementation (JGit), the default.</li>
 *     <li>{@link NTxSystemGitProvider} — shells out to the native {@code git} executable.</li>
 * </ul>
 *
 * The active provider is resolved by {@link NTxGitProviderFactory}.
 */
public interface NTxGitProvider {

    /**
     * @return provider unique identifier ({@code "jgit"} or {@code "system"}).
     */
    String name();

    /**
     * Clone {@code url} into {@code targetRepositoryDirectory}.
     *
     * @param url                       remote repository url
     * @param targetRepositoryDirectory target directory that becomes the cloned repository;
     *                                  its parent must exist
     * @throws RuntimeException when the clone fails
     */
    void clone(String url, NPath targetRepositoryDirectory);

    /**
     * Update an existing local repository with a pull.
     *
     * @param repositoryDirectory existing local git repository directory
     * @throws RuntimeException when the pull fails
     */
    void pull(NPath repositoryDirectory);
}