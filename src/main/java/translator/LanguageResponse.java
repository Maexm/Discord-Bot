package translator;

import java.util.Map;

public class LanguageResponse {

    public Map<String, Language> translation;

    public class Language{
        public String name;
        public String nativeName;
        public String dir;
    }
}
