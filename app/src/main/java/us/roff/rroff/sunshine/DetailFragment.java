package us.roff.rroff.sunshine;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import us.roff.rroff.sunshine.data.WeatherContract;


/**
 * A placeholder fragment containing a simple view.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String DETAIL_URI = "URI";

    private static final String LOG_TAG = DetailFragment.class.getSimpleName();

    private static final String FORECAST_HASHTAG = "#SunshineApp";

    private static final int DETAIL_LOADER_ID = 1;

    private static final String[] DETAIL_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    private static final int COL_WEATHER_ID = 0;
    private static final int COL_WEATHER_DATE = 1;
    private static final int COL_WEATHER_DESC = 2;
    private static final int COL_WEATHER_MAX_TEMP = 3;
    private static final int COL_WEATHER_MIN_TEMP = 4;
    private static final int COL_WEATHER_HUMIDITY = 5;
    private static final int COL_WEATHER_WIND_SPEED = 6;
    private static final int COL_WEATHER_DEGREES = 7;
    private static final int COL_WEATHER_PRESSURE = 8;
    private static final int COL_WEATHER_CONDITION_ID = 9;

    private ShareActionProvider mShareActionProvider;

    private String mForecast;

    private Uri mUri;

    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(DETAIL_LOADER_ID, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.detailfragment, menu);

        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);

        // Get the provider and hold onto it to set/change the share intent.
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        // Attach an intent to this ShareActionProvider.  You can update this at any time,
        // like when the user selects a new piece of data they might like to share.
        if (mShareActionProvider != null ) {
            // If onLoadFinished happens before this, we can go ahead and set the share intent now.
            if (mForecast != null) {
                mShareActionProvider.setShareIntent(createShareForecastIntent());
            }
        } else {
            Log.d(LOG_TAG, "Share Action Provider is null?");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(DetailFragment.DETAIL_URI);
        }

        View view = inflater.inflate(R.layout.fragment_detail, container, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    private Intent createShareForecastIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mForecast + " " + FORECAST_HASHTAG);
        return shareIntent;
    }

    void onLocationChanged( String newLocation ) {
        // replace the uri, since the location has changed
        Uri uri = mUri;
        if (uri != null) {
            long date = WeatherContract.WeatherEntry.getDateFromUri(uri);
            Uri updatedUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(newLocation, date);
            mUri = updatedUri;
            getLoaderManager().restartLoader(DETAIL_LOADER_ID, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (mUri != null) {
            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.
            return new CursorLoader(
                    getActivity(),
                    mUri,
                    DETAIL_COLUMNS,
                    null,
                    null,
                    null
            );
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) { return; }

        ViewHolder viewHolder = (ViewHolder)getView().getTag();

        // Use weather art image, if available
        int weatherId = data.getInt(DetailFragment.COL_WEATHER_CONDITION_ID);
        Glide.with(this)
                .load(Utility.getArtUrlForWeatherCondition(getActivity(), weatherId))
                .error(Utility.getArtResourceForWeatherCondition(weatherId))
                .into(viewHolder.iconView);

        // Date/Day
        long dateInMillis = data.getLong(DetailFragment.COL_WEATHER_DATE);
        String dateString = Utility.getFormattedMonthDay(getActivity(), dateInMillis);
        viewHolder.dateView.setText(dateString);
        viewHolder.dayView.setText(Utility.getDayName(getActivity(), dateInMillis));

        // Forecast
        String description = data.getString(DetailFragment.COL_WEATHER_DESC);
        viewHolder.descriptionView.setText(description);
        viewHolder.descriptionView.setContentDescription(
                getString(R.string.a11y_forecast, description));

        // Add content description to icon for accessibility
        viewHolder.iconView.setContentDescription(
                getString(R.string.a11y_forecast_icon, description));

        // High
        boolean isMetric = Utility.isMetric(getActivity());
        Double maxTemp = data.getDouble(DetailFragment.COL_WEATHER_MAX_TEMP);
        String maxTempString = Utility.formatTemperature(getActivity(), maxTemp, isMetric);
        viewHolder.highTempView.setText(maxTempString);
        viewHolder.highTempView.setContentDescription(
                getString(R.string.a11y_high_temp, maxTempString));

        // Low
        Double minTemp = data.getDouble(DetailFragment.COL_WEATHER_MIN_TEMP);
        String minTempString = Utility.formatTemperature(getActivity(), minTemp, isMetric);
        viewHolder.lowTempView.setText(minTempString);
        viewHolder.lowTempView.setContentDescription(
                getString(R.string.ally_low_temp, minTempString));

        // Humidity
        Double humidity = data.getDouble(DetailFragment.COL_WEATHER_HUMIDITY);
        viewHolder.humidityView.setText(
                String.format(getActivity().getString(R.string.format_humidity), humidity));
        viewHolder.humidityView.setContentDescription(
                String.format(getActivity().getString(R.string.format_humidity), humidity));

        // Windspeed
        Float windspeed = data.getFloat(DetailFragment.COL_WEATHER_WIND_SPEED);
        Float degrees = data.getFloat(DetailFragment.COL_WEATHER_DEGREES);
        viewHolder.windspeedView.setText(Utility.getFormattedWind(getActivity(), windspeed, degrees));
        viewHolder.windspeedView.setContentDescription(
                Utility.getWindContentDescriptor(getActivity(), windspeed, degrees));

        // Pressure
        Double pressure = data.getDouble(DetailFragment.COL_WEATHER_PRESSURE);
        viewHolder.pressureView.setText(
                String.format(getActivity().getString(R.string.format_pressure), pressure));

        mForecast = String.format("%s - %s - %s/%s",
                dateString, description, maxTempString, minTempString);

        // If onCreateOptionsMenu has already happened, we need to update the share intent now.
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) { }

    /**
     * Cache of the children views for a forecast list item.
     */
    private static class ViewHolder {
        public final ImageView iconView;
        public final TextView dateView;
        public final TextView dayView;
        public final TextView descriptionView;
        public final TextView highTempView;
        public final TextView lowTempView;
        public final TextView humidityView;
        public final TextView windspeedView;
        public final TextView pressureView;

        public ViewHolder(View view) {
            iconView = (ImageView) view.findViewById(R.id.detail_icon);
            dateView = (TextView) view.findViewById(R.id.detail_date_textview);
            dayView = (TextView) view.findViewById(R.id.detail_day_textview);
            descriptionView = (TextView) view.findViewById(R.id.detail_forecast_textview);
            highTempView = (TextView) view.findViewById(R.id.detail_high_textview);
            lowTempView = (TextView) view.findViewById(R.id.detail_low_textview);
            humidityView = (TextView) view.findViewById(R.id.detail_humidity_textview);
            windspeedView = (TextView) view.findViewById(R.id.detail_wind_textview);
            pressureView = (TextView) view.findViewById(R.id.detail_pressure_textview);
        }
    }
}
