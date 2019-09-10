import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import ij.ImagePlus;
import ij.gui.ProgressBar;

public class Canvas implements ActionListener, MouseListener, DropTargetListener, ProgressListener {
	
	ImagePanel panel = new ImagePanel(); //Panel containing the image.
	JFrame frame = new JFrame(); //Constructs the screen
//	JButton load = new JButton("Import Image"); //Button to import image
	JButton accept = new JButton("Accept"); //Button after selected 3 points
	JTextField filename = new JTextField ("Drag File Here"); //TextField to enter file name
	JTextField startslice = new JTextField ("Enter Starting Slice"); // TextField to enter which slice the program will start on
	Container south = new Container(); //Contains the Textfield
	Container north = new Container(); //Contains the accept and load buttons
	Container west = new Container(); //Contains the node button
	BufferedImage image = null; //Imported image (that will be later assigned)
	JProgressBar progressBar;
		
	int[] xLandmarks = new int[3];
	int[] yLandmarks = new int[3];
	
	public Canvas() {
		//Frame setup
		frame.setSize(2400, 1800);
		frame.setLayout(new BorderLayout());
		frame.add(panel, BorderLayout.CENTER);
		
		//Container for east side buttons
		north.setLayout(new FlowLayout());
		progressBar = new JProgressBar();
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);
		//Background color white, progress bar is green.
		progressBar.setBackground(Color.WHITE);
		progressBar.setForeground(Color.GREEN);
		progressBar.setPreferredSize(new Dimension(2000, 80));
		north.add(progressBar);		
		frame.add(north, BorderLayout.NORTH);

		
		//Container for Textfield
		FlowLayout layout = new FlowLayout();
		layout.setHgap(30);
		south.setLayout(layout);
		
		south.add(filename);
		filename.setEditable(false);
		south.add(startslice);
		south.add(accept);
		accept.addActionListener(this);
		frame.add(south, BorderLayout.SOUTH);
		
		
		//Container with ImagePanel
		frame.add(panel, BorderLayout.CENTER);
		panel.addMouseListener(this);
		
		//Last steps
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		DropTarget dt = new DropTarget(filename, this);
		
		//ProgressBar		
	}

	public static void main(String[] args) {
		//Instantiate Canvas
		Canvas c = new Canvas();
	}
	
	
	//Importing an image
	public void loadImage() {
		String imagename = filename.getText();
		try {
			ImagePlus imp = new ImagePlus(imagename);
			//Removes the bottom bezel of the image as well as a few pixels on the left side of the image.
			image = imp.getBufferedImage().getSubimage(0, 0, imp.getWidth()-10, imp.getHeight()-200);
			panel.setImage(image);
		} catch (Exception e) { 
			e.printStackTrace();
		}
		frame.repaint();
	}
	
	public void mouseReleased(MouseEvent e) {
		//Three landmarks to create triangle
		Point p = new Point(e.getX(), e.getY());
		//Display the coordinates
		System.out.println(e.getX() + ", " + e.getY());
		if (panel.getLandmarks().size() == 3) {
			panel.clearLandmarks();
		}
		else if (panel.getLandmarks().size() == 0) {
			xLandmarks[0] = e.getX();
			yLandmarks[0] = e.getY();
		}
		else if (panel.getLandmarks().size() == 1) {
			xLandmarks[1] = e.getX();
			yLandmarks[1] = e.getY();
		}
		else if (panel.getLandmarks().size() == 2) {
			xLandmarks[2] = e.getX();
			yLandmarks[2] = e.getY();
		}
		for (int i=0; i<xLandmarks.length; i++) {
			xLandmarks[i] = (int)(xLandmarks[i]/panel.getScale()+0.5F);
			yLandmarks[i] = (int)(yLandmarks[i]/panel.getScale()+0.5F);
		}
		panel.addLandmark(p);
		frame.repaint();
		//System.out.println(xLandmarks[0]);
	}
	
	public void actionPerformed(ActionEvent e) {
		int startingslice = Integer.parseInt(startslice.getText());
		if(e.getSource().equals(accept)) {
			if (panel.getLandmarks().size() < 3) {
				JOptionPane.showMessageDialog(frame, "Oops, you need 3 landmarks!");
				return;
			}
			//Choose which slice/frame to start program on
			ImageRegistrationFinder irf = new ImageRegistrationFinder(filename.getText(), startingslice - 1, xLandmarks, yLandmarks);
			irf.addProgressListener(this);
			Thread t = new Thread(irf);
			t.start();
			// This timer is set so that we can re-enable the accept button
			Timer timer = new Timer();
			timer.schedule(new MyTimerTask(irf), 5000, 5000);
			//Disable button
			accept.setEnabled(false);
			//Added gray highlighting
			accept.setBackground(Color.LIGHT_GRAY);
			}
	}
	
	public void updateProgress(final int percent) {
//		System.out.println(percent);
//		SwingUtilities.invokeLater(new Runnable() {
//			public void run() {
				if (progressBar != null) {
					progressBar.setValue(percent);
				}
//			}
//		});
	}
	
	private class MyTimerTask extends TimerTask {
		ImageRegistrationFinder irf = null;
		public MyTimerTask(ImageRegistrationFinder irf) {
			this.irf = irf;
		}
		public void run() {
			if (!irf.isRunning()) {
				cancel();
				//Enabled button
				accept.setEnabled(true);
				JOptionPane.showMessageDialog(frame, "Alignment complete.");
				//Removed highlighting
				accept.setBackground(null);
			}		
		}
	}
	
	//Unused methods
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	
	public void drop(DropTargetDropEvent dtde) {
		filename.setBackground(Color.WHITE);
		DropTarget dt = (DropTarget)dtde.getSource();
		JTextField textField = (JTextField)dt.getComponent();
		
		Transferable tr = dtde.getTransferable();
		DataFlavor[] flavors = tr.getTransferDataFlavors();
		for (int i=0; i<flavors.length; i++) {
			if (flavors[i].isFlavorJavaFileListType()) {
				dtde.acceptDrop(DnDConstants.ACTION_COPY);
				try {
					List list = (List)tr.getTransferData(flavors[i]);
					if (list.get(0) instanceof File) {
						File f = (File)list.get(0);
						if (f.getName().endsWith(".tif") || f.getName().endsWith(".TIF")) {
							filename.setText(f.getAbsolutePath());
							Thread t = new Thread(new Runnable() {
								public void run() {
									loadImage();
								}
							});
							t.start();
							dtde.dropComplete(true);
							return;
						} else {
							dtde.dropComplete(false);
						}
					}
				} catch (Exception ex) {
					System.err.println(ex);
				}
			}
		}
	}
	
	public void dragEnter(DropTargetDragEvent dtde) {
		filename.setBackground(Color.green);}

	public void dragExit(DropTargetEvent dte) {
		filename.setBackground(Color.WHITE);}

	public void dragOver(DropTargetDragEvent dtde) {}
	public void dropActionChanged(DropTargetDragEvent dtde) {}
}
