package me.coley;

import javax.swing.UIManager;

import io.github.bmf.util.ClassLoadStatus;

public class Main {
    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        System.out.println(ClassLoadStatus.getLoadStatus("not/a/real/class"));
        System.out.println(ClassLoadStatus.getLoadStatus("java/lang/Integer"));
        System.out.println(ClassLoadStatus.getLoadStatus("me/coley/Options"));
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Program program = new Program();
        program.showGui();
    }
}
