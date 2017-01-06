package org.usfirst.frc.team3042.goalrecognition;

import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import org.usfirst.frc.team3042.goalrecognition.R;
import org.opencv.core.Core;
import org.opencv.core.CvType;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class OpencvActivity extends Activity implements CvCameraViewListener2{
	
	//Gets the frame from the camera, gives it to opencv to mess around with, then sends to screen?
	private CameraBridgeViewBase mOpenCvCameraView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Keeps screen from darkening, or going to sleep
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		setContentView(R.layout.activity_opencv);
		
		//Set up the communication between the camera and opencv
		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.cameraView);
        mOpenCvCameraView.setCvCameraViewListener(this);
        
        //Create a connection button
        final Button connectionButton = (Button) findViewById(R.id.TestConnect);
        connectionButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int result = ADBTestConnection.connect();
                if(result==3){
                	//connectionButton.setTextColor(Color.GREEN);
                	connectionButton.setText("Connected!");
                }else if(result == 1){
                	//connectionButton.setTextColor(Color.RED);
                	connectionButton.setText("Unknown Host!");
                }else if(result == 2){
                	//connectionButton.setTextColor(Color.BLUE);
                	connectionButton.setText("Error Creating Socket!");
                }else{
                	//connectionButton.setTextColor(Color.MAGENTA);
                	connectionButton.setText("No Idea!");
                }
                ADBTestConnection.endex();
            }
        });
	}
	
	public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
	
	@Override
	public void onPause()
	{
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}
	
	@Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
            case LoaderCallbackInterface.SUCCESS:
            {
                mOpenCvCameraView.enableView();
            } break;
            default:
            {
                super.onManagerConnected(status);
            } break;
            }
        }
    };
    
	//Vars for imagerecognition
	private static Mat stencil;
	private static Mat contoursFrame;
	
	private static Scalar lowerHSVBound;
	private static Scalar upperHSVBound;
	
	private static Mat rgba;
	private static Mat filteredFrame;
	private static Mat erodedFrame;
	private static Mat dilatedFrame;
	private static List<MatOfPoint> contours;
	private static Point[] targetConvexHull;
	private static MatOfPoint target;
	@Override
	public Mat onCameraFrame(CvCameraViewFrame frame) {
		
		//An attempt at memory management
		stencil.release();
		contoursFrame.release();
		rgba.release();
		filteredFrame.release();
		erodedFrame.release();
		dilatedFrame.release();
		for(MatOfPoint p : contours){
			p.release();
		}
		target.release();
		//
		
		//Get the rgba image from the camera stored in a matrix
		rgba = frame.rgba(); //CV_8UC4 432 * 768
		
		//Manipulate the rgba image
		filteredFrame = EthanOCV_Utils.filterImageHSV(rgba,lowerHSVBound,upperHSVBound);
		 
		erodedFrame = EthanOCV_Utils.erodeImage(filteredFrame);
		
		dilatedFrame = EthanOCV_Utils.dilateImage(erodedFrame);
		
		contours = EthanOCV_Utils.getContours(dilatedFrame);
		
		contoursFrame = dilatedFrame.clone(); //CV_8UC1
		
	    Imgproc.cvtColor(contoursFrame, contoursFrame, Imgproc.COLOR_GRAY2BGR); //CV_8UC3
	    
	    Imgproc.drawContours(contoursFrame, contours, -1, new Scalar(255, 255, 0), 1);
	    
	    //Finding most similar contour to the desired target
	    target = EthanOCV_Utils.processContours(contours, stencil);
	    
	    //Creating a convex hull around the target and displaying it
	    targetConvexHull = EthanOCV_Utils.calculateConvexHull(target);
	    
	    EthanOCV_Utils.outputOverlayImage(contoursFrame, targetConvexHull); //432 *768
		
	    Imgproc.cvtColor(contoursFrame, contoursFrame, Imgproc.COLOR_BGR2BGRA);
	    
	    Core.bitwise_or(contoursFrame, rgba, rgba);
	    
		
		return rgba;
	}

	@Override
	public void onCameraViewStarted(int arg0, int arg1) {
		// TODO Auto-generated method stub
        //Creating u shape to match with the goal
        stencil = new Mat(8, 1, CvType.CV_32SC2);
        stencil.put(0, 0, new int[]{/*p1*/32, 0, /*p2*/ 26, 76, /*p3*/ 184, 76, /*p4*/ 180, 0, /*p5*/ 203, 0, 
        		/*p6*/ 212, 100, /*p7*/ 0, 100, /*p8*/ 9, 0});
        
        //Set up the hsv filters
        lowerHSVBound = new Scalar(0, 62, 57, 0);
        upperHSVBound = new Scalar(95, 255, 255, 0);
	}

	@Override
	public void onCameraViewStopped() {
		// TODO Auto-generated method stub
		if(stencil != null)
			stencil.release();
		
		stencil = null;
	}
}
