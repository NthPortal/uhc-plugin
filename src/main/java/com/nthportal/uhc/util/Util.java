package com.nthportal.uhc.util;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public final class Util {
    private Util() {}

    public static List<String> filterAndSort(Collection<String> collection, String startsWith) {
        return collection.stream()
                .filter(s -> s.startsWith(startsWith))
                .sorted()
                .collect(Collectors.toList());
    }
}
