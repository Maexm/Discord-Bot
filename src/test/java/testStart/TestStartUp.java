package testStart;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import start.RuntimeVariables;
import start.StartUp;

public class TestStartUp {
    
    @Test
    public void rejectEmptyToken(){
        boolean exceptionThrown = false;
        try{
           StartUp.main(new String[]{""}); 
        }
        catch(Exception e){
            exceptionThrown = true;
        }
        
        assertTrue("Empty tokens should throw an exception during startUp" , exceptionThrown);
    }

    @Test
    public void noDebugOnStandard(){
        try{
            StartUp.main(new String[]{""}); 
         }
         catch(Exception e){    
         }
         assertFalse("Debug should not be activated without args[1] value", RuntimeVariables.IS_DEBUG);

         try{
             StartUp.main(new String[]{"", "anything but not DEBUG"});
         }
         catch(Exception e){
         }
         assertFalse("Debug should not be activated with wrong args[1] value", RuntimeVariables.IS_DEBUG);
    }

    @Test
    public void debugWithFlag(){
        try{
            StartUp.main(new String[]{"", "DEBUG"}); 
         }
         catch(Exception e){    
         }
         assertTrue("Debug should be activated when debug is set in args", RuntimeVariables.IS_DEBUG);
    }
}
