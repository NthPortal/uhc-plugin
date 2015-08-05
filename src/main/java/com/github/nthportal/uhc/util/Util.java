package com.github.nthportal.uhc.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Util {
    public static List<String> filterAndCollect(Collection<String> collection, String startsWith) {
        List<String> list = new ArrayList<>();
        for (String s : collection) {
            if (s.startsWith(startsWith)) {
                list.add(s);
            }
        }
        Collections.sort(list);
        return list;
    }
}
