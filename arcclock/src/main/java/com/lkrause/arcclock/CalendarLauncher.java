package com.lkrause.arcclock;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.format.Time;
import android.util.Log;
import android.widget.RemoteViews;

class CalFace
{
    /* contains all information required to draw a clock face of a certain, constant size
       Main purpose is
     */
    public CalFace(int size)
    {
        CANVAS_SIZE = size+1;
        canvas = new Canvas();

        borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.FILL);

        hourPaint = new Paint(borderPaint);
        hourPaint.setStyle(Paint.Style.FILL);

        minutePaint = new Paint(hourPaint);
        minutePaint.setStyle(Paint.Style.FILL);
        minutePaint.setARGB(255,255,255,255);

        bitmap = Bitmap.createBitmap(CANVAS_SIZE, CANVAS_SIZE, Config.ARGB_8888);

    }

    final int CANVAS_SIZE;
    final Canvas canvas;
    final Bitmap bitmap;
    final Paint borderPaint;
    final Paint hourPaint;
    final Paint minutePaint;
}

public class CalendarLauncher extends AppWidgetProvider {


	public static final String CLOCK_UPDATE = "com.lkrause.bitclock16.CAL_UPDATE";
	public static final String OPEN_CALENDAR_ACTION = "com.lkrause.bitclock16.OPEN_CALENDAR";
    // public static final String CLICK_ACTION = "com.lkrause.bitclock16.CLICK";
    // rounding issues
    // http://stackoverflow.com/questions/2748590/clickable-widgets-in-android
	private static final long TICK =  10000;
	private static final int CANVAS_SIZE = 192;
	private static final float cx = CANVAS_SIZE / 2;
	private static final float cy = CANVAS_SIZE / 2;
    private static final float baseWidth = CANVAS_SIZE / 32f;
    private static final float borderWidth = CANVAS_SIZE/7f;

	// Colors
    static Bitmap bitmap = Bitmap.createBitmap(CANVAS_SIZE, CANVAS_SIZE, Config.ARGB_8888);
    static Canvas canvas = new Canvas(bitmap);
    static Paint p = new Paint();
    static final int bgColorBottom = Color.argb(210,96, 96, 255);
    static final int bgColorTop = Color.argb(210, 64, 64, 224);
    static final int bgColorThree = Color.argb(210, 192,192,192);
    static private final float textSize = CANVAS_SIZE / 2.5f;
    static final int textColor = Color.argb(255,224,224,224);
    static final float radius = borderWidth;
    static final float upperHeight = CANVAS_SIZE/16f;

	// Time Control

    // private android.hardware.camera2.CameraManager cameraManager;

    public void updateTime()
    {

    }

    public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds)
    {

    }
    @Override
	public void onReceive(Context context, Intent intent) {

		super.onReceive(context, intent);

        updateTime();

		// Get the widget manager and ids for this widget provider, then call the shared clock update method.
		ComponentName thisAppWidget = new ComponentName(context.getPackageName(), getClass().getName());
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

		// Clock Update Event
		if (CLOCK_UPDATE.equals(intent.getAction())) {
			int ids[] = appWidgetManager.getAppWidgetIds(thisAppWidget);
			for (int appWidgetID : ids) {
				updateClock(context, appWidgetManager, appWidgetID);
			}
		}


        // Touch Event
        if (OPEN_CALENDAR_ACTION.equals(intent.getAction())) {
            openCalendar(context);
            // update all clocks for new visuals to take effect
            int ids[] = appWidgetManager.getAppWidgetIds(thisAppWidget);
            for (int appWidgetID : ids) {
                updateClock(context, appWidgetManager, appWidgetID);
            }
        }
/*
        if (CLICK_ACTION.equals(intent.getAction()))
        {
            maxAlpha = 254;
            try {
                cameraManager = (android.hardware.camera2.CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
                String[] ids = cameraManager.getCameraIdList();
                cameraManager.setTorchMode(ids[0], true);
            } catch(android.hardware.camera2.CameraAccessException x) {

            } catch(java.lang.Exception x) {
            }
        }
*/


	}
    
    private PendingIntent createClockTickIntent(Context context) {
		Intent intent = new Intent(CLOCK_UPDATE);
		return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	private PendingIntent createColorSwitchIntent(Context context) {
		Intent intent = new Intent(OPEN_CALENDAR_ACTION);
		return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	@Override
	public void onDisabled(Context context) {

		super.onDisabled(context);

		// Stop the Timer
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(createClockTickIntent(context));
	}

	@Override
	public void onEnabled(Context context)
    {
		super.onEnabled(context);
        p.setAntiAlias(true);

		// Create the Timer
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), TICK, createClockTickIntent(context));
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

		// Perform this loop procedure for each App Widget that belongs to this provider
		for (int appWidgetId : appWidgetIds) {

			// Get the layout for the App Widget and attach an on-click listener to the button
			AppWidgetProviderInfo appInfo = appWidgetManager.getAppWidgetInfo(appWidgetId);
			if (context == null || appInfo == null) {
				return;
			}
			RemoteViews views = new RemoteViews(context.getPackageName(), appInfo.initialLayout);

			// Update The clock label using a shared method
			updateClock(context, appWidgetManager, appWidgetId);

            // Touch Intent
            PendingIntent p = createColorSwitchIntent(context);
            views.setOnClickPendingIntent(R.id.image, p);

			// Tell the AppWidgetManager to perform an update on the current app widget
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}


	public static void updateClock(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

		// Get a reference to our Remote View
		AppWidgetProviderInfo appInfo = appWidgetManager.getAppWidgetInfo(appWidgetId);
		RemoteViews view = new RemoteViews(context.getPackageName(), appInfo.initialLayout);
        Time today = new Time(Time.getCurrentTimezone());
        today.setToNow();

        bitmap.eraseColor(Color.TRANSPARENT);
        p.setAntiAlias(true);
        p.setColor(bgColorThree);
        canvas.drawRoundRect(new RectF(baseWidth,0, CANVAS_SIZE, CANVAS_SIZE), radius, radius, p);
        // draw background lower
        p.setColor(bgColorBottom);
        canvas.drawRoundRect(new RectF(baseWidth, baseWidth, CANVAS_SIZE, CANVAS_SIZE), radius, radius, p);

        // draw date
        p.setColor(textColor);
        p.setStyle(Paint.Style.FILL);
        p.setTextSize(textSize);

        // draw number of day in month
        String monthDay = String.valueOf(today.monthDay);
        p.setTextSize(textSize);
        final float tw = p.measureText(monthDay);
        final float ty = (p.descent() + p.ascent());
        final float dayY = cy-ty/2;
        canvas.drawText(monthDay, cx-tw/2, dayY, p);

        // draws day of week
        final String weekday = today.format("%a");
        p.setTextSize(textSize/2f);
        final float smallHeight = (p.descent() + p.ascent());
        final float weekdayWidth = p.measureText(weekday);
        canvas.drawText(weekday, cx-weekdayWidth/2f,dayY-smallHeight+borderWidth, p);

        // draw name of month
        String month = today.format("%h");
        canvas.drawText(month, cx-textSize/2,dayY-textSize/0.2f, p);

        // update widget view
        view.setImageViewBitmap(R.id.image, bitmap);
		appWidgetManager.updateAppWidget(appWidgetId, view);
	}


    private void openCalendar(Context context) {
        try
        {
            Log.d("openCalendar", "begin ");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("vnd.android.cursor.item/event");
            intent.putExtra("title", "Some title");
            intent.putExtra("description", "Some description");
//            intent.putExtra("beginTime", eventStartInMillis);
//            intent.putExtra("endTime", eventEndInMillis);
            // is there a better way?
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
        catch(Exception exc) {
            Log.i("openCalendar", "exception has happened "+exc.getMessage());
        }
    }}
