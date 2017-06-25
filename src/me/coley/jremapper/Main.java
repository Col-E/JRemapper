package me.coley.jremapper;

import javax.swing.UIManager;

public class Main {
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Exception e) {
			e.printStackTrace();
		}
		Program program = new Program();
		program.showGui();
		//program.onFileSelect(new java.io.File("JRemapper.jar"));
		//program.onClassSelect(program.getJarReader().getMapping().getOrCreateClassMapping("me/coley/bmf/FieldNode"));
	}

}
