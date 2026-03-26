package edu.northeastern.myapplication;

import java.util.List;

public class WeatherResult {
    private final String cityDisplayName;
    private final double currentTemp;
    private final double apparentTemp;
    private final double windSpeed;
    private final int currentWeatherCode;
    private final List<DailyForecast> dailyList;

    public WeatherResult(String cityDisplayName,
                         double currentTemp,
                         double apparentTemp,
                         double windSpeed,
                         int currentWeatherCode,
                         List<DailyForecast> dailyList) {
        this.cityDisplayName = cityDisplayName;
        this.currentTemp = currentTemp;
        this.apparentTemp = apparentTemp;
        this.windSpeed = windSpeed;
        this.currentWeatherCode = currentWeatherCode;
        this.dailyList = dailyList;
    }

    public String getCityDisplayName() {
        return cityDisplayName;
    }

    public double getCurrentTemp() {
        return currentTemp;
    }

    public double getApparentTemp() {
        return apparentTemp;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public int getCurrentWeatherCode() {
        return currentWeatherCode;
    }

    public List<DailyForecast> getDailyList() {
        return dailyList;
    }
}