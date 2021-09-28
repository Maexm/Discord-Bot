package translator;

public class TranslatorResponse {

    public Detection detectedLanguage;
    public Translation[] translations;
    
    public class Detection{
        public String language;
        public double score;
    }

    public class Translation{
        public String text;
        public String to;
    }
}
