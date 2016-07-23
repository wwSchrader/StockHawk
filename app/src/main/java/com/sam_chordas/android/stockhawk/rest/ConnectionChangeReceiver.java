package com.sam_chordas.android.stockhawk.rest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import com.sam_chordas.android.stockhawk.R;

/**
 * Created by Warren on 7/23/2016.
 */
public class ConnectionChangeReceiver extends BroadcastReceiver{

    //to detect network status changes and update sharedpreferences

    @Override
    public void onReceive(Context context, Intent intent){
       if(intent.getExtras() != null){
           NetworkInfo networkInfo = (NetworkInfo) intent.getExtras()
                   .get(ConnectivityManager.EXTRA_NETWORK_INFO);
           if (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED){
               //if network is connected
               SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
               SharedPreferences.Editor spe = sp.edit();
               spe.putInt(context.getString(R.string.pref_network_status_key), 1);
               spe.commit();
           } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)){
               //if network is disconnected
               SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
               SharedPreferences.Editor spe = sp.edit();
               spe.putInt(context.getString(R.string.pref_network_status_key), 0);
               spe.commit();
           }
       }
    }



}
