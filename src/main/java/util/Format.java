package util;

public class Format {
    
    public static double truncateDouble(double val, int digits){
        int ret = (int) (val * Math.pow(10, digits));
        return (1. * ret)/digits;
    }
}
