package me.coley;

import java.awt.EventQueue;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import me.coley.gui.Gui;

public class Program {

	/**
	 * GUI
	 */
	private final Gui window = new Gui(this);
	/**
	 * File chooser for selecting jars.
	 */
	private JFileChooser fileChooser;

	/**
	 * Displays the GUI.
	 */
	public void showGui() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					window.initialize();
					window.display();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Returns the file chooser. If it is null it is instantiated and set to the
	 * working directory. Only jar files can be selected.
	 * 
	 * @return
	 */
	public JFileChooser getFileChooser() {
		if (fileChooser == null) {
			fileChooser = new JFileChooser();
			FileNameExtensionFilter filter = new FileNameExtensionFilter("Java Archives", "jar");
			fileChooser.setFileFilter(filter);
			String dir = System.getProperty("user.dir");
			File fileDir = new File(dir);
			fileChooser.setDialogTitle("Open File");
			fileChooser.setCurrentDirectory(fileDir);
		}
		return fileChooser;
	}

	/**
	 * Creates and returns a file chooser set in the working directory.
	 * 
	 * @return
	 */
	public JFileChooser createFileSaver() {
		JFileChooser fileSaver = new JFileChooser();
		String dir = System.getProperty("user.dir");
		File fileDir = new File(dir);
		fileSaver.setCurrentDirectory(fileDir);
		fileSaver.setDialogTitle("Save to File");
		return fileSaver;
	}

	/**
	 * Returns the GUI.
	 * 
	 * @return
	 */
	public final Gui getWindow() {
		return window;
	}

	/**
	 * Called when a file is loaded.
	 * 
	 * @param file
	 *            Jar loaded.
	 */
	public void onFileSelect(File file) {
		// TODO: stub
	}

	/**
	 * Called when the current progress is saved as a mappings file.
	 * 
	 * @param selectedFile
	 */
	public void onSaveMappings(File selectedFile) {
		// TODO: stub
	}

	/**
	 * Called when the current progress is saved as a jar file.
	 * 
	 * @param selectedFile
	 */
	public void onSaveJar(File selectedFile) {
		// TODO: stub
	}
}
