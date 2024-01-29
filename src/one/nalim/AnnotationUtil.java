package one.nalim;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.function.Function;

final class AnnotationUtil {

    private static final int MAX_SCORE = 3;
    private static final int NO_MATCH = -1;

    static Library findLibrary(AnnotatedElement element, Os expectedOs, Arch expectedArch) {
        return find(
                Library.class,
                element.getAnnotation(LibrarySet.class),
                element.getAnnotation(Library.class),
                LibrarySet::value, Library::os, Library::arch,
                expectedOs, expectedArch);
    }

    static Code findCode(Method method, Os expectedOs, Arch expectedArch) {
        return find(
                Code.class,
                method.getAnnotation(CodeSet.class),
                method.getAnnotation(Code.class),
                CodeSet::value, Code::os, Code::arch,
                expectedOs, expectedArch);
    }

    private static <T extends Annotation, U extends Annotation> U find(
            Class<U> annotationType,
            T containerAnnotation,
            U annotation,
            Function<T, U[]> annotationsExtractor,
            Function<U, Os> osExtractor, Function<U, Arch> archExtractor,
            Os expectedOs, Arch expectedArch) {

        final U[] annotations;
        if (containerAnnotation == null) {
            if (annotation == null) {
                return null;
            }

            annotations = (U[]) Array.newInstance(annotationType, 1);
            annotations[0] = annotation;
        } else {
            annotations = annotationsExtractor.apply(containerAnnotation);
            if (annotations == null || annotations.length == 0) {
                return null;
            }
        }

        // Find the best-matching annotation by comparing their scores.
        U match = null;
        int matchScore = -1;
        for (final U a : annotations) {
            final int score = score(
                    expectedOs, expectedArch,
                    osExtractor.apply(a), archExtractor.apply(a));

            if (score > matchScore) {
                match = a;
                matchScore = score;
                if (score >= MAX_SCORE) {
                    break;
                }
            }
        }

        return match;
    }

    private static int score(Os expectedOs, Arch expectedArch, Os os, Arch arch) {
        if (os == expectedOs) {
            if (arch == expectedArch) {
                // Both OS and arch were specified and both match.
                return MAX_SCORE;
            }

            if (arch == Arch.UNSPECIFIED) {
                // Only OS was specified and it matches.
                return MAX_SCORE - 1;
            }

            // Both OS and arch were specified, but arch doesn't match.
            return NO_MATCH;
        }

        if (os == Os.UNSPECIFIED) {
            if (arch == expectedArch) {
                // Only Arch was specified, and it matches.
                return MAX_SCORE - 2;
            }

            if (arch == Arch.UNSPECIFIED) {
                // Neither OS nor arch were specified.
                return MAX_SCORE - 3;
            }

            // Only Arch was specified, but it doesn't match.
            return NO_MATCH;
        }

        // OS was specified, but it doesn't match.
        return NO_MATCH;
    }

    private AnnotationUtil() {
    }
}
