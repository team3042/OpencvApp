package org.usfirst.frc.team3042.goalrecognition;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

//Code adapted from team254 on github

public class ADBTestConnection {
	public static int ROBOT_PORT = 3042;
	public static String ROBOT_HOST = "localhost";
	private static Socket socket;
	
	static public int connect(){
		try {
			socket = new Socket("",ROBOT_PORT);
			socket.setReuseAddress(true);
		} catch (UnknownHostException e) {
			//If the IP address cannot be resolved
			e.printStackTrace();
			return 1;
		} catch (IOException e) {
			//If an error occurs while creating a socket
			e.printStackTrace();
			return 2;
		}
		
		if(socket.isConnected()){
			return 3; 
		}
		return 0;
	}
	
	public static void endex(){
		if(socket!=null){
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	int i = 0;
	
	public void runSomething(){
		i = getIValue();
	}
	
	public int getIValue(){
		return 5;
	}
}
