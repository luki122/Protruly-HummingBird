package com.hb.note.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternUtils {

    public static String replaceAllSpans(String source) {
        return source.replaceAll(Globals.SPAN_PATTERN_ALL, "");
    }

    public static List<String> find(final String source, String regex) {
        List<String> results = new ArrayList<>();

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            results.add(matcher.group(1));
        }

        return results;
    }

    public static List<String> findAllImages(final String source) {
        return find(source, Globals.SPAN_PATTERN_IMAGE);
    }

    public static int getCount(final String source, String regex) {
        int count = 0;

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            count++;
        }

        return count;
    }

    public static int getImageCount(final String source) {
        return getCount(source, Globals.SPAN_PATTERN_IMAGE);
    }
}
