package japanese;

public class ToRomajiConverter {

	public static String toRomaji(String input) {
		StringBuilder ret = new StringBuilder("");
		if(input == null) {
			return "";
		}
		input = //	V
				input.replace("ゔぁ", "va").replace("ヴァ", "va").replace("ゔぃ", "vi").replace("ヴィ", "vi").replace("ゔぅ", "vu").replace("ヴゥ", "vu").replace("ゔぇ", "ve").replace("ヴェ", "ve")
				.replace("ゔぉ", "vo").replace("ヴォ", "vo")
				//	W
				.replace("うぁ", "wa").replace("ウァ", "wa").replace("うぃ", "wi").replace("ウィ", "wi").replace("うぅ", "wu").replace("ウゥ", "wu").replace("うぇ", "we").replace("ウェ", "we")
				.replace("うぉ", "wo").replace("ウォ", "wo")
				//	W ORIGINAL
				.replace("ゐ", "wi").replace("ヰ", "wi").replace("ゑ", "we").replace("ヱ", "we")
				//	VOCALS
				.replace("あ", "a").replace("ア", "a").replace("い", "i").replace("イ", "i")
				.replace("う", "u").replace("ウ", "u").replace("え", "e").replace("エ", "e").replace("お", "o")
				.replace("オ", "o")
				//	K
				.replace("か", "ka").replace("カ", "ka").replace("きゃ", "kya").replace("キャ", "kya").replace("きょ", "kyo").replace("キョ", "kyo").replace("きゅ", "kyu").replace("キュ", "kyu")
				.replace("き", "ki").replace("キ", "ki").replace("く", "ku").replace("ク", "ku").replace("け", "ke").replace("ケ", "ke").replace("こ", "ko").replace("コ", "ko")
				//	G
				.replace("が", "ga").replace("ガ", "ga").replace("ぎゃ", "gya").replace("ギャ", "gya").replace("ぎょ", "gyo").replace("ギョ", "gyo").replace("ぎゅ", "gyu").replace("ギュ", "gyu")
				.replace("ぎ", "gi").replace("ギ", "gi").replace("ぐ", "gu").replace("グ", "gu").replace("げ", "ge").replace("ゲ", "ge").replace("ご", "go").replace("ゴ", "go")
				//	S
				.replace("さ", "sa").replace("サ", "sa").replace("しゃ", "sha").replace("シャ", "sha").replace("しゅ", "shu").replace("シュ", "shu").replace("しぇ", "she").replace("シェ", "she")
				.replace("しょ", "sho").replace("ショ", "sho").replace("し", "shi").replace("シ", "shi").replace("す", "su").replace("ス", "su").replace("せ", "se").replace("セ", "se")
				.replace("そ", "so").replace("ソ", "so")
				//	Z
				.replace("ざ", "za").replace("ザ", "za").replace("じゃ", "ja").replace("ジャ", "ja").replace("じゅ", "ju").replace("ジュ", "ju").replace("じぇ", "je").replace("ジェ", "je")
				.replace("じょ", "jo").replace("ジョ", "jo").replace("じ", "ji").replace("ジ", "ji").replace("ず", "zu").replace("ズ", "zu").replace("ぜ", "ze").replace("ゼ", "ze")
				.replace("ぞ", "zo").replace("ゾ", "zo")
				//	T
				.replace("た", "ta").replace("タ", "ta").replace("ちゃ", "cha").replace("チャ", "cha").replace("ちゅ", "chu").replace("チュ", "chu").replace("ちぇ", "che").replace("チェ", "che")
				.replace("ちょ", "cho").replace("チョ", "cho").replace("ち", "chi").replace("チ", "chi").replace("てぃ", "ti").replace("ティ", "ti").replace("つぁ", "tsa").replace("ツァ", "tsa")
				.replace("つぃ", "tsi").replace("ツィ", "tsi").replace("つぇ", "tse").replace("ツェ", "tse").replace("つぉ", "tso").replace("ツォ", "tso").replace("つ", "tsu").replace("ツ", "tsu")
				.replace("て", "te").replace("テ", "te").replace("と", "to").replace("ト", "to").replace("とぅ", "tu").replace("トゥ", "tu")
				//	D
				.replace("だ", "da").replace("ダ", "da").replace("ぢゃ", "ja").replace("ヂャ", "ja").replace("ぢゅ", "ju").replace("ヂュ", "ju").replace("ぢぇ", "je").replace("ヂェ", "je")
				.replace("ぢょ", "jo").replace("ヂョ", "jo").replace("ぢ", "ji").replace("ヂ", "ji").replace("でぃ", "di").replace("ディ", "di").replace("づぁ", "za").replace("ヅァ", "za")
				.replace("づぃ", "zi").replace("ヅィ", "zi").replace("づぇ", "ze").replace("ヅェ", "ze").replace("づぉ", "zo").replace("ヅォ", "zo").replace("づ", "zu").replace("ヅ", "zu")
				.replace("で", "de").replace("デ", "de").replace("ど", "do").replace("ド", "do").replace("どぅ", "du").replace("ドゥ", "du")
				//	N
				.replace("な", "na").replace("ナ", "na").replace("にゃ", "nya").replace("ニャ", "nya").replace("にゅ", "nyu").replace("ニュ", "nyu")
				.replace("にょ", "nyo").replace("ニョ", "nyo").replace("に", "ni").replace("ニ", "ni").replace("ぬ", "nu").replace("ヌ", "nu").replace("ね", "ne").replace("ネ", "ne")
				.replace("の", "no").replace("ノ", "no")
				//	H
				.replace("は", "ha").replace("ハ", "ha").replace("ひゃ", "hya").replace("ヒャ", "hya").replace("ひゅ", "hyu").replace("ヒュ", "hyu").replace("ひょ", "hyo").replace("ヒョ", "hyo")
				.replace("ひ", "hi").replace("ヒ", "hi").replace("ふぁ", "fa").replace("ファ", "fa").replace("ふぃ", "fi").replace("フィ", "fi").replace("ふぇ", "fe").replace("フェ", "fe")
				.replace("ふぉ", "fo").replace("フォ", "fo").replace("ふ", "fu").replace("フ", "fu").replace("へ", "he").replace("ヘ", "he").replace("ほ", "ho").replace("ホ", "ho")
				//	B
				.replace("ば", "ba").replace("バ", "ba").replace("びゃ", "bya").replace("ビャ", "bya").replace("びゅ", "byu").replace("ビュ", "byu").replace("びょ", "byo").replace("ビョ", "byo")
				.replace("び", "bi").replace("ビ", "bi").replace("ぶ", "bu").replace("ブ", "bu").replace("べ", "be").replace("ベ", "be").replace("ぼ", "bo").replace("ボ", "bo")
				//	P
				.replace("ぱ", "pa").replace("パ", "pa").replace("ぴゃ", "pya").replace("ピャ", "pya").replace("ぴゅ", "pyu").replace("ピュ", "pyu").replace("ぴょ", "pyo").replace("ピョ", "pyo")
				.replace("ぴ", "pi").replace("ピ", "pi").replace("ぷ", "pu").replace("プ", "pu").replace("ぺ", "pe").replace("ペ", "pe").replace("ぽ", "po").replace("ポ", "po")
				//	M
				.replace("ま", "ma").replace("マ", "ma").replace("みゃ", "mya").replace("ミャ", "mya").replace("みゅ", "myu").replace("ミュ", "myu").replace("みょ", "myo").replace("ミョ", "myo")
				.replace("み", "mi").replace("ミ", "mi").replace("む", "mu").replace("ム", "mu").replace("め", "me").replace("メ", "me").replace("も", "mo").replace("モ", "mo")
				//	R
				.replace("ら", "ra").replace("ラ", "ra").replace("りゃ", "rya").replace("リャ", "rya").replace("りゅ", "ryu").replace("リュ", "ryu").replace("りょ", "ryo").replace("リョ", "ryo")
				.replace("り", "ri").replace("リ", "ri").replace("る", "ru").replace("ル", "ru").replace("れ", "re").replace("レ", "re").replace("ろ", "ro").replace("ロ", "ro")
				//	Y
				.replace("や", "ya").replace("ヤ", "ya").replace("ゆ", "yu").replace("ユ", "yu").replace("よ", "yo").replace("ヨ", "yo")
				//	N
				.replace("ん", "n").replace("ン", "n")
				//	WO
				.replace("を", "o").replace("ヲ", "o")
				//	WA
				.replace("わ", "wa").replace("ワ", "wa")
				;
		
		for(int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if(c == 'ー' && i > 0) {
				ret.append(input.charAt(i-1));
			}
			else if((c == 'っ' || c == 'ッ') && i < input.length()-1) {
				ret.append(input.charAt(i+1));
			}
			else {
				ret.append(c);
			}
		}
		
		
		return ret.toString();
	}
}
