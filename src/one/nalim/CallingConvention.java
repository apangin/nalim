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
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.runtime.JVMCI;

import java.lang.annotation.Annotation;
import java.nio.ByteBuffer;
import java.util.Objects;

abstract class CallingConvention {

    static CallingConvention getInstance() {
        switch (Arch.CURRENT) {
            case AMD64:
                if (Objects.requireNonNull(Os.CURRENT) == Os.WINDOWS) {
                    return new AMD64WindowsCallingConvention();
                }
                return new AMD64LinuxCallingConvention();
            case AARCH64:
                return new AArch64CallingConvention();
            case RISCV64:
                return new RISCV64CallingConvention();
            default:
                throw new IllegalStateException(
                        "Unsupported architecture: " + System.getProperty("os.arch"));
        }
    }

    abstract void javaToNative(ByteBuffer buf, Class<?>[] types, Annotation[][] annotations);

    abstract void emitCall(ByteBuffer buf, long address);

    protected static int baseOffset(Class<?> type, Annotation[] annotations) {
        if (type.isArray() && type.getComponentType().isPrimitive()) {
            return arrayBaseOffset(type);
        }

        for (Annotation annotation : annotations) {
            if (annotation instanceof FieldOffset) {
                return fieldOffset(type, ((FieldOffset) annotation).value());
            }
        }

        throw new IllegalArgumentException("Unsupported argument type: " + type);
    }

    protected static int arrayBaseOffset(Class<?> arrayType) {
        MetaAccessProvider meta = JVMCI.getRuntime().getHostJVMCIBackend().getMetaAccess();
        JavaKind elementKind = JavaKind.fromJavaClass(arrayType.getComponentType());
        return meta.getArrayBaseOffset(elementKind);
    }

    protected static int fieldOffset(Class<?> type, String fieldName) {
        MetaAccessProvider meta = JVMCI.getRuntime().getHostJVMCIBackend().getMetaAccess();
        ResolvedJavaField[] fields = meta.lookupJavaType(type).getInstanceFields(true);
        if (fields == null || fields.length == 0) {
            throw new IllegalArgumentException(type.getName() + " does not have instance fields");
        }

        if (fieldName.isEmpty()) {
            return fields[0].getOffset();
        }

        for (ResolvedJavaField field : fields) {
            if (field.getName().equals(fieldName)) {
                return field.getOffset();
            }
        }
        throw new IllegalArgumentException("No such field: " + type.getName() + "." + fieldName);
    }

    protected static byte asByte(int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("Not in the byte range: " + value);
        }
        return (byte) value;
    }

    protected static void emit(ByteBuffer buf, int code) {
        if ((code >>> 24) != 0) buf.put((byte) (code >>> 24));
        if ((code >>> 16) != 0) buf.put((byte) (code >>> 16));
        if ((code >>> 8) != 0) buf.put((byte) (code >>> 8));
        if (code != 0) buf.put((byte) code);
    }
}
