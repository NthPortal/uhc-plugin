package com.nthportal.uhc.util;

import com.google.common.base.Preconditions;

import java.util.*;
import java.util.stream.Collectors;

public final class Util {
    private Util() {}

    public static List<String> filterAndSort(Collection<String> collection, String startsWith) {
        return collection.stream()
                .filter(s -> s.startsWith(startsWith))
                .sorted()
                .collect(Collectors.toList());
    }

    public static <T> T[] arrayTail(T[] array) {
        Preconditions.checkArgument(array.length > 0, "empty array has no tail");
        return Arrays.copyOfRange(array, 1, array.length);
    }

    @SafeVarargs
    public static <T> Set<T> unmodifiableSet(T... elements) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(elements)));
    }

    public static AssertionError impossible() {
        return new AssertionError("impossible");
    }
}
