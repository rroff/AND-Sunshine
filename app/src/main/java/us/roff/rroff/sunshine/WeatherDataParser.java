package us.roff.rroff.sunshine;

/**
 * Created by rroff on 6/10/2015.
 */
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WeatherDataParser {

    /**
     * Given a string of the form returned by the api call:
     * http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7
     * retrieve the maximum temperature for the day indicated by dayIndex
     * (Note: 0-indexed, so 0 would refer to the first day).
     */
    public static double getMaxTemperatureForDay(String weatherJsonStr, int dayIndex)
            throws JSONException {

        final String WEATHER_LIST_TAG = "list";
        final String WEATHER_TEMP_TAG = "temp";
        final String WEATHER_MAXTEMP_TAG = "max";

        JSONObject weather = new JSONObject(weatherJsonStr);
        JSONArray days = weather.getJSONArray(WEATHER_LIST_TAG);
        JSONObject dayInfo = days.getJSONObject(dayIndex);
        JSONObject temperatureInfo = dayInfo.getJSONObject(WEATHER_TEMP_TAG);

        return temperatureInfo.getDouble(WEATHER_MAXTEMP_TAG);
    }

}
