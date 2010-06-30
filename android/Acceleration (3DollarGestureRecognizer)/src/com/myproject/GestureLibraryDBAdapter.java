/*
 *  $3 Gesture Recognizer -- Implementation for Android Java
 *  
 *  gestureLibraryAdapter.java -- manages gestures in SQLite Database
 * 
 * (c) 2010 Sven Kratz
 * 	   Deutsche Telekom Laboratories, TU Berlin
 * 
 * 
 */
package com.myproject;

// import android.app.ListActivity;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.*;
import android.database.Cursor;
import android.util.Log;
import java.util.*;

public class GestureLibraryDBAdapter  {
	
	private boolean DEBUG = false;
	
	private SQLiteDatabase myDB = null;
	private final Context myCtx;
	public String testGestureStr = null;
	
	public HashMap<String, ArrayList<Gesture>>  allGestures = null;
	
	public String SQLITE_DATABASE_NAME = null;
	
	public boolean resetDatabase()
	{
		if (myDB != null)
		{
			this.initLibrary(SQLITE_DATABASE_NAME, true);
			if (DEBUG) Log.d("resetDatabase","success");
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public String testGesture()
	{
		if (testGestureStr != null)
		{
			return testGestureStr;
		}
		else
		{
			return "Is Null!";
		}
	}
	
	public GestureLibraryDBAdapter(Context ctx)
	{
		// no arguments initializer
		// use dafault library
		
		myCtx = ctx;
		this.initLibrary("GESTURES",false);
	}
	
	public GestureLibraryDBAdapter(Context ctx, String LibraryName)
	{
		// initializer using libraryname argument
		SQLITE_DATABASE_NAME = LibraryName;
		myCtx = ctx;
		this.initLibrary(SQLITE_DATABASE_NAME, false);
	}
	
	private byte[] objectToByteArray(Object o)
	{
		/*
		 * Convenience-Function
		 * Generate byte array from an object
		 * 
		 */
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream s = null;
	
		
		
		try
		{
			 s = new ObjectOutputStream(baos);
		}
		catch (Exception e)
		{
			Log.e("objectToByteArray", "ObjectOutputStream Exception" + e.toString());
		}
		/* git -test */
		try {
			s.writeObject(o);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e("objectToByteArray","writeObject Error"+e.toString());
		}
		
		return baos.toByteArray();
		
		
	}
	
	public boolean insertGestureToDB(Gesture g)
	{
		/*
		 * 
		 * Saves a single gesture to the database
		 * 
		 */
		
		if (myDB != null)
		{
			ContentValues dbData = new ContentValues(3);
			
			
			
			/* create data blob */
			
			// dbData.put("id_", g.databaseID );
			dbData.put("traceData", this.objectToByteArray(g.gestureTrace));
			dbData.put("gestureID", g.gestureID);
			dbData.put("dateAdded", System.currentTimeMillis());
			
			/* push to db */
			// myDB.update("gestureTraces", dbData, null, null);
			myDB.insert("gestureTraces", "", dbData);
			
		}
		else
		{
			Log.e("insertGestureToDB","database not initialized (null)");
			return false;
		}
		
		return true;
	}
	
	public boolean updateGestureToDB(Gesture g)
	{
		/*
		 * 
		 * Saves a single gesture to the database
		 * 
		 */
		
		if (myDB != null)
		{
			ContentValues dbData = new ContentValues(4);
			
			
			
			/* create data blob */
			
			dbData.put("id_", g.databaseID );
			dbData.put("traceData", this.objectToByteArray(g.gestureTrace));
			dbData.put("gestureID", g.gestureID);
			dbData.put("dateAdded", System.currentTimeMillis());
			
			/* push to db */
			myDB.update("gestureTraces", dbData, null, null);
			
		}
		else
		{
			Log.e("updateGestureToDB","database not initialized (null)");
			return false;
		}
		
		return true;
	}
	
	public boolean saveGesturesToDB(HashMap<String, ArrayList<Gesture>> aGestures)
	{
		/*
		 * Updates an entire gesture Library to the 
		 * Apps database
		 * 
		 */
		Collection<ArrayList<Gesture>> all_gestures =  aGestures.values();
		
		if (all_gestures.size() > 0)
		{
		
			Iterator<ArrayList<Gesture>> i = all_gestures.iterator();
		
			
		
			while (i.hasNext())
			{
				ArrayList<Gesture> currentGestureList = i.next();
				Iterator <Gesture> k = currentGestureList.iterator();
				while (k.hasNext())
				{
					Gesture g = k.next();
					if (g.databaseID > 0)
					{
						// existing gesture --> update
						updateGestureToDB(g);
					}
					else
					{
						// new Gesture --> add
						insertGestureToDB(g);
					}
				}
				
			}
		}
		else
		{
			Log.e("updateGesturesToDB", "no gestures to add!");
			return false;
		}
		
		
		return true;
	}
	
	public HashMap<String, ArrayList<Gesture>> getAllGesturesInDB()
	{
		// retrieve all gestures from DB 
		// this should be passed to the runtime gesture library
		HashMap<String, ArrayList<Gesture>> gestureMap = new HashMap<String, ArrayList<Gesture>> ();
		
		ArrayList<String> gestureIDs = getDistinctGestureIDsInDB();
		
		if (gestureIDs.size() > 0)
		{
			Iterator<String> it = gestureIDs.iterator();
			while (it.hasNext())
			{
				// retrieve gestures from DB
				String current_gid = it.next();
				Log.d("getAllGesturesInLibrary","Retrieving Gestures GID" + current_gid);
				ArrayList<Gesture> db_gestures = this.getGesturesByID(current_gid);
				
				gestureMap.put(current_gid, db_gestures);
			}
		}
		
	
		return gestureMap;
	}
	
	private ArrayList<String> getDistinctGestureIDsInDB()
	{
		/* 
		 * Return all gesture IDs found in the DB
		 * 
		 */
		
		ArrayList<String> gestureIDs = new ArrayList<String>();
		if (myDB != null)
		{
	
			Cursor c = myDB.query(
					/*distinct, 
					table, 
					columns, 
					selection, 
					selectionArgs, 
					groupBy, 
					having, 
					orderBy, 
					limit*/
					true, /* make results distinct */
					"gestureTraces",
					new String[]{"gestureID"},
					null,
					null,
					null,
					null,
					null,
					null
					);
			if (c!=null)
			{
				int gid_column = c.getColumnIndex("gestureID");
				if (c.getCount() > 0)
				{
					
					c.moveToFirst();
					while (!c.isAfterLast())
					{
						String currentID = c.getString(gid_column);
						gestureIDs.add(currentID);
						c.moveToNext();
					}
					c.close();
				}
			}
		}
		else
		{
			Log.e("getGesturesIDsInDB","database is null");
			gestureIDs = null;
		}
		return gestureIDs;
	}
	
	

	private ArrayList<Gesture> getGesturesByID(String gestureID)
	{
		/* Return a single gesture Set */
		
		
		
		ArrayList<Gesture> out = new ArrayList<Gesture>(10);
		if (myDB != null)
		{
			
				Cursor c = myDB.query(
						"gestureTraces", 
						new String[]{"id_,traceData", "dateAdded", "gestureID"},
						"gestureID='" +gestureID+"'", 
						null,//selectionArgs,
						null,//groupBy, 
						null,//having, 
						null);//orderBy);
				Log.d("getGesturesByID", "cursor initialized"+ c.getCount());
		
			
			
			// verify if we have something
			if (c!= null )
			{
				// get the column indices
				int gestureTraceColumn = c.getColumnIndex("traceData");
				int dateAddedColumn = c.getColumnIndex("dateAdded");
				int dbIDColumn = c.getColumnIndex("id_");
				// verify if c has > 0 entries
				Log.d("gestGesturesByID", "selected columns");

				if (c.getCount() >0)
				{
					c.moveToFirst();
					do
					{
						
						//long timestamp = c.getLong(dateAddedColumn);
						byte[] gestureTraceBytes =  c.getBlob(gestureTraceColumn);
						
						ObjectInputStream in = null;
						try {
							 in  = new ObjectInputStream(new ByteArrayInputStream(gestureTraceBytes));
						} catch (StreamCorruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							Log.e("getGesturesByID", "StreamCorruptedException" + e.toString());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							Log.e("getGesturesByID", "IOException" + e.toString());
						}
						
						// float [][] gestureTrace = null;
						ArrayList<float[]> gestureTrace = null;
						
						try {
							gestureTrace = (ArrayList<float[]>) in.readObject();
						} catch (OptionalDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							Log.e("getGesturesByID", "OptionalDataException" + e.toString());

						} catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							Log.e("getGesturesByID", "ClassNotFoundException" + e.toString());

						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							Log.e("getGesturesByID", "IOException" + e.toString());
						}
						
						Log.d("getGesturesByID", "Got Trace:" + gestureTrace.toString());
						if (gestureTrace.size() > 0)
						{
							// raw_gesture_traces.add(gestureTrace);
							Gesture g = new Gesture();
							g.gestureTrace = gestureTrace;
							g.gestureID = gestureID; // arg of the method
							g.gestureAdded = c.getLong(dateAddedColumn);
							g.databaseID = c.getLong(dbIDColumn);
							out.add(g);
			
						}
						
						this.testGestureStr = "";
						for (int k = 0; k < gestureTrace.size(); k++ )
						{
							String append_str = Arrays.toString(gestureTrace.get(k));
							// Log.d("getGesturesByID", append_str);
							this.testGestureStr+=append_str;
						}
						
						// move the cursor forwards
						
						c.moveToNext();
						
					}
					while (!c.isLast());
					c.close();
				}
			}
		}
		else
		{
			Log.e("getGesturesByID", "No DB Initialized" );
			return null;
		}
		
		Log.d("getGesturesByID", 
				"Retrieved "+out.size()+" gestures with ID "+gestureID);
		return out;
	}
	
	public void closeDB()
	{
		if (DEBUG) Log.d("closeDB", "closing the database, bye!");
		if (myDB != null)
		{
			myDB.close();
			myDB = null;
		}
	}
	
	private void initLibrary(String libraryName, boolean reset)
	{
		/*
		 *  initializes the SQLite DB Containing the Gestures
		 *  setting reset to true wipes all existing data
		 *  
		 */
	 	if (DEBUG) Log.d("initLibrary", "============================\nSTART\n============================\n============================\n");
	 	if (DEBUG) Log.d("initLibrary", this.SQLITE_DATABASE_NAME);

		
		try
		{
			// reset = true;
			 // myDB = SQLiteDatabase.openOrCreateDatabase(libraryName, null);
			/*if (myDB != null)
			{
				this.closeDB();
			}*/
			myDB = myCtx.openOrCreateDatabase(this.SQLITE_DATABASE_NAME, 0, null);
			 /* Create a Table in the Database. Drop and recreate if not exists */ 
			 if (reset)
				 {
				 	//myDB.beginTransaction();
				 	if (DEBUG) Log.d("initLibrary", "dropping table!");
				 	myDB.execSQL("DROP TABLE IF EXISTS gestureTraces");
				 	// myDB.endTransaction();
				 }
			 
             myDB.execSQL("CREATE TABLE IF NOT EXISTS gestureTraces"
            		 +"(id_ INTEGER PRIMARY KEY AUTOINCREMENT, traceData BLOB, gestureID VARCHAR(255), dateAdded INTEGER);");
             

		}
		catch (Exception e)
		{
			Log.e("initLibray", "DB Init Exception"+ e.toString());
		}
		
		if (DEBUG)
		{
			// test insert a gesture trace
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream s = null;
			try
			{
				
	
				 s = new ObjectOutputStream(baos);
			
			}
			catch (Exception e)
			{
				Log.e("initLibrary", "ObjectOutputStream Exception" + e.toString());
			}
		
		
		
			try {
				s.writeObject(new float[][]{{1.0f,0.0f,0.0f},
											{0.0f,2.0f,0.0f},
											{0.0f,0.0f,3.0f},
											{0.0f,4.0f,0.0f},
											{5.0f,0.0f,0.0f},
											{0.0f,6.0f,0.0f},
											{0.0f,0.0f,7.0f},
											{0.0f,8.0f,0.0f},
											{9.0f,0.0f,0.0f},
											{0.0f,10.0f,0.0f},
											{0.0f,0.0f,11.0f},
											{0.0f,12.0f,0.0f},
											{13.0f,0.0f,0.0f},
											{3333.032f,0.0f,1278.3233f},
											
				});
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e("initLibrary","writeObject Error"+e.toString());
			}
			
			
			
			ContentValues dbData = new ContentValues(3);
			
			dbData.put("traceData", baos.toByteArray() );
			dbData.put("gestureID", "1");
			dbData.put("dateAdded", System.currentTimeMillis());
			
			myDB.insert("gestureTraces", "", dbData);
			
			Log.d("getGesturesByID","Attempting to Retrieve Gestures");
	
			
			
			
			// now try to retrieve the gesture information
			// ArrayList<float[][]> library_gestures = this.getGesturesByID("1");
			
			
			/*
			if (library_gestures != null)
			{
				Log.d("getGesturesByID","Successfully got library gestures");
				
			}*/
		
		}
		

		
	}

}
