package com.jpaulmorrison.graphics;

//import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
//import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;







import com.jpaulmorrison.graphics.DrawFBP.FileChooserParms;
import com.jpaulmorrison.graphics.DrawFBP.GenLang;
import com.jpaulmorrison.graphics.DrawFBP.Side;

public class Diagram {
	// LinkedList<Block> blocks;
	HashMap<Integer, Block> blocks;

	HashMap<Integer, Arrow> arrows;

	File diagFile;

	DrawFBP driver;
	int tabNum = -1;
	DrawFBP.SelectionArea area; 

	String title;

	String desc;

	GenLang diagLang;

	boolean changed = false;

	Arrow currentArrow = null;

	int maxBlockNo = 0;

	int maxArrowNo = 0;

	int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;

	int maxX = 0, maxY = 0;

	Block foundBlock;

	Arrow foundArrow;

	boolean clickToGrid;

	int xa, ya;

	// File imageFile = null;

	boolean findArrowCrossing = false;
	Enclosure cEncl = null;
	LinkedList<Arrow> clla = null;

	//String genCodeFileName;
	
	JPopupMenu jpm;
	//String targetLang;
	
	FileChooserParms[] fCPArr = new FileChooserParms[7];
	String[] filterOptions = {"", "All (*.*)"};
	//Rectangle curMenuRect = null;
	
	FoundPoint arrowRoot = null;
	
	Diagram(DrawFBP drawFBP) {
		driver = drawFBP;
		//driver.curDiag = this;
		blocks = new HashMap<Integer, Block>();
		arrows = new HashMap<Integer, Arrow>();
		clickToGrid = true;
		driver.grid.setSelected(clickToGrid);
		//file = null;
		diagLang = driver.defaultCompLang;
		for (int i = 0; i < fCPArr.length; i++){
			fCPArr[i] = driver.fCPArray[i];
		}
		
		fCPArr[DrawFBP.CLASS] = driver.new FileChooserParms("currentClassDir",
				"Select component from class directory", ".class",
				driver.new JavaClassFilter(), "Class files");
		
		fCPArr[DrawFBP.PROCESS] = driver.new FileChooserParms(diagLang.srcDirProp, "Select "
				+ diagLang.showLangs() + " component from directory",
				diagLang.suggExtn, diagLang.filter, "Components: "
						+ diagLang.showLangs() + " " + diagLang.showSuffixes());
		
		fCPArr[DrawFBP.GENCODE] = driver.new FileChooserParms(diagLang.netDirProp,
				"Specify file name for generated code",
				"." + diagLang.suggExtn, diagLang.filter,
				diagLang.showLangs());		
				
	}

	public File open(File f) {
		File file = null;
		String fileString = null;

		if (f != null) 
		file = f;
		else {

			String s = driver.properties.get("currentDiagramDir");
			if (s == null)
				s = System.getProperty("user.home");
			File f2 = new File(s);
			if (!f2.exists()) {
				MyOptionPane.showMessageDialog(driver.frame, "Directory '" + s
						+ "' does not exist - reselect");
				// return null;
				f2 = new File(".");
			}

			MyFileChooser fc = new MyFileChooser(f2, fCPArr[DrawFBP.DIAGRAM]);

			int returnVal = fc.showOpenDialog();

			if (returnVal == MyFileChooser.APPROVE_OPTION)  
				file = new File(fc.getSelectedFile());
			if (file == null)
				return null; 
		}
		
		if (!(hasSuffix(file.getName()))) {
			String name = file.getAbsolutePath();
			name += ".drw";
			file = new File(name);
		}
		if (!(file.exists()))   
			return file;
			//if (file.getName().equals("(empty folder)"))
			//	MyOptionPane.showMessageDialog(driver.frame,
			//			"Invalid file name: " + file.getAbsolutePath());
			//else 
			//	MyOptionPane.showMessageDialog(driver.frame,
			//			"File does not exist: " + file.getAbsolutePath());

			//return null;
		//}
		if (null == (fileString = readFile(file))) {
			MyOptionPane.showMessageDialog(driver.frame, "Unable to read file "
					+ file.getName());
			return null;
		}
		 
		File currentDiagramDir = file.getParentFile();
		driver.properties.put("currentDiagramDir",
				currentDiagramDir.getAbsolutePath());
		driver.propertiesChanged = true;

		if (!(file.getName().toLowerCase().endsWith(".drw"))) {
			if (diagramIsOpen(file.getAbsolutePath()))
				return null;
			diagFile = file;
			return file;
		}

		title = file.getName();
		if (title.toLowerCase().endsWith(".drw"))
			title = title.substring(0, title.length() - 4);
		if (diagramIsOpen(file.getAbsolutePath()))
			return null;
		diagFile = file;
		blocks.clear();
		arrows.clear();
		desc = " ";

		DiagramBuilder.buildDiag(fileString, driver.frame, this);
		// driver.jtp.setRequestFocusEnabled(true);
		// driver.jtp.requestFocusInWindow();
		if (diagLang != null)
			driver.changeLanguage(diagLang);
		return file;

	}

	/* General save function */

	public File genSave(File file, DrawFBP.FileChooserParms fCP, Object contents) {

		boolean saveAs = false;
		File newFile = null;
		String fileString = null;
		//Diagram.FileChooserParms si = curDiag.fCPArray[type];
		if (file == null)
			saveAs = true;
		

		if (saveAs) {

			String suggestedFileName = "";
			// if (f == null) {

			String s = driver.properties.get(fCP.propertyName);
			if (s == null)
				s = System.getProperty("user.home");

			File f = new File(s);
			if (!f.exists()) {
				MyOptionPane.showMessageDialog(driver.frame, "Directory '" + s
						+ "' does not exist - reselect");

				f = new File(System.getProperty("user.home"));
			}
			// } else {

			String fn = "";
			suggestedFileName = "";
			File g = diagFile;  
			if (g != null) {
				fn = g.getName();
				suggestedFileName = s + File.separator + fn;
				int i = suggestedFileName.lastIndexOf(".");
				suggestedFileName = suggestedFileName.substring(0, i)
						+ fCP.fileExt;
			}

			MyFileChooser fc = new MyFileChooser(f, fCP);

			fc.setSuggestedName(suggestedFileName);

			int returnVal = fc.showOpenDialog(saveAs);

			// String s;
			if (returnVal == MyFileChooser.APPROVE_OPTION) {
				newFile = new File(fc.getSelectedFile());
				s = newFile.getAbsolutePath();

				if (s.endsWith("(empty folder)")) {
					MyOptionPane.showMessageDialog(driver.frame, "Invalid file name: "
							+ newFile.getName());
					return null;
				}

				if (newFile.getParentFile() == null) {
					MyOptionPane.showMessageDialog(driver.frame, "Missing parent file for: "
							+ newFile.getName());
					return null;
				}
				if (!(newFile.getParentFile().exists())) {
					MyOptionPane.showMessageDialog(driver.frame, "Invalid file name: "
							+ newFile.getAbsolutePath());
					return null;
				}

				if (s.toLowerCase().endsWith("~")) {
					MyOptionPane.showMessageDialog(driver.frame,
							"Cannot save into backup file: " + s);
					return null;
				}
				File f2 = new File(s);
				if (!(f2.exists()) || f2.isDirectory()) {

					String suff = getSuffix(s);
					if (suff == null)
						newFile = new File(s + fCP.fileExt);
					else {
						// if (!(s.toLowerCase().endsWith(fCP.fileExt))) {
						if (!((driver.new ImageFilter()).accept(new File(s)))) {
							newFile = new File(s.substring(0,
									s.lastIndexOf(suff))
									+ fCP.fileExt.substring(1));
						}
					}
				}

				// newFile.getParentFile().mkdirs();
			}

			if (newFile == null)
				return null;

			if (fCP.fileExt.equals(".drw")
					&& diagramIsOpen(newFile.getAbsolutePath()))
				return null;

			int response;
			if (newFile.exists()) {
				if (newFile.isDirectory()) {
					MyOptionPane.showMessageDialog(driver.frame, newFile.getName()
							+ " is a directory");
					return null;
				}
								
				String t = "Overwrite existing file: " + newFile.getAbsolutePath()
						+ "?";
				 JOptionPane pane = new JOptionPane(t);
				 DrawFBP.applyOrientation(pane);   
			     pane.setOptionType(JOptionPane.OK_CANCEL_OPTION);
			     pane.setMessageType(JOptionPane.QUESTION_MESSAGE);
			     int h = 100;
			     int w = t.length() * driver.fontWidth;
			     pane.setPreferredSize(new Dimension(w, h));
			     JDialog dialog = pane.createDialog(driver.frame, "Confirm Overwrite");			     
			     if (!dialog.isResizable()) {
		             dialog.setResizable(true);
		         }
			     dialog.setVisible(true);
			     Object selectedValue = pane.getValue();
			     if (selectedValue == null)
			    	 return null;
			     response = ((Integer) selectedValue).intValue();
				if (response == -1 || response == JOptionPane.CANCEL_OPTION)
					return null;
			}
			file = newFile;

			if (!(file.exists())) {				
				
				String t = "Create new file: " + newFile.getAbsolutePath()
						+ "?";
				 JOptionPane pane = new JOptionPane(t);
				 DrawFBP.applyOrientation(pane);   
			     pane.setOptionType(JOptionPane.OK_CANCEL_OPTION);
			     pane.setMessageType(JOptionPane.QUESTION_MESSAGE);
			     int h = 100;
			     int w = t.length() * driver.fontWidth;
			     pane.setPreferredSize(new Dimension(w, h));
			     JDialog dialog = pane.createDialog(driver.frame, "Confirm create");
			     if (!dialog.isResizable()) {
		             dialog.setResizable(true);
		         }
			     dialog.setVisible(true);
			     Object selectedValue = pane.getValue();
			     if (selectedValue == null)
			    	 return null;
			     response = ((Integer) selectedValue).intValue();
				if (response == -1 || response == JOptionPane.CANCEL_OPTION)
					return null;
			}
		}

	 
		if (fCP == fCPArr[DrawFBP.IMAGE]) {
			Path path = file.toPath();
			try {
				Files.deleteIfExists(path);
				file = null;
				file = path.toFile();

				String suff = getSuffix(file.getAbsolutePath());
				BufferedImage bi = (BufferedImage) contents;

				ImageIO.write(bi, suff, file);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//return file;
		} else {
			if (fCP.fileExt.equals(".drw")) {
				fileString = readFile(file);
				diagFile = file;
				// if (file == null){
				// int i = 0;
				// }
				if (fileString != null) {
					String s = file.getAbsolutePath();
					File oldFile = file;
					file = new File(s.substring(0, s.length() - 1) + "~");
					writeFile(file, fileString);
					file = oldFile;
				}
				fileString = buildFile();
				//file = diagFile;
			} else
				fileString = (String) contents;

			writeFile(file, fileString);
			//return diagFile;
		}

		return file;
	}
	
	
	// returns false if CANCEL option chosen
	public boolean askAboutSaving() {

		String fileString = null;
		String name;
		if (changed) {

			if (title == null)
				name = "(untitled)";
			else {
				name = title;
				if (!name.toLowerCase().endsWith(".drw"))
					name += ".drw";
			}

			int answer = MyOptionPane.showConfirmDialog(driver.frame, 
					 "Save changes to " + name + "?", "Save changes",
					JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.INFORMATION_MESSAGE);
			File file = null;
			if (answer == JOptionPane.YES_OPTION) {
				// User clicked YES.
				if (diagFile == null) { // choose file

					file = genSave(null, fCPArr[DrawFBP.DIAGRAM], name);
					if (file == null) {
						MyOptionPane.showMessageDialog(driver.frame,
								"File not saved");
						return false;
					}
				} else {
					file = diagFile;
					fileString = buildFile();

					writeFile(file, fileString);

				}

				File currentDiagramDir = file.getParentFile();
				driver.properties.put("currentDiagramDir",
						currentDiagramDir.getAbsolutePath());
				driver.propertiesChanged = true;

				// }
				return true;
			}
			if (answer == JOptionPane.NO_OPTION)
				// User clicked NO.
				return true;
			else
				return false;

		}
		return true;

	}

	public String buildFile() {
		String fileString = "<?xml version=\"1.0\"?> \n ";
		fileString += "<drawfbp_file " +
"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
"xsi:noNamespaceSchemaLocation=\"https://github.com/jpaulm/drawfbp/blob/master/lib/drawfbp_file.xsd\">";

		if (desc != null)
			fileString += "<net> <desc>" + desc + "</desc> ";

		// if (title != null)
		// fileString += "<title>" + title + "</title> ";

		if (diagLang != null)
			fileString += "<complang>" + diagLang.label + "</complang> ";

		// if (genCodeFileName != null)
		// fileString += "<genCodeFileName>" + genCodeFileName
		// + "</genCodeFileName> ";

		
		fileString += "<clicktogrid>" + (clickToGrid?"true":"false") + "</clicktogrid> \n" ;

		fileString += "<blocks>";

		for (Block block : blocks.values()) {
			if (!block.deleteOnSave) { // exclude deleteOnSave blocks
				// String s = block.diagramFileName;
				// block.diagramFileName = DrawFBP.makeRelFileName(
				// s, file.getAbsolutePath());
				fileString += block.serialize();
			}
		}
		fileString += "</blocks> <connections>\n";

		for (Arrow arrow : arrows.values()) {
			if (!arrow.deleteOnSave) // exclude deleteOnSave arrows
				fileString += arrow.serialize();
		}
		fileString += "</connections> ";

		fileString += "</drawfbp_file>";

		return fileString;
	}


	public String readFile(File file) {
		StringBuffer fileBuffer;
		String fileString = null;
		int i;
		try {
			FileInputStream in = new FileInputStream(file);
			InputStreamReader dis = new InputStreamReader(in, "UTF-8");
			fileBuffer = new StringBuffer();
			try {
				while ((i = dis.read()) != -1) {
					fileBuffer.append((char) i);
				}

				in.close();

				fileString = fileBuffer.toString();
			} catch (IOException e) {
				fileString = "";
			}

		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
		return fileString;
	} // readFile

	/**
	 * Use a BufferedWriter, which is wrapped around an OutputStreamWriter,
	 * which in turn is wrapped around a FileOutputStream, to write the string
	 * data to the given file.
	 */

	public boolean writeFile(File file, String fileString) {
		if (file == null)
			return false;
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file), "UTF-8"));
			out.write(fileString);
			out.flush();
			out.close();
		} catch (IOException e) {
			System.err.println("File error writing " + file.getAbsolutePath());
			return false;
		}

		return true;
	} // writeFile
	
	boolean diagramIsOpen(String s) {
		for (int i = 0; i < driver.jtp.getTabCount(); i++) {
			ButtonTabComponent b = (ButtonTabComponent) driver.jtp
					.getTabComponentAt(i);

			Diagram d = b.diag;
			if (d == null)
				continue;
			File f = d.diagFile;
			if (f == null)
				continue;
			if (i == tabNum)
				continue;
			String t = f.getAbsolutePath();
			if (s.equals(t)) {
				File fs = new File(s);
				MyOptionPane.showMessageDialog(driver.frame,
						"File " + fs.getName() + " is open");
				return true;
			}
		}
		return false;
	}
	
	String getSuffix(String s) {
		int i = s.lastIndexOf(File.separator);
		if (i == -1)
			i = s.lastIndexOf("/");
		String t = s.substring(i + 1);
		int j = t.lastIndexOf(".");
		if (j == -1)
			return null;
		else
			return t.substring(j + 1);
	}

	boolean hasSuffix(String s) {
		int i = s.lastIndexOf(File.separator);
		int j = s.substring(i + 1).lastIndexOf(".");
		return j > -1;
	}

	boolean matchArrow(int x, int y, Arrow arrow) {

		int x1 = arrow.fromX;
		int y1 = arrow.fromY;
		int x2, y2;
		if (arrow.bends != null) {
			for (Bend bend : arrow.bends) {
				x2 = bend.x;
				y2 = bend.y;
				if (driver.nearpln(x, y, x1, y1, x2, y2)) {
					return true;
				} else {
					x1 = bend.x;
					y1 = bend.y;
				}
			}
		}

		x2 = arrow.toX;
		y2 = arrow.toY;
		return driver.nearpln(x, y, x1, y1, x2, y2);
	}
	void delArrow(Arrow arrow) {
		LinkedList<Arrow> ll = new LinkedList<Arrow>();
		// go down list repeatedly - until no more arrows to remove
		while (true) {
			boolean found = false;
			for (Arrow arr : arrows.values()) {
				if (arr.endsAtLine && arr.toId == arrow.id) {
					arr.toId = -1;
					ll.add(arr);
					found = true;
					break;
				}
			}
			if (!found)
				break;
		}
		for (Arrow arr : ll) {
			Integer aid = new Integer(arr.id);
			arrows.remove(aid);
		}
		Integer aid = new Integer(arrow.id);
		arrows.remove(aid);
	}

	void delBlock(Block block, boolean choose) {
		if (choose
				&& JOptionPane.YES_OPTION != MyOptionPane.showConfirmDialog( 
						driver.frame, "Do you want to delete this block?", "Delete block",

						 JOptionPane.YES_NO_OPTION))
			return;

		// go down list repeatedly - until no more arrows to remove
		LinkedList<Arrow> ll = new LinkedList<Arrow>();
		while (true) {
			boolean found = false;
			for (Arrow arrow : arrows.values()) {
				if (arrow.fromId == block.id) {
					// arrow.delArrow();
					ll.add(arrow);
					arrow.fromId = -1;
					found = true;
					break;
				}
				if (arrow.toId == block.id) {
					// arrow.delArrow();
					ll.add(arrow);
					arrow.toId = -1;
					found = true;
					break;
				}
			}
			if (!found)
				break;
		}

		for (Arrow arrow : ll)
			delArrow(arrow);

		changed = true;
		Integer aid = new Integer(block.id);
		blocks.remove(aid);
		// changeCompLang();

		driver.frame.repaint();
	}

	void excise(Enclosure enc, int tabno, String diagfn) {

		// *this* is a *new* diagram, which will contain all enclosed blocks and
		// arrows, plus external ports

		clla = new LinkedList<Arrow>(); // crossing arrows

		Diagram oldDiag = enc.diag;
		driver.curDiag = this;

		maxBlockNo = Math.max(oldDiag.maxBlockNo, enc.id); // will be used for
															// new double-lined
															// block

		enc.calcEdges();

		//File file = new File(diagfn);

		findEnclosedBlocksAndArrows(enc);
		for (Block b : enc.llb) {
			blocks.put(new Integer(b.id), b);
			changed = true;
		}

		for (Arrow a : enc.lla) {
			Arrow arr2 = a.makeCopy(this); // delBlock will delete old ones

			arrows.put(new Integer(arr2.id), arr2); // add copy of arrow to new
													// diagram
			changed = true;
		}
		findCrossingArrows(enc);

		// now go through arrows & blocks that were totally enclosed, and delete
		// them from oldDiag
		if (enc.lla != null) {
			for (Arrow arrow : enc.lla) {
				oldDiag.delArrow(arrow);
			}
			enc.lla = null;
		}
		boolean NOCHOOSE = false;
		if (enc.llb != null) {
			for (Block block : enc.llb) {
				oldDiag.delBlock(block, NOCHOOSE);
			}
			enc.llb = null;
		}

		// now go through arrows, looking for unattached ends
		for (Arrow arrow : arrows.values()) {
			Block from = blocks.get(new Integer(arrow.fromId));
			Block to = blocks.get(new Integer(arrow.toId));
			if (from == null) {
				ExtPortBlock eb = new ExtPortBlock(this);
				eb.cx = arrow.fromX - eb.width / 2;
				eb.cy = arrow.fromY;
				eb.type = Block.Types.EXTPORT_IN_BLOCK;
				maxBlockNo++;
				eb.id = maxBlockNo;
				arrow.fromId = eb.id;
				if (enc.subnetPorts != null) {
					for (SubnetPort snp : enc.subnetPorts) {
						if (snp.side == Side.LEFT && arrow.toY == snp.y) {
							eb.substreamSensitive = snp.substreamSensitive;
							eb.description = snp.name;
						}
					}
				}
				blocks.put(new Integer(arrow.fromId), eb);
				eb.calcEdges();
			}
			if (to == null) {
				ExtPortBlock eb = new ExtPortBlock(this);
				eb.cx = arrow.toX + eb.width / 2;
				eb.cy = arrow.toY;
				eb.type = Block.Types.EXTPORT_OUT_BLOCK;
				maxBlockNo++;
				eb.id = maxBlockNo;
				arrow.toId = eb.id;
				if (enc.subnetPorts != null) {
					for (SubnetPort snp : enc.subnetPorts) {
						if (snp.side == Side.RIGHT && arrow.toY == snp.y) {
							eb.substreamSensitive = snp.substreamSensitive;
							eb.description = snp.name;
						}
					}
				}
				blocks.put(new Integer(arrow.toId), eb);
				eb.calcEdges();
			}
		}

		/*
		 * build double-lined block in old diagram
		 */

		Block block = new ProcessBlock(oldDiag);
		// if (block.editDescription(DrawFBP.EDIT_NO_CANCEL)) {

		block.cx = oldDiag.xa;
		block.cy = oldDiag.ya;
		block.calcEdges();
		oldDiag.maxBlockNo++;
		block.id = oldDiag.maxBlockNo;
		oldDiag.blocks.put(new Integer(block.id), block);
		oldDiag.changed = true;
		driver.selBlockP = block;
		block.diagramFileName = diagfn;
		block.isSubnet = true;

		block.description = enc.description;
		enc.description = "Enclosure can be deleted";

		desc = block.description;
		if (desc != null)
			desc = desc.replace('\n', ' ');

		block.cx = enc.cx;
		block.cy = enc.cy;
		block.calcEdges();
		driver.frame.repaint();

		// now go through crossing arrows that were held and reinsert them
		// in old diagram,
		// connecting their unconnected ends...

		Integer aid;
		for (Arrow arrow : clla) {
			aid = new Integer(arrow.id);
			oldDiag.arrows.put(aid, arrow);

			Block from = oldDiag.blocks.get(new Integer(arrow.fromId));
			Block to = oldDiag.blocks.get(new Integer(arrow.toId));
			if (to == null) {
				arrow.toId = block.id;
				Side side = findQuadrant(arrow.fromX, arrow.fromY, block);
				if (side != null) {
					if (side == Side.TOP) {
						arrow.toY = block.cy - block.height / 2;
						arrow.toX = block.cx;
					} else {
						arrow.toX = block.cx - block.width / 2;
						arrow.toY = block.cy;
					}
				}
			}

			if (from == null) {
				arrow.fromId = block.id;
				Side side = findQuadrant(arrow.toX, arrow.toY, block);
				if (side != null) {
					if (side == Side.BOTTOM) {
						arrow.fromY = block.cy + block.height / 2;
						arrow.fromX = block.cx;
					} else {
						arrow.fromX = block.cx + block.width / 2;
						arrow.fromY = block.cy;
					}
				}
			}

		}

	}

	Side findQuadrant(int x, int y, Block b) {

		double yd = b.cy - y;
		double xd = b.cx - x;
		if (xd == 0)
			if (yd > 0)
				return Side.TOP;
			else
				return Side.BOTTOM;

		double s = yd / xd;
		if ((s > 1 || s < -1) && yd > 0)
			return Side.TOP;
		if ((s > 1 || s < -1) && yd < 0)
			return Side.BOTTOM;
		if ((s <= 1 && s >= -1) && xd < 0)
			return Side.LEFT;
		return Side.RIGHT;

	}

	void findEnclosedBlocksAndArrows(Enclosure enc) {
		// look for blocks which are within enclosure
		Diagram oldDiag = enc.diag; // old diagram, saved whenever a block is
									// created
		enc.llb = new LinkedList<Block>();
		for (Block block : oldDiag.blocks.values()) {
			if (block == enc)
				continue;
			if (block.cx - block.width / 2 >= enc.cx - enc.width / 2
					&& block.cx + block.width / 2 <= enc.cx + enc.width / 2
					&& block.cy - block.height / 2 >= enc.cy - enc.height / 2
					&& block.cy + block.height / 2 <= enc.cy + enc.height / 2) {
				enc.llb.add(block); // set aside for action
			}
		}

		// look for arrows connecting blocks which are within enclosure
		enc.lla = new LinkedList<Arrow>();
		for (Arrow arrow : oldDiag.arrows.values()) {

			Block from = oldDiag.blocks.get(new Integer(arrow.fromId));
			Block to = oldDiag.blocks.get(new Integer(arrow.toId));

			if (enc.llb.contains(from) || enc.llb.contains(to))
				enc.lla.add(arrow);
		}
	}

	void findCrossingArrows(Enclosure enc) {
		Diagram oldDiag = enc.diag;
		for (Arrow arrow : oldDiag.arrows.values()) {
			// now test if arrow crosses a boundary
			// if (crosses(arrow, enc, DrawFBP.Side.LEFT)
			// || crosses(arrow, enc, DrawFBP.Side.RIGHT)
			// || crosses(arrow, enc, DrawFBP.Side.TOP)
			// || crosses(arrow, enc, DrawFBP.Side.BOTTOM))
			Block from = oldDiag.blocks.get(new Integer(arrow.fromId));
			Block to = oldDiag.blocks.get(new Integer(arrow.toId));
			if (enc.llb.contains(from) && !(enc.llb.contains(to))
					|| !(enc.llb.contains(from)) && enc.llb.contains(to)) {

				Arrow arr2 = arrow.makeCopy(this);
				Integer aid = new Integer(arr2.id);
				arrows.put(aid, arr2);
				clla.add(arrow); // keep old arrow
				// arrow.deleteOnSave = true;
				changed = true;
			}
		}
	}

	

}
