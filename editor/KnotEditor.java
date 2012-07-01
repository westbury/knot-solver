/*
 * KnotDrawing.java
 *
 * Created on January 21, 2004, 7:06 PM
 */

/**
 *
 * @author  Nigel
 */

// Java core packages
import java.awt.*;
import java.awt.event.*;

// Java extension packages
import javax.swing.*;

public class KnotEditor extends JApplet /*implements ActionListener*/ {
    
    String encodedValue;
    
    JTextPane textPane = new JTextPane();

    Drawing drawingComponent;
    
    /** Creates a new instance of KnotDrawing */
    public KnotEditor() {
    }
    
    public void init() {
        // Get the applet's GUI component display area
        Container container = getContentPane();
        container.setBackground(Color.pink);
        container.setLayout(new BorderLayout());
        
        // Set up the toolbar
        KnotToolBar toolBar = new KnotToolBar(this);
        toolBar.setMaximumSize(toolBar.getSize());
        
        // Set up the pane containing the text that describes the knot
//        textPane.setBorder(new BevelBorder(BevelBorder.LOWERED));
        drawingComponent = new Drawing(this);
        JScrollPane scrollPane = new JScrollPane(drawingComponent);
        
        container.add(toolBar.toolBar, BorderLayout.NORTH);
        container.add(textPane, BorderLayout.SOUTH);
        container.add(scrollPane, BorderLayout.CENTER);
        
        // Set the trefoil knot for testing purposes
        setEncodedValue("OROLTLSRSRCLC");
        
                try
        {
            System.out.println("");
            System.out.println("");
            drawingComponent.trace();
        }
        catch (Exception e)
        {
            System.out.println("some other error: " + e.toString());
            return;
        }
    }
    
    public void clear() {
        encodedValue = "";
        textPane.setText(encodedValue);
        drawingComponent.clear();
    }
    
    public void addMaximum() {
        drawingComponent.addMaximum();
        encodedValue += 'O';
        textPane.setText(encodedValue);
    }
    
    public void addMinimum() {
        try {
            drawingComponent.addMinimum();
            encodedValue += 'C';
            textPane.setText(encodedValue);
        } catch (KnotSpecificationException e) {
//            textPane.
        }
            
    }
    
    public void addOverCrossing() {
        try {
            drawingComponent.addOverCrossing();
            encodedValue += 'S';
            textPane.setText(encodedValue);
        } catch (KnotSpecificationException e) {
//            textPane.
        }
    }
    
    public void addUnderCrossing() {
        try {
            drawingComponent.addUnderCrossing();
            encodedValue += 'T';
            textPane.setText(encodedValue);
        } catch (KnotSpecificationException e) {
//            textPane.
        }
    }
    
    public void addMoveToLeft() {
        try {
            drawingComponent.addMoveToLeft();
            // If the red marker position is moved right then left then these cancel
            // each other out and we add nothing to the character string.
            if (encodedValue.endsWith("R")) {
                encodedValue = encodedValue.substring(0, encodedValue.length() - 1);
            } else {
                encodedValue += 'L';
            }
            textPane.setText(encodedValue);
            
        } catch (KnotCursorAtLimitException e) {
            // The red marker is already at the rightmost position.
            // Do nothing, and leave encodedValue unaltered.
        }
    }
    
    public void addMoveToRight() {
        try {
            drawingComponent.addMoveToRight();
            
            // If the red marker position is moved left then right then these cancel
            // each other out and we add nothing to the character string.
            if (encodedValue.endsWith("L")) {
                encodedValue = encodedValue.substring(0, encodedValue.length() - 1);
            } else {
                encodedValue += 'R';
            }
            textPane.setText(encodedValue);
        } catch (KnotCursorAtLimitException e) {
            // The red marker is already at the rightmost position.
            // Do nothing, and leave encodedValue unaltered.
        }
    }
    
    public void undoAction() {
        if (encodedValue.length() > 0) {
            // First remove all U and D characters from the end of the string.
            while (encodedValue.endsWith("L") || encodedValue.endsWith("R")) {
                encodedValue = encodedValue.substring(0, encodedValue.length() - 1);
            }
            // Now remove the real action character.
            encodedValue = encodedValue.substring(0, encodedValue.length() - 1);
            
            textPane.setText(encodedValue);
            drawingComponent.undoAction();
        }
    }
    
    public void setEncodedValue(String encodedValue) {
        clear();
   
        textPane.setText(encodedValue);
        
        try {
            for (int i = 0; i < encodedValue.length(); i++) {
                addAction(encodedValue.charAt(i));
            }
        } catch (KnotSpecificationException e) {
            System.out.println(e.getMessage());
        }
    }
        
    void addAction(char actionCode) throws KnotSpecificationException {
        switch(actionCode) {
            case 'O':
                addMaximum();
                break;
            case 'C':
                addMinimum();
                break;
            case 'S':
                addOverCrossing();
                break;
            case 'T':
                addUnderCrossing();
                break;
            case 'L':
                addMoveToLeft();
                break;
            case 'R':
                addMoveToRight();
                break;
            default:
                // Set error message
        }
    }
    
    public String getEncodedValue() {
        return encodedValue;
    }
    
    public void actionPerformed(ActionEvent e) {
    }
    
    /**
     * This method is here so that this class can be run as an application.
     * This is generally used to give more debugging possibilities and to
     * enable build scripts to build the 'ser' files from the 'xml' files
     * from each dance.  It is expected
     * that this class would more normally be exectuted as either an applet
     * or as a bean.
    */
    public static void main(String[] args) {
        KnotEditor theApplet = new KnotEditor();
        
        // Create a window for the applet to run in.
        JFrame theFrame = new JFrame();
        theFrame.setSize(300, 300);
        
        // Place the applet in the window
        theFrame.getContentPane().add("Center", theApplet);
      
        // First argument should be the characters defining the initial knot.

        try {
            theApplet.setEncodedValue(args[0]);
        }
        catch (Exception e) {
            System.out.println("Error in the default knot: " + e.toString());
        }
        
        // start the applet
        // Don't call init because that gets applet parameters which are not
        // available.
        //      theApplet.init();
        theApplet.start();
        
        // display the window
        theFrame.setVisible(true);
    }
    
}

