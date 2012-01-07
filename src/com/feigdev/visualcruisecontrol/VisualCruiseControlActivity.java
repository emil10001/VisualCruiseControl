package com.feigdev.visualcruisecontrol;

import java.util.ArrayList;

import com.feigdev.simplelocations.LocationController;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.Editable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Chronometer.OnChronometerTickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class VisualCruiseControlActivity extends Activity {
	private ArrayList<Location> gpsLocations;
	private ArrayList<Location> netLocations;
	private LocationManager locMan;
	private LocationController locGPS;
	private LocationController locNet;
	private float setSpeed;
	private TextView curSpeed;
	private TextView targetSpeed;
	private TextView gpsCurSpeed;
	private TextView netCurSpeed;
	private TextView gpsAvgSpeed;
	private TextView netAvgSpeed;
	private Button pauseButton;
	private LinearLayout background;
	private boolean pause;
	private Chronometer mChronometer;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        pauseButton = (Button)findViewById(R.id.pauseButton);
        
        targetSpeed = (TextView)findViewById(R.id.targetSpeed);
        gpsCurSpeed = (TextView)findViewById(R.id.gpsCurSpeed);
        netCurSpeed = (TextView)findViewById(R.id.netCurSpeed);
        gpsAvgSpeed = (TextView)findViewById(R.id.gpsAvgSpeed);
        netAvgSpeed = (TextView)findViewById(R.id.netAvgSpeed);
        curSpeed = (TextView)findViewById(R.id.curSpeed);
        background = (LinearLayout)findViewById(R.id.background);
        mChronometer = (Chronometer)findViewById(R.id.chronometer1);
        
        init();
    }
    
    private void init(){
    		setSpeed = (float)0.0;
	        pause = false;
	        setTarget();
	    	locMan = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
	    	locGPS = new LocationController(gpsHandle);
	    	locNet = new LocationController(netHandle);
	    	updateLocManager();
	    	
	    	
	    	mChronometer.setOnChronometerTickListener(new OnChronometerTickListener() {                      
	            @Override
	            public void onChronometerTick(Chronometer chronometer) {
	                long elapsedMillis = SystemClock.elapsedRealtime() - chronometer.getBase();
	                if(pause){
	                	mChronometer.setText(((Double)(((Long)elapsedMillis).floatValue()/1000.0)).toString());
	                }
	                else {
	                	mChronometer.stop();
	                	mChronometer.setBase(SystemClock.elapsedRealtime());
	                	mChronometer.setText("0.00");
	                }
	            }
	    	});
	    	pauseButton.setOnClickListener(new OnClickListener() {
			    @Override
			    public void onClick(View v) {
			    	if (!pause){
			    		pauseButton.setText("un-pause");
						pause = true;
						locMan.removeUpdates(locGPS);
						locMan.removeUpdates(locNet);
						timerStart();
					}
					else {
						pauseButton.setText("pause");
						pause = false;
						timerStop();
						updateLocManager();
					}
			    }
			  });
	    	
    	
    }
    
    /***
     * This method is good for changing the gps check time based on the statusCheckTime variable
     */
	protected void updateLocManager() {
		locMan.removeUpdates(locGPS);
		locMan.removeUpdates(locNet);
		if (locGPS.isEnabled()){
			locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locGPS);
		}
		if (locNet.isEnabled()){
			locMan.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locNet);
		}
	}
	
	private void updateLocations(){
		gpsLocations = locGPS.getLocations();
		float avgGpsSpeed = 0;
		float avgNetSpeed = 0;
		float gpsSpeed = 0;
		float netSpeed = 0;
		for (int i = 0 ; i < gpsLocations.size(); i++){
			avgGpsSpeed += gpsLocations.get(i).getSpeed();
			gpsSpeed = gpsLocations.get(i).getSpeed();
		}
		avgGpsSpeed = (float) ((avgGpsSpeed / gpsLocations.size()) * 2.23693629);
		
		netLocations = locNet.getLocations();
		for (int i=0; i < netLocations.size(); i++){
			avgNetSpeed += netLocations.get(i).getSpeed();
			netSpeed = netLocations.get(i).getSpeed();
		}
		avgNetSpeed = (float) ((avgNetSpeed / netLocations.size()) * 2.23693629);
		
		gpsSpeed = (float)(gpsSpeed * 2.23693629);
		netSpeed = (float)(netSpeed * 2.23693629);
		
		gpsCurSpeed.setText(((Float)gpsSpeed).toString() + " mph");
		curSpeed.setText(((Float)gpsSpeed).toString() + " mph");
		netCurSpeed.setText(((Float)netSpeed).toString() + " mph");
		gpsAvgSpeed.setText(((Float)avgGpsSpeed).toString() + " mph");
		netAvgSpeed.setText(((Float)avgNetSpeed).toString() + " mph");
		
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
	
	/***
	 * Handlers are the backbone of this asynchronous app
	 * 
	 * They allow for messages to be received from other classes
	 * 
	 * This particular handler is the heart of this app. It routes messages from all over to where they need to go.
	 */
    public Handler gpsHandle = new Handler() {
    	Intent myIntent;
        /* (non-Javadoc)
         * @see android.os.Handler#handleMessage(android.os.Message)
         */
        @Override
        public void handleMessage(Message msg) {
        	switch (msg.what) {
        	// Stop the splash-screen splash
        	// Determines what to do next
            case LocationController.UPDATE:
            	updateLocations();
            	break;
            case LocationController.STATUS:
            	break;
            case LocationController.AVAILABLE:
            	break;
            case LocationController.UNAVAILABLE:
            	break;
        	}
        }
    };
    
	/***
	 * Handlers are the backbone of this asynchronous app
	 * 
	 * They allow for messages to be received from other classes
	 * 
	 * This particular handler is the heart of this app. It routes messages from all over to where they need to go.
	 */
    public Handler netHandle = new Handler() {
    	Intent myIntent;
        /* (non-Javadoc)
         * @see android.os.Handler#handleMessage(android.os.Message)
         */
        @Override
        public void handleMessage(Message msg) {
        	switch (msg.what) {
        	// Stop the splash-screen splash
        	// Determines what to do next
            case LocationController.UPDATE:
            	updateLocations();
            	break;
            case LocationController.STATUS:
            	break;
            case LocationController.AVAILABLE:
            	break;
            case LocationController.UNAVAILABLE:
            	break;
        	}
        }
    };
    
    
	private void setTarget(){
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Set target speed");

		final EditText input = new EditText(this);
		input.setText("0.0");
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			  Editable value = input.getText();
			  setSpeed = new Float(value.toString());
			  targetSpeed.setText(((Float)setSpeed).toString());
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
    	mChronometer.setText("0.00");
	}
	
//	@Override
//	public boolean onTouch(View v, MotionEvent event) {
//		// TODO Auto-generated method stub
//		if (!pause){
//			pause = true;
//			locMan.removeUpdates(locGPS);
//			locMan.removeUpdates(locNet);
//			timerStart();
//		}
//		else {
//			pause = false;
//			timerStop();
//			updateLocManager();
//		}
//		
//		return true;
//	}
	
	//Chronometer code lifted from stackexchange
	// http://stackoverflow.com/questions/526524/android-get-time-of-chronometer-widget
	
	
}