package me.coley.gui.listener;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import me.coley.Program;
import me.coley.gui.component.JavaTextArea;

public class JavaCaretListener implements CaretListener {
	private final Program callback;
	private final JavaTextArea text;

	public JavaCaretListener(Program callback, JavaTextArea text) {
		this.callback = callback;
		this.text = text;
	}

	@Override
	public void caretUpdate(CaretEvent e) {
		if (!text.canParse()){
			return;
		}
		int dot = e.getDot();
		if (dot == 0 || dot >= text.getText().length()-1) {
			return;
		}
		// Getting the line data
		int lineEnd = text.getText().substring(dot).indexOf("\n");
		String firstPart =text.getText().substring(0, dot + lineEnd);
		int lineStart = firstPart.lastIndexOf("\n")+1;
		String line = firstPart.substring(lineStart, firstPart.length());
		String word = getCaretWord(dot - lineStart,line);
		// Parsing the line data
		if (word.length() > 0)
		{
			int lineNum = firstPart.split("\n").length - 1;

			System.out.println(lineNum + ":" + text.getContext(lineNum) + ":" + line);

		}
		
		
		//@formatter:off
	    /*
	            Alpha / idea code
	      
	      try {
			if (!canUpdate) return;
			int spot = dot;
			String s = getCaretWord(spot);
			if (s.isEmpty())
				return;
			
			for (String name : read.getClassEntries().keySet()) {
				String newName = read.getMapping().getClassName(name).getValue();
				String orig = name.contains("/") ? name.substring(name.lastIndexOf("/") + 1) : name;
				String alt = newName.contains("/") ? newName.substring(newName.lastIndexOf("/") + 1) : newName;
				srcPos = spot;
				boolean n1 = orig.equals(s);
				boolean n2 = alt.equals(s);
				if (n1 || n2) {
					lastClass = name;
					System.out.println(name);
					canUpdate = false;
					break;
				}
			}
		} catch (StringIndexOutOfBoundsException ee) {}
	*/
	}
	
	private String getCaretWord(int spot, String text) {
		int index;
		int index2;
		for (index = spot - 1; index > 0 && !isNonWord(text.charAt(index), false); index--);
		for (index2 = spot; index2 < text.length() && !isNonWord(text.charAt(index2), false); index2++);
		return text.substring(index + 1, index2);
	}


	/**
	 * 
	 * @param c
	 * @param extra True if including '/' as a word-character.
	 * @return
	 */
	private boolean isNonWord(char c, boolean extra) {
		if (c >= 'a' && c <= 'z') {
			return false;
		}
		if (c >= 'A' && c <= 'Z') {
			return false;
		}
		if (extra && c == '/') {
			return false;
		}
		return true;
	}
}
