package org.bitcoin.core;

import com.google.common.io.BaseEncoding;

public class Utils {

    public static final BaseEncoding HEX = BaseEncoding.base16().lowerCase();


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
    /**
     * Returns a copy of the given byte array in reverse order.
     */
    public static byte[] reverseBytes(byte[] bytes) {
        // We could use the XOR trick here but it's easier to understand if we don't. If we find this is really a
        // performance issue the matter can be revisited.
        byte[] buf = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++)
            buf[i] = bytes[bytes.length - 1 - i];
        return buf;
    }
}
