package one.nalim.example;

import one.nalim.Arch;
import one.nalim.Code;
import one.nalim.Linker;
import one.nalim.Os;

public class CpuId {

    @Code(
            os = Os.WINDOWS,
            arch = Arch.AMD64,
            value = "4989D9 89D0 0FA2 418900 41895804 41894808 4189500C 4C89CB C3"
    )
    @Code(
            os = Os.LINUX,
            arch = Arch.AMD64,
            value = "4889D7 89F0 4889DE 0FA2 8907 895F04 894F08 89570C 4889F3 C3"
    )
    private static native void cpuid(int func, int[] out);

    static {
        Linker.linkClass(CpuId.class);
    }
}
