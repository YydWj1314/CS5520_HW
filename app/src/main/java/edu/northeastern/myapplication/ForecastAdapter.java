package edu.northeastern.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.VH> {

    private final List<DailyForecast> data;
    private final boolean isFahrenheit;

    public ForecastAdapter(List<DailyForecast> data, boolean isFahrenheit) {
        this.data = data;
        this.isFahrenheit = isFahrenheit;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_forecast, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        DailyForecast f = data.get(position);

        h.tvDate.setText(f.getDate());
        h.tvTemp.setText(String.format(Locale.US, "%.1f° / %.1f°",
                f.getMaxTemp(), f.getMinTemp()));
        h.tvRain.setText(String.format(Locale.US, "Rain: %.0f%%", f.getRainProbability()));
        String desc = getWeatherDescription(f.getWeatherCode());
        h.tvDesc.setText(desc);
        h.ivIcon.setImageResource(getWeatherIconRes(f.getWeatherCode()));
        h.ivIcon.setContentDescription(desc);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDate, tvTemp, tvRain, tvDesc;
        ImageView ivIcon;

        VH(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTemp = itemView.findViewById(R.id.tvTemp);
            tvRain = itemView.findViewById(R.id.tvRain);
            tvDesc = itemView.findViewById(R.id.tvDesc);
            ivIcon = itemView.findViewById(R.id.ivIcon);
        }
    }

    public static String getWeatherDescription(int code) {
        if (code == 0) return "Clear sky";
        if (code == 1 || code == 2 || code == 3) return "Cloudy";
        if (code == 45 || code == 48) return "Fog";
        if (code == 51 || code == 53 || code == 55) return "Drizzle";
        if (code == 61 || code == 63 || code == 65) return "Rain";
        if (code == 71 || code == 73 || code == 75) return "Snow";
        if (code == 80 || code == 81 || code == 82) return "Rain showers";
        if (code == 95 || code == 96 || code == 99) return "Thunderstorm";
        return "Unknown";
    }

    public static int getWeatherIconRes(int code) {
        if (code == 0) return R.drawable.ic_weather_sun;
        if (code == 1 || code == 2 || code == 3) return R.drawable.ic_weather_cloud;
        if (code == 45 || code == 48) return R.drawable.ic_weather_fog;
        if (code == 51 || code == 53 || code == 55) return R.drawable.ic_weather_rain;
        if (code == 61 || code == 63 || code == 65 || code == 80 || code == 81 || code == 82) {
            return R.drawable.ic_weather_rain;
        }
        if (code == 71 || code == 73 || code == 75) return R.drawable.ic_weather_snow;
        if (code == 95 || code == 96 || code == 99) return R.drawable.ic_weather_thunder;
        return R.drawable.ic_weather_cloud;
    }
}