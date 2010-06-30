package com.myproject;

import java.util.ArrayList;

public class Gesture {
	
	public ArrayList<float[]> gestureTrace = null;

	public String gestureID = null;
	public long gestureAdded = -1;
	public long databaseID = -1;
	
	public Gesture()
	{
		
	}
	
	public Gesture(String gestureID, ArrayList<float[]> gestureTrace)
	{
		this.gestureID = gestureID;
		this.gestureTrace = gestureTrace;
		this.gestureAdded = System.currentTimeMillis();
		this.databaseID = -1;
	}
	
	public static String printAnyTrace(ArrayList<float[]> trace)
	{
		/*
		 * Prints any sort of Trace in $3
		 * 
		 */
		String out = "";
		if (trace == null)
		{
			return "null";
		}
		else
		{
			out +="[ ";
			for (float [] p : trace)
			{
				out += "["+ p[0]+","+p[1]+","+p[0]+"], ";
			}
			out +="]";
		}
		return out;
	}
	
	public  String print_trace()
	{
		/*
		 * Print Trace of Gesture Instance
		 * 
		 */
		String out = "";
		if (this.gestureTrace == null)
		{
			return "null";
		}
		else
		{
			out +="[ ";
			for (float [] p : this.gestureTrace)
			{
				out += "["+ p[0]+","+p[1]+","+p[0]+"], ";
			}
			out +="]";
		}
		return out;
	}
	
	

}
