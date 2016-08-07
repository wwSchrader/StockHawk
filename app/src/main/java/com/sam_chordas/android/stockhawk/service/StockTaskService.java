package com.sam_chordas.android.stockhawk.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService{
  private String LOG_TAG = StockTaskService.class.getSimpleName();

  private OkHttpClient client = new OkHttpClient();
  private Context mContext;
  private StringBuilder mStoredSymbols = new StringBuilder();
  private boolean isUpdate;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STOCK_STATUS_OK, STOCK_STATUS_SERVER_DOWN, STOCK_STATUS_SERVER_INVALID, STOCK_STATUS_UNKNOWN})
    public @interface StockStatus {}

    public static final int STOCK_STATUS_OK = 0;
    public static final int STOCK_STATUS_SERVER_DOWN = 1;
    public static final int STOCK_STATUS_SERVER_INVALID = 2;
    public static final int STOCK_STATUS_UNKNOWN = 3;

    public static final int COL_INDEX_STOCK_SYMBOL = 0;

    public static final int DAYS_IN_YEAR = 365;

  public static final String ACTION_DATA_UPDATED =
          "com.sam_chordas.android.stockhawk.app.ACTION_DATA_UPDATED";

  private static String dateFormatString= "yyyy-MM-dd";

  private String startingStockList = "\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")";

  public StockTaskService(){}

  public StockTaskService(Context context){
    mContext = context;
  }
  String fetchData(String url) throws IOException{
    Request request = new Request.Builder()
        .url(url)
        .build();

    Response response = client.newCall(request).execute();
    return response.body().string();
  }

  @Override
  public int onRunTask(TaskParams params){
    int result;
    if (mContext == null){
      mContext = this;
    }

    switch (params.getTag()){
      case "init": case "periodic": case "add":
        result = updateStockList(params);
        result = addHistoricalData(params);
        break;
      case "historical":
        result = addHistoricalData(params);
        break;
      default:
        result = GcmNetworkManager.RESULT_FAILURE;
      }
    updateWidgets();
    return result;
    }

  private void updateWidgets() {
    Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED)
            .setPackage(mContext.getPackageName());
    mContext.sendBroadcast(dataUpdatedIntent);
  }

  private int addHistoricalData(TaskParams params) {
    Cursor initQueryCursor;
    int result = GcmNetworkManager.RESULT_FAILURE;

    mContext.getContentResolver().delete(QuoteProvider.HistoricalData.CONTENT_URI, null, null);
    SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatString);
    Calendar cal = Calendar.getInstance();
    String todaysDate = dateFormat.format(cal.getTime());
    cal.add(Calendar.DATE, DAYS_IN_YEAR * -1);
    String dateOneYearAgo = dateFormat.format(cal.getTime());

    initQueryCursor = mContext.getContentResolver().query(
            QuoteProvider.Quotes.CONTENT_URI,
            new String[] { "Distinct " + QuoteColumns.SYMBOL },
            null,
            null,
            null);
    if (initQueryCursor != null) {
      initQueryCursor.moveToFirst();

      for (int i = 0; i < initQueryCursor.getCount(); i++){
        result = grabHistoricalJsonString(initQueryCursor.getString(COL_INDEX_STOCK_SYMBOL), dateOneYearAgo, todaysDate);
        initQueryCursor.moveToNext();
      }

      initQueryCursor.close();
    }


    return result;
  }


  private int grabHistoricalJsonString(String stockSymbol, String dateOneYearAgo, String todaysDate){

    StringBuilder urlStringBuilder = new StringBuilder();
    try{

      // Base URL for the Yahoo query getting historical data for the past year
      urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=" +
              URLEncoder.encode("select * from yahoo.finance.historicaldata where symbol = \" "
              + stockSymbol + "\" and startDate = \"" +
              dateOneYearAgo + "\" and endDate = \"" + todaysDate + "\"", "UTF-8"));


      // finalize the URL for the API query.
      urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
              + "org%2Falltableswithkeys&callback=");



    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    String urlString;
    String getResponse;
    int result = GcmNetworkManager.RESULT_FAILURE;

    if (urlStringBuilder != null){
      urlString = urlStringBuilder.toString();
      try{
        getResponse = fetchData(urlString);
        result = GcmNetworkManager.RESULT_SUCCESS;
        try {
          // update ISCURRENT to 0 (false) so new data is current
          mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                    Utils.historicalJsonToContentVals(getResponse));
        }catch (RemoteException | OperationApplicationException e){
          Log.e(LOG_TAG, "Error applying batch insert", e);
          //invalid data from server
          setStockStatus(mContext, STOCK_STATUS_SERVER_INVALID);
        }
      } catch (IOException e){
        e.printStackTrace();
        //no data collected from server
        setStockStatus(mContext, STOCK_STATUS_SERVER_DOWN);
      }
    }
    //data was retrieved successfully
    setStockStatus(mContext, STOCK_STATUS_OK);
    return result;
  }

  private int updateStockList(TaskParams params){
    Cursor initQueryCursor;
    StringBuilder urlStringBuilder = new StringBuilder();
    try{
      // Base URL for the Yahoo query
      urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
      urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
              + "in (", "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    if (params.getTag().equals("init") || params.getTag().equals("periodic")){
      isUpdate = true;
      initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
              new String[] { "Distinct " + QuoteColumns.SYMBOL }, null,
              null, null);
      if (initQueryCursor.getCount() == 0 || initQueryCursor == null){
        // Init task. Populates DB with quotes for the symbols seen below
        try {
          urlStringBuilder.append(
                  URLEncoder.encode(startingStockList, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      } else if (initQueryCursor != null){
        DatabaseUtils.dumpCursor(initQueryCursor);
        initQueryCursor.moveToFirst();
        for (int i = 0; i < initQueryCursor.getCount(); i++){
          mStoredSymbols.append("\""+
                  initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol"))+"\",");
          initQueryCursor.moveToNext();
        }
        mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
        try {
          urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      }
    } else if (params.getTag().equals("add")){
      isUpdate = false;
      // get symbol from params.getExtra and build query
      String stockInput = params.getExtras().getString("symbol");
      try {
        urlStringBuilder.append(URLEncoder.encode("\""+stockInput+"\")", "UTF-8"));
      } catch (UnsupportedEncodingException e){
        e.printStackTrace();
      }
    }
    // finalize the URL for the API query.
    urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
            + "org%2Falltableswithkeys&callback=");

    String urlString;
    String getResponse;
    int result = GcmNetworkManager.RESULT_FAILURE;

    if (urlStringBuilder != null){
      urlString = urlStringBuilder.toString();
      try{
        getResponse = fetchData(urlString);
        result = GcmNetworkManager.RESULT_SUCCESS;
        try {
          ContentValues contentValues = new ContentValues();
          // update ISCURRENT to 0 (false) so new data is current
          if (isUpdate){
            contentValues.put(QuoteColumns.ISCURRENT, 0);
            mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                    null, null);
          }
          if (Utils.checkForInvalidStocks(getResponse)){
            Handler mHandler = new Handler(mContext.getMainLooper());
            mHandler.post(new Runnable() {
              @Override
              public void run() {
                Toast.makeText(mContext, getString(R.string.invalid_stock), Toast.LENGTH_SHORT).show();
              }
            });
          } else {

            mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                    Utils.quoteJsonToContentVals(getResponse));
          }
          //data was retrieved successfully
          setStockStatus(mContext, STOCK_STATUS_OK);

        }catch (RemoteException | OperationApplicationException e ){
          Log.e(LOG_TAG, "Error applying batch insert", e);
          //invalid data from server
          setStockStatus(mContext, STOCK_STATUS_SERVER_INVALID);
        } catch (NumberFormatException e){
          Log.e(LOG_TAG, "Possible null input received from a critical value: ", e);
          //invalid data from server
          setStockStatus(mContext, STOCK_STATUS_SERVER_INVALID);
        }
      } catch (IOException e){
        e.printStackTrace();
        //no data collected from server
        setStockStatus(mContext, STOCK_STATUS_SERVER_DOWN);
      }
    }

    return result;
  }

  //sets location status into shared preference
  static private void setStockStatus(Context c, @StockStatus int locationStatus){
      SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
      SharedPreferences.Editor spe = sp.edit();
      spe.putInt(c.getString(R.string.pref_stock_status_key), locationStatus);
      spe.commit();
  }

}
