package net.thevpc.ntexup.cmdline.options;

public enum OutputFormat {
    PDF, IMAGE, HTML_STATIC, NTXP,
    /** render every page in memory (low resolution) to surface errors, write nothing */
    CHECK
}
