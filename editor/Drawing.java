/*
 * Drawing.java
 *
 * Created on January 24, 2004, 1:36 PM
 */

/**
 *
 * @author  Nigel
 */

import java.util.*;    // for Vector
import java.awt.*;     // for Graphics etc.
import java.awt.event.*;     // for MouseListener

class Drawing extends javax.swing.JComponent {
    
    /** This structure is used to paint the diagram. */
    Vector segments = new Vector();
    
    /**
     * The current y co-ordinate.  This value increases by one each time
     * we add the next letter.
     */
    int yCurrent = 0;
    
    /**
     * The index in the ordered active segments at which the red line is currently located.
     */
    int redLinePosition = 0;
    
    /** Minimum x co-ordinate.
     * The maximum and minimum x co-ordinates are maintained so that the diagram
     * can be scaled to fit in the window.
     */
    int xMinimum = 0;
    
    /** Maximum x co-ordinate.
     * The maximum and minimum x co-ordinates are maintained so that the diagram
     * can be scaled to fit in the window.
     */
    int yMaximum = 0;
    
    /* Sink for event notifications */
    KnotEditor knotEditorApplet;
    
    final int unitWidth = 20;
    final int unitHeight = 20;
    
    // Values calculated from given width and height
    int littleCircleXRadius;
    int littleCircleYRadius;
    int littleCircleXDiameter;
    int littleCircleYDiameter;
    int littleCircleDelta1;
    int littleCircleDelta2;

    // Radius of the string.
    int stringRadius = 3;
    
    public Drawing(KnotEditor knotEditorApplet) {
        this.knotEditorApplet = knotEditorApplet;
        
        MouseListener l = new DrawingMouseListener();;
        addMouseListener(l);
    }
    
    public void clear() {
        segments.clear();
        yCurrent = 0;
        redLinePosition = 0;
    }
    
    public void addMaximum() {
        // A new maximum is to be created at the current position of the red line.
        // This creates two new segments.
        
        // Before we can add the maximum, we may have to move other segments outwards
        // to create room.
        
        Segment orderedSegments[] = buildOrderedSegments();
        int xRedLinePosition;
        if (orderedSegments.length == 0) {
            xRedLinePosition = 0;
        } else if (redLinePosition == 0) {
            xRedLinePosition = orderedSegments[0].xCurrent - 3;
        } else if (redLinePosition == orderedSegments.length) {
            xRedLinePosition = orderedSegments[orderedSegments.length-1].xCurrent + 3;
        } else {
            xRedLinePosition = ((orderedSegments[redLinePosition-1].xCurrent + orderedSegments[redLinePosition].xCurrent) / 4) * 2;
            
            // Ensure enough room to the left.
            int position = redLinePosition - 1;
            int xPosition = xRedLinePosition - 1;
            while (position >= 0 && orderedSegments[position].xCurrent == xPosition) {
                // Move this segment left.
                orderedSegments[position].shiftLeft();
                position--;
                xPosition -= 2;
            }
            
            // Ensure enough room to the right.
            position = redLinePosition;
            xPosition = xRedLinePosition + 1;
            while (position < orderedSegments.length && orderedSegments[position].xCurrent == xPosition) {
                // Move this segment right.
                orderedSegments[position].shiftRight();
                position++;
                xPosition += 2;
            }
            
        }
        
        // Now we have a gap into which we place put the two new segments
        // created by this maximum.
        Segment leftSegment = new Segment(xRedLinePosition, yCurrent);
        Segment rightSegment = new Segment(xRedLinePosition, yCurrent);
        
        segments.add(leftSegment);
        segments.add(rightSegment);
        
        leftSegment.add(new MaximumToLeftSection(xRedLinePosition, yCurrent));
        rightSegment.add(new MaximumToRightSection(xRedLinePosition, yCurrent));
        
        // Add one to the red line position so it is positioned between
        // the two newly created segments.
        redLinePosition++;
        
        // Complete all other segments by drawing a line stright down to get
        // to the next y level.
        setYCurrent(yCurrent + 1);
        
        repaint();
    }
    
    public void addMinimum() throws KnotSpecificationException {
        // A new minimum is to be created at the current position of the red line.
        // This joins two segments together.
        
        Segment orderedSegments[] = buildOrderedSegments();
        
        if (orderedSegments.length < 2) {
            throw new KnotSpecificationException("You cannot add a minimum unless there are at least two incomplete segments.");
        }
        
        if (redLinePosition == 0 || redLinePosition == orderedSegments.length) {
            throw new KnotSpecificationException("Minimum cannot occur when red line is to left of all segments or to the right of all segments.");
        }
        
        // Find the innermost segments to the left and to the right of the current
        // position of the red line.
        Segment leftSegment = orderedSegments[redLinePosition-1];
        Segment rightSegment = orderedSegments[redLinePosition];
        
        // We must first shift the lines so that they are next to each other.
/*        
        while (leftSegment.xCurrent < rightSegment.xCurrent-2) {
            leftSegment.shiftRight();
            if (leftSegment.xCurrent < rightSegment.xCurrent-2) {
                rightSegment.shiftLeft();
            } else {
                rightSegment.add(new StraightSection(rightSegment.xCurrent, rightSegment.yCurrent));
            }
        }
*/        
        int xShift = (rightSegment.xCurrent - leftSegment.xCurrent) / 2;
        leftSegment.add(new MinimumFromTheLeftSection(leftSegment.xCurrent, leftSegment.yCurrent, xShift));
        rightSegment.add(new MinimumFromTheRightSection(rightSegment.xCurrent, rightSegment.yCurrent, xShift));
        
        // Adjust the red line position so that it remains in the same location.
        redLinePosition--;
        
        // Complete all other segments by drawing a line stright down to get
        // to the next y level.
        setYCurrent(leftSegment.yCurrent);
        
        repaint();
    }
    
    public void addOverCrossing() throws KnotSpecificationException {
        // Cross two segments at the current position of the red line.
        
        Segment orderedSegments[] = buildOrderedSegments();
        
        if (orderedSegments.length < 2) {
            throw new KnotSpecificationException("You cannot add a crossing unless there are at least two incomplete segments.");
        }
        
        if (redLinePosition == 0 || redLinePosition == orderedSegments.length) {
            throw new KnotSpecificationException("A crossing cannot occur when red line is to left of all segments or to the right of all segments.");
        }
        
        // Find the innermost segments to the left and to the right of the current
        // position of the red line.
        Segment leftSegment = orderedSegments[redLinePosition-1];
        Segment rightSegment = orderedSegments[redLinePosition];
        
        // The segments could be quite far apart.  However, after they cross, we leave
        // them at cosecutive odd x co-ordinates.  That is, we move the two segments
        // sideways just enough so that they cross.
        int averageX = (leftSegment.xCurrent + rightSegment.xCurrent) / 2;
        int rightSegmentEndingX = averageX - 1;
        int leftSegmentEndingX = averageX + 1;
        leftSegment.add(new CrossOnTopSection(leftSegment.xCurrent, leftSegment.yCurrent, leftSegmentEndingX - leftSegment.xCurrent));
        rightSegment.add(new CrossUnderneathSection(rightSegment.xCurrent, rightSegment.yCurrent, rightSegmentEndingX - rightSegment.xCurrent));
        
        // Complete all other segments by drawing a line stright down to get
        // to the next y level.
        setYCurrent(yCurrent + 1);
        
        repaint();
    }
    
    public void addUnderCrossing() throws KnotSpecificationException {
        // Cross two segments at the current position of the red line.
        
        Segment orderedSegments[] = buildOrderedSegments();
        
        if (orderedSegments.length < 2) {
            throw new KnotSpecificationException("You cannot add a crossing unless there are at least two incomplete segments.");
        }
        
        if (redLinePosition == 0 || redLinePosition == orderedSegments.length) {
            throw new KnotSpecificationException("A crossing cannot occur when red line is to left of all segments or to the right of all segments.");
        }
        
        // Find the innermost segments to the left and to the right of the current
        // position of the red line.
        Segment leftSegment = orderedSegments[redLinePosition-1];
        Segment rightSegment = orderedSegments[redLinePosition];
        
        int averageX = (leftSegment.xCurrent + rightSegment.xCurrent) / 2;
        int rightSegmentEndingX = averageX - 1;
        int leftSegmentEndingX = averageX + 1;
        leftSegment.add(new CrossUnderneathSection(leftSegment.xCurrent, leftSegment.yCurrent, leftSegmentEndingX - leftSegment.xCurrent));
        rightSegment.add(new CrossOnTopSection(rightSegment.xCurrent, rightSegment.yCurrent, rightSegmentEndingX - rightSegment.xCurrent));
        
        // Complete all other segments by drawing a line stright down to get
        // to the next y level.
        setYCurrent(yCurrent + 1);
        
        repaint();
    }
    
    public void addMoveToLeft() throws KnotCursorAtLimitException {
        if (redLinePosition == 0) {
            throw new KnotCursorAtLimitException();
        }
        redLinePosition--;
        repaint();
    }
    
    public void addMoveToRight() throws KnotCursorAtLimitException {
        Segment orderedSegments[] = buildOrderedSegments();
        
        if (redLinePosition == orderedSegments.length) {
            throw new KnotCursorAtLimitException();
        }
        redLinePosition++;
        repaint();
    }
    
    /**
     * Undo the last action.
     *
     * This function will undo the last action if the last action was a knot
     * altering function (add maximum, mimimum, or crossover).
     * Note that a movement of the red line marker is not considered an action
     * by this class.
     * The red line marker will be moved to the position of action that was
     * undone.
     */
    public void undoAction() {
        int xRedLinePosition = 0;
        
        for (Iterator iter = segments.iterator(); iter.hasNext(); ) {
            Segment segment = (Segment)iter.next();
            
            if (segment.yCurrent == yCurrent) {
                Section lastSection = (Section)segment.sections.elementAt(segment.sections.size() - 1);
                segment.sections.setSize(segment.sections.size() - 1);
                segment.xCurrent -= lastSection.getXShift();
                segment.yCurrent--;
                
                if (!(lastSection instanceof StraightSection)) {
                    xRedLinePosition += segment.xCurrent;
                }
                
                if (segment.yCurrent == segment.yStart) {
                    // Nothing is left in this segment so remove it.
                    iter.remove();
                }
            }
            
        }
        
        yCurrent--;
        
        // There are always two segments involved in each operation,
        // so halve the total to get the x co-ordinate of a point halfway between
        // the two segments.  This will be the position where we want to leave
        // the red line.
        xRedLinePosition /= 2;

        // Count the number of segments to the left of this point.
        // This gives the desired red line position.
        redLinePosition = 0;
        for (Iterator iter = segments.iterator(); iter.hasNext(); ) {
            Segment segment = (Segment)iter.next();
            if (segment.xCurrent < xRedLinePosition) {
                redLinePosition++;
            }
        }
            
        repaint();
    }
    
    protected void setYCurrent(int yNew) {
        // Paint each segment from top to bottom.
        for (Iterator iter = segments.iterator(); iter.hasNext(); ) {
            Segment segment = (Segment)iter.next();
            
            if (!segment.isComplete()) {
                while (segment.yCurrent < yNew) {
                    segment.add(new StraightSection(segment.xCurrent, segment.yCurrent));
                }
            }
        }
        
        yCurrent = yNew;
    }
    
    /**
     * Each segment class has a paint method to paint the segment.
     * However, the paint method does not paint the inital straight line part of
     * the segment nor does it paint the final straight line part of the segment.
     * This must be done by this method.  This allows this method to examine two
     * consecutive sements and draw a smooth curve going from one to the other.
     */

//    int xx;
    protected void paintComponent(Graphics g) {
/*        
        xx = 0;
        // Calculate the values that are constant for this particular painting
        // of the knot.  These values are useful when painting the sections of
        // the knot.
        littleCircleXRadius = unitWidth/4;
        littleCircleYRadius = unitHeight/4;
        littleCircleXDiameter = littleCircleXRadius * 2;
        littleCircleYDiameter = littleCircleYRadius * 2;
        littleCircleDelta1 = littleCircleXRadius / 3;
        littleCircleDelta2 = littleCircleYRadius * 2 / 3;
*/        
        if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        
        // Paint each segment from top to bottom.
        for (Iterator iter = segments.iterator(); iter.hasNext(); ) {
            Segment segment = (Segment)iter.next();
            
            //        {
            //          Segment segment = (Segment)segments.elementAt(1);
            int x = segment.xStart;
            int y = segment.yStart;

            Section previousSection = null;
            for (Iterator iter2 = segment.sections.iterator(); iter2.hasNext(); ) {
                Section section = (Section)iter2.next();

                if (previousSection != null) {
                    // We must paint the transition.
                    paintTransition(g, previousSection, section);
                }
                
                section.paint(g);
                x += section.getXShift();
                y++;
                
                previousSection = section;
            }
            
            // If this segment is incomplete then we draw the final straight segment.
            // (If there were a following section then some or all of this straight
            // segment would have been curved to meet the next section.  However,
            // until a further section is added, we can only draw it straight).
            if (!segment.isComplete()) {
//        double x = MapXCoordinate(previousSection.getFinalX());
//        double y = MapYCoordinate(previousSection.getFinalY());
        double previousLength = previousSection.getFinalStraightLineLength();
        double previousDirection = previousSection.getFinalStraightLineDirection()
                     + Math.PI;
            drawLinePipe(g,
                (int)(MapXCoordinate(x) + previousLength * Math.cos(previousDirection)),
                (int)(MapYCoordinate(y) - previousLength * Math.sin(previousDirection)),
                (int)MapXCoordinate(x),
                (int)MapYCoordinate(y));
            }
        }
        
        Segment orderedSegments[] = buildOrderedSegments();
        int xRedLinePosition;
        if (orderedSegments.length == 0) {
            xRedLinePosition = 0;
        } else if (redLinePosition == 0) {
            xRedLinePosition = orderedSegments[0].xCurrent - 3;
        } else if (redLinePosition == orderedSegments.length) {
            xRedLinePosition = orderedSegments[orderedSegments.length-1].xCurrent + 3;
        } else {
            xRedLinePosition = ((orderedSegments[redLinePosition-1].xCurrent + orderedSegments[redLinePosition].xCurrent) / 4) * 2;
        }
        
        final int nPoints = 7;
        final int xPointDeltas[] = { 0, 5, 2, 2,-2,-2,-5};
        final int yPointDeltas[] = { 0,-2,-2,-8,-8,-2,-2};
        
        int[] xPoints = new int[nPoints];
        int[] yPoints = new int[nPoints];
        // Paint the red line marker
        for (int i = 0; i < nPoints; i++) {
            xPoints[i] = MapXCoordinate(xRedLinePosition) + xPointDeltas[i];
            yPoints[i] = MapYCoordinate(yCurrent) + yPointDeltas[i];
        }
        g.drawPolygon(xPoints, yPoints, nPoints);
    }
    
    /**
     *
     */
    protected void paintTransition(Graphics g, Section previousSection, Section followingSection) {
        // The point at which the two sections would meet if we do not do any smoothing
        // of the curve.
        double x = MapXCoordinate(followingSection.x);
        double y = MapYCoordinate(followingSection.y);
        
        // Determine the size of the circle.
        
        // r = min of two * tan (angle / 2)
        
        // Note that one direction is the direction towards the intersection point
        // and the other is the direction away from the intersection point,
        // so we add 180.
        double previousLength = previousSection.getFinalStraightLineLength();
        double followingLength = followingSection.getInitialStraightLineLength();
        double previousDirection = previousSection.getFinalStraightLineDirection()
                     + Math.PI;
        double followingDirection = followingSection.getInitialStraightLineDirection();

        // temp for debugging
        int previousDirectionD = RadiansToDegrees(previousDirection);
        int followingDirectionD = RadiansToDegrees(followingDirection);

        double shortestLength;
        double longestLength;
        double longestLengthDirection;
        if (previousLength < followingLength) {
            shortestLength = previousLength;
            longestLength = followingLength;
            longestLengthDirection = followingDirection;
        } else {
            shortestLength = followingLength;
            longestLength = previousLength;
            longestLengthDirection = previousDirection;
        }
        
        // Find the angle between the two lines.
        double angleDifference = followingDirection - previousDirection;
        
        // Add or subtract multiples of 360 degrees to get angle in range -180 to 180.
        while (angleDifference > Math.PI) {
            angleDifference -= Math.PI * 2;
        }
        while (angleDifference < -Math.PI) {
            angleDifference += Math.PI * 2;
        }
        
        double angle = Math.abs(angleDifference) / 2;
        
        // temp for debugging
        int angleD = RadiansToDegrees(angle);

        if (angle > Math.PI/2 - 0.0001) {
            // Two two lines are as good as lined up so no smoothing is necessary.
            // We just draw a straight line.
            // Note that previousDirection is the direction TO the intersection.
            drawLinePipe(g,
                (int)(x + previousLength * Math.cos(previousDirection)),
                (int)(y - previousLength * Math.sin(previousDirection)),
                (int)(x + followingLength * Math.cos(followingDirection)),
                (int)(y - followingLength * Math.sin(followingDirection)));
        } else {
            double radius = shortestLength * Math.tan(angle);
            
            // Find the center of the arc
            
            double distanceOfCenter = shortestLength / Math.cos(angle);
            double directionOfCenter = previousDirection + angleDifference / 2;

            // temp for debugging
            int directionOfCenterD = RadiansToDegrees(directionOfCenter);
            
            double centerOfArcX = x + distanceOfCenter * Math.cos(directionOfCenter);
            double centerOfArcY = y - distanceOfCenter * Math.sin(directionOfCenter);
            
            // Note that previous section's final direction is the direction TO the
            // intersection.
            int startArcAngle;
            int endArcAngle;
            if (angleDifference > 0) {
                startArcAngle = RadiansToDegrees(previousDirection) - 90;
                endArcAngle = RadiansToDegrees(followingDirection) + 90;
            } else {
                startArcAngle = RadiansToDegrees(previousDirection) + 90;
                endArcAngle = RadiansToDegrees(followingDirection) - 90;
            }
            
            // Reduce the arc angle to the range -180 to 180, otherwise
            // we get a whole circle.
            int arcAngle = endArcAngle - startArcAngle;
            while (arcAngle > 180) {
                arcAngle -= 360;
            }
            while (arcAngle < -180) {
                arcAngle += 360;
            }
            
//         if (xx++ == 0) {
            drawArcPipe(g, 
            (int)(centerOfArcX - radius),
            (int)(centerOfArcY - radius),
            (int)(radius * 2),
            (int)(radius * 2),
            startArcAngle,
            arcAngle);
  //       }
            // The arc goes from one of the straight line parts to the other so replaces
            // an equal distance from both.  If one straight line part was longer than the
            // other then we must paint the remainder of that straight line.
            
            if (longestLength - shortestLength > 2) {
                drawLinePipe(g, 
                (int)(x + shortestLength * Math.cos(longestLengthDirection)),
                (int)(y - shortestLength * Math.sin(longestLengthDirection)),
                (int)(x + longestLength * Math.cos(longestLengthDirection)),
                (int)(y - longestLength * Math.sin(longestLengthDirection)));
            }
        }
    }
    
    void drawLinePipe(Graphics g, int x1, int y1, int x2, int y2) {
        double length = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
        int xComponent = (int)(stringRadius * (y2 - y1) / length + 0.5);
        int yComponent = (int)(stringRadius * (x2 - x1) / length + 0.5);
        // Left side of string
        g.drawLine(x1 + xComponent, y1 - yComponent, x2 + xComponent, y2 - yComponent);
        // Right side of string
        g.drawLine(x1 - xComponent, y1 + yComponent, x2 - xComponent, y2 + yComponent);
    }

    void drawArcPipe(Graphics g, int x, int y, int width, int height, int startAngle, int arcAngle) {
        // Outer side of string
        g.drawArc(x - stringRadius, y - stringRadius, width + 2 * stringRadius, height + 2 * stringRadius, startAngle, arcAngle);
        // Inner side of string
        g.drawArc(x + stringRadius, y + stringRadius, width - 2 * stringRadius, height - 2 * stringRadius, startAngle, arcAngle);
    }

    void drawArcPipeAndFill(Graphics g, int x, int y, int width, int height, int startAngle, int arcAngle) {
        g.setXORMode(g.getColor());
        
        // Outer side of string
        g.fillArc(x - stringRadius, y - stringRadius, width + 2 * stringRadius, height + 2 * stringRadius, startAngle, arcAngle);
        // Inner side of string
        g.fillArc(x + stringRadius, y + stringRadius, width - 2 * stringRadius, height - 2 * stringRadius, startAngle, arcAngle);
        
        g.setPaintMode();
        
        // Draw the conflicting 
    }

    int RadiansToDegrees(double radians) {
        return (int)(radians * 180 / Math.PI);
    }
    
    double DegreesToRadians(int degrees) {
        return degrees * Math.PI / 180;
    }
    
    int MapXCoordinate(int x) {
        return x * unitWidth + 100;
    }
    
    int MapYCoordinate(int y) {
        return y * unitHeight + 5;
    }
    
    Segment[] buildOrderedSegments() {
        int count = 0;
        for (Iterator iter = segments.iterator(); iter.hasNext(); ) {
            Segment segment = (Segment)iter.next();
            if (!segment.isComplete()) {
                count++;
            }
        }
        
        Segment result[] = new Segment[count];
        
        int index = 0;
        for (Iterator iter = segments.iterator(); iter.hasNext(); ) {
            Segment segment = (Segment)iter.next();
            if (!segment.isComplete()) {
                result[index++] = segment;
            }
        }
        
        Arrays.sort(result);
        
        // Trace the results
        System.out.println("");
        System.out.println("Active segments in order");
        System.out.println("--------");
        for (int i = 0; i < result.length; i++) {
            System.out.println("    Segment ending at (" + result[i].xCurrent + ", " + result[i].yCurrent + ")");
        }
        System.out.println("");
        
        return result;
    }
    
    void trace() {
        // Trace out the hand movements that result from the second pass
        System.out.println("");
        System.out.println("Segments");
        System.out.println("--------");
        
        for (Iterator iter = segments.iterator(); iter.hasNext(); ) {
            Segment segment = (Segment)iter.next();
            segment.trace();
        }
        
        System.out.println("Red line position is " + redLinePosition);
    }
    
    /**
     * A segment of the knot that extends from a maximum down to a minimum.
     */
    private class Segment implements Comparable {
        // Current position of the bottom end of the line
        int xCurrent;
        int yCurrent;
        
        /**
         * Starting position of the segment.  This will always be a point at which
         * a maximum is located.
         */
        int xStart;
        int yStart;
        
        Vector sections = new Vector();
        
        Segment(int x, int y) {
            this.xStart = x;
            this.yStart = y;
            this.xCurrent = x;
            this.yCurrent = y;
        }
        
        void add(Section section) {
            sections.add(section);
            xCurrent += section.getXShift();
            yCurrent++;
        }
        
        void shiftLeft() {
            // kludge - should not use this class here
            sections.add(new CrossOnTopSection(xCurrent, yCurrent, -2));
            xCurrent -= 2;
            yCurrent++;
        }
        
        void shiftRight() {
            // kludge - should not use this class here
            sections.add(new CrossOnTopSection(xCurrent, yCurrent, 2));
            xCurrent += 2;
            yCurrent++;
        }
        
        int getXPosition() {
            return xCurrent;
        }
        
        boolean isComplete() {
            Section lastSection = (Section)sections.elementAt(sections.size()-1);
            return (lastSection instanceof MinimumFromTheLeftSection
            || lastSection instanceof MinimumFromTheRightSection);
        }
        
        void trace() {
            // Trace out the hand movements that result from the second pass
            System.out.println("");
            int x = xStart;
            int y = yStart;
            
            for (Iterator iter = sections.iterator(); iter.hasNext(); ) {
                Section section = (Section)iter.next();
                
                System.out.print("    (" + x + ", " + y + ") ");
                section.trace();
                x += section.getXShift();
                y++;
                System.out.println();
            }
            System.out.println("    (" + x + ", " + y + ") ");
            System.out.println("current is (" + xCurrent + ", " + yCurrent + ") ");
            
        }
        
        public int compareTo(Object o) {
            if (o == null || !(o instanceof Segment)) {
                throw new ClassCastException();
            }
            
            Segment other = (Segment)o;
            if (this.xCurrent < other.xCurrent) {
                return -1;
            } else if (this.xCurrent > other.xCurrent) {
                return 1;
            } else {
                return 0;
            }
        }
        
    }
    
    /**
     * Each segment of the knot is made up of sections.
     * A section could be, say, a straight line, or a curve in a given direction.
     * <P>
     * This is an abstract class.  A class is derived from this for each type
     * of curve.
     * <P>
     * We want the knot to be drawn as a smooth curve.  We also want the sections
     * to fit together smoothly without lots of curves going this way and that.
     * This is done using the following rules.
     * <LI>Every section must begin and end with a straight line.<\LI>
     * <LI>One segment must end where the next segment starts
     * <LI>The straight line ending one segment and the straight line starting
     *     the next may not in lined up.  To avoid the sharp change in direction,
     *     an arc is taken that touches the two straight lines.  The radius of the
     *     arc is the largest possible given the abruptness of the change in direction
     *     and the length of the straight segments.  At least one of the two straight
     *     segments will completely disappear.  Consider a circle on the inside of the
     *     elbow, touching the two straight lines.  Now make that circle bigger until
     *     it touches at the starting point of one of the straight lines.
     *
     * To enable this to be done, each segment object must provide the length and direction
     * of the straight line at the start and end of the segment.  
     * This information is provided using the getInitialStraightLineLength, 
     * getInitialStraightLineDirection, getFinalStraightLineLength, and getFinalStraightLineDirection
     * methods.
     * <P>  
     * The Section.paint method
     * is not responsible for painting the straight line parts; it just paints the part of
     * the segment between the straight line parts.  
     */
    private abstract class Section {
        protected int x;
        protected int y;
        
        abstract double getInitialStraightLineLength();
        abstract double getInitialStraightLineDirection();
        abstract double getFinalStraightLineLength();
        abstract double getFinalStraightLineDirection();
        abstract int getXShift();
        abstract void paint(Graphics g);
        abstract void trace();
  /*      
        int getFinalX() {
            return x + getXShift();
        }

        int getFinalY() {
            return y + 1;
        }
   */
    }
    
    /**
     * A curve from a maximum point down to the left.
     * All segments start with a section of this class or of MaximumToRightSection.
     */
    private class MaximumToLeftSection extends Section {
        MaximumToLeftSection(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        int getXShift() {
            return -1;
        }
        
        void paint(Graphics g) {
            drawArcPipe(g, MapXCoordinate(x-1), MapYCoordinate(y) + unitHeight/4, unitWidth*2, unitHeight, 90, 90);
        }
        
        void trace() {
            System.out.print("maximum curving left to");
        }
        
        double getInitialStraightLineLength() {
//            assert(false);
            return 0;
        }
        
        double getInitialStraightLineDirection() {
//            assert(false);
            return 0;
        }
        
        double getFinalStraightLineLength() {
            return unitHeight / 4;
        }
        
        double getFinalStraightLineDirection() {
            return DegreesToRadians(270);
        }
        
    }
    
    /**
     * A curve from a maximum point down to the right.
     * All segments start with a section of this class or of MaximumToLeftSection.
     */
    private class MaximumToRightSection extends Section {
        MaximumToRightSection(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        int getXShift() {
            return 1;
        }
        
        void paint(Graphics g) {
            drawArcPipe(g, MapXCoordinate(x-1), MapYCoordinate(y) + unitHeight/4, unitWidth*2, unitHeight, 0, 90);
        }
        
        void trace() {
            System.out.print("maximum curving right to");
        }
        
        double getInitialStraightLineLength() {
//            assert(false);
            return 0;
        }
        
        double getInitialStraightLineDirection() {
//            assert(false);
            return 0;
        }
        
        double getFinalStraightLineLength() {
            return unitHeight / 4;
        }
        
        double getFinalStraightLineDirection() {
            return DegreesToRadians(270);
        }
        
    }
    
    /**
     * A section that curves down to the right to a minimum point.
     * All segments end with a section of this class or of MinimumFromTheRightSection.
     */
    private class MinimumFromTheLeftSection extends Section {
        private int xShift;
        
        MinimumFromTheLeftSection(int x, int y, int xShift) {
            this.x = x;
            this.y = y;
            this.xShift = xShift;
        }
        
        int getXShift() {
            return xShift;
        }
        
        void paint(Graphics g) {
            drawArcPipe(g, MapXCoordinate(x), MapYCoordinate(y) - unitHeight/4, unitWidth*xShift*2, unitHeight, 180, 90);
        }
        
        void trace() {
            System.out.print("curving right to minimum at");
        }
        
        double getInitialStraightLineLength() {
            return unitHeight / 4;
        }
        
        double getInitialStraightLineDirection() {
            return DegreesToRadians(270);
        }
        
        double getFinalStraightLineLength() {
//            assert(false);
            return 0;
        }
        
        double getFinalStraightLineDirection() {
//            assert(false);
            return 0;
        }
        
    }
    
    /**
     * A section that curves down to the left to a minimum point.
     * All segments end with a section of this class or of MinimumFromTheLeftSection.
     */
    private class MinimumFromTheRightSection extends Section {
        private int xShift;

        MinimumFromTheRightSection(int x, int y, int xShift) {
            this.x = x;
            this.y = y;
            this.xShift = xShift;
        }
        
        int getXShift() {
            return -xShift;
        }
        
        void paint(Graphics g) {
            drawArcPipe(g, MapXCoordinate(x-2*xShift), MapYCoordinate(y) - unitHeight/4, unitWidth*xShift*2, unitHeight, 270, 90);
        }
        
        void trace() {
            System.out.print("curving left to minimum at");
        }
        
        double getInitialStraightLineLength() {
            return unitHeight / 4;
        }
        
        double getInitialStraightLineDirection() {
            return DegreesToRadians(270);
        }
        
        double getFinalStraightLineLength() {
//            assert(false);
            return 0;
        }
        
        double getFinalStraightLineDirection() {
//            assert(false);
            return 0;
        }
        
    }
    
    /**
     * Cross another line, going down and to the left or to the right,
     * and passing on top of the other line.
     */
    private class CrossOnTopSection extends Section {
        private int xShift;

        CrossOnTopSection(int x, int y, int xShift) {
            this.x = x;
            this.y = y;
            this.xShift = xShift;
        }
        
        int getXShift() {
            return xShift;
        }
        
        void paint(Graphics g) {
            // There is nothing to paint here because there is nothing in this section
            // between the first half of a straight section and the second half of
            // a straight section, and these straight sections are handled by
            // the transitioning code that draws a smooth curve between one section
            // and another.
            
            // We are painting a section of the string that passes over another
            // section of string.  It is very complex to have to paint the
            // underneath part of the string so that we stop painting when we
            // get to the edge of the overlying part of the string.
            // We therefore paint the overlying part of the string in such a way
            // that we fill the inside with the background color.
            
            // This is easy for a straight line.  We simply use the fillPolygon.
            // It is a little more tricky for arcs.  Here is how we do it:
            // 1. Read the documentation for Graphics.XORMode.  This function can
            //    be used in conjunction with fillArc to reverse the background
            //    and the drawing colors.
            // 2. By reversing the larger arc area once and the smaller arc area
            //    once, the area between the two arcs of the pipe can be reversed.
            // 3. Paint the underneath section, reverse the area inside the pipe, 
            //    paint the underneath section again, then reverse the area inside
            //    the pipe again.
            
            
        }
        
        void trace() {
            System.out.print("crossing by " + xShift + " units, passing on top");
        }
        
        double getInitialStraightLineLength() {
            return Math.sqrt(unitWidth * unitWidth * xShift * xShift + unitHeight * unitHeight) / 2;
        }
        
        double getInitialStraightLineDirection() {
            return Math.atan2(-unitHeight, unitWidth * xShift);
        }
        
        double getFinalStraightLineLength() {
            return Math.sqrt(unitWidth * unitWidth * xShift * xShift + unitHeight * unitHeight) / 2;
        }
        
        double getFinalStraightLineDirection() {
            return Math.atan2(-unitHeight, unitWidth * xShift);
        }
        
    }
    
    /**
     * Cross another line, going down and either to the left or the right, and 
     * passing underneath the other line.
     */
    private class CrossUnderneathSection extends Section {
        
        private int xShift;

        double shortenAmount;
        
        /**
         * @param xShift The number of units that the line shifts as it crosses down.
         *      This is negative if the line crosses to the left and positive
         *      if the line crosses to the right.
         */
        CrossUnderneathSection(int x, int y, int xShift) {
            this.x = x;
            this.y = y;
            this.xShift = xShift;

            // We draw only the very short section of string that overlaps with
            // the other line.  This means we only have one side of the string
            // to draw on one side of the crossing string, and the other side
            // of the string to draw on the other side of the crossing string.
            
            // Calculate angle between two lines.
            // This is the angle that, when added to the direction of this line, 
            // gives the direction of the other line.
            double angle = Math.atan2(unitWidth * Math.abs(xShift), unitHeight) * 2;

            // It does not matter whether the crossing (obscuring) line is going
            // is one direction or the opposite direction, so reduce modulo 180
            // to the range -90 to 90.
            while (angle < -Math.PI/2) {
                angle += Math.PI;
            }
            while (angle > Math.PI/2) {
                angle -= Math.PI;
            }
            
            double rightSideShortenedDistance;
            double leftSideShortenedDistance;
            double centerShortenedDistance;
            
            if (angle > 0) {
                // angle is in range 0 to 90
                // shortest is right line, longest is left line
                
                rightSideShortenedDistance =
                stringRadius * (Math.tan(Math.PI/2 - angle) + 1 / Math.sin(angle));
                leftSideShortenedDistance =
                stringRadius * (-Math.tan(Math.PI/2 - angle)  + 1 / Math.sin(angle));
                centerShortenedDistance = 1 / Math.sin(angle);
                
                shortenAmount = rightSideShortenedDistance;
            } else {
                // angle is in range -90 to 0
                // shortest is right line, longest is left line
                
                rightSideShortenedDistance =
                stringRadius * (-Math.tan(Math.PI/2 + angle) + 1 / Math.sin(-angle));
                leftSideShortenedDistance =
                stringRadius * (Math.tan(Math.PI/2 + angle)  + 1 / Math.sin(-angle));
                centerShortenedDistance = 1 / Math.sin(-angle);

                shortenAmount = leftSideShortenedDistance;
            }
            
            
        }
        
        int getXShift() {
            return xShift;
        }
        
        void paint(Graphics g) {
            // TODO: paint the part of the string where one side is hidden and the
            // other side shows.
        }
        
        void trace() {
            System.out.print("crossing by " + xShift + " units, passing underneath");
        }
        
        double getInitialStraightLineLength() {
            return Math.sqrt(unitWidth * unitWidth * xShift * xShift + unitHeight * unitHeight) * (Math.abs(xShift)-1) / Math.abs(xShift) - shortenAmount;
        }
        
        double getInitialStraightLineDirection() {
            return Math.atan2(-unitHeight, unitWidth * xShift);
        }
        
        double getFinalStraightLineLength() {
            return Math.sqrt(unitWidth * unitWidth * xShift * xShift + unitHeight * unitHeight) / Math.abs(xShift) - shortenAmount;
        }
        
        double getFinalStraightLineDirection() {
            return Math.atan2(-unitHeight, unitWidth * xShift);
        }
    }
    
    /**
     * A line straight down.
     * This section is used as a filler.  Whenever a section needs to be added to
     * a segment or segments, a stright line is added to all other segments
     * to bring them down to the same height.
     * curve from a maximum point down to the left.
     * All segments start with a section of this class or of MaximumToRightSection.
     */
    private class StraightSection extends Section {
        StraightSection(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        int getXShift() {
            return 0;
        }
        
        void paint(Graphics g) {
//            g.drawLine(MapXCoordinate(x), MapYCoordinate(y), MapXCoordinate(x), MapYCoordinate(y+1));
        }
        
        void trace() {
            System.out.print("straight down to");
        }
        
        double getInitialStraightLineLength() {
            return unitHeight/2;
        }
        
        double getInitialStraightLineDirection() {
            return DegreesToRadians(270);
        }
        
        double getFinalStraightLineLength() {
            return unitHeight/2;
        }
        
        double getFinalStraightLineDirection() {
            return DegreesToRadians(270);
        }
        
    }
    
    private class DrawingMouseListener implements MouseListener {
        
        /**
         * Invoked when the mouse button has been clicked (pressed
         * and released) on a component.
         */
        public void mouseClicked(MouseEvent e) {
            int i;
            Segment orderedSegments[] = buildOrderedSegments();
            for (i = 0; i < orderedSegments.length; i++) {
                if (e.getX() < MapXCoordinate(orderedSegments[i].xCurrent)) {
                    break;
                }
            }
            
            int newRedLinePosition = i;
            while (newRedLinePosition != redLinePosition) {
                if (newRedLinePosition < redLinePosition) {
                    knotEditorApplet.addMoveToLeft();
                } else {
                    knotEditorApplet.addMoveToRight();
                }
            }
            
        }
        
        /**
         * Invoked when a mouse button has been pressed on a component.
         */
        public void mousePressed(MouseEvent e) {};
        
        /**
         * Invoked when a mouse button has been released on a component.
         */
        public void mouseReleased(MouseEvent e) {};
        
        /**
         * Invoked when the mouse enters a component.
         */
        public void mouseEntered(MouseEvent e) {};
        
        /**
         * Invoked when the mouse exits a component.
         */
        public void mouseExited(MouseEvent e) {};
        
    }
    
}
