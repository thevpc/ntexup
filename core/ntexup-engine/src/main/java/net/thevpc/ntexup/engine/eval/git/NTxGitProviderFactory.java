package net.thevpc.ntexup.engine.eval.git;

import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.nuts.command.NExec;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NStringUtils;

import java.util.Locale;

/**
 * Resolves and caches the {@link NTxGitProvider} used for {@code github://} includes.
 *
 * <p>Selection order (first non-blank wins):</p>
 * <ol>
 *     <li>engine-configured provider (see {@code engine.setEnv("git.provider", ...)}
 *     / the {@code --git-provider} CLI option),</li>
 *     <li>system property {@code ntexup.git.provider},</li>
 *     <li>environment variable {@code NTEXUP_GIT_PROVIDER},</li>
 *     <li>default: {@link #PROVIDER_JGIT jgit}.</li>
 * </ol>
 *
 * <p>A value of {@link #PROVIDER_SYSTEM system} is honoured only when a native
 * {@code git} executable is actually available (checked once and cached); otherwise the
 * implementation falls back to JGit with a warning.</p>
 */
public final class NTxGitProviderFactory {

    public static final String PROVIDER_JGIT = "jgit";
    public static final String PROVIDER_SYSTEM = "system";

    public static final String PROPERTY_GIT_PROVIDER = "ntexup.git.provider";
    public static final String ENV_GIT_PROVIDER = "NTEXUP_GIT_PROVIDER";

    private static volatile String configuredProvider;
    private static volatile NTxGitProvider cachedProvider;
    private static volatile Boolean systemGitAvailable;

    private NTxGitProviderFactory() {
    }

    /**
     * Configure the provider from the engine (used by
     * {@code engine.setEnv("git.provider", ...)}). Passing {@code null} clears the
     * explicit preference.
     *
     * @param provider requested provider ({@code "jgit"}, {@code "system"} or {@code null})
     * @param messages logger for warnings (may be {@code null})
     */
    public static void configure(String provider, NTxLogger messages) {
        configuredProvider = normalize(provider);
        if (configuredProvider != null
                && !PROVIDER_JGIT.equals(configuredProvider)
                && !PROVIDER_SYSTEM.equals(configuredProvider)) {
            warn(messages, "unknown git provider '" + provider + "', falling back to '" + PROVIDER_JGIT + "'");
            configuredProvider = PROVIDER_JGIT;
        }
        cachedProvider = null;
    }

    public static String configuredProvider() {
        return configuredProvider;
    }

    /**
     * @return the active {@link NTxGitProvider}, cached after first resolution.
     */
    public static NTxGitProvider getProvider(NTxLogger messages) {
        NTxGitProvider p = cachedProvider;
        if (p == null) {
            synchronized (NTxGitProviderFactory.class) {
                p = cachedProvider;
                if (p == null) {
                    p = createProvider(messages);
                    cachedProvider = p;
                }
            }
        }
        return p;
    }

    private static NTxGitProvider createProvider(NTxLogger messages) {
        String selected = normalize(NStringUtils.firstNonBlank(
                configuredProvider,
                System.getProperty(PROPERTY_GIT_PROVIDER),
                System.getenv(ENV_GIT_PROVIDER)
        ));
        if (PROVIDER_SYSTEM.equals(selected)) {
            if (isSystemGitAvailable()) {
                return new NTxSystemGitProvider();
            }
            warn(messages, "preferring system git but no 'git' executable was found, falling back to '" + PROVIDER_JGIT + "'");
        }
        return new NTxJGitProvider();
    }

    /**
     * Probe once (process-wide) whether a native {@code git} executable is usable.
     *
     * @return {@code true} if {@code git --version} runs successfully
     */
    public static boolean isSystemGitAvailable() {
        Boolean v = systemGitAvailable;
        if (v == null) {
            synchronized (NTxGitProviderFactory.class) {
                v = systemGitAvailable;
                if (v == null) {
                    v = probeSystemGit();
                    systemGitAvailable = v;
                }
            }
        }
        return v;
    }

    private static boolean probeSystemGit() {
        try {
            NExec git = NExec.ofSystem("git", "--version");
            git.grabOut().grabErr().run();
            return !git.resultException().isPresent();
        } catch (Throwable th) {
            return false;
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String s = NStringUtils.strip(value).toLowerCase(Locale.ROOT);
        if (s.isEmpty()) {
            return null;
        }
        return s;
    }

    private static void warn(NTxLogger messages, String message) {
        if (messages != null) {
            messages.log(NMsg.ofC("%s", message).asWarning());
        }
    }
}