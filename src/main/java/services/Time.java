package services;

import java.util.Calendar;

import start.RuntimeVariables;

public class Time {

    public static Calendar getNext(int hour, int minute, int sec) {
        Calendar ret = Time.getNow();
        int initMil = ret.get(Calendar.MILLISECOND);

        // Set millisecond to 0
        if (initMil != 0) {
            ret.add(Calendar.MILLISECOND, 1000 - initMil);
        }

        // Set seconds
        if (ret.get(Calendar.SECOND) > sec) {
            ret.add(Calendar.SECOND, 60 - ret.get(Calendar.SECOND));
        }
        ret.add(Calendar.SECOND, sec - ret.get(Calendar.SECOND));
        // Set minutes
        if (ret.get(Calendar.MINUTE) > minute) {
            ret.add(Calendar.MINUTE, 60 - ret.get(Calendar.MINUTE));
        }
        ret.add(Calendar.MINUTE, minute - ret.get(Calendar.MINUTE));
        // Set hours
        if (ret.get(Calendar.HOUR_OF_DAY) > hour) {
            ret.add(Calendar.HOUR_OF_DAY, 24 - ret.get(Calendar.HOUR_OF_DAY));
        }
        ret.add(Calendar.HOUR_OF_DAY, hour - ret.get(Calendar.HOUR_OF_DAY));

        return ret;
    }

    public static Calendar getNow() {
        return Calendar.getInstance(RuntimeVariables.HOME_TIMEZONE);
    }

    public final static long SECOND = 1000l;
    public final static long MINUTE = 60000l;
    public final static long HOUR = 3600000l;
    public final static long QUART_DAY = 21600000l;
    public final static long HALF_DAY = 43200000l;
    public final static long DAY = 86400000l;
    public final static long WEEK = 604800000l;
    public final static long MONTH = 2419200000l;
    public final static long YEAR = 31536000000l;

}
