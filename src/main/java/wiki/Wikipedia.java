package wiki;

import java.util.Map;

import services.HTTPRequests;

public class Wikipedia {

    public static WikiResult getWikiResult(String keyword){
        keyword = HTTPRequests.neutralize(keyword);
        // TODO
        return null;
    }

    public static String buildMessage(WikiResult wikiRes){
        return "TODO";
    }
    

    public class WikiResult{
        query query;
    }

    public class query{
        normalized[] normalized;
        Map<String, Page> pages;
    }

    public class normalized{
        String from;
        String to;
    }

    public class Page{
        long pageid;
        String title;
        String missing;
        String extract;
    }
}
