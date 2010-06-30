package com.myproject;

public abstract class GestureRecognizer {
	
	public abstract String recognize_gesture(Gesture G);
	
	
	public abstract Gesture prepare_gesture_for_library(Gesture g);

	
}
