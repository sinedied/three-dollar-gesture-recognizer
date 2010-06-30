package com.myproject;

import java.util.Comparator;

public class Score implements Comparator<Score>{
	public String gid =null;
	public int idnr = 0;
	public float distance = Float.MAX_VALUE;
	public float score = 0.0f;
	
	public  int compare(Score g1, Score g2) {
		// TODO Auto-generated method stub
		int order = -1; // descending (hopefully)
		if (g1.score < g2.score) return -order;
		else if (g1.score == g2.score) return 0;
		else if (g1.score > g2.score) return order;
		else return 0;
	}
	
	public String toString()
	{
		return gid+"\t"+idnr+"\t"+score+"\t"+distance;
	}

}
