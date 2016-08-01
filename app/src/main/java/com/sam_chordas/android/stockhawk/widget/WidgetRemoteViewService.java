package com.sam_chordas.android.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteDatabase;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

/**
 * Created by Warren on 7/31/2016.
 */
public class WidgetRemoteViewService extends RemoteViewsService {
    private final static String LOG_TAG = WidgetRemoteViewService.class.getSimpleName();

    private static final String[] QUOTE_COLUMNS = {
            QuoteDatabase.QUOTES + "." + QuoteColumns._ID,
            QuoteColumns.SYMBOL,
            QuoteColumns.BIDPRICE,
            QuoteColumns.PERCENT_CHANGE,
            QuoteColumns.CHANGE,
            QuoteColumns.ISUP
    };

    private static final int INDEX_QUOTE_ID = 0;
    private static final int INDEX_SYMBOL = 1;
    private static final int INDEX_BIDPRICE = 2;
    private static final int INDEX_PERCENT_CHANGE = 3;
    private static final int INDEX_CHANGE = 4;
    private static final int INDEX_ISUP = 5;
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {

            }

            @Override
            public void onDataSetChanged() {
                if (data != null){
                    data.close();
                }

                final long identityToken = Binder.clearCallingIdentity();
                data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                        QUOTE_COLUMNS,
                        QuoteColumns.ISCURRENT + " = ?",
                        new String[]{"1"},
                        null);

            }

            @Override
            public void onDestroy() {
                if (data != null){
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int i) {
                if (i == AdapterView.INVALID_POSITION || data == null || !data.moveToPosition(i)){
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_list_item);

                String symbol = data.getString(INDEX_SYMBOL);
                String bidPrice = data.getString(INDEX_BIDPRICE);
                String change = data.getString(INDEX_CHANGE);

                Log.v(LOG_TAG, "widget symbol: " + symbol);
                Log.v(LOG_TAG, "widget bid price: " + bidPrice);
                Log.v(LOG_TAG, "widget change: " + change);

                views.setTextViewText(R.id.widget_stock_symbol, symbol);
                views.setTextViewText(R.id.widget_bid_price, bidPrice);
                views.setTextViewText(R.id.widget_change, change);

                final Intent fillInIntent = new Intent();
                fillInIntent.setData(QuoteProvider.Quotes.withSymbol(data.getString(INDEX_SYMBOL)));
                views.setOnClickFillInIntent(R.id.widget_list, fillInIntent);
                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int i) {
                if (data.moveToPosition(i))
                    return data.getLong(INDEX_QUOTE_ID);
                return i;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
