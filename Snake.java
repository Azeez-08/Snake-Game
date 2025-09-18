package Project;

import org.jogamp.java3d.*;
import org.jogamp.java3d.utils.geometry.Box;
import org.jogamp.java3d.utils.geometry.ColorCube;
import org.jogamp.vecmath.*;

import java.awt.AWTEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;

public class Snake {
    private TransformGroup snakeTG;
    private ArrayList<TransformGroup> segments;
    private Vector3f headPosition;
    private Vector3f direction;
    private final float FIXED_Y = -0.4f;
    
    public Snake(Vector3f startPosition) {
        snakeTG = new TransformGroup();
        snakeTG.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
        snakeTG.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
        
        segments = new ArrayList<>();
        headPosition = new Vector3f(startPosition);
        headPosition.y = FIXED_Y;
        direction = new Vector3f(0f, 0f, 0.01f);
        
        TransformGroup headSegment = createSegment(headPosition);
        segments.add(headSegment);
        snakeTG.addChild(headSegment);
    }
    
    private TransformGroup createSegment(Vector3f pos) {
        TransformGroup segTG = new TransformGroup();
        segTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        Transform3D t3d = new Transform3D();
        t3d.setTranslation(pos);
        segTG.setTransform(t3d);
        // Replace the ColorCube with a custom green cube.
        segTG.addChild(createGreenCube(0.1f));
        return segTG;
    }

    private Node createGreenCube(float halfSize) {
        // Create a Box with the desired half extents and set its appearance to green.
        Appearance greenApp = new Appearance();
        ColoringAttributes ca = new ColoringAttributes(new Color3f(0f, 1f, 0f), ColoringAttributes.NICEST);
        greenApp.setColoringAttributes(ca);
        // Box takes half extents (so 0.1f means the full cube is 0.2 units on a side).
        Box box = new Box(halfSize, halfSize, halfSize, greenApp);
        return box;
    }
    
    public void move() {
        ArrayList<Vector3f> prevPositions = new ArrayList<>();
        for (TransformGroup seg : segments) {
            Transform3D t3d = new Transform3D();
            seg.getTransform(t3d);
            Vector3f pos = new Vector3f();
            t3d.get(pos);
            prevPositions.add(pos);
        }

        // Update head position using the current direction
        headPosition.add(direction);
        headPosition.y = FIXED_Y;

        // Border collision check
        if (headPosition.x < -5f || headPosition.x > 5f ||
            headPosition.z < -5f || headPosition.z > 5f) {
            System.out.println("Game Over: Snake hit the border!");
            if (MainApp.instance != null) {
                MainApp.instance.showGameOverScreen();
            }
            return;
        }

        // Update the head's position
        Transform3D headT3d = new Transform3D();
        headT3d.setTranslation(headPosition);
        segments.get(0).setTransform(headT3d);

        // Move the rest of the segments
        for (int i = 1; i < segments.size(); i++) {
            Transform3D segT3d = new Transform3D();
            segT3d.setTranslation(prevPositions.get(i - 1));
            segments.get(i).setTransform(segT3d);
        }
    }

    
    
    public void addSegment() {
        Transform3D tailT3d = new Transform3D();
        segments.get(segments.size() - 1).getTransform(tailT3d);
        Vector3f tailPos = new Vector3f();
        tailT3d.get(tailPos);
        TransformGroup newSegment = createSegment(new Vector3f(tailPos));
        segments.add(newSegment);
        BranchGroup bg = new BranchGroup();
        bg.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        bg.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        bg.addChild(newSegment);
        snakeTG.addChild(bg);
        System.out.println("New snake segment added!");
    }
    
    public void turnLeft() {
        // Rotate the direction vector 90 degrees counterclockwise (left)
        Transform3D rot = new Transform3D();
        rot.rotY(Math.toRadians(90));  // Rotate 90 degrees counterclockwise
        rot.transform(direction);
        System.out.println("Turn left");
    }

    public void turnRight() {
        // Rotate the direction vector 90 degrees clockwise (right)
        Transform3D rot = new Transform3D();
        rot.rotY(Math.toRadians(-90));  // Rotate 90 degrees clockwise
        rot.transform(direction);
        System.out.println("Turn right");
    }

    public Vector3f getDirection() {
        return direction;
    }
    
    public TransformGroup getTransformGroup() {
        return snakeTG;
    }
    
    public Vector3f getHeadPosition() {
        Transform3D headT3d = new Transform3D();
        segments.get(0).getTransform(headT3d);
        Vector3f pos = new Vector3f();
        headT3d.get(pos);
        return pos;
    }
    
    public class SnakeBehavior extends Behavior {
        private WakeupCondition wakeupCondition;
        
        @Override
        public void initialize() {
            wakeupCondition = new WakeupOnAWTEvent(KeyEvent.KEY_PRESSED);
            wakeupOn(wakeupCondition);
        }
        
        @Override
        public void processStimulus(Iterator<WakeupCriterion> criteria) {
            while (criteria.hasNext()) {
                WakeupCriterion wc = criteria.next();
                if (wc instanceof WakeupOnAWTEvent) {
                    AWTEvent[] events = ((WakeupOnAWTEvent) wc).getAWTEvent();
                    for (AWTEvent event : events) {
                        if (event instanceof KeyEvent) {
                            KeyEvent ke = (KeyEvent) event;
                            if (ke.getKeyCode() == KeyEvent.VK_LEFT) {
                                turnLeft();
                            } else if (ke.getKeyCode() == KeyEvent.VK_RIGHT) {
                                turnRight();
                            }
                        }
                    }
                }
            }
            wakeupOn(wakeupCondition);
        }
    }
}
