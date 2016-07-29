package com.sam_chordas.android.stockhawk.ui;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.HistoricalColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import java.util.ArrayList;

/**
 * Created by Warren on 7/23/2016.
 */
public class LineGraphActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    private LineChartView lineChartView;
    private LineSet dataSet;
    private String[] labels = {"January", "February", "March"};
    private float[] values = {53.99f, 48.77f, 63.77f};
    private String stockSymbol;
    private Intent mServiceIntent;
    boolean isConnected;
    Context mContext;
    CursorAdapter mCursorAdapter;
    Cursor mCursor;
    private static int CURSOR_LOADER_ID = 0;
    private final String LOG_TAG = MyStocksActivity.class.getSimpleName();

    String[] cursorParamaters = {HistoricalColumns.SYMBOL, HistoricalColumns.DATE, HistoricalColumns.ADJ_CLOSE};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        Intent intent = getIntent();
        stockSymbol = "%20" + intent.getStringExtra(getString(R.string.intent_stock_symbol));
        setContentView(R.layout.activity_line_graph);
        lineChartView = (LineChartView) findViewById(R.id.linechart);
        updateGraph();

    }

    @Override
    public void onResume(){
        super.onResume();
        getSupportLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
    }

    public void networkToast(){
        Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args){
        return new CursorLoader(mContext,
                QuoteProvider.HistoricalData.CONTENT_URI,
                cursorParamaters,
                HistoricalColumns.SYMBOL + " LIKE ?",
                new String[] {stockSymbol},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        ArrayList<String> date = new ArrayList<>();
        ArrayList<Float> closingPrice = new ArrayList<>();
        mCursor = data;
        int j = 0;


        if (mCursor.moveToFirst()){
            String selectedSymbol = mCursor.getString(0);
            Log.v(LOG_TAG, selectedSymbol);

            for (int i = 0; i < mCursor.getCount(); i++){
                date.add(mCursor.getString(1));
                closingPrice.add(mCursor.getFloat(2));
                mCursor.moveToNext();
            }
            labels = date.toArray(new String[date.size()]);
            values = new float[date.size()];

            for (Float f : closingPrice){
                values[j++] = ( f != null ? f: Float.NaN);
            }

            dataSet = new LineSet(labels, values);
            dataSet.setColor(Color.BLUE)
                    .setSmooth(true);
            lineChartView.addData(dataSet);

            lineChartView.setAxisColor(Color.WHITE)
                    .setLabelsColor(Color.WHITE);

            lineChartView.show();
        }



        getLoaderManager().destroyLoader(CURSOR_LOADER_ID);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        getLoaderManager().destroyLoader(CURSOR_LOADER_ID);
    }

    private void updateGraph() {
        getSupportLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);
    }


}
