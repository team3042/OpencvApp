package org.usfirst.frc.team3042.goalrecognition;

import org.spectrum3847.RIOdroid.AndroidAccessory;

public class RIO_COM {
	private static String manufacterer = "";
	private static String modelName = "";
	private static String description = "";
	private static String version = "";
	private static String uri = "";
	private static String serialNumber = "";
	
	private static AndroidAccessory accessor;
	
	public static void createDefaultAccessor(){
		accessor = new AndroidAccessory(manufacterer, modelName, description, version, uri, serialNumber);
	}
	
	public void test(){
	}
}
