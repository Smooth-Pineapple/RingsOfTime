package com.example.ringsoftime;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;


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
    private static Float _outerRadius = null;
    private static Float _smoothness = null;

    private static Boolean _haveInit = false;

    private static int _hours;
    private static int _minutes;

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

        /*
        Radians = Take the radius (Line from center to edge of circle) and wrap it around the edge of the circle. The angle this little pizza makes is the "Radian"
        Half a circle has Pi radians, thus 2 * Math.PI is all the pizzas in a circle.
        We use 'smoothness' to define how many points will be draw to make this circumference.
        */
        Float segment = 2 * (float)Math.PI / _smoothness;
        Float minuteAsSegment = (float) ((2 * Math.PI) / 60); //How many segments in the outer ring make 1 minute
        Float minutesAsSegment = minuteAsSegment * _minutes;


        Float colourSegments = (255f/ _smoothness) * 6; //Want around 6 fades over all the points
        List<Float> rgb = new ArrayList<>(3);
        rgb.add(255f);
        rgb.add(0f);
        rgb.add(0f);

        //Draw outer ring
        Paint outerBrush = new Paint();
        outerBrush.setStrokeWidth(_brushThickness);
        drawCircle(canvas, outerBrush, rgb, _outerRadius, minutesAsSegment, segment, colourSegments);

        rgb.set(0, 255f);
        rgb.set(1, 0f);
        rgb.set(2, 0f);

        //Draw inner rings in a loop
        for(int i = 0; i < _hours; i++) {
            Paint innerBrush = new Paint();
            innerBrush.setStrokeWidth(_brushThickness / 2);

            Random r = new Random();
            transitionStepRGB(rgb, 255f/ 2);//Want to do a half fade between rings
            //Log.i("GOLI", "R: " + String.valueOf(rgb.get(0)) + ", G: " + String.valueOf(rgb.get(1)) + ", B: " + String.valueOf(rgb.get(2)));

            Float innerRadius = i * (_outerRadius / 12);
            drawCircle(canvas, innerBrush, rgb, innerRadius, minuteAsSegment * 60, segment, null);
        }

        //Rotate as my calculations start horizontally
        Matrix matrix = new Matrix();
        matrix.postRotate(-90);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        //... Then display this painted canvas
        views.setImageViewBitmap(R.id.outer_ring_id, rotatedBitmap);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Calendar currentTime = Calendar.getInstance();
        this._hours = currentTime.get(Calendar.HOUR) + 1; //Want a little  dot in the middle to represent that the widget is there!
        this._minutes = currentTime.get(Calendar.MINUTE);

        Log.i("GOLI time", String.valueOf(_hours));
        Log.i("GOLI time", String.valueOf(_minutes));

        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
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

            _outerRadius = _xCenter - _brushThickness - 1; //Account for brush thickness

            return true;
        } catch (Resources.NotFoundException e) {
            Log.e("RoT init exception", e.getMessage());
            return false;
        }
    }

    private static void drawCircle(Canvas canvas, Paint brush, List<Float> rgb, Float radius, Float totalCircumference,  Float segment, Float totalColourSegments) {
        //Now that the circle has been split into segments, draw them!
        for(float angle = 0f;  angle < totalCircumference;  angle += segment) {
            if(totalColourSegments != null)
                transitionStepRGB(rgb, totalColourSegments);

            brush.setColor(Color.rgb(Math.round(rgb.get(0)), Math.round(rgb.get(1)), Math.round(rgb.get(2))));

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
            float x = (float) (_xCenter + radius * Math.cos(angle));
            float y = (float) (_yCenter + radius * Math.sin(angle));

            //Draw to the canvas...
            canvas.drawPoint(x, y, brush);
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