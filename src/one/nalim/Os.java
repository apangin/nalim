package one.nalim;

import java.util.Locale;

public enum Os {
    UNSPECIFIED,
    LINUX,
    MACOS,
    WINDOWS,
    UNKNOWN;

    static final Os CURRENT;

    static {
        final String os = System.getProperty("os.name").toLowerCase(Locale.US);
        if (os.contains("linux")) {
            CURRENT = Os.LINUX;
        } else if (os.contains("windows")) {
            CURRENT = Os.WINDOWS;
        } else if (os.contains("mac") || os.contains("darwin") || os.contains("os x")) {
            CURRENT = Os.MACOS;
        } else {
            CURRENT = UNKNOWN;
        }
    }
}
