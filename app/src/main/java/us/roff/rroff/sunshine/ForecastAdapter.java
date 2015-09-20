package us.roff.rroff.sunshine;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class ForecastAdapter extends CursorAdapter {

    private final static int VIEW_TYPE_TODAY = 0;
    private final static int VIEW_TYPE_FUTURE_DAY = 1;

    private boolean mUseTodayLayout;

    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
    }

    @Override
    public int getItemViewType(int position) {
        return ((position == 0) && (mUseTodayLayout)) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Choose layout type
        int viewType = getItemViewType(cursor.getPosition());
        int layoutId = -1;

        if (viewType == VIEW_TYPE_TODAY) {
            layoutId = R.layout.list_item_forecast_today;
        } else {
            layoutId = R.layout.list_item_forecast;
        }

        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    /*
        This is where we fill-in the views with the contents of the cursor.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder viewHolder = (ViewHolder)view.getTag();

        // Placeholder image
        int viewType = getItemViewType(cursor.getPosition());
        int weatherId = cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
        int fallbackIconId;
        if (viewType == VIEW_TYPE_TODAY) {
            fallbackIconId = Utility.getArtResourceForWeatherCondition(weatherId);
        } else {
            fallbackIconId = Utility.getIconResourceForWeatherCondition(weatherId);
        }

        String artUrl = Utility.getArtUrlForWeatherCondition(context, weatherId);
        Glide.with(context)
                .load(artUrl)
                .error(fallbackIconId)
                .crossFade()
                .into(viewHolder.mIconView);

        // No content description needed for icon in list view, as it would be a duplicate of the
        // description
        viewHolder.mIconView.setContentDescription(null);

        // Date
        long dateInMillis = cursor.getLong(ForecastFragment.COL_WEATHER_DATE);
        viewHolder.mDateView.setText(Utility.getFriendlyDayString(context, dateInMillis));
        viewHolder.mDateView.setContentDescription(Utility.getFriendlyDayString(context, dateInMillis));

        // Forecast
        String description = cursor.getString(ForecastFragment.COL_WEATHER_DESC);
        viewHolder.mDescriptionView.setText(description);
        viewHolder.mDescriptionView.setContentDescription(
                context.getString(R.string.a11y_forecast, description));

        // High
        boolean isMetric = Utility.isMetric(context);
        Double maxTemp = cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP);
        String high = Utility.formatTemperature(context, maxTemp, isMetric);
        viewHolder.mHighTempView.setText(high);
        viewHolder.mHighTempView.setContentDescription(
                context.getString(R.string.a11y_high_temp, high));

        // Low
        Double minTemp = cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP);
        String low = Utility.formatTemperature(context, minTemp, isMetric);
        viewHolder.mLowTempView.setText(low);
        viewHolder.mLowTempView.setContentDescription(
                context.getString(R.string.ally_low_temp, low));
    }

    /**
     * Cache of the children views for a forecast list item.
     */
    private static class ViewHolder {
        public final ImageView mIconView;
        public final TextView mDateView;
        public final TextView mDescriptionView;
        public final TextView mHighTempView;
        public final TextView mLowTempView;

        public ViewHolder(View view) {
            mIconView        = (ImageView)view.findViewById(R.id.list_item_icon);
            mDateView        = (TextView)view.findViewById(R.id.list_item_date_textview);
            mDescriptionView = (TextView)view.findViewById(R.id.list_item_forecast_textview);
            mHighTempView    = (TextView)view.findViewById(R.id.list_item_high_textview);
            mLowTempView     = (TextView)view.findViewById(R.id.list_item_low_textview);
        }
    }
}