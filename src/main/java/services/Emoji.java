package services;

public class Emoji {
	
	public final static String getVol(int vol) {
		if(vol > 100) {
			return ":loud_sound:";
		}
		else {
			return ":speaker:";
		}
	}

}
