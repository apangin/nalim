package one.nalim.example;

import one.nalim.Link;
import one.nalim.Linker;

public class Mem {

    @Link(name = "malloc")
    public static native long allocate(long size);

    @Link(name = "free")
    public static native void release(long ptr);

    static {
        Linker.linkClass(Mem.class);
    }
}
