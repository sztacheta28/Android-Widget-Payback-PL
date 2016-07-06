package pl.edu.uksw.paybackwidget;

import java.io.IOException;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import pl.edu.uksw.paybackwidget.helper.Payback;

public class WidgetProvider extends AppWidgetProvider {

    SharedPreferences settings = null;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {

        for (int appWidgetId : appWidgetIds) {
            Intent intent = new Intent(context, WidgetProvider.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_layout);

            views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
        
        new MyAsyncTask(context).execute();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent intent2 = new Intent(context, WidgetProvider.class);
        intent2.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent2, 0);

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.widget_layout);

        remoteViews.setTextViewText(R.id.pointsView, context.getString(R.string.update));
        remoteViews.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);

        ComponentName componentName = new ComponentName(context,
                WidgetProvider.class);
        AppWidgetManager.getInstance(context).updateAppWidget(
                componentName, remoteViews);
        
        new MyAsyncTask(context).execute();


        super.onReceive(context, intent);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        onUpdate(context, appWidgetManager, new int[]{appWidgetId});
    }
    
    private class MyAsyncTask extends AsyncTask<String, Integer, String>{
        Context ctx;

        public MyAsyncTask(Context context) {
            ctx = context;
        }
        
        @Override
        protected String doInBackground(String... params) {
              return runRequest(ctx);
        }
       
        protected void onPostExecute(String result){
            Intent intent2 = new Intent(ctx, WidgetProvider.class);
            intent2.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, 0, intent2, 0);

            RemoteViews views = new RemoteViews(ctx.getPackageName(),
                    R.layout.widget_layout);
            
            views.setTextViewText(R.id.pointsView, result);
            views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);

            ComponentName componentName = new ComponentName(ctx,
                    WidgetProvider.class);
            AppWidgetManager.getInstance(ctx).updateAppWidget(
                    componentName, views);
        }
    }
    
    private String runRequest(Context ctx) {
        settings = PreferenceManager.getDefaultSharedPreferences(ctx);

        String cardNumber = settings.getString("card_number_preference", "");
        String type = settings.getString("type_preference", "available");
        String whenError = settings.getString("get_error_preference", "last");
        String lastValue = settings.getString("lastValue", "");

        try {
            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url("https://occssl.payback.pl/balance?member-id=" + cardNumber)
                    .build();

            Response response = client.newCall(request).execute();
            String res = response.body().string();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            Log.d("TEST2", res);

            Payback jsonRes = gson.fromJson(res, Payback.class);

            //Payback jsonRes = gson.fromJson(res, Payback.class);

            String points = "";

            if(type.equals("available")){
                points = jsonRes.availablePointsAmount;
            }else if(type.equals("total")){
                points = jsonRes.totalPointsAmount;
            }

            settings.edit().putString("lastValue", points).commit();
            
            return points;
        }  catch (IOException e) {
            e.printStackTrace();
        } catch (JsonSyntaxException e){
            e.printStackTrace();
        }
        
        if(whenError.contains("last")) {
            return lastValue;
        }
        
        return ctx.getString(R.string.error);
    }
}
