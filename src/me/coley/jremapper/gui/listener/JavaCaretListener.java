package me.coley.jremapper.gui.listener;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import me.coley.bmf.mapping.AbstractMapping;
import me.coley.bmf.mapping.ClassMapping;
import me.coley.bmf.mapping.MemberMapping;
import me.coley.jremapper.JRemapper;
import me.coley.jremapper.gui.component.JavaTextArea;

public class JavaCaretListener implements CaretListener {
	private JRemapper jremap;
	private JavaTextArea text;

	public JavaCaretListener(JRemapper jremap, JavaTextArea text) {
		this.jremap = jremap;
		this.text = text;
	}

	@Override
	public void caretUpdate(CaretEvent e) {
		if (!text.canParse()) {
			return;
		}
		int dot = e.getDot();
		if (dot == 0 || dot >= text.getText().length() - 1) {
			return;
		}
		AbstractMapping am = text.getSelectedMapping();
		boolean clazz = am instanceof ClassMapping;
		if (am == null) {
			jremap.getWindow().setTitle("?");
			jremap.getWindow().getSourceArea().setEditable(false);
		} else {
			jremap.getWindow().getSourceArea().setEditable(true);
			if (clazz){
				jremap.getWindow().setTitle("C : " + am.name.getValue());
			} else {
				MemberMapping mm = (MemberMapping) am;
				String prefix = mm.desc.original.contains("(") ? "M" : "F";
				jremap.getWindow().setTitle(prefix + " : " + am.name.getValue());
			}
			text.setEditable(true);
		}
	}
}