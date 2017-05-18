package me.coley.jremapper.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import me.coley.bmf.mapping.ClassMapping;
import me.coley.bmf.mapping.MemberMapping;
import me.coley.jremapper.Program;

public class ActionRenameCurrentMembers implements ActionListener {

	private Program callback;

	public ActionRenameCurrentMembers(Program callback) {
		this.callback = callback;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		ClassMapping cm = callback.getCurrentClass();
		if (cm != null) {
			int i = 2;
			for (MemberMapping mm : cm.getMembers()) {
				mm.name.setValue(getName(i));
				i *= 2;
			}
			callback.onClassSelect(cm);
		}
	}

	private static String getName(int i) {
		return getString("abcdefghijklmnopqrstuvwxyz", i, 24);
	}

	public static String getString(String alpha, int i, int n) {
		char[] charz = alpha.toCharArray();
		if (n < 2) {
			n = 2;
		} else if (n > alpha.length()) {
			n = alpha.length();
		}
		final char[] array = new char[33];
		final boolean b = i < 0;
		int n2 = 32;
		if (!b) {
			i = -i;
		}
		while (i <= -n) {
			array[n2--] = charz[-(i % n)];
			i /= n;
		}
		array[n2] = charz[-i];
		if (b) {
			array[--n2] = '-';
		}
		return new String(array, n2, 33 - n2);
	}
}
