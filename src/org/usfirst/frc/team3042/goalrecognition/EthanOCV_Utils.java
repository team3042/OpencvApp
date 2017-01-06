package org.usfirst.frc.team3042.goalrecognition;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.MatOfPoint;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class EthanOCV_Utils {
	
	//Outputting an image with overlaid contours and convex hull on target
	public static void outputOverlayImage(Mat contoursFrame, Point[] targetConvexHull) {
		//Overlaying target onto original image
		Imgproc.line(contoursFrame, targetConvexHull[0], targetConvexHull[1], new Scalar(255, 255, 255), 2);
		Imgproc.line(contoursFrame, targetConvexHull[1], targetConvexHull[2], new Scalar(255, 255, 255), 2);
		Imgproc.line(contoursFrame, targetConvexHull[2], targetConvexHull[3], new Scalar(255, 255, 255), 2);
		Imgproc.line(contoursFrame, targetConvexHull[3], targetConvexHull[0], new Scalar(255, 255, 255), 2);
	}
	
	//Apply an hsv filter
	public static Mat filterImageHSV(Mat image, Scalar lowerHSVBound, Scalar upperHSVBound) {
    	Mat hsvFrame = new Mat();
		Mat filteredFrame = new Mat();
		
		//Converting to HSV and filtering to a binary image
		Imgproc.cvtColor(image, hsvFrame, Imgproc.COLOR_BGR2HSV);
		Core.inRange(hsvFrame, lowerHSVBound, upperHSVBound, filteredFrame);
		filteredFrame.convertTo(filteredFrame, CvType.CV_8UC1);
		
		//An attempt at memory management
		hsvFrame.release();
		
		return filteredFrame;
    }
	
	public static Mat erodeImage(Mat image) {
		Mat output = new Mat();
		Imgproc.erode(image, output, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2,2)));
		
		return output;
	}
	
	public static Mat dilateImage(Mat image) {
		Mat output = new Mat();
		Imgproc.dilate(image, output, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2,2)));
		
		return output;
	}
	
	public static List<MatOfPoint> getContours(Mat image) {
    	List<MatOfPoint> contours = new ArrayList<MatOfPoint>();    
	    Imgproc.findContours(image, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
	    
	    return contours;
    }
	
	//Calculating the convex hull of a roughly rectangular contour
	public static Point[] calculateConvexHull(MatOfPoint contour) {
		Point[] targetPoints = contour.toArray();
		Point[] convexHull = new Point[4];
		convexHull[0] = new Point(10000, 10000);
		convexHull[1] = new Point(0, 10000);
		convexHull[2] = new Point(0, 0);
		convexHull[3] = new Point(10000, 0);

		//Iterating through all points in the contour to find farthest in each direction
		for(int i = 0; i < targetPoints.length; i++) {
			Point currentPoint = targetPoints[i];
			if (convexHull[0].x + convexHull[0].y > currentPoint.x + currentPoint.y) convexHull[0] = currentPoint;
			if (convexHull[1].y - convexHull[1].x > currentPoint.y - currentPoint.x) convexHull[1] = currentPoint;
			if (convexHull[2].x + convexHull[2].y < currentPoint.x + currentPoint.y) convexHull[2] = currentPoint;
			if (convexHull[3].x - convexHull[3].y > currentPoint.x - currentPoint.y) convexHull[3] = currentPoint;
		}

		return convexHull;
	}
	
	public static MatOfPoint processContours(List<MatOfPoint> contours, Mat stencil) {
		double[] similarities = new double[contours.size()];
		for(int i = 0; i < contours.size(); i++) {
			MatOfPoint currentContour = contours.get(i);
						
			//Filtering out small contours
			if(Imgproc.contourArea(currentContour) > 400) {
				//Calculating similarity to the u shape of the goal
				double similarity = Imgproc.matchShapes(currentContour, stencil, Imgproc.CV_CONTOURS_MATCH_I3, 0);
				//System.out.println(similarity);
				if(similarity < 20) {
					similarities[i] = similarity;
				}
				else similarities[i] = 1000;
			}
			else {
				similarities[i] = 1000;
			}
		}
		
		//Finding 2 most similar of the contours, lower similarity is better
		//2 targets found as up to two goals could be in vision
		int mostSimilarGoals[] = {-1, -1};
		for(int i = 0; i < similarities.length; i++) {
			if(similarities[i] != 1000) {
				if(similarities[i] < ((mostSimilarGoals[1] == -1)? 1000: similarities[mostSimilarGoals[1]])) {
					if(similarities[i] < ((mostSimilarGoals[0] == -1)? 1000: similarities[mostSimilarGoals[0]])) {
						mostSimilarGoals[1] = mostSimilarGoals[0];
						mostSimilarGoals[0] = i;
					}
					else {
						mostSimilarGoals[1] = i;
					}
				}
			}
		}
		
		//Find widest of the goals if 2 were detected
		int mostSimilar = 0;
		if(mostSimilarGoals[1] != -1) {
			Point[][] convexHulls = {calculateConvexHull(contours.get(mostSimilarGoals[0])),
					calculateConvexHull(contours.get(mostSimilarGoals[1]))};
			double[] widths = {convexHulls[0][2].y - convexHulls[0][1].y,
					convexHulls[1][2].y - convexHulls[1][1].y};
			
			mostSimilar = (widths[0] > widths[1])? 0 : 1;
		}
			
		MatOfPoint targetContour;
		if(mostSimilarGoals[mostSimilar] == -1) {
			targetContour = new MatOfPoint();
		}
		else {
			targetContour = contours.get(mostSimilarGoals[mostSimilar]);
		}
		
		return targetContour;
	}
	
	
	
}
