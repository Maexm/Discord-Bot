package weather;

import weather.WeatherResponseBlocks.*;

public class WeatherResponses {

    public class SingleResponse{
        coord coord;
        weather[] weather;
        String base;
        main main;
        int visibility;
        wind wind;
        clouds clouds;
        long dt;
        sys sys;
        int timezone;
        long id;
        /**
         * Single only
         */
        String name;
        /**
         * Single only
         */
        int cod;
    }

    public class ForecastResponse{
        String cod;
        int message;
        int cnt;
        // TODO: continue
    }
}
