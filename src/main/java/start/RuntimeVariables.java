package start;

import java.util.Calendar;
import java.util.TimeZone;

public class RuntimeVariables {

	public final static TimeZone HOME_TIMEZONE = TimeZone.getTimeZone("CET");
	public final static Calendar START_TIME = Calendar.getInstance(RuntimeVariables.HOME_TIMEZONE);
	public static boolean IS_DEBUG = false;
	public final static String VERSION = "1.0.3.4";
	
}
