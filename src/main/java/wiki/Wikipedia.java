package wiki;

import java.util.Map;

import com.google.gson.Gson;

import exceptions.IllegalMagicException;
import services.HTTPRequests;
import services.Markdown;
import util.Format;

public class Wikipedia {

    public final static String WIKI_API_BASE_URL = "wikipedia.org/w/api.php?";
    public final static String WIKI_NORMAL_BASE_URL = "wikipedia.org/wiki/";
    public final static String QUERY_PARAMS = "action=query&prop=extracts&format=json&explaintext=true&exintro=true&exchars=700&redirects=true&titles=";
    public final static String[] LANGUAGES = {"de", "en"};

    public static WikiPage getWikiPage(String keyword){
        keyword = HTTPRequests.neutralize(keyword);
        keyword = Format.firstCharsCapitalized(keyword, " ");
        keyword = HTTPRequests.urlEncode(keyword);
        WikiPage ret = null;

        // Try configured languages
        for(String language : Wikipedia.LANGUAGES){
           String url = "https://"+language+"."+Wikipedia.WIKI_API_BASE_URL + Wikipedia.QUERY_PARAMS + keyword;
           WikiResult resp = Wikipedia.fetchResult(url); // Fetch result
           
           ret = Wikipedia.retrievePage(resp.query.pages); // Get page object (null if there is no response)

           // ret: Page != null && pageId != 0 => found a valid page => return
           if(ret != null && ret.pageid != 0){
               ret.CUSTOM_PROP_LANGUAGE = language;
               return ret;
           }
        }
        return null;
    }

    private static WikiResult fetchResult(final String url){
        String response = HTTPRequests.getModern(url);
        Gson gson = new Gson();
        return gson.fromJson(response, WikiResult.class);
    }

    private static WikiPage retrievePage(Map<String, WikiPage> pages){
        return pages.values().stream().findFirst().orElse(null);
    }

    public static String buildMessage(WikiPage wikiRes){
        if(wikiRes == null){
            return "konnte unter diesem Begriff nichts finden!";
        }
        if(wikiRes.CUSTOM_PROP_LANGUAGE == null || wikiRes.CUSTOM_PROP_LANGUAGE.equals("")){
            throw new IllegalMagicException("CUSTOM_PROP_LANGUAGE must not be null or empty");
        }

        final String humanUrl = "https://"+wikiRes.CUSTOM_PROP_LANGUAGE+"."+Wikipedia.WIKI_NORMAL_BASE_URL + HTTPRequests.urlEncode(wikiRes.title.replace(" ", "_"));
        String ret = Markdown.toMultilineBlockQuotes(
            Markdown.toUnderlinedBold(wikiRes.title)+"\n\n"
            + wikiRes.extract+"\n\n\n"
        )
        + "Ausführlichere Informationen gibt es hier: "+humanUrl;

        return ret;
    }
    

    public class WikiResult{
        public query query;
    }

    public class query{
        public normalized[] normalized;
        public Map<String, WikiPage> pages;
    }

    public class normalized{
        public String from;
        public String to;
    }

    public class WikiPage{
        public long pageid;
        public String title;
        public String missing;
        public String extract;

        public String CUSTOM_PROP_LANGUAGE;
    }
}