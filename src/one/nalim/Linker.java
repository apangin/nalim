/*
 * Copyright (C) 2022 Andrei Pangin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package one.nalim;

import jdk.vm.ci.code.site.DataPatch;
import jdk.vm.ci.code.site.Site;
import jdk.vm.ci.hotspot.HotSpotCompiledCode;
import jdk.vm.ci.hotspot.HotSpotCompiledNmethod;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.runtime.JVMCI;
import jdk.vm.ci.runtime.JVMCIBackend;
import jdk.vm.ci.runtime.JVMCICompiler;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Links native methods to the provided machine code using JVMCI.
 */
public class Linker {
    private static final JVMCIBackend jvmci = JVMCI.getRuntime().getHostJVMCIBackend();
    private static final ConcurrentHashMap<String, Boolean> nativeLibraries = new ConcurrentHashMap<>();
    private static final CallingConvention callingConvention = CallingConvention.getInstance();

    static {
        loadLibrary("java");
    }

    public static void loadLibrary(String name) {
        if (nativeLibraries.putIfAbsent(name, Boolean.TRUE) == null) {
            if (name.indexOf('/') >= 0 || name.indexOf('\\') > 0) {
                System.load(name);
            } else {
                System.loadLibrary(name);
            }
        }
    }

    public static long findAddress(String symbol) {
        try {
            Method m = JavaInternals.getPrivateMethod(ClassLoader.class, "findNative", ClassLoader.class, String.class);
            return (long) m.invoke(null, Linker.class.getClassLoader(), symbol);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void linkClass(Class<?> c) {
        Library library = AnnotationUtil.findLibrary(c, Os.CURRENT, Arch.CURRENT);
        if (library != null) {
            loadLibrary(library.value());
        }

        for (Method m : c.getDeclaredMethods()) {
            if (isStaticNative(m)) {
                linkMethod(m);
            }
        }
    }

    public static void linkMethod(Method m) {
        checkMethodType(m);

        Code code = AnnotationUtil.findCode(m, Os.CURRENT, Arch.CURRENT);
        if (code != null) {
            installCode(m, parseHex(code.value()));
            return;
        }

        Link link = m.getAnnotation(Link.class);
        if (link != null) {
            linkMethod(m, link.name(), link.naked());
        } else {
            linkMethod(m, m.getName(), false);
        }
    }

    public static void linkMethod(Method m, String symbol, boolean naked) {
        checkMethodType(m);

        Library library = AnnotationUtil.findLibrary(m, Os.CURRENT, Arch.CURRENT);
        if (library != null) {
            loadLibrary(library.value());
        }

        if (symbol.isEmpty()) {
            symbol = m.getName();
        }
        long address = findAddress(symbol);
        if (address == 0) {
            throw new IllegalArgumentException("Symbol not found: " + symbol);
        }

        ByteBuffer buf = ByteBuffer.allocate(100).order(ByteOrder.nativeOrder());
        if (!naked) {
            callingConvention.javaToNative(buf, m.getParameterTypes(), m.getParameterAnnotations());
        }
        callingConvention.emitCall(buf, address);

        installCode(m, buf.array(), buf.position());
    }

    private static void checkMethodType(Method m) {
        if (!isStaticNative(m)) {
            throw new IllegalArgumentException("Method must be static native: " + m);
        }
    }

    private static boolean isStaticNative(Method m) {
        int modifiers = m.getModifiers();
        return Modifier.isStatic(modifiers) && Modifier.isNative(modifiers);
    }

    private static byte[] parseHex(String hex) {
        hex = hex.replaceAll("\\s+", "");

        int length = hex.length();
        if ((length & 1) != 0) {
            throw new IllegalArgumentException("Invalid hex string");
        }

        byte[] code = new byte[length / 2];
        for (int i = 0; i < code.length; i++) {
            code[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return code;
    }

    public static void installCode(Method m, byte[] code) {
        installCode(m, code, code.length);
    }

    public static void installCode(Method m, byte[] code, int length) {
        ResolvedJavaMethod rm = jvmci.getMetaAccess().lookupJavaMethod(m);

        HotSpotCompiledNmethod nm = new HotSpotCompiledNmethod(
                m.getName(),
                code,
                length,
                new Site[0],
                new Assumptions.Assumption[0],
                new ResolvedJavaMethod[0],
                new HotSpotCompiledCode.Comment[0],
                new byte[0],
                1,
                new DataPatch[0],
                true,
                0,
                null,
                (HotSpotResolvedJavaMethod) rm,
                JVMCICompiler.INVOCATION_ENTRY_BCI,
                1,
                0,
                false
        );

        jvmci.getCodeCache().setDefaultCode(rm, nm);
    }
}
