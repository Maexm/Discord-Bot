package testHttp;


import org.junit.Test;

import exceptions.IllegalMagicException;
import start.RuntimeVariables;
import weather.Weather;

public class WeatherTest {
    
    @Test(expected = IllegalMagicException.class)
    public void shouldFailDueToMissingApiKey(){
        Weather.getWeatherResponse(RuntimeVariables.HOME_TOWN);
    }
}
