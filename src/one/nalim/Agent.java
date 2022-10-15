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

import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Agent {

    public static void premain(String args, Instrumentation inst) throws Exception {
        openJvmciPackages(inst);

        if (args != null) {
            for (String className : args.split(",")) {
                Linker.linkClass(Class.forName(className));
            }
        }
    }

    private static void openJvmciPackages(Instrumentation inst) {
        Optional<Module> jvmciModule = ModuleLayer.boot().findModule("jdk.internal.vm.ci");
        if (jvmciModule.isEmpty()) {
            throw new IllegalStateException("JVMCI module not found. Use -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI");
        }

        Set<Module> unnamed = Set.of(
                ClassLoader.getPlatformClassLoader().getUnnamedModule(),
                ClassLoader.getSystemClassLoader().getUnnamedModule()
        );

        Map<String, Set<Module>> extraExports = Map.of(
                "jdk.vm.ci.code", unnamed,
                "jdk.vm.ci.code.site", unnamed,
                "jdk.vm.ci.hotspot", unnamed,
                "jdk.vm.ci.meta", unnamed,
                "jdk.vm.ci.runtime", unnamed
        );

        inst.redefineModule(jvmciModule.get(), Collections.emptySet(), extraExports,
                Collections.emptyMap(), Collections.emptySet(), Collections.emptyMap());
    }
}
