package com.teleapps.schedulemate.util;
import java.io.PrintWriter;
import java.io.StringWriter;

public class StackTrace {
	
	private StackTrace() {
		
	}
	
	public static String getMessage(Exception ex) {
		StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
	}
}