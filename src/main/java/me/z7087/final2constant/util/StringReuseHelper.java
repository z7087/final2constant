package me.z7087.final2constant.util;

import java.util.HashSet;
import java.util.Iterator;

public final class StringReuseHelper {
    private final HashSet<String> used = new HashSet<>();
    private final HashSet<String> unused = new HashSet<>();
    private int generatedNameCount = 0;

    private static boolean verifyMethodName(String str) {
        final int length = str.length();
        if (length == 0) return false;
        for (int i = 0; i < length; ++i) {
            char c = str.charAt(i);
            switch (c) {
                case '.':
                case ';':
                case '[':
                case '/':
                case '<':
                case '>':
                    return false;
            }
        }
        return true;
    }

    public void add(String str) {
        if (verifyMethodName(str) && !used.contains(str)) {
            unused.add(str);
        }
    }
    public String addL(String str) {
        add(str);
        return str;
    }

    public void markUsed(String str) {
        if (used.add(str)) {
            unused.remove(str);
        }
    }
    public String markUsedL(String str) {
        markUsed(str);
        return str;
    }

    public String poolUnusedOrNull() {
        final Iterator<String> iter = unused.iterator();
        if (iter.hasNext()) {
            final String result = iter.next();
            markUsed(result);
            return result;
        }
        return null;
    }

    public String poolUnused() {
        final Iterator<String> iter = unused.iterator();
        if (iter.hasNext()) {
            final String result = iter.next();
            markUsed(result);
            return result;
        }
        for (int count = generatedNameCount; count != -1; count++) {
            final String result = Integer.toUnsignedString(count) + "&";
            if (used.add(result)) {
                generatedNameCount = count + 1;
                return result;
            }
        }
        generatedNameCount = -1;
        throw new AssertionError("GeneratedNameCount overflowed!");
    }

    public String poolUnusedPlaceholder() {
        for (int count = generatedNameCount; count != -1; count++) {
            final String result = Integer.toUnsignedString(count) + "&";
            if (used.add(result)) {
                unused.remove(result);
                generatedNameCount = count + 1;
                return result;
            }
        }
        generatedNameCount = -1;
        throw new AssertionError("GeneratedNameCount overflowed!");
    }
}
