package testUtils;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;

import util.Format;
import util.Pair;

public class TestFormat {
    
    @Test
    public void testCapitalizeFirstChars(){
        ArrayList<Pair<String, String>> pairs = new ArrayList<>();

        pairs.add(new Pair<String, String>("Es Passiert Nichts", "Es Passiert Nichts"));
        pairs.add(new Pair<String, String>("alles wird groß", "Alles Wird Groß"));

        for(Pair<String,String> pair : pairs){
            assertEquals(pair.value, Format.firstCharsCapitalized(pair.key, ' '));
        }
    }
}
