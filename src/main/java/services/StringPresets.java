package services;

public class StringPresets {
    
    public static String getUnderScore(int length){
        String ret = "";
        for(int i = 0; i < length; i++){
            ret += "\\_";
        }
        return ret;
    }
}