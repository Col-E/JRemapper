package me.coley;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.benf.cfr.reader.PluginRunner;
import io.github.bmf.ClassWriter;
import io.github.bmf.JarReader;
import io.github.bmf.mapping.ClassMapping;
import me.coley.gui.Gui;

public class Program {

	private JarReader jar;
	/**
	 * GUI
	 */
	private final Gui window = new Gui(this);
	/**
	 * File chooser for selecting jars.
	 */
	private JFileChooser fileChooser;
	/**
	 * Current class in text area.
	 */
	private ClassMapping currentClass;
	/**
	 * Options not pertaining to CFR.
	 */
	private Options options = new Options();

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
	 * Called when a file is loaded.
	 * 
	 * @param file
	 *            Jar loaded.
	 */
	public void onFileSelect(File file) {
		// Load jar file into BMF
		// Set up mappings
		jar = new JarReader(file, true, true);
		//
		window.getFileTree().setup(jar);

	}

	/**
	 * Called when the current progress is saved as a mappings file.
	 * 
	 * @param selectedFile
	 */
	public void onSaveMappings(File selectedFile) {
		try {
			jar.saveMappingsTo(selectedFile,true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Called when the current progress is saved as a jar file.
	 * 
	 * @param selectedFile
	 */
	public void onSaveJar(File selectedFile) {
		jar.saveJarTo(selectedFile);
	}

	/**
	 * Updates a tree path given the initial/after values of a class rename.
	 * 
	 * @param original
	 * @param renamed
	 */
	public void updateTreePath(String original, String renamed) {
		window.getFileTree().update(jar, original, renamed);
	}

	/**
	 * Called when a class in the file tree is selected.
	 * 
	 * @param mapping
	 */
	public void onClassSelect(ClassMapping clazz) {
		try {
			String originalName = clazz.name.original;
			final byte[] bytes = ClassWriter.write(jar.getClassEntries().get(originalName));
			if (bytes == null) {
				// Failed to decompile
				window.getSourceArea().setText("Error: Failed to get class bytes");
			} else {
				// Update
				this.currentClass = clazz;
				// Decompile using CFR, send text to the text-area.
				PluginRunner pluginRunner = new PluginRunner(CFRSetting.toStringMap(), new CFRSourceImpl(bytes));
				String decomp = pluginRunner.getDecompilationFor(originalName);
				window.getSourceArea().setText(decomp);
			}
		} catch (Exception e) {
			// Failed to decompile
			window.getSourceArea().setText(e.toString());
		}
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
	 * Returns the current ClassMapping of the class in the text area.
	 * 
	 * @return
	 */
	public ClassMapping getCurrentClass() {
		return currentClass;
	}

	/**
	 * Returns the JarReader.
	 * 
	 * @return
	 */
	public JarReader getJarReader() {
		return jar;
	}

	/**
	 * Returns program options.
	 * 
	 * @return
	 */
	public Options getOptions() {
		return options;
	}
}
