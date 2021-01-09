package testHttp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import wiki.Wikipedia;
import wiki.Wikipedia.WikiPage;

public class WikiTest {
    
    @Test
    public void wikiresultsShouldFecthCorrectly(){
        
        WikiPage[] results = {Wikipedia.getWikiPage("Konosuba"), Wikipedia.getWikiPage("Yume Nikki"), Wikipedia.getWikiPage("mirai nikki")}; // Yume Nikki should only produce english results
        for(WikiPage result : results){
            assertNotNull("Should get valid response from Wikipedia", result);
        }
        
        assertNull("Should return null when there is no page available", Wikipedia.getWikiPage("gyigas"));
    }

    @Test
    public void wikiresultShouldFetchImageUrl(){
        WikiPage page = Wikipedia.getWikiPage("Japan");
        assertNotNull("Optional should be present", page.original);
        assertNotNull("Image url should be present", page.original.source);
    }

    @Test
    public void wikiresultWithoutImage(){
        WikiPage page = Wikipedia.getWikiPage("yotsuba");
        assertNull("No image url should be present", page.original);
    }
}
