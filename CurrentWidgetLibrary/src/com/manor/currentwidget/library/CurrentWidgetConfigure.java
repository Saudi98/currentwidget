/*
 *  Copyright (c) 2010-2011 Ran Manor
 *  
 *  This file is part of CurrentWidget.
 *    
 * 	CurrentWidget is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CurrentWidget is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CurrentWidget.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.manor.currentwidget.library;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.chart.TimeChart;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.KeyEvent;
import android.widget.Toast;

public class CurrentWidgetConfigure extends PreferenceActivity {

	int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;	
	
	public final static String SHARED_PREFS_NAME = "currentWidgetPrefs";
	
	public CurrentWidgetConfigure() {
		super();
		
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
	
		getPreferenceManager().setSharedPreferencesName(SHARED_PREFS_NAME);
		
		addPreferencesFromResource(R.xml.prefs);

		// get widget id
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {
			mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		}

		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);

		// init result as ok
		setResult(RESULT_OK, resultValue);
		
		if (this.getApplicationContext().getPackageName().equals("com.manor.currentwidgetpaid")) {
			findPreference("donate").setTitle("Thank you for donating!");
		}
	
		
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				Intent updateIntent = new Intent(this.getApplicationContext(), CurrentWidget.class);
				updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
				updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { mAppWidgetId } );
				updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
				updateIntent.setData(Uri.withAppendedPath(Uri.parse("droidrm://widget/id/"), String.valueOf(mAppWidgetId)));
				
				sendBroadcast(updateIntent);
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private void startGraphActivity() {
		
		SharedPreferences settings = getSharedPreferences(SHARED_PREFS_NAME, 0);
		
		//Intent i = new Intent(getApplicationContext(), GraphActivity.class);
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		TimeSeries series = new TimeSeries("Electric Current");
		
		FileInputStream logFile = null;
		
		try {
			logFile = new FileInputStream(settings.getString(getApplicationContext().getString(R.string.pref_log_filename_key), "/sdcard/currentwidget.log"));
			DataInputStream ds = new DataInputStream(logFile);
			
			String line = null;
			int x = 0;			
			while ( ( line = ds.readLine() ) != null ) {
				
				// 0 is datetime , 1 is value, 3 all the rest
				String[] tokens = line.split(",", 3);
				
				// add to graph series
				//tokens[1]	
				//Log.d("CurrentWidget", line);
				if (tokens.length > 1) {
					try {
						
						series.add(new Date(tokens[0]), Double.parseDouble(tokens[1].substring(0, tokens[1].length() - 2)));
						
						x = x + 1;
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}
				}					
				       
			}		
			
			ds.close();
			logFile.close();			
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		dataset.addSeries(series);
		
	    XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
	    
	    XYSeriesRenderer r = new XYSeriesRenderer();
	    r.setColor(Color.BLUE);
	    r.setPointStyle(PointStyle.SQUARE);
	    r.setFillBelowLine(true);
	    r.setFillBelowLineColor(Color.WHITE);
	    r.setFillPoints(true);
	    renderer.addSeriesRenderer(r);
	    
	    /*r = new XYSeriesRenderer();
	    r.setPointStyle(PointStyle.CIRCLE);
	    r.setColor(Color.GREEN);
	    r.setFillPoints(true);
	    renderer.addSeriesRenderer(r);*/
	    
	    renderer.setAxesColor(Color.DKGRAY);
	    renderer.setLabelsColor(Color.LTGRAY);

		
		Intent i = ChartFactory.getTimeChartIntent(this.getApplicationContext(), dataset, renderer, null);
		startActivity(i);

	}
	
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		
		if (preference.getKey().equals("view_log")) {
			String logFilename = getPreferenceManager().getSharedPreferences().getString(getString(R.string.pref_log_filename_key), "/sdcard/currentwidget.log");
			File logFile = new File(logFilename);
			if (logFile.exists()) {
				Intent viewLogIntent = new Intent(Intent.ACTION_VIEW);					
				viewLogIntent.setDataAndType(Uri.fromFile(logFile), "text/plain");
				startActivity(Intent.createChooser(viewLogIntent, "CurrentWidget"));
			}
			else {
				new AlertDialog.Builder(this).setMessage("Log file not found").setPositiveButton("OK", null).show();						
			}
			
			return true;

		} else if (preference.getKey().equals("donate")) {
			
			try {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.manor.currentwidgetpaid"));						
				startActivity(intent);
			}
			catch (Exception ex) {
				ex.printStackTrace();
				// market not installed, send to browser
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://market.android.com/details?id=com.manor.currentwidgetpaid"));						
				startActivity(intent);
			}			
			return true;
		} else if (preference.getKey().equals("view_graph")) {
			
			SharedPreferences settings = getSharedPreferences(SHARED_PREFS_NAME, 0);
			File f = new File(settings.getString(getApplicationContext().getString(R.string.pref_log_filename_key), "/sdcard/currentwidget.log"));
			
			if (f.exists()) {
				startGraphActivity();
			}
			else {
				new AlertDialog.Builder(this).setMessage("Log file not found").setPositiveButton("OK", null).show();
			}
				
			
			return true;
			
		} else if (preference.getKey().equals("clear_log")) {

			SharedPreferences settings = getSharedPreferences(SHARED_PREFS_NAME, 0);
			File f = new File(settings.getString(getApplicationContext().getString(R.string.pref_log_filename_key), "/sdcard/currentwidget.log"));
			Toast t = null;
			if (f.exists()) {
				if (f.delete())
					t = Toast.makeText(getApplicationContext(), "Log file deleted", Toast.LENGTH_LONG);
				else
					t = Toast.makeText(getApplicationContext(), "Error deleting log file", Toast.LENGTH_LONG);
			}
			else {
				t = Toast.makeText(getApplicationContext(), "No log file", Toast.LENGTH_LONG);
			}
			
			t.show();
			
			return true;
		}
		
		return false;
	};
	
}