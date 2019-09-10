import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JProgressBar;

import Jama.Matrix;
import ij.IJ;
import ij.ImagePlus;

public class ImageRegistrationFinder implements Runnable {
	String imageFilename = null;
	int targetIndex = 0;
	private int[] xLandmarks;
	private int[] yLandmarks;
	private boolean running = false;
	private final ArrayList<ProgressListener> listeners = new ArrayList<ProgressListener>();
	
	public ImageRegistrationFinder(String imageFilename, int targetIndex, int[] xLandmarks, int[] yLandmarks) {
		this.imageFilename = imageFilename;
		this.targetIndex = targetIndex;
		this.xLandmarks = xLandmarks;
		this.yLandmarks = yLandmarks;
	}
	
	public void addProgressListener(ProgressListener listener) {
		listeners.add(listener);
	}
	
	public void removeProgressListener(ProgressListener listener) {
		listeners.remove(listener);
	}
	
	public void run() {
		running = true;
		ImagePlus imp = new ImagePlus(imageFilename);
		imp.setSlice(targetIndex);
		BufferedImage target = imp.getBufferedImage().getSubimage(0, 0, imp.getWidth()-10, imp.getHeight()-200);
		double[] pixels = target.getRaster().getSamples(0, 0, target.getWidth(), target.getHeight(), 0, (double[])null);
		double min = Double.MAX_VALUE;
		double max = -1;
		for (int i=0; i<pixels.length; i++) {
			if (pixels[i] < min) {
				min = pixels[i];
			}
			if (pixels[i] > max) {
				max = pixels[i];
			}
		}
		double r = 255.0/(max-min);
		for (int i=0; i<pixels.length; i++) {
			pixels[i] = (pixels[i]-min)*r;
		}
		// Set them back to the picture
		target.getRaster().setSamples(0, 0, target.getWidth(), target.getHeight(), 0, pixels);
		try {
			//frame_000 is target.
			ImageIO.write(target, "PNG", new File("C:/temp/frame_000.png"));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		int w = target.getWidth();
		int h = target.getHeight();
//		int[] xLandmarks = new int[3];
//		int[] yLandmarks = new int[3];
		xLandmarks[0] = 100;
		yLandmarks[0] = 100;
		xLandmarks[1] = w-100;
		yLandmarks[1] = 100;
		xLandmarks[2] = w/2;
		yLandmarks[2] = h-100;
		
		String targetImageFilename = "C:/temp/frame_000.png";
		
		DecimalFormat df = new DecimalFormat("000");
		double percentInc = 100.0/imp.getStackSize();
		double percent = (targetIndex+1)*percentInc;
		for (int i=targetIndex+1; i<imp.getStackSize(); i++) {
			imp.setSlice(i);
			BufferedImage source = imp.getBufferedImage().getSubimage(0, 0, imp.getWidth()-10, imp.getHeight()-200);
			pixels = source.getRaster().getSamples(0, 0, target.getWidth(), target.getHeight(), 0, pixels);
			min = Double.MAX_VALUE;
			max = -1;
			for (int j=0; j<pixels.length; j++) {
				if (pixels[j] < min) {
					min = pixels[j];
				}
				if (pixels[j] > max) {
					max = pixels[j];
				}
			}
			r = 255.0/(max-min);
			for (int j=0; j<pixels.length; j++) {
				pixels[j] = (pixels[j]-min)*r;
			}
			// Set them back to the picture
			source.getRaster().setSamples(0, 0, source.getWidth(), source.getHeight(), 0, pixels);			
			try {
				ImageIO.write(source, "PNG", new File("C:/temp/source.png"));
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			Matrix m = findAffineTransformMatrix(targetImageFilename, "C:/temp/source.png", target.getWidth(), target.getHeight(), xLandmarks, yLandmarks);
			System.out.println("Frame "+ i +" affine transformation matrix:");
			m.print(7, 3);
			AffineTransform at = new AffineTransform(m.get(0, 0), m.get(1, 0), m.get(0, 1), m.get(1, 1), m.get(0, 2), m.get(1, 2));
			
			BufferedImage outputImage = new BufferedImage(target.getWidth(), target.getHeight(), BufferedImage.TYPE_BYTE_GRAY);	
			Graphics2D g = outputImage.createGraphics();
			g.drawImage(source, at, null);
			//breakpoint here
			g.dispose();
			try {
				File f = new File("C:/temp/frame_"+df.format(i)+".png");
				ImageIO.write(outputImage, "PNG", f);
				targetImageFilename = f.getAbsolutePath();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			
			targetImageFilename = "C:/temp/frame_"+df.format(i)+".png";
			for (int j=0; j<listeners.size(); j++) {
				listeners.get(j).updateProgress((int)(percent+0.5));
			}
			percent += percentInc;
		}
		running = false;
		
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
	        //System.out.println("UV:");
	        //uv.print(7, 2);
	
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
	        //System.out.println("XY:");
	        //xy.print(7, 2);
	
	        // Find affine transform matrix
	        m = uv.times(xy.inverse());
	        
	        //m.print(7, 3);
	        
	    } catch (Exception ex) {
	        ex.printStackTrace();
	    }
	    return m;
	}
	 
	 public boolean isRunning() {
		 return running;
	 }
}
