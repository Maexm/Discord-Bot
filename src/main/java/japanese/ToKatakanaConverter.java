package japanese;

public class ToKatakanaConverter {

	
	public static String testConvert(String text){
		text =
		//Kleines TSU
			 text.replace("#smallTSU", "ッ")
		
		//-
			 .replace("-", "ー")
		
		//K
			 .replace("ka", "カ")
		
			 .replace("ki", "キ")
		
			 .replace("ku", "ク")
		
			 .replace("ke", "ケ")
		
			 .replace("ko", "コ")
		
		//G
			 .replace("ga", "ガ")
		
			 .replace("gi", "ギ")
		
			 .replace("gu", "グ")
		
			 .replace("ge", "ゲ")
		
			 .replace("go", "ゴ")
		
		//S
			 .replace("sa", "サ")
		
			 .replace("shi", "シ")
		
			 .replace("su", "ス")
		
			 .replace("se", "セ")
		
			 .replace("so", "ソ")
		
		//Z
			 .replace("za", "ザ")
		
			 .replace("ji", "ジ")
		
			 .replace("zu", "ズ")
		
			 .replace("ze", "ゼ")
		
			 .replace("zo", "ゾ")
		
		//T
			 .replace("ta", "タ")
		
			 .replace("chi", "チ")
		
			 .replace("tsu", "ツ")
		
			 .replace("te", "テ")
		
			 .replace("to", "ト")
		
		//D
			 .replace("da", "ダ")
		
			 .replace("de", "デ")
		
			 .replace("do", "ド")
		
		//N
			 .replace("na", "ナ")
		
			 .replace("ni", "ニ")
		
			 .replace("nu", "ヌ")
		
			 .replace("ne", "ネ")
		
			 .replace("no", "ノ")
		
		//B
			 .replace("ba", "バ")
		
			 .replace("bi", "ビ")
		
			 .replace("bu", "ブ")
		
			 .replace("be", "ベ")
		
			 .replace("bo", "ボ")
		
		//P
			 .replace("pa", "パ")
		
			 .replace("pi", "ピ")
		
			 .replace("pu", "プ")
		
			 .replace("pe", "ペ")
		
			 .replace("po", "ポ")
		
		//M
			 .replace("ma", "マ")
		
			 .replace("mi", "ミ")
		
			 .replace("mu", "ム")
		
			 .replace("me", "メ")
		
			 .replace("mo", "モ")
		
		//R
			 .replace("ra", "ラ")
		
			 .replace("ri", "リ")
		
			 .replace("ru", "ル")
		
			 .replace("re", "レ")
		
			 .replace("ro", "ロ")
		
		//Y
			 .replace("ya", "ヤ")
		
			 .replace("yu", "ユ")
		
			 .replace("yo", "ヨ")
		
		//WA mit Special-Compound-Katakana
			 .replace("wa", "ワ")
		
			 .replace("wi", "ウィ")
		
			 .replace("wu", "ウゥ")
		
			 .replace("we", "ウェ")
		
			 .replace("wo", "ウォ")
		
		//VA
			 .replace("va", "ヴァ")
		
			 .replace("vi", "ヴィ")
		
			 .replace("vu", "ヴゥ")
		
			 .replace("ve", "ヴェ")
		
			 .replace("vo", "ヴォ")
		
		//TS
			 .replace("tsa", "ツァ")
		
			 .replace("tsi", "ツィ")
		
			 .replace("tse", "ツェ")
		
			 .replace("tso", "ツォ")
		
		//TI, TU, DI, DU
			 .replace("ti", "ティ")
		
			 .replace("tu", "トゥ")
		
			 .replace("di", "ディ")
		
			 .replace("du", "ドゥ")
		
		//F
			 .replace("fa", "ファ")
		
			 .replace("fi", "フィ")
		
			 .replace("fe", "フェ")
		
			 .replace("fo", "フォ")
		
		//CHE
			 .replace("che", "チェ")
		
		//JE
			 .replace("je", "ジェ")
		
		//SHE
			 .replace("she", "シェ")
		
		//Standard Compound-Katakana
			 .replace("rya", "リャ")
		
			 .replace("ryu", "リュ")
		
			 .replace("ryo", "リョ")
		
			 .replace("mya", "ミャ")
		
			 .replace("myu", "ミュ")
		
			 .replace("myo", "ミョ")
		
			 .replace("pya", "ピャ")
		
			 .replace("pyu", "ピュ")
		
			 .replace("pyo", "ピョ")
		
			 .replace("bya", "ビャ")
		
			 .replace("byu", "ビュ")
		
			 .replace("byo", "ビョ")
		
			 .replace("hya", "ヒャ")
		
			 .replace("hyu", "ヒュ")
		
			 .replace("hyo", "ヒョ")
		
			 .replace("nya", "ニャ")
		
			 .replace("nyu", "ニュ")
		
			 .replace("nyo", "ニョ")
		
			 .replace("cha", "チャ")
		
			 .replace("chu", "チュ")
		
			 .replace("cho", "チョ")
		
			 .replace("ja", "ジャ")
		
			 .replace("ju", "ジュ")
		
			 .replace("jo", "ジョ")
		
			 .replace("sha", "シャ")
		
			 .replace("shu", "シュ")
		
			 .replace("sho", "ショ")
		
			 .replace("gya", "ギャ")
		
			 .replace("gyu", "ギュ")
		
			 .replace("gyo", "ギョ")
		
			 .replace("kya", "キャ")
		
			 .replace("kyu", "キュ")
		
			 .replace("kyo", "キョ")
		
		//H
			 .replace("ha", "ハ")
		
			 .replace("hi", "ヒ")
		
			 .replace("fu", "フ")
		
			 .replace("he", "ヘ")
		
			 .replace("ho", "ホ")
		
		//Vokale
			 .replace("a", "ア")
		
			 .replace("i", "イ")
		
			 .replace("u", "ウ")
		
			 .replace("e", "エ")
		
			 .replace("o", "オ")
		
		//N
			 .replace("n", "ン");
		
		return text;
	
		}
}
