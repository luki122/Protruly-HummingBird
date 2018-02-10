package com.hmb.manager.widget.textcounter.formatters;

import java.text.DecimalFormat;

import com.hmb.manager.widget.textconter.Formatter;

/**
 * Created by prem on 10/28/14.
 */
public class DecimalFormatter implements Formatter {

    private final DecimalFormat format = new DecimalFormat("#.00");

    @Override
    public String format(String prefix, String suffix, float value) {
        return prefix + format.format(value) + suffix;
    }
}
