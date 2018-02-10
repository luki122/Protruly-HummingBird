package com.hb.thememanager.job.parser;

/**
 * Interface to parse data,it will given you a out put object or collection,
 * you can used this class to parse any original data,such as, XML,JSON
 * TXT etc.
 *
 * @param <OUTPUT>  
 * @param <INPUT> original input data that need to parse
 */
public interface ThemeParser<OUTPUT,INPUT> {
	public static final String ENCODING = "utf-8";
	/**
	 * Implements this method to parse your data
	 * @param input original data
	 * @return result of you needed
	 */
	public OUTPUT parser(INPUT input);
}
