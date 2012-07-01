/*
 * KnotToolBar.java
 *
 * Created on January 24, 2004, 2:08 PM
 */

/**
 *
 * @author  Nigel
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

public class KnotToolBar extends JPanel {
    
    public JToolBar toolBar;
    
    private KnotEditor knotEditorApplet;
    
    /** Creates a new instance of KnotToolBar */
    public KnotToolBar(KnotEditor knotEditorApplet) {
        this.knotEditorApplet = knotEditorApplet;
        
        // Create the set of action buttons

        JButton maximumPointAction = createKnotActionButton(
            "Add a Maximum Point", "resource/Maximum.gif", 'O');
        JButton minimumPointAction = createKnotActionButton(
            "Add a Minimum Point", "resource/Minimum.gif", 'C');
        JButton overCrossingAction = createKnotActionButton(
            "Cross with Left Strand on Top", "resource/CrossLeftOnTop.gif", 'S');
        JButton underCrossingAction = createKnotActionButton(
            "Cross with Right Strand on Top", "resource/CrossRightOnTop.gif", 'T');
        JButton moveToLeftAction = createKnotActionButton(
            "Move Cursor to Left", "resource/MoveLeft.gif", 'L');
        JButton moveToRightAction = createKnotActionButton(
            "Move Cursor to Right", "resource/MoveRight.gif", 'R');
        JButton undoAction = createKnotActionButton(
            "Undo Last Action", "resource/UnDo.gif", 'A');
        
        toolBar = new JToolBar("Actions");
        toolBar.add(maximumPointAction);
        toolBar.add(minimumPointAction);
        toolBar.add(overCrossingAction);
        toolBar.add(underCrossingAction);
        toolBar.addSeparator();
        toolBar.add(moveToLeftAction);
        toolBar.add(moveToRightAction);
        toolBar.addSeparator();
        toolBar.add(undoAction);
    }

    private JButton createKnotActionButton(String toolTip, String iconFileName, char actionCode) {
        JButton button = new JButton();
        
        button.setToolTipText(toolTip);
        button.addActionListener(new KnotAction(actionCode));
        java.net.URL imageURL = KnotToolBar.class.getResource(iconFileName);
        if (imageURL != null) {
            button.setIcon(new ImageIcon(imageURL, toolTip));
        }
        return button;
    }
    
    class KnotAction implements ActionListener {
        char actionCode;
        
        public KnotAction(char actionCode) {
            this.actionCode = actionCode;
        }
        
        public void actionPerformed(ActionEvent event) {
            try {
                if (actionCode == 'A') {
                    knotEditorApplet.undoAction();
                } else {
                    knotEditorApplet.addAction(actionCode);
                }
            } catch (KnotSpecificationException e) {
                // Should not happen because only allowed toolbar buttons
                // are enabled.
                System.out.println(e.getMessage());
            }
        }

    }
}
