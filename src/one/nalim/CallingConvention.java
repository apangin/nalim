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

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.runtime.JVMCI;

import java.nio.ByteBuffer;

abstract class CallingConvention {

    static CallingConvention getInstance() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (!arch.contains("64")) {
            throw new IllegalStateException("Unsupported architecture: " + arch);
        }

        if (arch.contains("aarch") || arch.contains("arm")) {
            return new AArch64CallingConvention();
        }

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            return new AMD64WindowsCallingConvention();
        } else {
            return new AMD64LinuxCallingConvention();
        }
    }

    abstract void javaToNative(ByteBuffer buf, Class<?>... types);

    abstract void emitCall(ByteBuffer buf, long address);

    protected static int arrayBaseOffset(Class<?> arrayType) {
        JavaKind elementKind = JavaKind.fromJavaClass(arrayType.getComponentType());
        return JVMCI.getRuntime().getHostJVMCIBackend().getMetaAccess().getArrayBaseOffset(elementKind);
    }

    protected static void emit(ByteBuffer buf, int code) {
        if ((code >>> 24) != 0) buf.put((byte) (code >>> 24));
        if ((code >>> 16) != 0) buf.put((byte) (code >>> 16));
        if ((code >>> 8) != 0) buf.put((byte) (code >>> 8));
        if (code != 0) buf.put((byte) code);
    }
}
