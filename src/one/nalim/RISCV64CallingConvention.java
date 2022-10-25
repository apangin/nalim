/*
 * Copyright (C) 2022 Andrei Pangin
 * Copyright (C) 2022 Ludovic Henry
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

class RISCV64CallingConvention extends CallingConvention {

    // RISCV64 calling convention:
    //     Java: x10, x11, x12, x13, x14, x15, x16, x17, stack
    //   Native: x10, x11, x12, x13, x14, x15, x16, x17, stack

    @Override
    public void javaToNative(ByteBuffer buf, Class<?>[] types, Annotation[][] annotations) {
        // nothing to be done, the Java and Native calling convention are the same
    }

    @Override
    public void emitCall(ByteBuffer buf, long address) {
        long imm = address >> 17;
        long upper = imm, lower = imm;
        lower = (lower << 52) >> 52;
        upper -= lower;

        int a0 = (int)(upper);
        int a1 = (int)(lower);
        int a2 = (int)((address >> 6) & 0x7ff);
        int a3 = (int)((address) & 0x3f);

        int zr = 0; // x0
        int t0 = 5; // x5

        buf.putInt(0b0110111 | (t0 << 7) | (a0 << 12));                              // lui t0, a0
        buf.putInt(0b0010011 | (t0 << 7) | (0b000 << 12) | (t0 << 15) | (a1 << 20)); // addi t0, t0, a1
        buf.putInt(0b0010011 | (t0 << 7) | (0b001 << 12) | (t0 << 15) | (11 << 20)); // slli t0, t0, 11
        buf.putInt(0b0010011 | (t0 << 7) | (0b000 << 12) | (t0 << 15) | (a2 << 20)); // addi t0, t0, a2
        buf.putInt(0b0010011 | (t0 << 7) | (0b001 << 12) | (t0 << 15) | ( 6 << 20)); // slli t0, t0, 6
        buf.putInt(0b1100111 | (zr << 7) | (0b000 << 12) | (t0 << 15) | (a3 << 20)); // jalr a3(t0)
    }
}
