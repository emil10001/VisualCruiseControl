package com.feigdev.visualcruisecontrol;

import java.text.DecimalFormat;
import java.util.ArrayList;

import com.feigdev.simplelocations.LocationController;
import com.feigdev.simplelocations.LocationUpdateListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputFilter;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Chronometer.OnChronometerTickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class VisualCruiseControlActivity extends Activity implements LocationUpdateListener {
	private ArrayList<Location> gpsLocations;
	private LocationManager locMan;
	private LocationController locGPS;
	private double setSpeed;
	private TextView curSpeed;
	private TextView targetSpeed;
	private TextView gpsCurSpeed;
	private TextView gpsAvgSpeed;
	private TextView bearing;
	private Button pauseButton;
	private LinearLayout background;
	private boolean pause;
	private Chronometer mChronometer;
	private static final double CONVERSION_FACTOR = 2.23693629;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        pauseButton = (Button)findViewById(R.id.pauseButton);
        
        targetSpeed = (TextView)findViewById(R.id.targetSpeed);
        gpsCurSpeed = (TextView)findViewById(R.id.gpsCurSpeed);
        gpsAvgSpeed = (TextView)findViewById(R.id.gpsAvgSpeed);
        curSpeed = (TextView)findViewById(R.id.curSpeed);
        bearing = (TextView)findViewById(R.id.Bearing);
        background = (LinearLayout)findViewById(R.id.background);
        mChronometer = (Chronometer)findViewById(R.id.chronometer1);
        getWindow().addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );
        
        init();
    }
    
    private Handler handler = new Handler();
    
    @Override 
    public void onResume(){
    	super.onResume();
    	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }
    
    private void init(){
    		setSpeed = (double)0.0;
	        pause = true;
	        setTarget("");
	    	locMan = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
	    	locGPS = new LocationController(this);
	    	timerStart();
	    	
	    	mChronometer.setOnChronometerTickListener(new OnChronometerTickListener() {                      
	            @Override
	            public void onChronometerTick(Chronometer chronometer) {
	                long elapsedMillis = SystemClock.elapsedRealtime() - chronometer.getBase();
	                if(pause){
	                	mChronometer.setText(String.format("%.1f s", (elapsedMillis/1000.0)));
	                }
	                else {
	                	mChronometer.stop();
	                	mChronometer.setBase(SystemClock.elapsedRealtime());
	                	mChronometer.setText("0.0 s");
	                }
	            }
	    	});
	    	pauseButton.setOnClickListener(new OnClickListener() {
			    @Override
			    public void onClick(View v) {
			    	if (!pause){
			    		pauseButton.setText("Start");
						pause = true;
						locMan.removeUpdates(locGPS);
						timerStart();
					}
					else {
						pauseButton.setText("Stop");
						pause = false;
						timerStop();
						updateLocManager();
					}
			    }
			  });
	    	
    	
    }
    
    @Override
    public void onDestroy(){
    	locMan.removeUpdates(locGPS);
		super.onDestroy();
    }
    
    /***
     * This method is good for changing the gps check time based on the statusCheckTime variable
     */
	protected void updateLocManager() {
		locMan.removeUpdates(locGPS);
		if (locGPS.isEnabled()){
			locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locGPS);
		}
	}
	
	private void setTarget(String msg){
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Set target speed in mph");
		alert.setMessage(msg);
		final EditText input = new EditText(this);
		
		input.setFilters(new InputFilter[] {
		    DigitsKeyListener.getInstance(false,true), 
		});

		// Digits only & use numeric soft-keyboard.
		input.setKeyListener(DigitsKeyListener.getInstance(false,true));
		
		input.setText("0.0");
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			  Editable value = input.getText();
			  try {
				  setSpeed = Double.parseDouble(value.toString());
				  targetSpeed.setText(((Double)setSpeed).toString() + " mph");
			  }
			  catch (NumberFormatException ex){
				  handler.post(new Runnable(){
					  @Override
					  public void run() {
						  setTarget("Please enter a numeric value.");
					  }
				  });
			  }
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
			  finish();
		  }
		});
		
		alert.show();
	}

	private void timerStart(){
		mChronometer.setBase(SystemClock.elapsedRealtime());
		mChronometer.start();
	}
	
	private void timerStop(){
		mChronometer.stop();
    	mChronometer.setText("0.0");
	}

	/***
	 * Using this to ensure that I am not overflowing any values. 
	 * Checked the maximum result by multiplying two of the maximum double 
	 * shifted longs together and it is much less than the size of a long. 
	 * This will not work in the general case, however it is sufficient for my needs.
	 * 
	 * @param value
	 * @return
	 */
	private long doubleToShiftedLong(double value){
		value = value * 10000.0;
		return Long.valueOf(new DecimalFormat("##########").format(value));
	}
	
	private double shiftBack(long value){
		return (double)(value / 10000.0);
	}
	
	private double doubleShiftBack(long value){
		return (double)((value / 10000.0) / 10000.0);
	}
	
	@Override
	public void onLocUpdate() {
		if (null == locGPS.getLocations()){
			return;
		}
		if (0 == locGPS.getLocations().size()){
			return;
		}
		gpsLocations = locGPS.getLocations();
		
		double avgGpsSpeed = 0;
		double gpsSpeed = 0;
		String bear = "";
		double doubleBear = 0;
		int validSpeedCounter = 0;
		
		int gpsLocsSize = gpsLocations.size();
		Location lastGpsLoc = gpsLocations.get(gpsLocsSize - 1);
		
		for (int i = 0 ; i < gpsLocsSize; i++){
			if (null != gpsLocations.get(i)){
				if (gpsLocations.get(i).hasSpeed()){
					validSpeedCounter++;
					avgGpsSpeed += gpsLocations.get(i).getSpeed();
				}
			}
		}
		
		avgGpsSpeed = doubleShiftBack((doubleToShiftedLong(avgGpsSpeed) / (long)(validSpeedCounter)) * doubleToShiftedLong(CONVERSION_FACTOR));
		
		if (lastGpsLoc.hasSpeed()){
			gpsSpeed = doubleShiftBack(doubleToShiftedLong(lastGpsLoc.getSpeed()) * doubleToShiftedLong(CONVERSION_FACTOR));
		}
		
		if (lastGpsLoc.hasBearing()){
			doubleBear = lastGpsLoc.getBearing() % 360;
			if ((doubleBear > 0.0 && doubleBear <= 22.5) || (doubleBear > 337.5 && doubleBear <= 360.0 )){
				bear = "N";
			}
			else if (doubleBear > 22.5 && doubleBear <= 67.5 ){
				bear = "NE";
			}
			else if (doubleBear > 67.5 && doubleBear <= 112.5 ){
				bear = "E";
			}
			else if (doubleBear > 112.5 && doubleBear <= 157.5 ){
				bear = "SE";
			}
			else if (doubleBear > 157.5 && doubleBear <= 202.5 ){
				bear = "S";
			}
			else if (doubleBear > 202.5 && doubleBear <= 247.5 ){
				bear = "SW";
			}
			else if (doubleBear > 247.5 && doubleBear <= 292.5 ){
				bear = "W";
			}
			else if (doubleBear > 292.5 && doubleBear <= 337.5 ){
				bear = "NW";
			}
		}
		
		gpsCurSpeed.setText(new DecimalFormat("###.#").format(gpsSpeed) + " mph");
		curSpeed.setText(new DecimalFormat("###.#").format(gpsSpeed) + " mph");
		gpsAvgSpeed.setText(new DecimalFormat("###.#").format(avgGpsSpeed) + " mph");
		bearing.setText(bear);
		if (avgGpsSpeed < setSpeed){
			background.setBackgroundColor(Color.GREEN);
		}
		else if (avgGpsSpeed > setSpeed){
			background.setBackgroundColor(Color.RED);
		}
		else {
			background.setBackgroundColor(Color.DKGRAY);
		}
	}

	@Override
	public void onStatusUpdate() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAvailable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUnAvailable() {
		// TODO Auto-generated method stub
		
	}
	
	//Chronometer code lifted from stackexchange
	// http://stackoverflow.com/questions/526524/android-get-time-of-chronometer-widget
	
	
}