package com.myproject;

import java.util.ArrayList;

import com.myproject.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.hardware.*; 
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.*;
import android.util.Log;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
// import AppSettings.*;

public class Acceleration extends Activity 
implements android.content.DialogInterface.OnClickListener
{
	
	//Declare Sensor Manager class object

	private SensorManager mSensorManager;
	
	// Gesture Library and Recognizer
	private GestureLibrary myGestureLibrary = null;
	private GestureRecognizer myGestureRecognizer = null;
	
	private ArrayList<float[]> recordingGestureTrace = null;
	private String recordingGestureIDString = "default";

	//Next get the handle to the Sensor service
	private Gesture currentGesture = null;
	
	private boolean RECORD_GESTURE = false;
	
	private boolean DEBUG = true;
	private boolean VERBOSE = false;
	
	
	// dialog view for entering learning gesture
	
	private AlertDialog learning_dialog = null;
	private View 		learning_dialog_view = null;
	

	final Handler alertHandler = new Handler();
	public String detected_gid = "Unknown";
	final Runnable showAlert = new Runnable() {
		public void run()
		{
			show_alert_box();
		}
	};
	
	
	
	private App.STATES state = App.STATES.STATE_LEARN;
	
	// private MENUITEMS menuitems;

	public void show_alert_box()
	{
		/*
		 * Shows alert box when gesture recognition thread returns
		 * 
		 * also change the statusText back
		 * 
		 * also turn acceleration sensor back on
		 */
		
		// status text
		TextView statusText = (TextView) findViewById(R.id.statusText);
		statusText.setText("Gesture recognition mode");
		Log.d("show_alert_box", "ALLLLLEEEEERRRRRTTTTT");
		// display a dialog 
		new AlertDialog.Builder(this)
			.setMessage("Recognized Gesture: "+this.detected_gid)
			.setPositiveButton("OK", null)
			.show();
		// turn on the acc sensor
		mSensorManager.registerListener(sensorListener,
		    	SensorManager.SENSOR_ACCELEROMETER,
		    	SensorManager.SENSOR_DELAY_GAME);
		    	
	}
	

	@SuppressWarnings("deprecation")
	private final SensorListener sensorListener = new SensorListener(){
		public void onSensorChanged(int sensor, float[] values)
		{

		
		//Retrieve the values from the float array values which contains sensor data
		Float dataX = values[SensorManager.DATA_X];

		Float dataY = values[SensorManager.DATA_Y];

		Float dataZ = values[SensorManager.DATA_Z];
		
	//	Context c = getApplicationContext();

		//Now we got the values and we can use it as we want
		if (VERBOSE)
		{
			Log.i("X - Value, "+dataX,"");

			Log.i("Y - Value, "+dataY,"");

			Log.i("Z - Value, "+dataZ,"");
		}
		TextView tv1  = (TextView)findViewById(R.id.accX);
		TextView tv2  = (TextView)findViewById(R.id.accY);
		TextView tv3  = (TextView)findViewById(R.id.accZ);
		
		tv1.setText(dataX.toString());
		tv2.setText(dataY.toString());
		tv3.setText(dataZ.toString());
		
		if (RECORD_GESTURE)
			{
		
				float[] traceItem = {	dataX.floatValue(),
								dataY.floatValue(),
								dataZ.floatValue()};
				if (recordingGestureTrace != null)
					{
						recordingGestureTrace.add(traceItem);
					}
		
			
			}

		}
	
		public void onAccuracyChanged(int sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}
	};
	 
	private synchronized void startRecordingGesture()
	{
		/*
		 *  initiate recording of gesture
		 *  usually called after onTouchListener, ACTION_DOWN
		 */
		// recordingGesture = new Gesture();
		
		
		if (DEBUG)
		{
			Log.d("MainActivity", "Starting Gesture Recording");
		}
		
		recordingGestureTrace = new ArrayList<float[]>(250);
		
		RECORD_GESTURE = true;
	}
	
	private synchronized void stopRecordingGesture()
	{/*
		 *  stop recording of gesture
		 *  usually called after onTouchListener, ACTION_UP / CANCEL
		 */
		
		TextView  statusText = (TextView) findViewById(R.id.statusText);
		if (DEBUG)
		{
			Log.d("stopRecordingGesture", "Stopping Gesture Recording");
		}
		
		RECORD_GESTURE = false;
		// Object[] gestureTrace = recordingGestureTrace.toArray();
		/*int numItems = recordingGestureTrace.size();
		// malloc LOL
		float [][] traces = new float [numItems][3];
		// "copy" gesture info to traces!!!*/
		// traces = recordingGestureTrace.toArray(traces);
		
		
		// clear the existing trace
		switch (state)
		{
		case STATE_LEARN: 
		// recordingGestureTrace = null;
			// note that the arraylist is being copied
			statusText.setText("Saving gesture to DB...");
			Gesture ng = new Gesture(recordingGestureIDString, new ArrayList<float[]>(recordingGestureTrace));
			// add gesture to library but prepare with recognizer settings first
			myGestureLibrary.addGesture(ng.gestureID, this.myGestureRecognizer.prepare_gesture_for_library(ng), false);
			if (DEBUG) Log.d("stopRecordingGesture", "Recorded Gesture ID " + recordingGestureIDString + " Gesture Trace Length:"+ recordingGestureTrace.size());
			statusText.setText("Press button to train gesture.");
			break;
		case STATE_RECOGNIZE:
			statusText.setText("Recognizing gesture...");
			// stop accelerometer
	    	mSensorManager.unregisterListener(sensorListener);

			
			// save a reference to activity for this context
			Thread t = new Thread()
			{
				public void run()
				{
						Gesture candidate = new Gesture(null, new ArrayList<float[]>(recordingGestureTrace));
						if (DEBUG) Log.d("stopRecordingGesture-recogThread","Attempting Gesture Recognition Trace-Length: " + recordingGestureTrace.size());
						String gid = myGestureRecognizer.recognize_gesture(candidate);
						if (DEBUG) Log.d("stopRecordingGesture-recogThread","===================================== \n" +
								"Recognized Gesture: "+ gid+
								"\n===================================");
						// set gid as currently detected gid
						detected_gid = gid;
						
						// show the alert
						alertHandler.post(showAlert);

				}
			};	
			t.start();	
			if (DEBUG) Log.d("stopRecordingGesture", "STATE_RECOGNIZE --> thread dispatched");
			break;	
		case STATE_LIBRARY:
			break;
		default:
			break;
		}
		recordingGestureTrace.clear();

	}
	
	
				
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        
        // acc sensor update rate
        /*
		TextView statusText = (TextView) findViewById(R.id.statusText);
		statusText.setText("Press button to train gesture");*/
        
        // create database test code
        /*Log.d("main", "will create database");
        GestureLibraryDBAdapter db = new GestureLibraryDBAdapter(getApplicationContext());
        Log.d("main", "created database");
        Log.d("main", "test Gesture"+ db.testGesture());*/
        
        // create gesture library
        
        myGestureLibrary = new GestureLibrary("GESTURES", getApplicationContext());
        myGestureRecognizer = new gesturerec3d(myGestureLibrary,50);
        
        // points to the XML file specifying the content
        setContentView(R.layout.main);
        
        
    	mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
    	
    	mSensorManager.registerListener(sensorListener,
    	    	SensorManager.SENSOR_ACCELEROMETER,
    	    	SensorManager.SENSOR_DELAY_FASTEST
    	    	/*SensorManager.SENSOR_DELAY_GAME*/);
    	    
    	
    	final Button mainButton = (Button)findViewById(R.id.Button01);
    	
    	
    	mainButton.setOnTouchListener(new View.OnTouchListener()
    
    	
    	{ 
    		public  boolean onTouch(View v, MotionEvent event)
    		{
    			
    			if (event.getAction() == MotionEvent.ACTION_DOWN)
    			{
    				// button pressed, start recording the trace!
    				Log.d("OnTouch","Down!");
    				startRecordingGesture();
    			}
    			else if (event.getAction() == MotionEvent.ACTION_UP) 
    					
    			{
    				Log.d("OnTouch","Up!");
    				stopRecordingGesture();
    			}
    			else if (event.getAction() == MotionEvent.ACTION_CANCEL)
    			{
    				Log.d("OnTouch","Cancel!");
    				stopRecordingGesture();
    			}
    			
    			if (VERBOSE)
    			Log.d("Ontouch", "Touched:"+event.getX()+" "+event.getY()+" "+event.getPressure()+" "+event.getAction());
    			
    			
    			
    			return false;
    		}
    	}
    	
    	
    	
    	
    	);
    	
    	
    	
    	
    	
    	
    	mainButton.setOnClickListener(new View.OnClickListener()
    	
    			{
    				public void onClick(View v)
    				{
    					// clicked
    					//Log.d("onClick", "Clicked");
    					/*if (mainButton.isFocused())
    					{
    						Log.d("onClick", "InFocus");
    					}
    					else
    					{
    						Log.d("onClic", "NotInFocus");
    					}*/
    					
    				}
    		
    		
    			}
    	
    	
    	
    	);
    	
    	
    	
    	
    
    	
    	

    }
    
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	/*
    	 * Create Activity Menu Items
    	 */
    	//  super.onCreateOptionsMenu(menu);
    	
    	
    	
    	menu.add(0, // group
    			 App.MENUITEMS.ITEM_LEARN.ordinal(), // item id
    			 0, // order id
    			 "Train");
    	menu.add(1,
    			App.MENUITEMS.ITEM_RECOGNIZE.ordinal(),
    			1,
    			"Recognize");
    	menu.add(2,
    			App.MENUITEMS.ITEM_LIBRARY.ordinal(),
    			2,
    			"Gesture Library");
    	
    	return true;
    }
    
	public void onClick(DialogInterface dialog, int buttonID) {
		/*
		 * android.content.DialogInterface.OnClickListener callback
		 * 
		 */
		if (dialog == this.learning_dialog)
		{
			if (this.learning_dialog_view != null)
			{
				//this.recordingGestureIDString = 
				EditText et = 	(EditText) learning_dialog_view.findViewById(R.id.EditText01);
				this.recordingGestureIDString = et.getText().toString();
				if (DEBUG) Log.d("onClick", "recordingGestureIDString set to: "+ this.recordingGestureIDString);
			}
		}
		
	}
    
    
    public void stateChanged()
    {
		TextView statusText = (TextView) findViewById(R.id.statusText);

    	if (DEBUG) Log.d("stateChanged", "current State is: "+ this.state.toString());
    	switch (this.state)
    	{
    	case STATE_LEARN: 
    		// show dialog with which the user can enter the gesture id
    		
    		if (DEBUG) Log.d("stateChanged", "STATE_LEARN");
    		statusText.setText("Press button to train gesture");
    		Context ctx = getApplicationContext();
    		LayoutInflater li = LayoutInflater.from(this);
    		// final so that i can use it in the inner class uargh!
    		
    		View dialog = li.inflate(R.layout.learngesturedialog, null );
    		if (DEBUG) Log.d("stateChanged", "inflated");
    		// Dialog dialog = new Dialog(this);
    		// dialog.setContentView(R.layout.learngesturedialog);
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		
    		// Prompt Listener
    		// PromptListener pl = new PromptListener(dialog);
    		
    				
    		builder.setTitle("Enter Gesture ID");
    		builder.setView(dialog);
    		builder.setPositiveButton("OK", this);
    		builder.setNegativeButton("Cancel", this);
    		
    		// show the actual dialog
    		AlertDialog ad = builder.create();
    		this.learning_dialog = ad;
    		this.learning_dialog_view = dialog;
    		ad.show();
    		
    		// this.recordingGestureIDString = pl.getPromptReply();
    		// if (DEBUG) Log.d("statechanged","recording gesture id now changed to:" + this.recordingGestureIDString);
    		
    		
    		// this.recordingGestureIDString = Alerts.showPrompt("Please Enter Gesture ID", this);
    		
    		
    		break;
    	case STATE_RECOGNIZE: 
    		statusText.setText("Gesture recognition mode");
    		break;
    	case STATE_LIBRARY: 
    		break;
    	default:
    		break;
    	}
    }
    
    public void onActivityResult (int requestCode, int resultCode, Intent data)    
    {
    		if (DEBUG)
    		{
    			Log.d("onActivityResult", "Code: "+requestCode+" resultCode " + resultCode/*+ " data " + data.getDataString()*/);
    		}
    		// change state and map to enum value
    		this.state = App.STATES.values()[resultCode];
    		//update activitie's state
    		this.stateChanged();
    }
    
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	/*
    	 * React to selection of options item
    	 */
    	
    	if (DEBUG) Log.d("onOptionsItemSelected","Selected: "+item.getItemId());
    	
    	App.MENUITEMS m;
    	
    	m = App.MENUITEMS.ITEM_LEARN;
    
    	int value = item.getItemId();
    	
    	if (value == App.MENUITEMS.ITEM_LEARN.ordinal())
    	{
    		// do something here
    		state = App.STATES.STATE_LEARN;
    		stateChanged();
    		
    	}
    	else if (value == App.MENUITEMS.ITEM_RECOGNIZE.ordinal())
    	{
    		// activate recognition here
    		state = App.STATES.STATE_RECOGNIZE;
    		stateChanged();
    	}
    	else if (value == App.MENUITEMS.ITEM_LIBRARY.ordinal())
    	{
    		// library stats here
    		state = App.STATES.STATE_LIBRARY;
    		stateChanged();
    		
    		// start library activity
    		Intent i = new Intent(this.getApplicationContext(), DBManagerUIActivity.class );
    		startActivityForResult(i,0);
    		
    	}
    	
  
    	return super.onContextItemSelected(item);
    }
    
    
    
    protected void onResume() {

    	super.onResume();

    	mSensorManager.registerListener(sensorListener,
    	SensorManager.SENSOR_ACCELEROMETER,
    	SensorManager.SENSOR_DELAY_GAME);
    	}
    
    protected void onStop() {
    	
    	super.onStop();

    	}
    
    protected void onDestroy()
    {
    	if(DEBUG) Log.d("onDestroy","killing this here app!");
    	mSensorManager.unregisterListener(sensorListener);
    	// this.myGestureLibrary.onApplicationStop();
    	super.onDestroy();
    }
   
}