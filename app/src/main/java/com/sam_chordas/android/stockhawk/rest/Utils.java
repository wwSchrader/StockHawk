package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.HistoricalColumns;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.service.StockTaskService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

  private static String LOG_TAG = Utils.class.getSimpleName();

  public static boolean showPercent = true;

  public static String queryJsonString = "query";
  public static String countJsonString = "count";
  public static String resultsJsonString = "results";
  public static String quoteJsonString = "quote";
  public static String nameJsonString = "Name";
  public static String changeJsonString = "Change";
  public static String symbolJsonString = "symbol";
  public static String upperCaseSymbolJsonString = "Symbol";
  public static String bidJsonString = "Bid";
  public static String changeInPercentJsonString = "ChangeinPercent";
  public static String dateJsonString = "Date";
  public static String openJsonString = "Open";
  public static String highJsonString = "High";
  public static String lowJsonString = "Low";
  public static String closeJsonString = "Close";
  public static String volumeJsonString = "Volume";
  public static String adjCloseJsonString = "Adj_Close";

  public static ArrayList quoteJsonToContentVals(String JSON){
    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    JSONObject jsonObject = null;
    JSONArray resultsArray = null;
    try{
      jsonObject = new JSONObject(JSON);
      if (jsonObject != null && jsonObject.length() != 0){
        jsonObject = jsonObject.getJSONObject(queryJsonString);
        int count = Integer.parseInt(jsonObject.getString(countJsonString));
        if (count == 1){
          jsonObject = jsonObject.getJSONObject(resultsJsonString)
              .getJSONObject(quoteJsonString);
          batchOperations.add(buildBatchOperation(jsonObject));
        } else{
          resultsArray = jsonObject.getJSONObject(resultsJsonString).getJSONArray(quoteJsonString);

          if (resultsArray != null && resultsArray.length() != 0){
            for (int i = 0; i < resultsArray.length(); i++){
              jsonObject = resultsArray.getJSONObject(i);
              batchOperations.add(buildBatchOperation(jsonObject));
            }
          }
        }
      }
    } catch (JSONException e){
      Log.e(LOG_TAG, "quoteJsonToContentVals String to JSON failed: " + e);
    }
    return batchOperations;
  }

  public static ArrayList historicalJsonToContentVals(String JSON){
    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    JSONObject jsonObject = null;
    JSONArray resultsArray = null;
    try{
      jsonObject = new JSONObject(JSON);
      if (jsonObject != null && jsonObject.length() != 0){
        jsonObject = jsonObject.getJSONObject(queryJsonString);
        int count = Integer.parseInt(jsonObject.getString(countJsonString));
        if (count == 1){
          jsonObject = jsonObject.getJSONObject(resultsJsonString)
                  .getJSONObject(quoteJsonString);
          batchOperations.add(buildBatchOperationHistorical(jsonObject));
        } else{
          resultsArray = jsonObject.getJSONObject(resultsJsonString).getJSONArray(quoteJsonString);

          if (resultsArray != null && resultsArray.length() != 0){
            for (int i = resultsArray.length() - 1; i >= 0; i--){
              jsonObject = resultsArray.getJSONObject(i);
              batchOperations.add(buildBatchOperationHistorical(jsonObject));
            }
          }
        }
      }
    } catch (JSONException e){
      Log.e(LOG_TAG, "historicalJsonToContentVals String to JSON failed: " + e);
    }
    return batchOperations;
  }

  public static boolean checkForInvalidStocks(String JSON){
    JSONObject jsonObject = null;
    JSONArray resultsArray = null;
    Boolean invalidStock = false;

    //checks to see if any of the name values in the JSON string is null
    try{
      jsonObject = new JSONObject(JSON);
      if (jsonObject != null && jsonObject.length() != 0){
        jsonObject = jsonObject.getJSONObject(queryJsonString);
        int count = Integer.parseInt(jsonObject.getString(countJsonString));
        if (count == 1){
          jsonObject = jsonObject.getJSONObject(resultsJsonString)
                  .getJSONObject(quoteJsonString);
          if (jsonObject.getString(nameJsonString).matches("null"))
            invalidStock = true;

        } else{
          resultsArray = jsonObject.getJSONObject(resultsJsonString).getJSONArray(quoteJsonString);

          if (resultsArray != null && resultsArray.length() != 0){
            for (int i = 0; i < resultsArray.length(); i++){
              jsonObject = resultsArray.getJSONObject(i);
              if (jsonObject.getString(nameJsonString).matches("null"))
                invalidStock = true;
            }
          }
        }
      }
    } catch (JSONException e){
      Log.e(LOG_TAG, "Error in checking for valid stocks: " + e);
    }
    return invalidStock;
  }

  public static String truncatePrice(String price){
    price = String.format("%.2f", Float.parseFloat(price));
    return price;
  }

  public static String truncateChange(String change, boolean isPercentChange){
    String weight = change.substring(0,1);
    String ampersand = "";
    if (isPercentChange){
      ampersand = change.substring(change.length() - 1, change.length());
      change = change.substring(0, change.length() - 1);
    }
    change = change.substring(1, change.length());
    double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
    change = String.format("%.2f", round);
    StringBuffer changeBuffer = new StringBuffer(change);
    changeBuffer.insert(0, weight);
    changeBuffer.append(ampersand);
    change = changeBuffer.toString();
    return change;
  }


  public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject){
    ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
        QuoteProvider.Quotes.CONTENT_URI);
    try {
      String change = jsonObject.getString(changeJsonString);
      builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString(symbolJsonString));
      builder.withValue(QuoteColumns.BIDPRICE, truncatePrice(jsonObject.getString(bidJsonString)));
      builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
          jsonObject.getString(changeInPercentJsonString), true));
      builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
      builder.withValue(QuoteColumns.ISCURRENT, 1);
      if (change.charAt(0) == '-'){
        builder.withValue(QuoteColumns.ISUP, 0);
      }else{
        builder.withValue(QuoteColumns.ISUP, 1);
      }

    } catch (JSONException e){
      e.printStackTrace();
    }
    return builder.build();
  }



  public static ContentProviderOperation buildBatchOperationHistorical(JSONObject jsonObject){
    ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
            QuoteProvider.HistoricalData.CONTENT_URI);
    try {
      builder.withValue(HistoricalColumns.SYMBOL, jsonObject.getString(upperCaseSymbolJsonString));
      builder.withValue(HistoricalColumns.DATE, jsonObject.getString(dateJsonString));
      builder.withValue(HistoricalColumns.OPEN, jsonObject.getString(openJsonString));
      builder.withValue(HistoricalColumns.HIGH, truncatePrice(jsonObject.getString(highJsonString)));
      builder.withValue(HistoricalColumns.LOW, jsonObject.getString(lowJsonString));
      builder.withValue(HistoricalColumns.CLOSE, truncatePrice(jsonObject.getString(closeJsonString)));
      builder.withValue(HistoricalColumns.VOLUME, jsonObject.getString(volumeJsonString));
      builder.withValue(HistoricalColumns.ADJ_CLOSE, truncatePrice(jsonObject.getString(adjCloseJsonString)));

    } catch (JSONException e){
      e.printStackTrace();
    }
    return builder.build();
  }

  //check network availability. Return true if available
  static public boolean isNetworkAvailable(Context c){
    ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);

    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
  }

  //return stock status int type
  @SuppressWarnings("ResourceType")
  static public @StockTaskService.StockStatus
  int getStockStatus(Context c){
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
    return sp.getInt(c.getString(R.string.pref_stock_status_key), StockTaskService.STOCK_STATUS_UNKNOWN);
  }

}
