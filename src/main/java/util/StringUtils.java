package util;

public class StringUtils {
    
    public final static boolean isNullOrWhiteSpace(String test){
        return test == null || test.replaceAll(" ", "") == "";
    }
}
