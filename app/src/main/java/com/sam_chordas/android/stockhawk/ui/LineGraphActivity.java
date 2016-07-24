package com.sam_chordas.android.stockhawk.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;

/**
 * Created by Warren on 7/23/2016.
 */
public class LineGraphActivity extends AppCompatActivity{

    private LineChartView lineChartView;
    private LineSet dataSet;
    private String[] labels = {"January", "February", "March"};
    private float[] values = {53.99f, 48.77f, 63.77f};
    private String stockSymbol;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        stockSymbol = intent.getStringExtra(getString(R.string.intent_stock_symbol));
        setContentView(R.layout.activity_line_graph);

        lineChartView = (LineChartView) findViewById(R.id.linechart);

        dataSet = new LineSet(labels, values);
        dataSet.setColor(Color.BLUE)
                .setSmooth(true);
        lineChartView.addData(dataSet);

        lineChartView.setAxisColor(Color.WHITE)
                .setLabelsColor(Color.WHITE);

        lineChartView.show();
    }

}
