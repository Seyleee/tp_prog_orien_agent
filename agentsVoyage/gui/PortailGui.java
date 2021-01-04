package gui;

import agents.AgenceAgent;
import agents.PortailAgence;
import jade.gui.GuiEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

@SuppressWarnings("serial")
public class PortailGui extends JFrame {

    private static int nbPortailGui = 0;
    private int noPortailGui;

    /** Text area */
    private JTextArea jTextArea;

    private PortailAgence myPortal;

    public PortailGui(PortailAgence a) {
        super(a.getName());
        noPortailGui = ++nbPortailGui;

        myPortal = a;

        jTextArea = new JTextArea();
        jTextArea.setBackground(new Color(180, 180, 180));
        jTextArea.setEditable(false);
        jTextArea.setColumns(40);
        jTextArea.setRows(5);
        JScrollPane jScrollPane = new JScrollPane(jTextArea);
        getContentPane().add(BorderLayout.CENTER, jScrollPane);

        // Make the agent terminate when the user closes
        // the GUI using the button on the upper right corner
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // SEND AN GUI EVENT TO THE AGENT !!!
                GuiEvent guiEv = new GuiEvent(this, PortailAgence.EXIT);
                myPortal.postGuiEvent(guiEv);
                // END SEND AN GUI EVENT TO THE AGENT !!!
            }
        });

        setResizable(true);
    }


    public void display() {
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) screenSize.getWidth();
        int width = this.getWidth();
        int xx = (noPortailGui * width) % screenWidth;
        int yy = ((noPortailGui * width) / screenWidth) * getHeight();
        setLocation(xx, yy);
        setTitle(myPortal.getLocalName());
        setVisible(true);
    }

    /** add a string to the text area */
    public void println(String chaine) {
        String texte = jTextArea.getText();
        texte = texte + chaine + "\n";
        jTextArea.setText(texte);
        jTextArea.setCaretPosition(texte.length());
    }
}
