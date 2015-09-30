package us.roff.rroff.sunshine;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.TextView;

import us.roff.rroff.sunshine.data.WeatherContract;
import us.roff.rroff.sunshine.sync.SunshineSyncAdapter;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>,
                   SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String LOG_TAG = ForecastFragment.class.getSimpleName();

    private static final String SELECTED_POSITION_KEY = "SelectedPosition";

    private static final int FORECAST_LOADER_ID = 1;

    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_LOCATION_SETTING = 5;
    public static final int COL_WEATHER_CONDITION_ID = 6;
    public static final int COL_COORD_LAT = 7;
    public static final int COL_COORD_LONG = 8;

    private RecyclerView mForecastView;

    private TextView mEmptyView;

    private ForecastAdapter mForecastAdapter;

    private int mSelectedPosition = RecyclerView.NO_POSITION;

    private boolean mUseTodayLayout;

    private boolean mAutoSelectView;

    private int mChoiceMode;

    public ForecastFragment() {
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
        if (mForecastAdapter != null) {
            mForecastAdapter.setUseTodayLayout(mUseTodayLayout);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER_ID, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);
        TypedArray a = activity.obtainStyledAttributes(attrs, R.styleable.ForecastFragment,
                0, 0);
        mChoiceMode = a.getInt(R.styleable.ForecastFragment_android_choiceMode, AbsListView.CHOICE_MODE_NONE);
        mAutoSelectView = a.getBoolean(R.styleable.ForecastFragment_autoSelectView, false);
        a.recycle();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mForecastView = (RecyclerView)rootView.findViewById(R.id.recyclerview_forecast);
        mEmptyView = (TextView)rootView.findViewById(R.id.empty_forecast);
        mForecastView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mForecastAdapter = new ForecastAdapter(getActivity(),new ForecastAdapter.ForecastAdapterOnClickHandler() {
            @Override
            public void onClick(Long date, ForecastAdapter.ForecastAdapterViewHolder vh) {
                String locationSetting = Utility.getPreferredLocation(getActivity());
                Uri uri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                        locationSetting, date);

                Activity activity = getActivity();
                if (activity instanceof Callback) {
                    ((Callback) activity).onItemSelected(uri);
                }

                mSelectedPosition = vh.getAdapterPosition();
            }
        }, mChoiceMode);
        mForecastAdapter.setUseTodayLayout(mUseTodayLayout);
        mForecastView.setAdapter(mForecastAdapter);

        final View parallaxView = rootView.findViewById(R.id.parallax_bar);
        if ((parallaxView != null) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)) {
            mForecastView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    int max = parallaxView.getHeight();
                    if (dy > 0) {
                        parallaxView.setTranslationY(Math.max(-max, parallaxView.getTranslationY() - (dy/2)));
                    } else {
                        parallaxView.setTranslationY(Math.min(0, parallaxView.getTranslationY() - (dy/2)));
                    }
                }
            });
        }

        // If there's instance state, mine it for useful information.
        // The end-goal here is that the user never knows that turning their device sideways
        // does crazy lifecycle related things.  It should feel like some stuff stretched out,
        // or magically appeared to take advantage of room, but data or place in the app was never
        // actually *lost*.
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SELECTED_POSITION_KEY)) {
                // The Recycler View probably hasn't even been populated yet.  Actually perform the
                // swapout in onLoadFinished.
                mSelectedPosition = savedInstanceState.getInt(SELECTED_POSITION_KEY);
            }
            mForecastAdapter.onRestoreInstanceState(savedInstanceState);
        }

        return rootView;
    }

    @Override
    public void onPause() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    public void onDestroy() {
        if (mForecastView != null) {
            mForecastView.clearOnScrollListeners();
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_refresh) {
//            updateWeather();
//            return true;
//        }
        if (id == R.id.action_mapview) {
            openPreferredLocationInMap();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mSelectedPosition != RecyclerView.NO_POSITION) {
            outState.putInt(SELECTED_POSITION_KEY, mSelectedPosition);
        }
        mForecastAdapter.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);

    }

    private void updateWeather() {
        SunshineSyncAdapter.syncImmediately(getActivity());
    }

    public void onLocationChanged() {
        updateWeather();
        getLoaderManager().restartLoader(FORECAST_LOADER_ID, null, this);
    }

    private void openPreferredLocationInMap() {
        String location = Utility.getPreferredLocation(getActivity());

        if (mForecastAdapter != null) {
            Cursor cursor = mForecastAdapter.getCursor();

            if (cursor != null) {
                cursor.moveToFirst();
                String lat = cursor.getString(COL_COORD_LAT);
                String lon = cursor.getString(COL_COORD_LONG);

                Uri geolocation = Uri.parse("geo:" + lat + "," + lon);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(geolocation);
                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Log.w(LOG_TAG, "Unable to open geolocation intent ("
                            + geolocation.toString() + ")");
                }
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String locationSetting = Utility.getPreferredLocation(getActivity());

        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis());

        return new CursorLoader(getActivity(), weatherForLocationUri,
                FORECAST_COLUMNS, null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mForecastAdapter.swapCursor(cursor);

        if (mSelectedPosition != RecyclerView.NO_POSITION) {
            mForecastView.smoothScrollToPosition(mSelectedPosition);
        }

        updateEmptyView();
        if (cursor.getCount() > 0 ) {
            mForecastView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    // Since we know we're going to get items, we keep the listener around until
                    // we see Children.
                    if (mForecastView.getChildCount() > 0) {
                        mForecastView.getViewTreeObserver().removeOnPreDrawListener(this);
                        int itemPosition = mForecastAdapter.getSelectedItemPosition();
                        if (RecyclerView.NO_POSITION == itemPosition) {
                            itemPosition = 0;
                        }
                        RecyclerView.ViewHolder vh = mForecastView.findViewHolderForAdapterPosition(itemPosition);
                        if (null != vh && mAutoSelectView) {
                            mForecastAdapter.selectView(vh);
                        }
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mForecastAdapter.swapCursor(null);
    }

    private void updateEmptyView() {
        if (mEmptyView != null) {
            if (mForecastAdapter.getItemCount() == 0) {
                if (!Utility.isNetworkAvailable(getActivity())) {
                    mEmptyView.setText(R.string.empty_forecast_list_no_network);
                } else {
                    int message = R.string.empty_forecast_list;
                    switch (Utility.getLocationStatus(getActivity())) {
                        case SunshineSyncAdapter.LOCATION_STATUS_SERVER_DOWN:
                            message = R.string.empty_forecast_list_server_down;
                            break;
                        case SunshineSyncAdapter.LOCATION_STATUS_SERVER_INVALID:
                            message = R.string.empty_forecast_list_server_error;
                            break;
                        case SunshineSyncAdapter.LOCATION_STATUS_INVALID:
                            message = R.string.empty_forecast_list_invalid_location;
                            break;
                        default:
                            if (!Utility.isNetworkAvailable(getActivity())) {
                                message = R.string.empty_forecast_list_no_network;
                            }
                            break;
                    }
                    mEmptyView.setText(message);
                }
                mEmptyView.setVisibility(View.VISIBLE);
            } else {
                mEmptyView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_location_status_key))) {
            updateEmptyView();
        }
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(Uri dateUri);
    }
}
