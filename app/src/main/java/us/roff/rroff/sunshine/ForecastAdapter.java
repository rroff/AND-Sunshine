package us.roff.rroff.sunshine;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import us.roff.rroff.sunshine.data.WeatherContract;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastAdapterViewHolder> {

    private final static int VIEW_TYPE_TODAY = 0;
    private final static int VIEW_TYPE_FUTURE_DAY = 1;

    private final Context mContext;

    private final ForecastAdapterOnClickHandler mOnClickHandler;

    private final ItemChoiceManager mICM;

    private boolean mUseTodayLayout;

    private Cursor mCursor;

    public ForecastAdapter(Context context, ForecastAdapterOnClickHandler onClickHandler, int choiceMode) {
        mContext = context;
        mOnClickHandler = onClickHandler;
        mICM = new ItemChoiceManager(this);
        mICM.setChoiceMode(choiceMode);
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
    }

    @Override
    public int getItemCount() {
        if (mCursor == null) return 0;
        return mCursor.getCount();
    }

    @Override
    public int getItemViewType(int position) {
        return ((position == 0) && (mUseTodayLayout)) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public ForecastAdapterViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewGroup instanceof RecyclerView) {
            int layoutId = -1;

            if (viewType == VIEW_TYPE_TODAY) {
                layoutId = R.layout.list_item_forecast_today;
            } else {
                layoutId = R.layout.list_item_forecast;
            }
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(layoutId, viewGroup, false);
            view.setFocusable(true);
            return new ForecastAdapterViewHolder(view);
        } else {
            throw new RuntimeException("Not bound to RecyclerView");
        }
    }

    /*
        This is where we fill-in the views with the contents of the cursor.
     */
    @Override
    public void onBindViewHolder(ForecastAdapterViewHolder viewHolder, int position) {
        mCursor.moveToPosition(position);

        // Placeholder image
        int weatherId = mCursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
        int fallbackIconId;
        if (getItemViewType(position) == VIEW_TYPE_TODAY) {
            fallbackIconId = Utility.getArtResourceForWeatherCondition(weatherId);
        } else {
            fallbackIconId = Utility.getIconResourceForWeatherCondition(weatherId);
        }

        String artUrl = Utility.getArtUrlForWeatherCondition(mContext, weatherId);
        Glide.with(mContext)
                .load(artUrl)
                .error(fallbackIconId)
                .crossFade()
                .into(viewHolder.mIconView);

        // No content description needed for icon in list view, as it would be a duplicate of the
        // description
        viewHolder.mIconView.setContentDescription(null);

        // Date
        long dateInMillis = mCursor.getLong(ForecastFragment.COL_WEATHER_DATE);
        viewHolder.mDateView.setText(Utility.getFriendlyDayString(mContext, dateInMillis));
        viewHolder.mDateView.setContentDescription(Utility.getFriendlyDayString(mContext, dateInMillis));

        // Forecast
        String description = mCursor.getString(ForecastFragment.COL_WEATHER_DESC);
        viewHolder.mDescriptionView.setText(description);
        viewHolder.mDescriptionView.setContentDescription(
                mContext.getString(R.string.a11y_forecast, description));

        // High
        boolean isMetric = Utility.isMetric(mContext);
        Double maxTemp = mCursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP);
        String high = Utility.formatTemperature(mContext, maxTemp, isMetric);
        viewHolder.mHighTempView.setText(high);
        viewHolder.mHighTempView.setContentDescription(
                mContext.getString(R.string.a11y_high_temp, high));

        // Low
        Double minTemp = mCursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP);
        String low = Utility.formatTemperature(mContext, minTemp, isMetric);
        viewHolder.mLowTempView.setText(low);
        viewHolder.mLowTempView.setContentDescription(
                mContext.getString(R.string.ally_low_temp, low));

        mICM.onBindViewHolder(viewHolder, position);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        mICM.onRestoreInstanceState(savedInstanceState);
    }

    public void onSaveInstanceState(Bundle outState) {
        mICM.onSaveInstanceState(outState);
    }

    public Cursor getCursor() {
        return mCursor;
    }

    public int getSelectedItemPosition() {
        return mICM.getSelectedItemPosition();
    }

    public void selectView(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof ForecastAdapterViewHolder) {
            ForecastAdapterViewHolder vfh = (ForecastAdapterViewHolder)viewHolder;
            vfh.onClick(vfh.itemView);
        }
    }

    public void swapCursor(Cursor newCursor) {
        mCursor = newCursor;
        notifyDataSetChanged();
    }

    /**
     * Cache of the children views for a forecast list item.
     */
    public class ForecastAdapterViewHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener {
        public final ImageView mIconView;
        public final TextView mDateView;
        public final TextView mDescriptionView;
        public final TextView mHighTempView;
        public final TextView mLowTempView;

        public ForecastAdapterViewHolder(View view) {
            super(view);
            mIconView        = (ImageView)view.findViewById(R.id.list_item_icon);
            mDateView        = (TextView)view.findViewById(R.id.list_item_date_textview);
            mDescriptionView = (TextView)view.findViewById(R.id.list_item_forecast_textview);
            mHighTempView    = (TextView)view.findViewById(R.id.list_item_high_textview);
            mLowTempView     = (TextView)view.findViewById(R.id.list_item_low_textview);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            mCursor.moveToPosition(adapterPosition);
            int dateColumnIndex = mCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATE);
            mOnClickHandler.onClick(mCursor.getLong(dateColumnIndex), this);
            mICM.onClick(this);
        }
    }

    public static interface ForecastAdapterOnClickHandler {
        void onClick(Long date, ForecastAdapterViewHolder vh);
    }
}