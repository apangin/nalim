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

import java.lang.annotation.Annotation;
import java.nio.ByteBuffer;

class AArch64CallingConvention extends CallingConvention {

    // AArch64 calling convention:
    //     Java: x1, x2, x3, x4, x5, x6, x7, x0, stack
    //   Native: x0, x1, x2, x3, x4, x5, x6, x7, stack

    @Override
    public void javaToNative(ByteBuffer buf, Class<?>[] types, Annotation[][] annotations) {
        if (types.length >= 8) {
            // 8th Java argument clashes with the 1st native arg
            buf.putInt(0xaa0003e8);  // mov x8, x0
        }

        int index = 0;
        for (int i = 0; i < types.length; i++) {
            Class<?> type = types[i];
            if (type.isPrimitive()) {
                if (index < 8 && type != float.class && type != double.class) {
                    // mov x0, x1
                    buf.putInt((type == long.class ? 0xaa0003e0 : 0x2a0003e0) | index | (index + 1) << 16);
                    index++;
                }
            } else if (index < 8) {
                // add x0, x1, #offset
                buf.putInt(0x91000000 | index | (index + 1) << 5 | baseOffset(type, annotations[i]) << 10);
                index++;
            } else {
                throw new IllegalArgumentException("Too many object arguments");
            }
        }
    }

    @Override
    public void emitCall(ByteBuffer buf, long address) {
        int a0 = (int) address & 0xffff;
        int a1 = (int) (address >>> 16) & 0xffff;
        int a2 = (int) (address >>> 32) & 0xffff;
        int a3 = (int) (address >>> 48);

        buf.putInt(0xd2800009 | a0 << 5);               // movz x9, #0xffff
        if (a1 != 0) buf.putInt(0xf2a00009 | a1 << 5);  // movk x9, #0xffff, lsl #16
        if (a2 != 0) buf.putInt(0xf2c00009 | a2 << 5);  // movk x9, #0xffff, lsl #32
        if (a3 != 0) buf.putInt(0xf2e00009 | a3 << 5);  // movk x9, #0xffff, lsl #48

        buf.putInt(0xd61f0120);                         // br x9
    }
}
