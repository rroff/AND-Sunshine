package us.roff.rroff.sunshine.widget;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;

import java.util.concurrent.ExecutionException;

import us.roff.rroff.sunshine.R;
import us.roff.rroff.sunshine.Utility;
import us.roff.rroff.sunshine.data.WeatherContract;

public class DetailWidgetRemoteViewsService extends RemoteViewsService {
    private static final String LOG_TAG = DetailWidgetRemoteViewsService.class.getSimpleName();

    private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
    };
    // Indices matching the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_DATE = 1;
    private static final int INDEX_CONDITION_ID = 2;
    private static final int INDEX_SHORT_DESC = 3;
    private static final int INDEX_MAX_TEMP = 4;
    private static final int INDEX_MIN_TEMP = 5;

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new DetailWidgetRemoteViewsFactory();
    }

    public class DetailWidgetRemoteViewsFactory implements RemoteViewsFactory {
        private Cursor mCursor = null;

        @Override
        public void onCreate() { }

        @Override
        public void onDestroy() {
            if (mCursor != null) {
                mCursor.close();
                mCursor = null;
            }
        }

        /**
         * Called by the hosting app when the data has been changed.
         */
        @Override
        public void onDataSetChanged() {
            if (mCursor != null) {
                mCursor.close();
            }

            // Content provider is not exported, so the calling identity needs to be cleared
            // and restored.
            final long identityToken = Binder.clearCallingIdentity();
            String location = Utility.getPreferredLocation(DetailWidgetRemoteViewsService.this);
            Uri weatherForLocationUri = WeatherContract.WeatherEntry
                    .buildWeatherLocationWithStartDate(location, System.currentTimeMillis());
            mCursor = getContentResolver().query(weatherForLocationUri,
                        FORECAST_COLUMNS,
                        null,
                        null,
                        WeatherContract.WeatherEntry.COLUMN_DATE + " ASC");
            Binder.restoreCallingIdentity(identityToken);
        }

        @Override
        public int getCount() {
            return (mCursor == null ? 0 : mCursor.getCount());
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public RemoteViews getLoadingView() {
            return new RemoteViews(getPackageName(), R.layout.widget_detail_list_item);
        }

        @Override
        public RemoteViews getViewAt(int position) {
            if (  (position == AdapterView.INVALID_POSITION)
               || (mCursor == null)
               || (!mCursor.moveToPosition(position)) ) {
                return null;
            }

            RemoteViews views = new RemoteViews(getPackageName(),
                    R.layout.widget_detail_list_item);

            // Determine icon
            int weatherId = mCursor.getInt(INDEX_CONDITION_ID);
            int fallbackIconId = Utility.getIconResourceForWeatherCondition(weatherId);
            String artUrl = Utility.getArtUrlForWeatherCondition(
                    DetailWidgetRemoteViewsService.this, weatherId);
            Bitmap weatherArtImage = null;
            try {
                weatherArtImage = Glide.with(DetailWidgetRemoteViewsService.this)
                        .load(artUrl)
                        .asBitmap()
                        .error(fallbackIconId)
                        .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get();
            } catch (InterruptedException | ExecutionException e) {
                Log.e(LOG_TAG, "Error retrieving large icon from " + artUrl, e);
            }

            // Extract data from cursor, process if necessary
            long dateInMillis = mCursor.getLong(INDEX_DATE);
            String formattedDate = Utility.getFriendlyDayString(
                    DetailWidgetRemoteViewsService.this, dateInMillis);
            String description = mCursor.getString(INDEX_SHORT_DESC);
            double maxTemp = mCursor.getDouble(INDEX_MAX_TEMP);
            double minTemp = mCursor.getDouble(INDEX_MIN_TEMP);
            String formattedMaxTemperature = Utility.formatTemperature(
                    DetailWidgetRemoteViewsService.this, maxTemp);
            String formattedMinTemperature = Utility.formatTemperature(
                    DetailWidgetRemoteViewsService.this, minTemp);

            // Populate view
            if (weatherArtImage != null) {
                views.setImageViewBitmap(R.id.widget_icon, weatherArtImage);
            } else {
                views.setImageViewResource(R.id.widget_icon, fallbackIconId);
            }
            setRemoteContentDescription(views, description);

            views.setTextViewText(R.id.widget_date_textview, formattedDate);
            views.setTextViewText(R.id.widget_forecast_textview, description);
            views.setTextViewText(R.id.widget_high_textview, formattedMaxTemperature);
            views.setTextViewText(R.id.widget_low_textview, formattedMinTemperature);

            // Attach intent
            final Intent fillInIntent = new Intent();
            String locationSetting =
                    Utility.getPreferredLocation(DetailWidgetRemoteViewsService.this);
            Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                    locationSetting,
                    dateInMillis);
            fillInIntent.setData(weatherUri);
            views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);

            return views;
        }

        @Override
        public long getItemId(int position) {
            long id = position;
            if (mCursor.moveToPosition(position)) {
                id = mCursor.getLong(INDEX_WEATHER_ID);
            }
            return id;
        }

        private void setRemoteContentDescription(RemoteViews views, String description) {
            views.setContentDescription(R.id.widget_icon, description);
        }
    }
}
