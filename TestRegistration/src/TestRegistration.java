import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;

import javax.imageio.ImageIO;

import Jama.Matrix;

import ij.IJ;
import ij.ImagePlus;

public class TestRegistration {

	public static void main(String[] args) {
		TestRegistration tr = new TestRegistration();
		
		tr.work("C:\\Users\\Alberti5\\Desktop\\Projects\\Concatenated Stacks.tif");
	}
	
	private void work(String stackFilename) {
		
		ImagePlus imp = new ImagePlus(stackFilename);
		BufferedImage target = imp.getBufferedImage();
		try {
			ImageIO.write(target, "PNG", new File("C:/temp/target.png"));
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		int w = target.getWidth();
		int h = target.getHeight();
		
		int[] xLandmarks = new int[3];
		int[] yLandmarks = new int[3];
		
		xLandmarks[0] = w/8;
		yLandmarks[0] = 7*h/8;
		xLandmarks[1] = 7*w/8;
		yLandmarks[1] = 7*h/8;
		xLandmarks[2] = w/2;
		yLandmarks[2] = h/8;
		
		DecimalFormat df = new DecimalFormat("000");
		for (int i=1; i<imp.getStackSize(); i++) {
			imp.setSlice(i);
			BufferedImage source = imp.getBufferedImage();
			try {
				ImageIO.write(source, "PNG", new File("C:/temp/source.png"));
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			Matrix m = findAffineTransformMatrix("C:/temp/target.png", "C:/temp/source.png", w, h, xLandmarks, yLandmarks);
			System.out.println("Frame " + i + " affine transformation matrix:");
			m.print(7, 3);
			AffineTransform at = new AffineTransform(m.get(0, 0), m.get(1, 0), m.get(0, 1), m.get(1, 1), m.get(0, 2), m.get(1, 2));
			
			BufferedImage outputImage = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
			Graphics2D g = outputImage.createGraphics();
			g.drawImage(source, at, null);
			g.dispose();
			try {
				ImageIO.write(outputImage, "PNG", new File("C:/temp/frame_"+df.format(i)+".png"));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			
		}		
	}

	 private Matrix findAffineTransformMatrix(String sourceImageFilename, String targetImageFilename, int w, int h, int[] xlandmarks, int[] ylandmarks) {
	        Matrix m = null;
	        if (sourceImageFilename.equals(targetImageFilename)) {
	            m = new Matrix(2, 3);
	            m.set(0, 0, 1);
	            m.set(0, 1, 0);
	            m.set(0, 2, 0);
	            m.set(1, 0, 0);
	            m.set(1, 1, 1);
	            m.set(1, 2, 0);
	            return m;
	        }
	        try {
	            Object myTurboRegObject = IJ.runPlugIn("TurboReg_", "-align -file \""+sourceImageFilename+
	                    "\" 0 0 "+(w-1)+" "+(h-1)+" -file \""+targetImageFilename+"\" 0 0 "+(w-1)+" "+(h-1)+" -rigidBody "+
	                    xlandmarks[0]+" "+ylandmarks[0]+" "+xlandmarks[0]+" "+ylandmarks[0]+" "+
	                    xlandmarks[1]+" "+ylandmarks[1]+" "+xlandmarks[1]+" "+ylandmarks[1]+" "+
	                    xlandmarks[2]+" "+ylandmarks[2]+" "+xlandmarks[2]+" "+ylandmarks[2]+" -hideOutput");
	            Method method = myTurboRegObject.getClass().getMethod("getSourcePoints");
	            double[][] sourcePoints = (double[][])method.invoke(myTurboRegObject);
	            method = myTurboRegObject.getClass().getMethod("getTargetPoints");
	            double[][] targetPoints = (double[][])method.invoke(myTurboRegObject);
	            
	            // Source points, refined to match target points
	            Matrix uv = new Matrix(2, 3);
	            uv.set(0, 0, sourcePoints[0][0]);
	            uv.set(1, 0, sourcePoints[0][1]);
	            uv.set(0, 1, sourcePoints[1][0]);
	            uv.set(1, 1, sourcePoints[1][1]);
	            uv.set(0, 2, sourcePoints[2][0]);
	            uv.set(1, 2, sourcePoints[2][1]);
	    //        System.out.println("UV:");
	    //        uv.print(7, 2);

	            // The target points, extended matrix format
	            Matrix xy = new Matrix(3, 3);
	            xy.set(0, 0, targetPoints[0][0]);
	            xy.set(1, 0, targetPoints[0][1]);
	            xy.set(2, 0, 1);
	            xy.set(0, 1, targetPoints[1][0]);
	            xy.set(1, 1, targetPoints[1][1]);
	            xy.set(2, 1, 1);
	            xy.set(0, 2, targetPoints[2][0]);
	            xy.set(1, 2, targetPoints[2][1]);
	            xy.set(2, 2, 1);            
	    //        System.out.println("XY:");
	    //        xy.print(7, 2);

	            // Find affine transform matrix
	            m = uv.times(xy.inverse());
//	            m.print(7, 3);
	            
	        } catch (Exception ex) {
	            ex.printStackTrace();
	        }
	        return m;
	    }    
}
