package testHttp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import wiki.Wikipedia;
import wiki.Wikipedia.WikiPage;

public class TestHttp {
    
    @Test
    public void testWikiResponse(){
        
        WikiPage[] results = {Wikipedia.getWikiPage("Konosuba"), Wikipedia.getWikiPage("Yume Nikki"), Wikipedia.getWikiPage("mirai nikki")}; // Yume Nikki should only produce english results
        for(WikiPage result : results){
            assertNotNull("Should get valid response from Wikipedia", result);
        }
        
        assertNull("Should return null when there is no page available", Wikipedia.getWikiPage("gyigas"));
    }
}
