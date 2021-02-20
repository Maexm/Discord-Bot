package testStart;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import exceptions.StartUpException;
import start.RuntimeVariables;
import start.StartUp;

public class TestStartUp {
    
    @Test(expected = StartUpException.class)
    public void rejectEmptyToken(){
        StartUp.main(new String[]{""}); 
    }

    @Test
    public void noDebugOnStandard(){
        try{
            StartUp.main(new String[]{""}); 
         }
         catch(Exception e){    
         }
         assertFalse("Debug should not be activated without debug value", RuntimeVariables.isDebug());

         try{
             StartUp.main(new String[]{"", "",  "anything but not DEBUG"});
         }
         catch(Exception e){
         }
         assertFalse("Debug should not be activated with wrong debug value", RuntimeVariables.isDebug());
    }

    @Test
    public void debugWithFlag(){
        try{
            StartUp.main(new String[]{"DEBUG"}); 
         }
         catch(Exception e){    
         }
         assertTrue("Debug should be activated when debug is set in args", RuntimeVariables.isDebug());
    }
}
