package org.bitcoin.core;

public class Utils {
    private enum Runtime {
        ANDROID, OPENJDK, ORACLE_JAVA
    }

    private enum OS {
        LINUX, WINDOWS, MAC_OS
    }

    private static Runtime runtime = null;
    private static OS os = null;

    public static boolean isAndroidRuntime() {
        return runtime == Runtime.ANDROID;
    }

}
