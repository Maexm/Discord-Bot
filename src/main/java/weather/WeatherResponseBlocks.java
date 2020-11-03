package weather;

class WeatherResponseBlocks {
    
    class coord{
        double lon;
        double lat;
    }

    class weather{
        int id;
        String main;
        String description;
        String icon;
    }

    class main{
        double temp;
        double feels_like;
        double temp_min;
        double temp_max;
        int pressure;
        int humidity;
        /**
         * Forecast only
         */
        int sea_level;
        /**
         * Forecast only
         */
        int grnd_level;
    }

    class wind{
        double speed;
        int deg;
        double gust;
    }

    class clouds{
        int all;
    }

    class sys{
        int type;
        long id;
        String country;
        long sunrise;
        long sunset;
    }

    /**
     * Used only in forecast
     */
    class city{
        long id;
        String name;
        coord coord;
        String country;
        long population;
        int timezone;
        long sunrise;
        long sunset;
    }
}
