/*
 *  $3 Gesture Recognizer -- Implementation for Android Java
 *  
 *  gestureLibrary.java -- manages gestures at runtime
 * 
 * (c) 2010 Sven Kratz
 * 	   Deutsche Telekom Laboratories, TU Berlin
 * 
 * 
 *   Fundamental Data Structure: 
 *   All gestures are stored in a HasMap<String, ArrayList<Gesture>>, so lists of gestures corresponding to the
 *   same id are mapped to an id string
 * 
 */
package com.myproject;


import java.util.*;
import java.util.Map.Entry;
import java.util.Arrays.*;

import android.content.Context;
import android.util.Log;


public class GestureLibrary 
{
	public static GestureLibrary GLibrarySingleInstance = null;
	private final boolean DEBUG = true;
	private final String DEFAULT_GESTURE_TABLE = "GESTURES";
	
	private HashMap<String, ArrayList<Gesture>> myGestureMap = null;
	private GestureLibraryDBAdapter myDBAdapter = null;
	
	public GestureLibrary(String gestureDBName, Context ctx)
	{
		/*
		 * Initialize the gesture library
		 * 
		 * gestureTB / tableName can be null for now
		 * 
		 * --> TODO Feature
		 * 
		 */
		Log.d("GestureLibrary(init)","initializing Gesture Library");
		
		// initialize db adapter
		myDBAdapter = new GestureLibraryDBAdapter(ctx, DEFAULT_GESTURE_TABLE);
		
		// get all the gestures from the db
		myGestureMap = myDBAdapter.getAllGesturesInDB();
		
		
		
		if (DEBUG)
		{
			Log.d("GestureLibrary","opened DB, listing some stats");
			// generate some debug info
			printInfo("GestureLibrary");
			
		}
		
		GLibrarySingleInstance = this;
		
	}
	
	public  String[] getAllGestureIDs()
	{
		Set<String> keys = this.myGestureMap.keySet();
		String[] keyStrings =  keys.toArray(new String[keys.size()]);
		Arrays.sort(keyStrings);
		
		
		if (DEBUG) Log.d("getAllGestureIDs",keyStrings.toString());
		if (keyStrings.length > 0)
		{
			for (int i = 0; i < keyStrings.length; i++)
			{
				int entry_count = myGestureMap.get(keyStrings[i]).size();
				keyStrings[i]+=" : "+entry_count;
			}
			return keyStrings;
		}
		else
		{
			return new String[]{"No Gestures in Library"};
		}
		
	}
	
	public Set<Entry<String,ArrayList<Gesture>>> getGesturesAndID(int sample_length)
	{
		/*
		 * Return gesture / id pairs
		 * 
		 */
		return this.myGestureMap.entrySet();
	}
	
	public void onApplicationStop()
	{
		/*
		 * 
		 *  Close the ptr to the gesture DB
		 *  
		 *  
		 */
		
		myDBAdapter.closeDB();
	}
	
	public boolean removeAllGesturesFromLibrary()
	{
		if (myDBAdapter != null)
		{
			if ( myDBAdapter.resetDatabase())
			{
				// get all the gestures from the db
				myGestureMap = myDBAdapter.getAllGesturesInDB();
				
				
				
				if (DEBUG)
				{
					Log.d("GestureLibrary","reset and opened DB, listing some stats");
					// generate some debug info
					printInfo("GestureLibrary");
					
					
				}
				return true;
			}
			else
			{
				return true;
			}
		}
		else
		{
			return false;
		}
	}
	
	
	public ArrayList<Gesture> getGesturesByID (String id)
	{
		/*
		 *  Returns ArrayList of Gestures for a given gesture id
		 * 
		 */
		if (myGestureMap.containsKey(id))
		{
			return myGestureMap.get(id);
		}
		else
		{
			if (DEBUG)
			{
				Log.i("getGesturesByID","id not found");
			}
			return null;
		}
	}
	
	public ArrayList<ArrayList<float[]>> getGestureTracesByID (String id)
	{
		/*
		 *  this convenience method returns 
		 *  gesture traces as arrays
		 *  
		 */
		
		
		ArrayList<Gesture> gestures = this.getGesturesByID(id);
	
		if (gestures != null)
		{
			int g_count = gestures.size();
			ArrayList<ArrayList<float[]>>  out =  new ArrayList<ArrayList<float[]>> (g_count);//new float[g_count][][];
			ArrayList<float[][]> traces = new ArrayList<float[][]>(10);
		
			
			Iterator<Gesture> it = gestures.iterator(); 
			// collect the gesture traces (hopefully no copying!)
			int g_maxLength = 0; // max length of the gestures --> to fill the array (?)
			int g_idx = 0;
			while(	it.hasNext() )
			{
				Gesture g = it.next();
				out.add(g.gestureTrace);
			}
			
			return out;
			// no fill up the array
		}
		else
		{
			Log.i("getGestureTracesByID","No Gestures for id "+id);
			return  null;
		}
	}
	
	public void addGestureTrace(String id, ArrayList<float[]> trace, boolean addToDB)
	{
		/*
		 *  reads a gesture id and a trace and then adds it to
		 *  gesture hashmap and optionally saves it to the DB
		 */
		Gesture g = new Gesture();
		g.databaseID = -1; /* not known to database yet */ 
		g.gestureTrace = trace;
		g.gestureID = id;
		g.gestureAdded = System.currentTimeMillis();
		
		this.addGesture(id, g, addToDB);
	}
	
	public void addGesture(String id, Gesture g, boolean addtoDB)
	{
		// verify if the gesture ID exits
		if (myGestureMap.containsKey(id))
		{
			myGestureMap.get(id).add(g);
		}
		else
		{
			ArrayList<Gesture> gl = new ArrayList<Gesture>(10);
			gl.add(g);
			myGestureMap.put(id,  gl);
		}
		
		if (DEBUG)
		{
			Log.i("addGesture","attempting to insert to database");
		}
		
		myDBAdapter.insertGestureToDB(g);
		
		if(DEBUG)
		{
			this.printInfo("addGesture");
		}
	}
	
	public boolean hasGestures()
	{
		return myGestureMap.keySet().size()>0;
	}
	
	public void printInfo(String originatingMethod)
	{
		/*
		 * Prints Debug info
		 *  
		 */
		if (myGestureMap != null)
		{
			Set<String> gestureIDs = myGestureMap.keySet();
			Iterator<String> ids =  gestureIDs.iterator();
			int ctr = 0;
			while(ids.hasNext())
			{
				ctr++;
				String current_gid = ids.next();
				int gestureCount = myGestureMap.get(current_gid).size();
				if (originatingMethod != null)
				{
					Log.d("printInfo","[-->"+originatingMethod+"]"
							+"Gesture ID " +current_gid
							+  " has "
							+ gestureCount
							+ " gestures in Lib");
				}
				else
				{
					Log.d("printInfo","Gesture ID " +current_gid
							+  " has "
							+ gestureCount
							+ " gestures in Lib");
				}
			}
			
			if (ctr < 1)
			{
				Log.i("printInfo", "no gestures in Library!");
			}
		}
		else
		{
			Log.i("printInfo", "gesture Library isn't initialized");
		}
	}
		
		
		
		
}


