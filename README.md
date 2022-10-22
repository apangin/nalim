# nalim

Nalim is a library for linking Java methods to native functions using
[JVMCI](https://openjdk.org/jeps/243) (JVM compiler interface).

Unlike other Java frameworks for native library access, nalim does not
use JNI and therefore does not incur [JNI related overhead](https://stackoverflow.com/a/24747484/3448419).

When calling a native function with nalim
 - a thread does not switch from `in_Java` to `in_native` state and back;
 - no memory barrier is involved;
 - no JNI handles are created;
 - exception checks and safepoint checks are omitted;
 - native function can access primitive arrays directly in the heap.

As a result, native calls become faster comparing to JNI, especially when
a target function is short. In this sense, nalim is similar to
[JNI Critical Natives](https://stackoverflow.com/a/36309652/3448419),
but relies on a standard supported interface. JNI Critical Natives
have been [deprecated](https://bugs.openjdk.org/browse/JDK-8233343) in JDK 16
and [obsoleted](https://bugs.openjdk.org/browse/JDK-8258192) since JDK 18,
so nalim can serve as a replacement.

### Examples

#### 1. Basic usage

```java
public class Libc {

    @Link
    public static native int getuid();

    @Link
    public static native int getgid();

    static {
        Linker.linkClass(Libc.class);
    }
}
```
```
System.out.println("My user id = " + Libc.getuid());
```

#### 2. Linking by a different name 

```java
public class Mem {

   @Link(name = "malloc")
   public static native long allocate(long size);

   @Link(name = "free")
   public static native void release(long ptr);

   static {
      Linker.linkClass(Mem.class);
   }
}
```

#### 3. Working with arrays

```java
@Library("ssl")
public class LibSSL {

    public static byte[] sha256(byte[] data) {
        byte[] digest = new byte[32];
        SHA256(data, data.length, digest);
        return digest;
    }

    @Link
    private static native void SHA256(byte[] data, int len, byte[] digest);
}
```

#### 4. Inlining raw machine code

```java
public class Cpu {

    // rdtsc
    // shl    $0x20,%rdx
    // or     %rdx,%rax
    // ret
    @Code("0f31 48c1e220 4809d0 c3")
    public static native long rdtsc();

    static {
        Linker.linkClass(Cpu.class);
    }
}
```

### Running

#### 1. As an agent

```
java -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI \
     -javaagent:nalim.jar -cp <classpath> MainClass
```

This is the simplest way to add nalim to your application,
as the agent exports all required JDK internal packages for you.

The agent optionally accepts a list of classes whose native methods
will be automatically linked at startup:
```
-javaagent:nalim.jar=com.example.MyLib,com.example.OtherLib
```

#### 2. On the classpath

If not adding nalim as an agent, you'll have to add all required
`--add-exports` manually.

```
java -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI                \
     --add-exports jdk.internal.vm.ci/jdk.vm.ci.code=ALL-UNNAMED      \
     --add-exports jdk.internal.vm.ci/jdk.vm.ci.code.site=ALL-UNNAMED \
     --add-exports jdk.internal.vm.ci/jdk.vm.ci.hotspot=ALL-UNNAMED   \
     --add-exports jdk.internal.vm.ci/jdk.vm.ci.meta=ALL-UNNAMED      \
     --add-exports jdk.internal.vm.ci/jdk.vm.ci.runtime=ALL-UNNAMED   \
     -cp nalim.jar:app.jar MainClass 
```

### Performance

JMH benchmark for comparing regular JNI calls with nalim calls is available
[here](https://github.com/apangin/nalim/blob/master/example/one/nalim/bench). 

The following results were obtained on Intel Core i7-1280P CPU with JDK 17.0.4.1.

#### Simple native method

```
static native int add(int a, int b);
```

```
Benchmark           Mode  Cnt  Score   Error  Units
JniBench.add_jni    avgt   10  6,535 ± 0,225  ns/op
JniBench.add_nalim  avgt   10  0,862 ± 0,035  ns/op
```

#### Array processing

```
static native long max(long[] array, int length);
```

```
Benchmark           (length)  Mode  Cnt    Score   Error  Units
JniBench.max_jni          10  avgt   10   25,103 ± 0,994  ns/op
JniBench.max_jni         100  avgt   10   55,981 ± 2,930  ns/op
JniBench.max_jni        1000  avgt   10  433,106 ± 1,661  ns/op
JniBench.max_nalim        10  avgt   10    3,477 ± 0,215  ns/op
JniBench.max_nalim       100  avgt   10   38,368 ± 2,348  ns/op
JniBench.max_nalim      1000  avgt   10  420,540 ± 4,049  ns/op
```

### Supported platforms

 - **Linux:** amd64 aarch64
 - **macOS:** amd64 aarch64
 - **Windows:** amd64

### Limitations

A native function called with nalim has certain limitations comparing to a regular
JNI function.

1. It must be `static`.
2. It does not have access to `JNIEnv` and therefore cannot call JNI functions,
   in particular, it cannot throw exceptions.
3. Only primitive types and primitive arrays can be passed as arguments.
4. A function must return as soon as possible, since it blocks JVM from reaching 
   a safepoint.
