package testHttp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import japanese.Jisho;
import japanese.Jisho.JishoResponse;

public class JishoTest {
    
    @Test
    public void receiveValidJishoEntries(){
        JishoResponse[] responses = {Jisho.lookUpKeyWord("apple"), Jisho.lookUpKeyWord("フワフワ")};

        for(JishoResponse response : responses){
            assertNotNull("Response should not be null", response);
            assertFalse("Response should contain data", response.data.length == 0);
        }
    }

    @Test
    public void receiveEmptyJishoEntry(){
        JishoResponse response = Jisho.lookUpKeyWord("straßenlaterne");
        
        assertTrue("No data should be found", response.data.length == 0);
    }
}
