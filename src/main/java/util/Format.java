package util;

public class Format {
    
    public static double truncateDouble(double val, int digits){
        int ret = (int) (val * Math.pow(10, digits));
        return (1. * ret)/Math.pow(10, digits);
    }

    public static String firstCharsCapitalized(String s, final char delimiter){
        String ret = "";
        String lower = s.toLowerCase();
        String upper = s.toUpperCase();
        for(int i = 0; i < s.length(); i++){
            if(s.charAt(i) == lower.charAt(i) && (i == 0 || s.charAt(i-1) == delimiter)){
                ret += upper.charAt(i);
            }
            else{
                ret += s.charAt(i);
            }
        }
        return ret;
    }
}
