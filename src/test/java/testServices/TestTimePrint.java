package testServices;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;

import services.TimePrint;
import util.Pair;

public class TestTimePrint {
    
    @Test
    public void testMsToPretty(){
       ArrayList<Pair<Long,String>> pairs = new ArrayList<>();

       // second argument is the EXPECTED value
       pairs.add(new Pair<Long,String>(0l, "00:00"));
       pairs.add(new Pair<Long,String>(1000l, "00:01"));
       pairs.add(new Pair<Long,String>(30000l, "00:30"));
       pairs.add(new Pair<Long,String>(59000l, "00:59"));
       pairs.add(new Pair<Long,String>(60000l, "01:00"));
       pairs.add(new Pair<Long,String>(61000l, "01:01"));
       pairs.add(new Pair<Long,String>(2013000l, "33:33"));
       pairs.add(new Pair<Long,String>(3599000l, "59:59"));
       pairs.add(new Pair<Long,String>(3600000l, "01:00:00"));
       pairs.add(new Pair<Long,String>(120813000l, "33:33:33"));
       pairs.add(new Pair<Long,String>(215999000l, "59:59:59"));
       pairs.add(new Pair<Long,String>(216000000l, "60:00:00"));
       pairs.add(new Pair<Long,String>(12000813000l, "3333:33:33"));

       for(Pair<Long,String> pair: pairs){
           assertEquals("Long value should result in correct hh:mm:ss format", pair.value, TimePrint.msToPretty(pair.key));
       }
    }
}
