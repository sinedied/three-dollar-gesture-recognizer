/*
 *  $3 Gesture Recognizer Android Implementation
 *  
 *  DBManagerUIActivity.java -- manages Gesture Database items
 * 
 * (c) 2010 Sven Kratz
 * Deutsche Telekom Laboratories, TU Berlin
 * 
 */

package com.myproject;

import android.os.Bundle;
import com.myproject.R;
import android.app.Activity;
import android.app.ListActivity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
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
public class DBManagerUIActivity extends ListActivity {
	
	public App.STATES stateChange = App.STATES.STATE_LIBRARY;
	
	private boolean DEBUG = true;
	
	private GestureLibrary glibrary_instance = null; 
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
       
        
        
        
        // points to the XML file specifying the content
        setContentView(R.layout.dbui);
        stateChange = App.STATES.STATE_LIBRARY;
        
 // see if we have a gesture library
        
        if (GestureLibrary.GLibrarySingleInstance != null)
        {
        	this.glibrary_instance = GestureLibrary.GLibrarySingleInstance;
        	
        }
        
        // initialize the list view
        this.initListView();
        
        
        // button to delete all gestures
    	final Button mainButton = (Button)findViewById(R.id.deleteGesturesButton);
mainButton.setOnTouchListener(new View.OnTouchListener()
    
    	
    	{ 
    		public  boolean onTouch(View v, MotionEvent event)
    		{
    			
    			
    			 if (event.getAction() == MotionEvent.ACTION_UP) 
    					
    			{
    				 // Later add a confirmation dialog here! 
    				Log.d("OnTouch","Deleting Gestures in Library");
    				
    				glibrary_instance.removeAllGesturesFromLibrary();
    			}
    			
    			
    			
    			
    			
    			return false;
    		}
    	});
    

        
        
    }
    
    public void initListView()
    {
    	/* Inits the list view displaying gesture ids and counts */ 
    	
    	// see if we have a gesture library instance
    	if (DEBUG) Log.d("initListView","starting");
    	String[] gestureIDStrings;
    	if (this.glibrary_instance != null)
    	{
    		/* initialize the list view */
    		gestureIDStrings = this.glibrary_instance.getAllGestureIDs();
    	}
    	else
    	{
    		/* fake initialization */
    		gestureIDStrings = new String[]{"no library!"};
    	}
    	
       setListAdapter(new ArrayAdapter<String>(this,
    	          android.R.layout.simple_list_item_1, gestureIDStrings));
    	
    	
    }
    
    public void onRestart()
    {
        stateChange = App.STATES.STATE_LIBRARY;
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
    		
    		stateChange = App.STATES.STATE_LEARN;
    		// do something here
    		/*
    		state = App.STATES.STATE_LEARN;
    		stateChanged();*/ 
    		/*Intent i = new Intent(this.getApplicationContext(), Acceleration.class );
    		startActivityForResult(i,0);*/ 
    		setResult(stateChange.ordinal());
    		finish();
    	}
    	else if (value == App.MENUITEMS.ITEM_RECOGNIZE.ordinal())
    	{
    		stateChange = App.STATES.STATE_RECOGNIZE;
    		// activate recognition here
    		/*
    		state = App.STATES.STATE_RECOGNIZE;
    		stateChanged();*/
    		setResult(stateChange.ordinal());
    		finish();
    	}
    	else if (value == App.MENUITEMS.ITEM_LIBRARY.ordinal())
    	{
    		// don't do anything
    		
    		// library stats here
    		/*
    		state = App.STATES.STATE_LIBRARY;
    		stateChanged();*/
    		
    		// start library activity
    		/*
    		Intent i = new Intent(this.getApplicationContext(), DBManagerUIActivity.class );
    		startActivityForResult(i,0);*/
    	
    		
    	}
    	
  
    	return super.onContextItemSelected(item);
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

}
