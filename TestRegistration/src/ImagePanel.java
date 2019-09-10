//Check number rat34

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.JPanel;

public class ImagePanel extends JPanel {
	private static final Color LANDMARK_COLOR = Color.RED;
	private static final int LANDMARK_SIZE = 21;
	
	BufferedImage newImage;
	BufferedImage scaledImage;
	int width;
	int height;
	private float scale = 1;
	private ArrayList<Point> landmarks = new ArrayList<Point>();
	
	public ImagePanel() {
		super();
	}
	
	public void paintComponent(Graphics g) {
		g.drawImage(scaledImage, 0, 0, null);
		
		Color savedColor = g.getColor();
		g.setColor(LANDMARK_COLOR);
		for (int i=0; i<landmarks.size(); i++) {
			Point p = landmarks.get(i);			
			g.fillOval(p.x-(LANDMARK_SIZE/2), p.y-(LANDMARK_SIZE/2), LANDMARK_SIZE, LANDMARK_SIZE);
		}
		g.setColor(savedColor);
		g.dispose();
	}
	
	public void setImage(BufferedImage image) {
		newImage = image;
		if (newImage.getWidth() > getWidth() || newImage.getHeight() > getHeight()) {
			if (newImage.getHeight() > newImage.getWidth()) {
				// Scale on height;
				scale = (float)getHeight()/newImage.getHeight();
				
			} else {
				// Scale on width
				scale = (float)getWidth()/newImage.getWidth();
			}
			// Scaling original image according to the scale factor
			int newWidth = (int)(newImage.getWidth()*scale+0.5F);
			int newHeight = (int)(newImage.getHeight()*scale+0.5F);
			scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_BYTE_GRAY);
			Graphics2D g = scaledImage.createGraphics();
			g.drawImage(newImage, 0, 0, newWidth, newHeight, 0, 0, newImage.getWidth(), newImage.getHeight(), null);
			g.dispose();
		} else {
			scaledImage = newImage;
		}
	}
	
	public float getScale() {
		return scale;
	}
	
	public ArrayList<Point> getLandmarks() {
		return landmarks;
	}
	
	public void addLandmark(Point p) {
		landmarks.add(p);
	}
	
	public void clearLandmarks() {
		landmarks.clear();
	}
}