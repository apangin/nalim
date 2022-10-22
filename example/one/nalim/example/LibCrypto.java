package one.nalim.example;

import one.nalim.Library;
import one.nalim.Link;

@Library("crypto")
public class LibCrypto {

    public static byte[] sha256(byte[] data) {
        byte[] digest = new byte[32];
        SHA256(data, data.length, digest);
        return digest;
    }

    @Link
    private static native void SHA256(byte[] data, int len, byte[] digest);
}
