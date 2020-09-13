package japanese;

import discord4j.core.object.entity.channel.MessageChannel;
import services.Markdown;

public class RomajiPreparer {
	
	//KEYWORDS: #smallTSU
	
	private final static String WARN_V = "HINWEIS: Dein Wort enthält ein 'v', bitte ersetze es durch 'f', wenn es als 'f' ausgesprochen wird! Aktuell wird das 'v' als 'w' ausgesprochen!";

	public static String getPrepared(String work, MessageChannel channel){
		work = work.toLowerCase();
		if(work.contains("v")) {
			channel.createMessage(":warning: " + Markdown.toItalics(RomajiPreparer.WARN_V)).block();
		}
		work = preWork(work);
		// Changes to work do NOT affect result from now on!
		char text[] = work.toCharArray();
		work = "";
		//Pruefe gesamten, gegebenen Text...
		for(int i = 0; i < text.length; i++){
			if(isConsonant(text[i])){//Wenn KONSONANT
				if(text[i]=='l'){//Wenn L
					text[i] = 'r';
				}
				if(text[i] == 'z'){//TSU-Fall
					if(!RomajiPreparer.isLastChar(text, i) && isVowel(text[i+1])){
						work = work+"ts"+text[i+1];
						i++;
						continue;
					}
						work = work+"tsu";
						continue;	
				}
				//else if(text[i] == 'v') {//V Fall - Aussprache 'f' wenn: 
					//if( (RomajiPreparer.isLastChar(text, i) || (!RomajiPreparer.isLastChar(text, i) && !isVowel(text[i+1]))) || // (Wort- ODER Silbenende) ODER 
						//	((i == 0 || (i != 0 && !isVowel(text[i-1]))) && //(Wort- ODER Silbenanfang) UND 
						//	((!RomajiPreparer.isLastChar(text, i)) && (text[i+1] == 'l' || text[i+1] == 'r' || isVowel(text[i+1])))) ) {//Nachfolger = l,r oder vokal
						//text[i] = 'f';
						//System.out.println("Thats an f");
					//}//Sonst bleibt es bei v (Aussprache w)!
					//else {
						//System.out.println("Not an f");
						//text[i] = 'V';
				//	}
					//i--;//Go back and check again
					//continue;
				//}
				else if(!RomajiPreparer.isLastChar(text, i) && text[i] == text[i+1]){//DOPPELKONSONANT
					work = work+"#smallTSU";
					continue;
				}
				else if(i < text.length-3 && text[i] == 't' && text[i+1]=='s'){//CHI-Fall
					if(text[i+1]=='s' && text[i+2] == 'c' && text[i+3] == 'h'){
						work = work + "ch";
						i = i+3;
						if(!RomajiPreparer.isLastChar(text, i) && isConsonant(text[i+1])){//Sollte auf ch kein Vokal, sondern ein Konsonant, folgen
							work = work + "u";
						}
						continue;
					}
				}
				else if(text[i] == 'j'){//JI-Fall
					if(!RomajiPreparer.isLastChar(text, i) && isVowel(text[i+1])){
						work = work+ "j" + text[i+1];
						i++;
						continue;
					}
					work = work +"ji";
					continue;
				}
				else if(text[i]=='y'){//YA-,YU- und YO-Fall
					if(!RomajiPreparer.isLastChar(text, i) && text[i+1]=='y' || !RomajiPreparer.isLastChar(text, i) && text[i+1]=='u' || !RomajiPreparer.isLastChar(text, i) && text[i+1]=='o' || !RomajiPreparer.isLastChar(text, i) && text[i+1]=='a'){
						work = work+"y"+text[i+1];
						i++;
						continue;
					}
					else if(!RomajiPreparer.isLastChar(text, i) && isVowel(text[i+1])){
						work = work+text[i+1];
						i++;
						continue;
					}
					work = work +"yu";
					continue;
				}
				else if(!RomajiPreparer.isLastChar(text, i) && text[i+1]=='y'){//RYO-,usw.,HYO-Fall
					if(i < text.length-2 && isVowel(text[i+2])){
						work = work+text[i]+"y"+text[i+2];
						i = i+2;
						continue;
					}
					work = work+text[i]+"yu";
					i++;
					continue;
				}
				else if(!RomajiPreparer.isLastChar(text, i) && text[i]=='s'){//SHI-Fall
					if(text[i+1]=='h'){
						if(i < text.length-2 && isVowel(text[i+2])){
							work = work + "sh"+text[i+2];
							i = i+2;
							continue;
						}
						work = work + "shu";
						i++;
						continue;
					}
					else{
						if(isVowel(text[i+1])){
							work = work+"z"+text[i+1];
							i++;
							continue;
						}
						else{
							work = work+"su";
							continue;
						}
					}
				}
				else if(text[i] == 't'){//TO statt TU!
					if (!RomajiPreparer.isLastChar(text, i) && isVowel(text[i+1])){//Mach Nichts, wenn nachfolger Vokal
						work = work+text[i]+text[i+1];
						i++;
						continue;
					}
					else if((!RomajiPreparer.isLastChar(text, i) && text[i+1] != 't') || i == text.length-1){//H�ng ein TO an, wenn Nachfolger ungleich t ODER wenn letter Buchstabe
						work = work + "to";
						continue;
					}
				}
				else if(text[i] == 'd'){//DO statt DU!
					if (!RomajiPreparer.isLastChar(text, i) && isVowel(text[i+1])){
						work = work+text[i]+text[i+1];
						i++;
						continue;
					}
					else{
						work = work + "do";
						continue;
					}
				}
				else if(text[i] == 'n'){//N alleine geht!
					work = work+"n";
					continue;
				}
				//ACHTUNG NORMALFALL!!!!!
				else if(!RomajiPreparer.isLastChar(text, i) && isVowel(text[i+1])){//NORMAL-Fall
					work = work+text[i]+text[i+1];//Konsonant + Vokal - sonst u anh�ngen (siehe unterhalb else if)
					i++;
					continue;
				}//ENDE NORMALFALL
				work = work+text[i]+"u";//Kein CONTINUE mehr noetig
			}
			else{//Wenn VOKAL oder KEIN BUCHSTABE
				if(i > 0 && isVowel(text[i]) && text[i] == text[i-1]){//Doppel Vokal
					System.out.println("DOUBLE VOWEL");
					work = work +"-";
					continue;
				}
				work = work+text[i];
				continue;
			}
		}
		System.out.println("RomajiPreparer: Finished preparing! ToKatakanaConverter will work with '"+work+"'.");
		return work;
	}
	
	private static boolean isVowel(char probe){
		if(probe=='a' || probe=='i' || probe=='u' || probe=='e' || probe=='o'){
			return true;
		}
		else{
			return false;
		}
	}
	
	private static boolean isConsonant(char probe){
		final String character = String.valueOf(probe);
		final String consonants = "bcdfghjklmnpqrstvVwxyz";
		return consonants.contains(character);
	}
	private static String preWork(String sas){
		if(sas.contains("x")) {
			sas = sas.replace("x", "ks");
		}
		if(sas.contains("si")) {
			sas = sas.replace("si", "shi");
		}
		if(sas.contains("j")) {
			sas = sas.replace("j", "y");
		}
		if(sas.contains("eu")){
			sas = sas.replace("eu", "oi");
		}
		if(sas.contains("ei")){
			sas = sas.replace("ei", "ai");
		}
		if(sas.contains("sch")){
			sas = sas.replace("sch", "sh");
		}
		if(sas.contains("cher")){
			sas = sas.replace("cher", "haa");
		}
		if(sas.contains("aer") || sas.contains("ier") || sas.contains("uer")  || sas.contains("eer")  || sas.contains("oer") ){
			sas = sas.replace("er", "ya");
		}
		if(sas.endsWith("yer") || sas.endsWith("her")){
			char[] temp = sas.toCharArray();
			temp[temp.length-1] = 'a';
			sas = String.valueOf(temp) + "-";
			//sas = sas.replace("er", "ea");
		}
		if(sas.contains("h")){
			sas = sas.replace("ch", "h");
		}
		return sas;
	}
	
	private static boolean isLastChar(char[] text, int pos) {
		return pos == text.length-1; //i < text.length-1 ist das Gegenteil, also hat Nachfolger!
	}
	
}
