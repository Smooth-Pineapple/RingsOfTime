package com.example.ringsoftime;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


/**
 * Implementation of App Widget functionality.
 * This handles widget behaviour
 */
public class RingsOfTimeWidget extends AppWidgetProvider {

    private static Integer _colour = null;
    private static Float _bitmapWidth = null;
    private static Float _bitmapHeight = null;
    private static Float _brushThickness = null;
    private static Float _xCenter = null;
    private static Float _yCenter = null;
    private static Float _radius = null;
    private static Float _smoothness = null;

    private static Boolean _haveInit = false;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        Log.i("GOLI", "UPDATE");

        //Maybe only need to do this for development?
        if(Boolean.FALSE.equals(_haveInit)){
            _haveInit = initValues(context);
        }
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.rings_of_time_widget);

        //Create 2x2 bitmap with supposedly best way to store pixel RGBA data
        Bitmap bitmap = Bitmap.createBitmap(_bitmapWidth.intValue(), _bitmapHeight.intValue(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();

        paint.setStrokeWidth(_brushThickness);

        /*
        Radians = Take the radius (Line from center to edge of circle) and wrap it around the edge of the circle. The angle this little pizza makes is the "Radian"
        Half a circle has Pi radians, thus 2 * Math.PI is all the pizzas in a circle.
        We use 'smoothness' to define how many points will be draw to make this circumference.
        */
        Float segment = 2 * (float)Math.PI / _smoothness;

        Float totalSegments = (float) ((2 * Math.PI)/ segment);
        Float totalColourSegments = (255f/ totalSegments) * 6;

        List<Float> rgb = new ArrayList<>(3);
        rgb.add(255f);
        rgb.add(0f);
        rgb.add(0f);

        //Now that the circle has been split into segments, draw them!
        for(float angle = 0f;  angle < 2 * Math.PI;  angle += segment) {
            transitionStepRGB(rgb, totalColourSegments);
            paint.setColor(Color.rgb(Math.round(rgb.get(0)), Math.round(rgb.get(1)), Math.round(rgb.get(2))));

            /*
            We must "flatten" the pizza to do go from angles to x, y positions:

                                                     This point is hitting the circumference
                                                       /|
                                                     /  | y
            This point is hitting center of circle /____|
                                                     x

            Adjacent = Line next to angle, here the angle is happening next to the center!
            Opposite = Line not touching angle
            Hypotenuse = Long one

            Cosine = Gives us 'x' as it's the Adjacent/ Hypotenuse
            Sine = Gives us 'y' as it's Opposite/ Hypotenuse

            Then, as we have our angle, we just need to get the coordinates in relation to x/y and the radius
            */
            float x = (float) (_xCenter + _radius * Math.cos(angle));
            float y = (float) (_yCenter + _radius * Math.sin(angle));

            //Draw to the canvas...
            canvas.drawPoint(x, y, paint);
        }

        //... Then display this painted canvas
        views.setImageViewBitmap(R.id.outer_ring_id, bitmap);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }

        Date currentTime = Calendar.getInstance().getTime();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        String time = timeFormat.format(currentTime);

        Log.i("GOLI time", time);
    }

    @Override
    public void onEnabled(Context context) {
        Log.i("GOLI", "ENABLE");
        // Enter relevant functionality for when the first widget is created
        _haveInit = initValues(context);
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    private static Boolean initValues(Context context){
        try {
            _colour = context.getResources().getColor(R.color.green);
            _bitmapWidth = Float.parseFloat(context.getResources().getString(R.string.bitmap_width));
            _bitmapHeight = Float.parseFloat(context.getResources().getString(R.string.bitmap_height));
            _brushThickness = Float.parseFloat(context.getResources().getString(R.string.brush_thickness));
            _smoothness = Float.parseFloat(context.getResources().getString(R.string.smoothness));

            _xCenter = _bitmapWidth / 2f;
            _yCenter = _bitmapHeight / 2f;

            _radius = _xCenter - _brushThickness - 1; //Account for brush thickness

            return true;
        } catch (Resources.NotFoundException e) {
            Log.e("RoT init exception", e.getMessage());
            return false;
        }
    }

    /*
    When called in a loop this will transition across RGB like a slider on a colour chooser
    */
    private static void transitionStepRGB(List<Float> rgb, Float totalColourSegments) {
        for(int col = 0; col < 3; col++) {
            int next = col + 1;
            int prev = col - 1;

            if(next == 3)
                next = 0;
            if(prev == -1)
                prev = 2;

            if(rgb.get(col) == 255f) {
                if(rgb.get(next) == 255f) {
                    Float iGoDown = rgb.get(col) - totalColourSegments;
                    if(iGoDown < 0f)
                        iGoDown = 0f;

                    rgb.set(col, iGoDown);
                } else{
                    if(rgb.get(prev) != 0 ) {
                        Float prevGoDown = rgb.get(prev) - totalColourSegments;
                        if(prevGoDown < 0f)
                            prevGoDown = 0f;

                        rgb.set(prev, prevGoDown);
                    } else {
                        Float nextGoUp = rgb.get(next) + totalColourSegments;
                        if(nextGoUp > 255f)
                            nextGoUp = 255f;

                        rgb.set(next, nextGoUp);
                    }
                }

                break;
            }
        }
    }
}