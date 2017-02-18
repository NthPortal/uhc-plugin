package com.github.nthportal.uhc.util;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Util {
    public static List<String> filterAndCollect(Collection<String> collection, String startsWith) {
        return collection.stream()
                .filter(s -> s.startsWith(startsWith))
                .sorted()
                .collect(Collectors.toList());
    }
}
