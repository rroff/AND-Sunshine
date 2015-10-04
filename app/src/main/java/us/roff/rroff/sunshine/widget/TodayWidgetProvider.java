package us.roff.rroff.sunshine.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import us.roff.rroff.sunshine.MainActivity;
import us.roff.rroff.sunshine.R;
import us.roff.rroff.sunshine.Utility;

/**
 * Provider for a widget showing today's weather.
 */
public class TodayWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        int weatherArtResourceId = R.drawable.art_clear;
        String description = "Clear";
        double maxTemp = 24;
        String formattedMaxTemperature = Utility.formatTemperature(context, maxTemp);

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(
                    context.getPackageName(),
                    R.layout.widget_today_small);

            // Add data
            views.setImageViewResource(R.id.widget_icon, weatherArtResourceId);
            setRemoteContentDescription(views, description);
            views.setTextViewText(R.id.widget_high_textview, formattedMaxTemperature);

            // Create an Intent to launch MainActivity
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    private void setRemoteContentDescription(RemoteViews views, String description) {
        views.setContentDescription(R.id.widget_icon, description);
    }
}
