package util;

public class Range {

    public static boolean isInRangeIncl(int a, int b, int x){
        return a <= x && x <= b;
    }

    public static boolean isInRangeExcl(int a, int b, int x){
        return a < x && x < b;
    }
    
}
