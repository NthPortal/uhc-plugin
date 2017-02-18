package com.github.nthportal.uhc.util;

import lombok.val;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Util {
    public static List<String> filterAndCollect(Collection<String> collection, String startsWith) {
        val list = new ArrayList<String>();
        for (val s : collection) {
            if (s.startsWith(startsWith)) {
                list.add(s);
            }
        }
        Collections.sort(list);
        return list;
    }
}
