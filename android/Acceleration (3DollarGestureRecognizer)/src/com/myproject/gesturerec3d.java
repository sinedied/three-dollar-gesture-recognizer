/*
 *  $3 Gesture Recognizer - Implementation for Android Java
 *  
 *  (c) 2009/2010 Sven Kratz
 *  Deutsche Telekom Laboratories, TU Berlin
 *  
 */

package com.myproject;



import java.math.*;
import java.util.*;
import java.util.Map.Entry;

import android.util.*;

public class gesturerec3d extends GestureRecognizer {
	
	/*
	interface gestureCallback
	{
		void gestureCallback(String recognitionString);
	}*/
	
	public float PI = (float) Math.PI;
	public int path_length = 0;
	public final float[] zero = {0.0f,0.0f,0.0f};
	public ArrayList<float[]> raw_data;
	public ArrayList<float[]> gesture_path;
	public ArrayList<float[]> resampled_gesture;
	public ArrayList<float[]> rotated_gesture;
	public ArrayList<float[]> normalized_gesture;
	public float bbox_size = 100.0f;
	
	public final String INVALID_GESTURE_STRING = "not recognized!";
	public float DETECTION_THRESHOLD = 0.9f;
	public boolean RECOGNIZE_GESTURES = true;
	
	public int resample_amount;
	
	public boolean DEBUG = true;
	public boolean VERBOSE = false;
	
	public boolean ROTATE = true;
	public GestureLibrary gestureLibrary = null;
	
	public gesturerec3d(GestureLibrary glibrary, int resampleAmount)
	{
		super();
		this.resample_amount = resampleAmount;
		this.gestureLibrary = glibrary;
		this.reset();
	}
	
	public void reset()
	{
		// Called after gesture recognition has finished
		this.path_length = 0;
		this.gesture_path = null;
		this.resampled_gesture = null;
		this.rotated_gesture = null;
		this.normalized_gesture = null;
		this.bbox_size = 100.0f;	
		
		
	
	}
	
	public Gesture prepare_gesture_for_library(Gesture g)
	{
		/*
		 * Transform Gesture for saving in the Gesture Library
		 * 
		 */
		
		if (DEBUG) Log.d("prepare_gesture_for...","Starting to convert gesture to $3");
		
		ArrayList<float []> gList = g.gestureTrace;
		
		
		
		// ArrayList<float []> raw_d, path, resampled_g, rotated_g, normalized_g;
		
		this.reset();
		if (DEBUG) Log.d("prepare_gesture_for...","Trace Length: "+ gList.size());
		this.raw_data = gList;
		// create path from acceleration deltas
		if (VERBOSE) Log.d("prepare_gesture_for...", "RAW-Data" +Gesture.printAnyTrace(gList));
		
		this.gesture_path = this.create_path(gList);
		if (VERBOSE) Log.d("prepare_gesture_for...", "PATH-Data" +Gesture.printAnyTrace(this.gesture_path));
		if (DEBUG) Log.d("prepare_gesture_for...","Gesture Path Length: "+ this.gesture_path.size());
		//evenly resample path points
		this.resampled_gesture = this.resample_points(this.gesture_path, this.resample_amount);
		if (VERBOSE) Log.d("prepare_gesture_for...", "RESAMPLE-Data" +Gesture.printAnyTrace(this.resampled_gesture));
		if (DEBUG) Log.d("prepare_gesture_for...","Resampled Path Length: "+ this.resampled_gesture.size());

		if (ROTATE)
		{
			this.rotated_gesture = this.rotate_to_zero(this.resampled_gesture);
		}
		else
		{
			this.rotated_gesture = this.resampled_gesture;
		}
		// if (DEBUG) Log.d("prepare_gesture_for...","Rotated Path Length: "+ this.rotated_gesture.size());
		if (VERBOSE) Log.d("prepare_gesture_for...", "ROTATED-Data" +Gesture.printAnyTrace(this.rotated_gesture));


		
		this.normalized_gesture = this.scale_to_cube(this.rotated_gesture, this.bbox_size);
		if (VERBOSE) Log.d("prepare_gesture_for...", "NORMALIZED-Data" +Gesture.printAnyTrace(this.normalized_gesture));

		
		//if (DEBUG) Log.d("prepare_gesture_for...","Normalized Path Length: "+ this.normalized_gesture.size());

		
		// assign a copy of the new normalized gesture to g
		g.gestureTrace = new ArrayList<float []>(this.normalized_gesture);
		
		if (DEBUG) Log.d("prepare_gesture_for...","New Gesture Trace Length: "+ g.gestureTrace.size());

		
		if (DEBUG) Log.d("prepare_gesture_for...","Finished -- returning transformed gesture");
		
		//if (DEBUG) Log.d("prepare_gesture_for...", "Printing Gesture Trace:\n"+g.print_trace());
		
		return g;
		
		
	}
	
	public String recognize_gesture(Gesture g)
	{
		return recognize_gesture(g, true);
	}
	
	
	public String recognize_gesture(Gesture g, boolean rotated)
	{
		String recGest = this.INVALID_GESTURE_STRING;
		/*
		ArrayList<float []> gList = g.gestureTrace;
		
		String recGest = this.INVALID_GESTURE_STRING;
		
		this.reset();
		this.raw_data = gList;
		// create path from acceleration deltas
		this.gesture_path = this.create_path(gList);
		//evenly resample path points
		this.resampled_gesture = this.resample_points(this.gesture_path, this.resample_amount);
		if (ROTATE)
		{
			this.rotated_gesture = this.rotate_to_zero(this.resampled_gesture);
		}
		else
		{
			this.rotated_gesture = this.resampled_gesture;
		}
		
		this.normalized_gesture = this.scale_to_cube(this.rotated_gesture, this.bbox_size);*/
		
		this.prepare_gesture_for_library(g);
		
		if (this.gestureLibrary.hasGestures())
		{
			if (DEBUG) Log.d("recognize_gesture","==== I have Gestures, Starting Recognition =====");
			// all gesture entries
			Set<Entry<String, ArrayList<Gesture>>> library_gestures = this.gestureLibrary.getGesturesAndID(0);
			
			// score table
			ArrayList<Score> scoreTable = new ArrayList<Score>(30);
			
			
			float cutoff = 2.0f * (float)Math.PI*(15.0f/360.0f);
			
			
			for (Entry<String, ArrayList<Gesture>> entry : library_gestures)
			{
				
				int idnr = 0;
				// float distance = 
				String currentGestureID = entry.getKey();
				ArrayList<Gesture> gestureList = entry.getValue();
				for (Gesture lg : gestureList)
				{
					if (DEBUG) Log.d("recognize_gesture", "Comparing to Gesture id "+lg.gestureID+" nr: "+idnr
							+ " Sizes "+g.gestureTrace.size()+" "+ lg.gestureTrace.size());
					float distance = this.distance_at_best_angle((float)Math.PI/2, (float)Math.PI/2, (float)Math.PI/2, 0, g.gestureTrace, lg.gestureTrace, cutoff);
					float score = this.score(distance);
					Score s = new Score();
					s.distance = distance;
					s.gid = currentGestureID;
					s.score = score;
					s.idnr = idnr++;
					scoreTable.add(s);
				}
				
					
			}
			Log.d("recognize_gesture", "== Summary of Gesture Recognition ==");
			Log.d("recognize_gesture", "Sorted by highest Score");
			Log.d("recognize_gesture", "GID \t idnr \t Score \t Distance");
			Collections.sort(scoreTable, new Score());
			for (Score s : scoreTable)
			{
				Log.d("recognize_gesture", s.toString());
			}
			recGest = this.recognize_from_scoretable(scoreTable);
			
			
			
		}
		
		return recGest;
	}
	
	public String recognize_from_scoretable( ArrayList<Score> scoretable)
	{
		 /*Implements heuristic for gesture detection:
        int the top threee, detect at least two candidates of the same gesture id with a score >.55
        @param scoretable: scoretable sorted by score
        @return: recognized gesture code or invalid (-100) Gesture*/
		Log.d("recognize_from_scoretable","Started!");
		int count_h1 = 0;
		int count_h2 = 0;
		// FOR-Schleife ist buggy!
		for (Score s : (List<Score>) scoretable.subList(0, 2))
		{
			// high-probability match!
			if (s.score > this.DETECTION_THRESHOLD*1.1)
			{
				Log.d("recognize_from_scortable","Item: "+ s.toString());
				return s.gid;
			}
			// if not high prob, apply heuristic
			// BUG SOMEWHERE HERE!!!!!!
			ArrayList<Score> scoretable_cpy;// = (ArrayList<Score>) scoretable.clone();
			/*scoretable_cpy.remove(s);
			scoretable_cpy = (ArrayList<Score>) scoretable_cpy.subList(0, 3);*/
			scoretable_cpy = (ArrayList<Score>) scoretable.clone();
			scoretable_cpy.remove(s);
			
			
			if (s.score > this.DETECTION_THRESHOLD)
			{
				for (int i = 0; i < 2; i++)
				{
					Score other = scoretable_cpy.get(i);
					Log.d("recognize_from_scortable","Other: "+ other.toString());
					if (s.gid == other.gid && other.score >= this.DETECTION_THRESHOLD*0.95)
					{
						// heurstic 1
						Log.d("recognize_from_scoretable", "h1++");
						count_h1++;
						
					}
					if (s.gid == other.gid)
					{
						Log.d("recognize_from_scoretable", "h1++");
						count_h2++;
					}
					

				} // for
			} // if
			
			// see if heuristic has found likely match
			if (count_h1 >0)
			{
				Log.i("recognize_from_scoretable", "Decided by H1");
				return s.gid;
			}
			else if (count_h2 >1)
			{
				Log.i("recognize_from_scoretable", "Decided by H2");

				return s.gid;
			}
			else
			{
				count_h1 = 0;
				count_h2 = 0;
			}
				
			
			
		} // for (outer)
		// if all else fails, this gesture has not been recognized
		return "Unknown Gesture";
	}
	
	public float path_distance(ArrayList<float []> path1, ArrayList<float []> path2)
	{ /*
	   * Simple Euclidean Distance
	   * For debug only, this is not $3 recognizer!!!
	   *
	   */
	
		int length1 = path1.size();
		int length2 = path2.size();
		
		if (VERBOSE) Log.d("path_distance", "Path-sizes "+length1+" "+length2);
		
		float distance = 0.0f;
		int idx = 0;
		
		/*
		if (length1 >= length2)
		{
			for (float[] p1 : path1)
			{
				float[] p2 = path2.get(idx++);
				distance += this.distance_sqrt(p1, p2);	
			}
		}
		else
		{
			for (float[] p2 : path2)
			{
				float[] p1 = path1.get(idx++);
				distance += this.distance_sqrt(p1, p2);	
			}
		}*/
		
		
		if (length1 == length2)
		{
			for (int i = 0; i < length1; i++)
			{
				float[] v1 = path1.get(i);
				float[] v2 = path2.get(i);
				distance += this.distance_sqrt(v1, v2);
				return distance; // / length1;
			}
		}
		else
		{
			if (VERBOSE) Log.d("path_distances", "distances not equal, trimming");
			if (length1 < length2)
			{
				int diff = length2-length1;
				// trim items
				ArrayList<float[]> p2 = path2;/*(ArrayList<float[]>) path2.clone();*/
				for (int i = length1-1; i < diff+length1-1; i++)
				{
					// remove tail object
					p2.remove(p2.size()-1);
				}
				// recurse 
				return this.path_distance(path1, p2);
			}
			else
			{
				int diff = length1 - length2;
				ArrayList<float[]> p1 = path1;/*(ArrayList<float[]>) path1.clone();*/

				for (int i = length2-1; i < diff+length2-1; i++)
				{
					// remove tail object
					p1.remove(p1.size()-1);
				}
				//recurse
				return this.path_distance(p1, path2);
					
			}
		}
		return distance;
	}
	
	public float score(float distance)
	{
		/*
		 * Scoring heuristic, derived from Wobbrock paper.
		 */
		float b = this.bbox_size;
		return 1.0f - (distance /  (float)Math.sqrt(b*b+b*b+b*b));	
	}
	
	public ArrayList<float[]> create_path(ArrayList<float[]> gList)
	{
		
		// Problem Here: Deltas not used!! 
		//
		// creates a gesture path from gList 
		ArrayList<float[]> path = new ArrayList<float[]>(250);
		
		/*Iterator<float[]> i = gList.iterator();
		while (i.hasNext())*/
		for (float [] item : gList)
		{
			if (path.size() == 0)
				{
					path.add(item);
				}
			else
				{
					float[] last = path.get(path.size()-1);
					float[] next = item;
					float[] newItem = {	last[0]+next[0],
										last[1]+next[1],
										last[2]+next[2]};
					path.add(newItem);
				}
		}
		
	return path;
	}
	
	public ArrayList<float[]> resample_points(ArrayList<float[]> gList, int numSamples )
	{
		// performs linear interpolation on the path
		ArrayList<float[]> newpoints = new ArrayList<float[]>(250);
		
		float path_length = this.calculate_path_length(gList);
		float increment = path_length / ((float) numSamples);
		float sum_distance, delta;
		sum_distance = delta = 0.0f;
		float qx,qy,qz; qx = qy = qz = 0.0f;
		
		// float[] array;
		
		// convert the arrayList of floats to a standard array
		// float[][] gList = (float[][]) gList_.toArray();
		
		
		
		if (DEBUG)
		{
			// do debugging here
		}
		
		// step-through algorithm (like in the paper)
		
		Iterator<float[]> i = gList.iterator();
		/*
		float[] last = {0.0f,0.0f,0.0f};
		float[] p,pl;*/
		int index = 1;
		
		// note: we need to assert the number of elements (at least 2) 
		// at some later point in time
		
		/*#pl = i.next();
		p = i.next();*/
		
		/*p = gList[index];
		pl = gList[index-1];*/
		int count = 1;
		/*path.add(gList[index]);*/
		/*index++;*/
		
		ArrayList<float[]> path = new ArrayList<float[]>(250);

		
		while (i.hasNext())
		{
			float length = this.calculate_path_length(path);
			if (length < increment)
			{
				// continue adding segments
				index++;
				path.add(i.next());
			}
			else
			{
				// calculate unit vector from last two vectors in path
				float[] v1 = path.get(path.size()-1);
				float[] v2 = path.get(path.size()-2);
				float[] diff = {v1[0] - v2[0], v1[1] - v2[1], v1[2] - v2[2]};
				float[] unitV = this.unit_vector(diff);
				
				// remove the vector that overly extends the path
				//path.remove(v1);
				float missing_incr = length - increment;
				float[] newpoint = {v1[0] - missing_incr * unitV[0],
									v1[1] - missing_incr * unitV[1],
									v1[2] - missing_incr * unitV[2]};
				newpoints.add(newpoint);
				
				// del path
				// no need in java --> instantiate a new path and 
				// seems to be less work than creating a new obj.
				path.clear();
				path.add(newpoint);
				path.add(v1);
				count++;
			}
				
		}
		
		
		
		
		
		
		
		Log.d("resample points", "Amount: "+ newpoints.size());
		
		return newpoints;
	}
	
	public ArrayList<float[]> rotate_to_zero(ArrayList<float[]> points)
	{
		// Rotation Matrix is Buggy!!!
		ArrayList<float[]> rotated_points = new ArrayList<float[]>(250);
		float[] centroid = this.centroid(points);
		if (DEBUG) Log.d("rotate_to_zero", "centroid: " + centroid);
		
		float theta = this.angle3(centroid, points.get(0));
		float[] axis = this.unit_vector(this.orthogonal(points.get(0), centroid));
		float[][] r_matrix = this.rotationMatrixWithVector3(axis, theta);
		
		Iterator<float[]> i = points.iterator();
		
		while(i.hasNext())
		{
			// float[] newpoint = this.rotate3(i.next(),r_matrix);
			rotated_points.add( this.rotate3(i.next(),r_matrix) );
		}
		
		// TODO: Debug printout of rotated points
		/* nullangle = self.norm_dot_product(centroid, centroid)
	        angle = self.norm_dot_product(centroid, rotated_points[0])
	        if VERBOSE_: print "Cosine Null Angle",nullangle,"Cosine Debug Angle:", angle, " >>1.0 is good!"
	        return rotated_points*/
		
		
		return rotated_points;
	}
	
	public ArrayList<float[]> scale_to_cube(ArrayList<float[]> points, float size)
	{
		/* scale set of points to lie in a cube of standardized size 
		 *  Baustelle! Bearbeitet 18.01.10 --- sollte funzen
		 * */
		ArrayList<float[]> newpoints = new ArrayList<float[]>(250);
		
		float[][] bbox = this.bounding_box3(points);
	    float bwx = Math.abs(bbox[0][0] - bbox[0][1]);
        float bwy = Math.abs(bbox[1][0] - bbox[1][1]);
        float bwz = Math.abs(bbox[2][0] - bbox[2][1]);
        
        if (DEBUG) Log.d("scale_to_cube", "BBox Widhts: "+ bwx+ bwy+ bwz);
        for (float [] p : points)
        {
        	 float qx = p[0] * (size / bwx);
             float qy = p[1] * (size / bwy);
             float qz = p[2] * (size / bwz);
             
             newpoints.add(new float[]{qx,qy,qz});
             
        }
        	
		return newpoints;
	}
	
	public float distance_at_best_angle(float angularRangeX, 
										float angularRangeY, 
										float angularRangeZ, 
										float increment, 
										ArrayList<float []> candidate_points, 
										ArrayList<float []> library_points, 
										float cutoff_angle) 
	{
		/* @return: compares distance to candidate_points with points in library at various angles arouny x,y,z axis
            @param angularRange{X,Y,Z}: the search range (positive to negative that should be used)
            @param increment: search increment (in radians)
            @param candidate_points: the candidate point list (resampled, rotated, normalized)
            @param library_points: points from gesture library with (resampled, normalized, rotated, normalized) gestures
            @param cutoff_angle: angle at which Golden Section Search (GSS) is cut off default is 2 degrees
		 * 
		 *  UNFINISHED IMPL.
		 */
		
		if (DEBUG) Log.d("distance_at_best_angle", "Starting");
		
		float mind = Float.MAX_VALUE;
		float maxd = Float.MIN_VALUE;
		float minDistAngle = 0.0f;
		float maxDistAngle = 0.0f;
		
		
		
	
		
		// end kludge
		
		
		int length1  = candidate_points.size();
		int length2 = library_points.size();
		
		
		int sampleLength = 0;
		
		if (length1 < length2)
			sampleLength = length1;
		else
			sampleLength = length2;
		
		length1 = sampleLength;
		length2 = sampleLength;
		
		if (DEBUG) Log.d("distance_at_best_angle","Sample Length is: "+sampleLength);
		
		// todo: print out the lengths
		
		// Golden-Section Search 
		
		float theta_a = -angularRangeX;
		float theta_b = -theta_a;
		float theta_delta = cutoff_angle; // angle at which GSS cuts off
		
		// best angles for lower / upper bound
		
		float[] bestAngleLower = {0.0f, 0.0f, 0.0f};
		float[] bestAngleUpper = {0.0f, 0.0f, 0.0f};
		
		// minimum distances
		// initialize lower and upper values to max float
		float minDistL = Float.MAX_VALUE;
		float minDistU = Float.MAX_VALUE;
		
		// golden section
		float phi = 0.5f *(-1.0f+(float)Math.sqrt(5));
		
		// initial lower search angle
		float li = phi*theta_a+(1-phi)*theta_b;
		
		//result of the following function: [[mindist], [a1, a2, a3]]
		float[][] angle_search_result_lower = this.search_around_angle(candidate_points, library_points, li, bestAngleLower);
		// assign return values of previous function
		minDistL = angle_search_result_lower[0][0];
		bestAngleLower = angle_search_result_lower[1];
		
		if (DEBUG) Log.d("distance_at_best_angle","Initial Best angles (lower): "+bestAngleLower.toString()+" D: "+minDistL);

		
		// initial upper search angle
		float ui = (1-phi)*theta_a+ phi*theta_b; 
		
		float[][] angle_search_result_upper = this.search_around_angle(candidate_points, library_points, ui, bestAngleUpper);
		minDistU = angle_search_result_upper[0][0];
		bestAngleUpper = angle_search_result_upper[1];
		
		if (DEBUG) Log.d("distance_at_best_angle","Initial Best angles: "+bestAngleLower.toString()+" D: "+minDistL+" "+bestAngleUpper.toString()+" D: "+minDistU);
		
		while (Math.abs(theta_b-theta_a) > theta_delta)
		{
			if (minDistL <= minDistU)
			{	// continue searching on lower side
				theta_b = ui;
				ui = li;
				minDistU = minDistL;
				li = phi*theta_a+(1-phi)*theta_b;
				//result of the following function: [[mindist], [a1, a2, a3]]
				float [][] angle_search_result = this.search_around_angle(candidate_points, library_points, li, bestAngleLower);
				// decode result
				minDistL = angle_search_result[0][0];
				bestAngleLower = angle_search_result[1];
			}
			else
			{
				theta_a = li;
				li = ui;
				minDistL = minDistU;
				ui = (1-phi)*theta_a + phi*theta_b;
				float [][] angle_search_result = this.search_around_angle(candidate_points, library_points, ui, bestAngleUpper);
				// decode result
				minDistU = angle_search_result[0][0];
				bestAngleUpper = angle_search_result[1];
			}
			// maybe add angles later
			if (DEBUG) Log.d("distance_at_best_angle","minDistU "+minDistU+" minDistL "+ minDistL 
					+ "\t Intervals "+ ui + " "+ li);
		}
		if (DEBUG) Log.d("distance_at_best_angle", "GSS RESULTS "+ minDistU+" "+minDistL+ "\t Best Angles "+ bestAngleUpper.toString()+" "+bestAngleLower.toString());
		
		if (minDistU >= minDistL) return minDistL;
		else return minDistU;
		
		
		
	}
	
	private float[][] search_around_angle(ArrayList<float[]> candidate, ArrayList<float[]> template, float angle, float[] best_angles)
	{/*
		 """searches for minium distance around best_angles, using angle as offset
        @param candidate: the candidate points
        @param template: the template points
        @param best_angles: the angles where the last minimum distance was detected
        @param angle: angle to be checked around for improvement (add angle to best_angles)
        @return: minDist, newAngles (float[2][] containing the minimum distance and the best new angles)"""
        
        UNFINISHED IMPL
	*/
		
		float minDist = Float.MAX_VALUE;
		float[] minAngles = {0.0f, 0.0f, 0.0f};
		
		for (int i = 0; i < 8; i++)
		{
			float [] add = {best_angles[0],best_angles[1], best_angles[2]};
			// Greedy Search: go through all combinations (2^3)
			// of adding the angle
			if (i % 2 ==1)
				add[2]+=angle;
			if (i % 4> 1 )
				add[1]+=angle;
			if (i % 8 > 3)
				add[2]+=angle;
			float dist = this.distance_at_angles(candidate, template, add);
			if (dist < minDist)
			{
				minDist = dist;
				minAngles = new float[]{add[0],add[1],add[2]};
			}
			
		}
		if (DEBUG) Log.d("Search Around Angle", "minDist "+minDist);
		float[][] out = new float[][]{{minDist},minAngles};
		return out;
		
	}
	
	private float distance_at_angles(ArrayList<float[]>candidate, ArrayList<float[]>template, float[] angles)
	{
		float dist = Float.MAX_VALUE;
		
		// Being implemented 18.01.09
		
		float alpha = angles[0];
		float beta = angles[1];
		float gamma = angles[2];
		
		float [][] matrix = this.rotationMatrixWithAngles3(alpha, beta, gamma);
		
		// rotate path according to angles and calculate distance
		ArrayList<float []> newCandPoints = new ArrayList<float []>(candidate.size());
		for (float [] p : candidate)
		{
			float [] np = this.rotate3(p, matrix);
			newCandPoints.add(np);
		}
		dist = this.path_distance(newCandPoints, template);
		

		return dist;
	}
	
	private float[][] rotationMatrixWithAngles3(float a, float b, float g)
	{
		// returns three-angle rotation matrix
		float[][] out = {
							{(float) (Math.cos(a)*Math.cos(b)), (float)(Math.cos(a)*Math.sin(b)*Math.sin(g)-Math.sin(a)*Math.cos(g)), (float)(Math.cos(a)*Math.sin(b)*Math.cos(g)+Math.sin(a)*Math.sin(g))},
							{(float)(Math.sin(a)*Math.cos(b)), (float)(Math.sin(a)*Math.sin(b)*Math.sin(g)+Math.cos(a)*Math.cos(g)), (float)(Math.sin(a)*Math.sin(b)*Math.cos(g) - Math.cos(a)*Math.sin(g))},
							{(float)(-Math.sin(b)) , (float)(Math.cos(b)*Math.sin(g)), (float)(Math.cos(b)*Math.cos(g))}
						};
		return out;
	}
	
	
	
	private float[][] bounding_box3(ArrayList<float[]> points)
	{
		/* returns bounding box in 3d space of set of points */
		
		float[][] out = {	{0.0f,0.0f,0.0f},
							{0.0f,0.0f,0.0f},
							{0.0f,0.0f,0.0f}};
		
		Iterator<float[]> i = points.iterator();
		float [] p;
		// sanity check
		if (i.hasNext()) 
		{
				p = i.next();
		}
		else 
		{
				return out;
		}
		
		float[] mmx = { p[0], p[0]};
		float[] mmy = { p[1], p[1]};
		float[] mmz= { p[2], p[2]};
		
		// iterate over points to determine boundaries
		while (i.hasNext())
		{
			p = i.next();
			
			if (p[0] <= mmx[0])
			{
                mmx[0] = p[0];
			}       
			else if (p[0] > mmx[1])
			{
                mmx[1] = p[0];
			}
			
			if (p[1] <= mmy[0])
			{
                mmy[0] = p[1];
			}            
			else if (p[1] > mmy[1])
			{
                mmy[1] = p[1];
			}
			
			if (p[2] <= mmz[0])
			{
                mmz[0] = p[2];
			}         
			else if (p[2] > mmz[1])
			{
                mmz[1] = p[2];
			}	
		}
		
		out[0] = mmx;
		out[1] = mmy;
		out[2] = mmz;
		
		return out;
		
	}

	
	private float calculate_path_length(ArrayList<float[]> gList)
	{
		// calculate the length of a path
		float distance = 0;
		int index = 1;
		while (index < gList.size())
		{
			float[] p = gList.get(index);
			float[] pl = gList.get(index-1);
			float delta = this.distance_sqrt(pl,p);
			distance = distance + delta;
			index++;
		}
		return distance;
	}
	
	private float[] rotate3(float[] p, float[][] matrix)
	{
		// multiply 3x3 rotation matrix with point (no list comprehension here)
		float[] out = {0.0f,0.0f,0.0f};
		for (int i = 0; i < 3; i++)
		{
			float[] r = matrix[i];
			out[i] = p[0]*r[0] + p[1]*r[1]+ p[2]*r[2];
		}
		return out;
	}
	
	private float[][] rotationMatrixWithVector3(float[] axis, float angle)
	{
		// generate a rotation matrix for rotation along axis with the value theta
		float x = axis[0];
		float y = axis[1];
		float z = axis[2];
		int k;
		//float angle = (float) theta;
		float[] rx = {	(float) (1 + (1-Math.cos(angle))*(x*x-1)), 
						(float) ((float) -z*Math.sin(angle)+(1-Math.cos(angle))*x*y),
						(float) (y*Math.sin(angle)+(1-Math.cos(angle))*x*z)};
		float[] ry = { 	(float)(z*Math.sin(angle)+(1-Math.cos(angle))*x*y),  
						(float) (1 + (1-Math.cos(angle))*(y*y-1)),  
						(float) (-x*Math.sin(angle)+(1-Math.cos(angle))*y*z)};
		float[] rz = {(float) (-y*Math.sin(angle)+(1-Math.cos(angle))*x*z),    
		                (float) (x*Math.sin(angle)+(1-Math.cos(angle))*y*z),
		                (float) (1 + (1-Math.cos(angle))*(z*z-1))};
		float[][] matrix = {rx,ry,rz};
		if (DEBUG) Log.d("rotationMatrixWithVecor3", "rotation Matrix:" + matrix);
		return matrix;
	}
	
	private float[] orthogonal(float[] b, float[] c)
	{
		// returns vector orthogonal to (cross-product of) b and c
		// a = b x c, mnemonic: xyzzy
		float ax,ay,az;
		ax = b[1]*c[2] - b[2]*c[1]; //ByCz - BzCy
        ay = b[2]*c[0] - b[0]*c[2]; //BzCx - BxCz
        az = b[0]*c[1] - b[1]*c[0]; //BxCy - ByCx
		return (new float[] {ax,ay,az});
	}
	
	private float norm(float[] u)
	{
		return (float) Math.sqrt(u[0]*u[0]+u[1]*u[1]+u[2]*u[2]);
	}
	
	private float dot_product3(float[] p, float[] q)
	{
		return p[0]*q[0]+p[1]*q[1]+p[2]*q[2];
	}
	
	private float norm_dot_product(float[] u, float[] v)
	{
		// return normalized dot product (for angle calculation)
		return this.dot_product3(u, v) / (this.norm(u)*this.norm(v));
	}
	
	private float angle3(float[] u, float[] v)
	{
		float norm_product = this.norm_dot_product(u,v);
		if (norm_product <= 1.0f)
		{
			float theta = (float) Math.acos(norm_product);
			return theta;
		}
		else
		{
			return 0.0f;
		}
	}
	
	
	
	private float[] centroid(ArrayList<float[]> points)
	{
		// calculates centorid of point list
		float mx = 0.0f;
		float my = 0.0f;
		float mz = 0.0f;
		
		Iterator<float[]> i = points.iterator();
		while (i.hasNext())
		{
			float[] p = i.next();
			mx += p[0];
			my += p[1];
			mz += p[2];

			
		}
		// length of points
		int l = points.size();
		return (new float[] {mx/l,my/l,mz/l});
	}
	
	private float distance_sqrt(float[] u, float[] v)
	{
		// returns squared distance of vectors u and v
        return (float) Math.sqrt((u[0]-v[0])*(u[0]-v[0])+(u[1]-v[1])*(u[1]-v[1])+(u[2]-v[2])*(u[2]-v[2]));
	}
	
	private float[] unit_vector(float[] v)
	{
		// returns a unit vector with direction given by v
		float norm = 1.0f / this.distance_sqrt(v, this.zero);
		float[] out = {norm*v[0], norm*v[1], norm*v[2]};
		return out;
	}
	
	
	private float deg_to_rad(float angle)
	{
		return 2.0f * PI * angle / 360.0f;
	}
	
	private float rad_to_deg(float angle)
	{
		return (angle*360.0f) / (2.0f * PI);
	}

	

}
