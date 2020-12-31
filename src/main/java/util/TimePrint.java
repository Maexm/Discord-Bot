package util;

import java.util.Calendar;
import java.util.Locale;

public class TimePrint {

	/**
	 * 
	 * @param time
	 * @return "dd. MONAT JJJJ HH:MM:SS Uhr" 
	 */
	public static String DD_MMMM_YYYY_HH_MM_SS(Calendar time) {
		String[] fields = {Integer.toString(time.get(Calendar.DAY_OF_MONTH)),
				Integer.toString(time.get(Calendar.HOUR_OF_DAY)),
				Integer.toString(time.get(Calendar.MINUTE)),
				Integer.toString(time.get(Calendar.SECOND))};
		for(int i = 0; i < fields.length; i++) {
			if(fields[i].length() == 1) {
				fields[i] = "0" + fields[i];
			}
		}
		
		return fields[0]
				+". "+time.getDisplayName(Calendar.MONTH, Calendar.LONG_STANDALONE, Locale.GERMAN)
				+" "+time.get(Calendar.YEAR)
				+" "+fields[1]
				+":"+fields[2]
				+":"+fields[3]
				+" Uhr";
	}
	
	public static String msToPretty(long val) {
		String ret = "";
		//System.out.println(val);
		
		//int ms = (int) (val%1000);
		//ms -= ms%10;
		int sek = 0;
		int min = 0;
		int h = 0;
		if(val >= 1000) {
			sek = (int) ((val/1000)%60);

		}
		if(val >= 60000) {
			min = (int) ((val/60000)%60);

		}
		if(val >= 3600000) {
			h = (int) (val/3600000);

		}		
		if(h != 0) {
			if(h < 10) {
				ret += "0"+h;
			}
			else {
				ret+=h;
			}
			ret+=":";
		}
		if(min < 10) {
			ret += "0"+min;
		}
		else{
			ret += min;
		}
		ret+=":";
		if(sek < 10) {
			ret += "0"+sek;
		}
		else {
			ret += sek;
		}
		
		return ret;
	}
}
