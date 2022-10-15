package one.nalim.example;

import one.nalim.Link;
import one.nalim.Linker;

public class Time {

    public static long[] current() {
        long[] result = new long[2];
        clock_gettime(0, result);
        return result;
    }

    @Link
    private static native void clock_gettime(int clk_id, long[] tp);

    static {
        Linker.linkClass(Time.class);
    }
}
