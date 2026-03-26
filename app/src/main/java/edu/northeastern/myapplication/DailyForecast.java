package edu.northeastern.myapplication;

public class DailyForecast {
    private final String date;
    private final int weatherCode;
    private final double maxTemp;
    private final double minTemp;
    private final double rainProbability;

    public DailyForecast(String date, int weatherCode, double maxTemp, double minTemp, double rainProbability) {
        this.date = date;
        this.weatherCode = weatherCode;
        this.maxTemp = maxTemp;
        this.minTemp = minTemp;
        this.rainProbability = rainProbability;
    }

    public String getDate() {
        return date;
    }

    public int getWeatherCode() {
        return weatherCode;
    }

    public double getMaxTemp() {
        return maxTemp;
    }

    public double getMinTemp() {
        return minTemp;
    }

    public double getRainProbability() {
        return rainProbability;
    }
}