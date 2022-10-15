package one.nalim.example;

import one.nalim.Code;
import one.nalim.Linker;

public class Cpu {

    // rdtsc
    // shl    $0x20,%rdx
    // or     %rdx,%rax
    // ret
    @Code({15, 49, 72, -63, -30, 32, 72, 9, -48, -61})
    public static native long rdtsc();

    static {
        Linker.linkClass(Cpu.class);
    }
}
