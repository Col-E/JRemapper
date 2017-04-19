package me.coley.jremapper.gui.listener;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import me.coley.bmf.mapping.AbstractMapping;
import me.coley.bmf.mapping.ClassMapping;
import me.coley.bmf.mapping.MemberMapping;
import me.coley.jremapper.Program;
import me.coley.jremapper.gui.component.JavaTextArea;

public class JavaCaretListener implements CaretListener {
	private Program callback;
	private JavaTextArea text;

	public JavaCaretListener(Program callback, JavaTextArea text) {
		this.callback = callback;
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
			callback.getWindow().setTitle("?");
			callback.getWindow().getSourceArea().setEditable(false);
		} else {
			callback.getWindow().getSourceArea().setEditable(true);
			if (clazz){
				callback.getWindow().setTitle("C : " + am.name.getValue());
			} else {
				MemberMapping mm = (MemberMapping) am;
				String prefix = mm.desc.original.contains("(") ? "M" : "F";
				callback.getWindow().setTitle(prefix + " : " + am.name.getValue());
			}
			text.setEditable(true);
		}
	}
}