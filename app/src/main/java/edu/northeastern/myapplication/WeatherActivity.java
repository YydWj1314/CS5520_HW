package edu.northeastern.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherActivity extends AppCompatActivity {

    // UI components
    private EditText etCity;
    private RadioButton rbCelsius, rbFahrenheit;
    private Switch swForecast;
    private Button btnSearch;
    private ProgressBar progressBar;
    private TextView tvLoading, tvError;
    private TextView tvCityName, tvCurrentDesc, tvCurrentTemp, tvFeelsLike, tvWind;
    private ImageView ivCurrentIcon;
    private TextView tvForecastTitle;
    private RecyclerView rvForecast;

    // Threading: background executor + UI handler
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler loadingHandler = new Handler(Looper.getMainLooper());

    // Loading animation state
    private int loadingDots = 0;
    private boolean loadingActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        // Bind UI elements
        etCity = findViewById(R.id.etCity);
        rbCelsius = findViewById(R.id.rbCelsius);
        rbFahrenheit = findViewById(R.id.rbFahrenheit);
        swForecast = findViewById(R.id.swForecast);
        btnSearch = findViewById(R.id.btnSearch);
        progressBar = findViewById(R.id.progressBar);
        tvLoading = findViewById(R.id.tvLoading);
        tvError = findViewById(R.id.tvError);

        tvCityName = findViewById(R.id.tvCityName);
        tvCurrentDesc = findViewById(R.id.tvCurrentDesc);
        tvCurrentTemp = findViewById(R.id.tvCurrentTemp);
        tvFeelsLike = findViewById(R.id.tvFeelsLike);
        tvWind = findViewById(R.id.tvWind);
        ivCurrentIcon = findViewById(R.id.ivCurrentIcon);
        tvForecastTitle = findViewById(R.id.tvForecastTitle);

        // Setup RecyclerView
        rvForecast = findViewById(R.id.rvForecast);
        rvForecast.setLayoutManager(new LinearLayoutManager(this));
        rvForecast.setNestedScrollingEnabled(false);

        // Button click triggers weather search
        btnSearch.setOnClickListener(v -> searchWeather());
    }

    /**
     * Triggered when user clicks "Search"
     * - Validate input
     * - Start loading animation
     * - Run network request in background thread
     */
    private void searchWeather() {
        String city = etCity.getText().toString().trim();

        if (city.isEmpty()) {
            tvError.setText(R.string.err_enter_city);
            return;
        }

        tvError.setText("");
        setLoading(true);

        boolean isFahrenheit = rbFahrenheit.isChecked();
        boolean showForecast = swForecast.isChecked();

        executor.execute(() -> {
            try {
                // Step 1: convert city → latitude/longitude
                LocationData location = fetchLocation(city);

                // Step 2: fetch weather using coordinates
                WeatherResult result = fetchWeather(location, isFahrenheit, showForecast);

                // Step 3: update UI on main thread
                runOnUiThread(() -> {
                    setLoading(false);
                    bindWeatherResult(result, isFahrenheit, showForecast);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    String msg = e.getMessage() == null ? "" : e.getMessage();
                    if (msg.toLowerCase(Locale.US).contains("no location")) {
                        tvError.setText(R.string.err_city_not_found);
                    } else if (msg.toLowerCase(Locale.US).contains("http")) {
                        tvError.setText(R.string.err_service);
                    } else {
                        tvError.setText(R.string.err_network);
                    }
                    tvCityName.setText(R.string.placeholder_city);
                    tvCurrentDesc.setText(R.string.placeholder_weather_desc);
                    tvCurrentTemp.setText(R.string.placeholder_temp);
                    tvFeelsLike.setText(R.string.placeholder_feels);
                    tvWind.setText(R.string.placeholder_wind);
                    setForecastVisible(false);
                });
            }
        });
    }

    /**
     * Bind data to UI
     */
    private void bindWeatherResult(WeatherResult result, boolean isFahrenheit, boolean showForecast) {
        String unit = isFahrenheit ? "°F" : "°C";

        tvCityName.setText(result.getCityDisplayName());
        int code = result.getCurrentWeatherCode();
        tvCurrentDesc.setText(ForecastAdapter.getWeatherDescription(code));
        tvCurrentTemp.setText("Temp: " + result.getCurrentTemp() + unit);
        tvFeelsLike.setText("Feels: " + result.getApparentTemp() + unit);
        tvWind.setText("Wind: " + result.getWindSpeed());
        ivCurrentIcon.setImageResource(ForecastAdapter.getWeatherIconRes(code));
        ivCurrentIcon.setContentDescription(ForecastAdapter.getWeatherDescription(code));

        // Update RecyclerView
        if (showForecast) {
            rvForecast.setAdapter(new ForecastAdapter(result.getDailyList(), isFahrenheit));
        } else {
            rvForecast.setAdapter(new ForecastAdapter(new ArrayList<>(), isFahrenheit));
        }
        setForecastVisible(showForecast);
    }

    private void setForecastVisible(boolean visible) {
        tvForecastTitle.setVisibility(visible ? TextView.VISIBLE : TextView.GONE);
        rvForecast.setVisibility(visible ? RecyclerView.VISIBLE : RecyclerView.GONE);
    }

    /**
     * Control loading UI + animation
     */
    private void setLoading(boolean loading) {
        loadingActive = loading;
        progressBar.setVisibility(loading ? ProgressBar.VISIBLE : ProgressBar.GONE);
        tvLoading.setVisibility(loading ? TextView.VISIBLE : TextView.GONE);
        btnSearch.setEnabled(!loading);

        if (loading) {
            loadingDots = 0;
            loadingHandler.post(loadingRunnable);
        } else {
            loadingHandler.removeCallbacks(loadingRunnable);
            tvLoading.setText(R.string.loading);
        }
    }

    @Override
    protected void onDestroy() {
        loadingHandler.removeCallbacks(loadingRunnable);
        executor.shutdown();
        super.onDestroy();
    }

    /**
     * Animated "Loading..." text
     */
    private final Runnable loadingRunnable = new Runnable() {
        @Override
        public void run() {
            if (!loadingActive) return;

            loadingDots = (loadingDots + 1) % 4;
            StringBuilder sb = new StringBuilder(getString(R.string.loading));
            for (int i = 0; i < loadingDots; i++) sb.append(".");
            tvLoading.setText(sb.toString());

            loadingHandler.postDelayed(this, 500);
        }
    };

    /**
     * Call geocoding API → convert city name to coordinates
     */
    private LocationData fetchLocation(String city) throws Exception {
        String encoded = URLEncoder.encode(city, StandardCharsets.UTF_8.name());
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + encoded;
        String response = httpGet(url);

        JSONObject obj = new JSONObject(response);
        if (!obj.has("results")) {
            throw new Exception("No location results");
        }
        JSONArray arr = obj.getJSONArray("results");
        if (arr.length() == 0) {
            throw new Exception("No location results");
        }
        JSONObject first = arr.getJSONObject(0);

        return new LocationData(
                first.getString("name"),
                first.getDouble("latitude"),
                first.getDouble("longitude")
        );
    }

    /**
     * Call weather API → get current + 7-day forecast
     */
    private WeatherResult fetchWeather(LocationData loc, boolean isF, boolean show) throws Exception {

        String tempUnit = isF ? "fahrenheit" : "celsius";

        String url = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + loc.lat
                + "&longitude=" + loc.lon
                + "&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"
                + "&temperature_unit=" + tempUnit
                + "&wind_speed_unit=kmh"
                + "&timezone=auto";

        String response = httpGet(url);
        JSONObject obj = new JSONObject(response);

        JSONObject cur = obj.getJSONObject("current");

        double currentTemp = cur.getDouble("temperature_2m");
        double apparentTemp = cur.getDouble("apparent_temperature");
        double windSpeed = cur.getDouble("wind_speed_10m");
        int currentWeatherCode = cur.getInt("weather_code");

        List<DailyForecast> dailyList = new ArrayList<>();

        // Parse daily forecast
        if (show && obj.has("daily")) {
            JSONObject daily = obj.getJSONObject("daily");

            JSONArray timeArr = daily.getJSONArray("time");
            JSONArray codeArr = daily.getJSONArray("weather_code");
            JSONArray maxArr = daily.getJSONArray("temperature_2m_max");
            JSONArray minArr = daily.getJSONArray("temperature_2m_min");
            JSONArray rainArr = daily.getJSONArray("precipitation_probability_max");

            for (int i = 0; i < timeArr.length(); i++) {
                dailyList.add(new DailyForecast(
                        timeArr.getString(i),
                        codeArr.getInt(i),
                        maxArr.getDouble(i),
                        minArr.getDouble(i),
                        rainArr.getDouble(i)
                ));
            }
        }

        return new WeatherResult(
                loc.name,
                currentTemp,
                apparentTemp,
                windSpeed,
                currentWeatherCode,
                dailyList
        );
    }

    /**
     * Generic HTTP GET request
     */
    private String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new Exception("HTTP " + code);
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Simple data class for location
     */
    private static class LocationData {
        String name;
        double lat, lon;

        LocationData(String n, double la, double lo) {
            name = n;
            lat = la;
            lon = lo;
        }
    }
}