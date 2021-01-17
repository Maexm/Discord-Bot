package weather;

import com.google.gson.Gson;

import exceptions.IllegalMagicException;
import util.HTTPRequests;
import util.Markdown;
import start.RuntimeVariables;
import util.Format;
import util.Range;
import util.Units;
import weather.WeatherResponses.SingleResponse;

public class Weather {
    
    public final static String WEATHER_BASE_URL = "https://api.openweathermap.org/";
    public final static String SINGLE_CALL = "data/2.5/weather?appid=" + RuntimeVariables.WEATHER_API_KEY+ "&units=metric&lang=de";

    public static SingleResponse getWeatherResponse(String city){
        city = HTTPRequests.urlEncode(city);
        if(RuntimeVariables.WEATHER_API_KEY == null){
            throw new IllegalMagicException("Weather API Key not available!");
        }

        String url = Weather.WEATHER_BASE_URL+Weather.SINGLE_CALL+"&q="+city;

        String response = HTTPRequests.getModern(url);
			if(response == null) {
				return null;
            }
        
        Gson gson = new Gson();
        SingleResponse weatherResponse = gson.fromJson(response.toString(), SingleResponse.class);

        return weatherResponse;
    }

    public static String buildMessage(SingleResponse resp){
        if(resp.cod == 404){
            return "konnte diese Stadt nicht finden!";
        }

        String ret = "das aktuelle Wetter in " + Markdown.toBold(resp.name) + " ("+resp.sys.country+")"+ ":"
                    +"\n\n"
                    +Markdown.toBold(resp.weather[0].description)
                    +" bei "+Markdown.toBold(resp.main.temp+ " °C") + " (gefühlt "+resp.main.feels_like+" °C)."
                    +"\n\n"
                    +Markdown.toCodeBlock("Min/Max:")+" "+Markdown.toBold(resp.main.temp_min+" °C / "+resp.main.temp_max+" °C")
                    +"\n"
                    +Markdown.toCodeBlock("Luftfeuchtigkeit:")+" "+Markdown.toBold(resp.main.humidity+"%")
                    +"\n"
                    +Markdown.toCodeBlock("Luftdruck:")+" "+Markdown.toBold(resp.main.pressure+"hPa")
                    +"\n"
                    +Markdown.toCodeBlock("Wind:")+" "+Markdown.toBold(Format.truncateDouble(Units.msToKmh(resp.wind.speed), 2)+" km/h") + " aus " + Markdown.toBold(Weather.getWindDirection(resp.wind.deg))
                    +(resp.wind.gust != 0 ? ("\nEs sind Böen bis "+Markdown.toBold(Format.truncateDouble(Units.msToKmh(resp.wind.gust), 2)+ " km/h")+ " möglich! :dash:") : "");
        return ret;
    }

    public static String getWindDirection(int deg){
        // Note concerning compass degrees: North = 0 deg, move CLOCKWISE
        switch(deg){
            case 0:
            case 360:
                return "Norden";
            case 45:
                return "Nord-Osten";
            case 90:
                return "Osten";
            case 135:
                return "Süd-Osten";
            case 180:
                return "Süden";
            case 225:
                return "Süd-Westen";
            case 270:
                return "Westen";
            case 315:
                return "Nord-Westen";
            default:
                if(Range.isInRangeExcl(0, 45, deg)){
                    return "Nord-Nord-Osten";
                }
                if(Range.isInRangeExcl(45, 90, deg)){
                    return "Nord-Ost-Osten";
                }
                if(Range.isInRangeExcl(90, 135, deg)){
                    return "Süd-Ost-Osten";
                }
                if(Range.isInRangeExcl(135, 180, deg)){
                    return "Süd-Süd-Osten";
                }
                if(Range.isInRangeExcl(180, 225, deg)){
                    return "Süd-Süd-Westen";
                }
                if(Range.isInRangeExcl(225, 270, deg)){
                    return "Süd-West-Westen";
                }
                if(Range.isInRangeExcl(270, 315, deg)){
                    return "Nord-West-Westen";
                }
                if(Range.isInRangeExcl(315, 360, deg)){
                    return "Nord-Nord-Westen";
                }
        }
        throw new IllegalMagicException("Invalid degree");
    }
}
