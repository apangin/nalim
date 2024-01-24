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

class AMD64LinuxCallingConvention extends CallingConvention {

    // x64 calling convention (Linux, macOS):
    //     Java: rsi, rdx, rcx,  r8,  r9, rdi, stack
    //   Native: rdi, rsi, rdx, rcx,  r8,  r9, stack

    private static final int SAVE_LAST_ARG =
            0x4889f8;  // mov  rax, rdi

    private static final int[] MOVE_INT_ARG = {
            0x89f7,    // mov  edi, esi
            0x89d6,    // mov  esi, edx
            0x89ca,    // mov  edx, ecx
            0x4489c1,  // mov  ecx, r8d
            0x4589c8,  // mov  r8d, r9d
            0x4189c1,  // mov  r9d, eax
    };

    private static final int[] MOVE_LONG_ARG = {
            0x4889f7,  // mov  rdi, rsi
            0x4889d6,  // mov  rsi, rdx
            0x4889ca,  // mov  rdx, rcx
            0x4c89c1,  // mov  rcx, r8
            0x4d89c8,  // mov  r8, r9
            0x4989c1,  // mov  r9, rax
    };

    private static final int[] MOVE_OBJ_ARG = {
            0x488d7e,  // lea  rdi, [rsi+N]
            0x488d72,  // lea  rsi, [rdx+N]
            0x488d51,  // lea  rdx, [rcx+N]
            0x498d48,  // lea  rcx, [r8+N]
            0x4d8d41,  // lea  r8, [r9+N]
            0x4c8d48,  // lea  r9, [rax+N]
    };

    @Override
    public void javaToNative(ByteBuffer buf, Class<?>[] types, Annotation[][] annotations) {
        if (types.length >= 6) {
            // 6th Java argument clashes with the 1st native arg
            emit(buf, SAVE_LAST_ARG);
        }

        int index = 0;
        for (int i = 0; i < types.length; i++) {
            Class<?> type = types[i];
            if (type.isPrimitive()) {
                if (index < 6 && type != float.class && type != double.class) {
                    emit(buf, (type == long.class ? MOVE_LONG_ARG : MOVE_INT_ARG)[index++]);
                }
            } else if (index < 6) {
                emit(buf, MOVE_OBJ_ARG[index++]);
                buf.put(asByte(baseOffset(type, annotations[i])));
            } else {
                throw new IllegalArgumentException("Too many object arguments");
            }
        }
    }

    @Override
    public void emitCall(ByteBuffer buf, long address) {
        buf.putShort((short) 0xb848).putLong(address);  // mov rax, address
        buf.putShort((short) 0xe0ff);                   // jmp rax
    }
}
