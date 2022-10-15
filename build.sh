#!/bin/sh
javac --add-modules jdk.internal.vm.ci \
      --add-exports jdk.internal.vm.ci/jdk.vm.ci.code=ALL-UNNAMED \
      --add-exports jdk.internal.vm.ci/jdk.vm.ci.code.site=ALL-UNNAMED \
      --add-exports jdk.internal.vm.ci/jdk.vm.ci.hotspot=ALL-UNNAMED \
      --add-exports jdk.internal.vm.ci/jdk.vm.ci.meta=ALL-UNNAMED \
      --add-exports jdk.internal.vm.ci/jdk.vm.ci.runtime=ALL-UNNAMED \
      --source 11 --target 11 \
      -d . src/one/nalim/*.java

jar cfm nalim.jar MANIFEST.MF one
