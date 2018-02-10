package com.hb.note.util;

public class Globals {

    public static final String NEW_LINE = "\n";
    public static final char NEW_LINE_CHAR = '\n';

    public static final String FILE_PROTOCOL = "file://";
    public static final int FILE_PROTOCOL_LENGTH = FILE_PROTOCOL.length();

    public static final String SPAN_START = "<Note-Span>";
    public static final String SPAN_END = "</Note-Span>";

    public static final String SPAN_TITLE = "Title";
    public static final String SPAN_SUBTITLE = "Subtitle";
    public static final String SPAN_UNDER_LINE = "Under-Line";
    public static final String SPAN_STRIKE_THROUGH = "Strike-Through";
    public static final String SPAN_BULLET = "Bullet";
    public static final String SPAN_SYMBOL = "Symbol=";
    public static final String SPAN_IMAGE = "Image=";

    public static final String SPAN_SYMBOL_BILL = SPAN_SYMBOL + "BILL";
    public static final String SPAN_SYMBOL_BILL_DONE = SPAN_SYMBOL + "BILL_DONE";

    public static final int SPAN_START_LENGTH = SPAN_START.length();
    public static final int SPAN_END_LENGTH = SPAN_END.length();
    public static final int SPAN_IMAGE_LENGTH = SPAN_IMAGE.length();

    public static final String SPAN_PATTERN = "(.*?)";
    public static final String SPAN_PATTERN_ALL = SPAN_START + SPAN_PATTERN + SPAN_END;
    public static final String SPAN_PATTERN_IMAGE = SPAN_START + SPAN_IMAGE + SPAN_PATTERN + SPAN_END;
}
