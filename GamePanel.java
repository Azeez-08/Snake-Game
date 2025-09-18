package Project;

import org.jogamp.java3d.*;
import org.jogamp.java3d.utils.universe.SimpleUniverse;
import org.jogamp.java3d.utils.geometry.Box;
import org.jogamp.java3d.utils.geometry.Sphere;
import org.jogamp.java3d.utils.image.TextureLoader;
import org.jogamp.java3d.OrientedShape3D;
import org.jogamp.java3d.utils.picking.PickCanvas;
import org.jogamp.java3d.utils.picking.PickResult;
import org.jogamp.vecmath.*;
import org.jogamp.vecmath.Point3d;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Iterator;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class GamePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private MainApp parent;
    private Canvas3D canvas;
    private SimpleUniverse universe;
    private BranchGroup root;
    private String selectedMap; 
    private String selectedDifficulty;
    private boolean isHardMode = false;
    
    private TransformGroup snakeTG; // To hold the snake's transform group
    private Snake snake;  // Declare Snake object here

    public GamePanel(MainApp parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
    }

    public void setSelectedMap(String selectedMap) {
        this.selectedMap = selectedMap;
        System.out.println("Selected Map in GamePanel: " + selectedMap);
        loadTexture(selectedMap);
    }

    public void setSelectedDifficulty(String selectedDifficulty) {
        this.selectedDifficulty = selectedDifficulty;
        setDifficultyMode(selectedDifficulty); // Set difficulty restrictions here
    }

    private void setDifficultyMode(String difficulty) {
        if ("hard".equalsIgnoreCase(difficulty)) {
            isHardMode = true;  // Enable hard mode restrictions
        } else {
            isHardMode = false;  // Easy mode, remove restrictions
        }
    }

    public void start3D() {
        removeAll();
        canvas = new Canvas3D(SimpleUniverse.getPreferredConfiguration());
        add(canvas, BorderLayout.CENTER);
        revalidate();
        repaint();

        universe = new SimpleUniverse(canvas);
        universe.getViewingPlatform().setNominalViewingTransform();

        root = createSceneGraph();
        root.compile();
        universe.addBranchGraph(root);
    }

    public void stop3D() {
        if (universe != null) {
            universe.cleanup();
            universe = null;
        }
        removeAll();
        revalidate();
        repaint();
    }
    
    private BranchGroup createSceneGraph() {
        BranchGroup bg = new BranchGroup();

        // background and ambient light (dark background for hard mode)
        if (isHardMode) {
            // Hard mode setup: Dark background and spotlight
            Background bgColor = new Background(0.1f, 0.1f, 0.1f); // Dark background for hard mode
            bgColor.setApplicationBounds(new BoundingSphere(new Point3d(0, 0, 0), 100));
            bg.addChild(bgColor);

            Color3f ambientColor = new Color3f(0.2f, 0.2f, 0.2f); // Dim ambient light for hard mode
            AmbientLight ambient = new AmbientLight(ambientColor);
            ambient.setInfluencingBounds(new BoundingSphere(new Point3d(), 100));
            bg.addChild(ambient);

            // Add a spotlight centered on the snake's position
            addSpotlight(bg);
        } else {
            // Easy mode setup: Light background and standard ambient light
            Background bgColor = new Background(0.2f, 0.2f, 0.5f);  // Light background for easy mode
            bgColor.setApplicationBounds(new BoundingSphere(new Point3d(0, 0, 0), 100));
            bg.addChild(bgColor);

            Color3f ambientColor = new Color3f(1, 1, 1); // Standard ambient light for easy mode
            AmbientLight ambient = new AmbientLight(ambientColor);
            ambient.setInfluencingBounds(new BoundingSphere(new Point3d(), 100));
            bg.addChild(ambient);
        }

        bg.addChild(createGround());
        bg.addChild(createBorder());

        TransformGroup gameTG = new TransformGroup();
        gameTG.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
        bg.addChild(gameTG);

        // Initialize Snake object
        snake = new Snake(new Vector3f(0f, -0.4f, 0f));
        snakeTG = snake.getTransformGroup(); // Store snake's transform group
        gameTG.addChild(snakeTG);

        SnakeMoveBehavior moveBehavior = new SnakeMoveBehavior(snake);
        moveBehavior.setSchedulingBounds(new BoundingSphere(new Point3d(), 100));
        gameTG.addChild(moveBehavior);

        Snake.SnakeBehavior snakeBehavior = snake.new SnakeBehavior();
        snakeBehavior.setSchedulingBounds(new BoundingSphere(new Point3d(), 100));
        gameTG.addChild(snakeBehavior);

        TransformGroup appleTG = createMorphingAppleTG(2f, 1f);
        gameTG.addChild(appleTG);

        AppleCollisionBehavior appleCollision = new AppleCollisionBehavior(snake, appleTG);
        appleCollision.setSchedulingBounds(new BoundingSphere(new Point3d(), 100));
        gameTG.addChild(appleCollision);

        AppleMorphPickBehavior pickBehavior = new AppleMorphPickBehavior(canvas, bg);
        pickBehavior.setSchedulingBounds(new BoundingSphere(new Point3d(), 100));
        bg.addChild(pickBehavior);

        TransformGroup billboardTG = createBillboard();
        gameTG.addChild(billboardTG);

        // Camera behavior based on difficulty
        TransformGroup viewTG = universe.getViewingPlatform().getViewPlatformTransform();
        Transform3D viewTransform = new Transform3D();

        if (isHardMode) {
            // For hard mode: first-person POV camera (no zoom, but WASD movement allowed)
            updateCameraPosition(viewTransform);  // Update camera position based on snake's position and rotation
        } else {
            // Default camera setup for easy mode (zoom is enabled here)
            viewTransform.lookAt(new Point3d(5, 5, 3), new Point3d(0, -0.4, 0), new Vector3d(0, 1, 0));
            viewTransform.invert();

            CameraWASDBehavior camBehavior = new CameraWASDBehavior(viewTG);
            camBehavior.setSchedulingBounds(new BoundingSphere(new Point3d(), 100));
            bg.addChild(camBehavior);
        }

        universe.getViewingPlatform().getViewPlatformTransform().setTransform(viewTransform);

        return bg;
    }

    private void addSpotlight(BranchGroup bg) {
        // Create a spotlight at the snake's position
        Point3f position = new Point3f(0.0f, 0.5f, 1.0f);  // Example position, can adjust based on snake position
        SpotLight spotlight = new SpotLight(new Color3f(1.0f, 1.0f, 1.0f), position, new Point3f(0f, 0f, 0f), new Vector3f(0f, -1f, 0f), (float)Math.PI / 4, 1.0f);
        spotlight.setInfluencingBounds(new BoundingSphere(new Point3d(0, 0, 0), 100));
        bg.addChild(spotlight);
    }

    private void updateCameraPosition(Transform3D viewTransform) {
        // Get the snake's position
        Vector3f snakePosition = new Vector3f();
        Transform3D snakeTransform = new Transform3D();
        snakeTG.getTransform(snakeTransform); // Get the current transform of the snake
        snakeTransform.get(snakePosition); // Extract the position from the transform

        // Get the snake's forward direction (this assumes 'direction' is a normalized vector pointing the snake's forward direction)
        Vector3f forwardDirection = snake.getDirection(); // Use snake's direction method

        // We want the camera to be positioned behind and slightly above the snake
        // Camera position offset: behind the snake by 1.5 units, and above the snake by 0.5 units
        Vector3f cameraOffset = new Vector3f(0f, 0.5f, 1.5f);

        // Calculate camera's position relative to the snake
        Vector3f cameraPosition = new Vector3f(snakePosition);
        cameraPosition.add(forwardDirection);  // Adjust position based on snake’s forward direction
        cameraPosition.add(cameraOffset);  // Add the offset for positioning the camera behind the snake

        // Set the camera position
        viewTransform.setTranslation(cameraPosition);

        // Apply the snake's rotation to the camera (rotate the camera based on the snake's direction)
        Transform3D cameraRotation = new Transform3D();
        cameraRotation.rotY(Math.atan2(forwardDirection.z, forwardDirection.x));  // Rotate to align with snake's forward direction
        viewTransform.mul(cameraRotation);  // Apply the camera rotation to the view transform

        // Set the camera to always look at the snake's position
        viewTransform.lookAt(new Point3d(snakePosition.x, snakePosition.y, snakePosition.z),  // Focus on the snake
                              new Point3d(snakePosition.x, snakePosition.y, snakePosition.z - 1),  // Look slightly in front of the snake
                              new Vector3d(0, 1, 0));  // Camera's up vector is along the Y-axis
    }


    private BranchGroup createGround() {
        BranchGroup groundBG = new BranchGroup();
        QuadArray groundQuad = new QuadArray(4, GeometryArray.COORDINATES | GeometryArray.TEXTURE_COORDINATE_2);

        // Define coordinates for the ground
        groundQuad.setCoordinate(0, new Point3f(-5f, -0.5f, 5f));
        groundQuad.setCoordinate(1, new Point3f(5f, -0.5f, 5f));
        groundQuad.setCoordinate(2, new Point3f(5f, -0.5f, -5f));
        groundQuad.setCoordinate(3, new Point3f(-5f, -0.5f, -5f));

        // Set texture coordinates
        groundQuad.setTextureCoordinate(0, 0, new TexCoord2f(0, 0));
        groundQuad.setTextureCoordinate(0, 1, new TexCoord2f(1, 0));
        groundQuad.setTextureCoordinate(0, 2, new TexCoord2f(1, 1));
        groundQuad.setTextureCoordinate(0, 3, new TexCoord2f(0, 1));

        // Use the selected texture for the ground
        Texture texture = loadTexture(selectedMap);  // Use selected map name for texture
        if (texture == null) {
            System.out.println("Texture " + selectedMap + " not loaded, using default.");
            texture = loadTexture("groundTexture"); // Fallback texture
        }

        Appearance app = new Appearance();
        app.setTexture(texture);
        Shape3D groundShape = new Shape3D(groundQuad, app);
        TransformGroup groundTG = new TransformGroup();
        groundTG.addChild(groundShape);
        groundBG.addChild(groundTG);

        return groundBG;
    }

    // This method is used to load a texture based on the map name.
    private Texture loadTexture(String textureName) {
        try {
            String texturePath = "resources/" + textureName + ".jpg";
            File textureFile = new File(texturePath);

            // Check if the texture file exists
            if (!textureFile.exists()) {
                System.err.println("Texture file not found: " + texturePath);
                return null;
            }

            // Load the texture
            TextureLoader textureLoader = new TextureLoader(texturePath, this);
            Texture texture = textureLoader.getTexture();
            return texture;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading texture: " + textureName);
            return null;
        }
    }


    private Texture textured_App(String textureName) {
        // Construct the filename based on the texture name parameter
        String filename = "resources/" + textureName + ".jpg";  // E.g., resources/oceanTexture.jpg
        System.out.println("Loading texture from: " + filename);
        
        // Load the texture from the file
        TextureLoader loader = new TextureLoader(filename, null);
        ImageComponent2D image = loader.getImage(); // Load the image from the file

        // If the image is not found or cannot be loaded, print an error message
        if (image == null) {
            System.out.println("Cannot load file: " + filename);
            return null;  // Return null if the texture loading fails
        }

        // Create a 2D texture object
        Texture2D texture = new Texture2D(Texture.BASE_LEVEL,
                Texture.RGBA, image.getWidth(), image.getHeight());
        texture.setImage(0, image); // Set the loaded image as the texture

        return texture;  // Return the texture object
    }

    private BranchGroup createBorder() {
        BranchGroup borderBG = new BranchGroup();

        // Left border with a red color
        Appearance leftBorderApp = new Appearance();
        ColoringAttributes leftCA = new ColoringAttributes(new Color3f(1f, 0f, 0f), ColoringAttributes.NICEST); // Red
        leftBorderApp.setColoringAttributes(leftCA);
        Box leftBorder = new Box(0.05f, 0.5f, 5f, leftBorderApp);
        TransformGroup leftTG = new TransformGroup();
        Transform3D leftPos = new Transform3D();
        leftPos.setTranslation(new Vector3f(-5f, -0.45f, 0f));
        leftTG.setTransform(leftPos);
        leftTG.addChild(leftBorder);
        borderBG.addChild(leftTG);

        // Right border with a green color
        Appearance rightBorderApp = new Appearance();
        ColoringAttributes rightCA = new ColoringAttributes(new Color3f(0f, 1f, 0f), ColoringAttributes.NICEST); // Green
        rightBorderApp.setColoringAttributes(rightCA);
        Box rightBorder = new Box(0.05f, 0.5f, 5f, rightBorderApp);
        TransformGroup rightTG = new TransformGroup();
        Transform3D rightPos = new Transform3D();
        rightPos.setTranslation(new Vector3f(5f, -0.45f, 0f));
        rightTG.setTransform(rightPos);
        rightTG.addChild(rightBorder);
        borderBG.addChild(rightTG);

        // Top border with a blue color
        Appearance topBorderApp = new Appearance();
        ColoringAttributes topCA = new ColoringAttributes(new Color3f(0f, 0f, 1f), ColoringAttributes.NICEST); // Blue
        topBorderApp.setColoringAttributes(topCA);
        Box topBorder = new Box(5f, 0.5f, 0.05f, topBorderApp);
        TransformGroup topTG = new TransformGroup();
        Transform3D topPos = new Transform3D();
        topPos.setTranslation(new Vector3f(0f, -0.45f, 5f));
        topTG.setTransform(topPos);
        topTG.addChild(topBorder);
        borderBG.addChild(topTG);

        // Bottom border with a yellow color
        Appearance bottomBorderApp = new Appearance();
        ColoringAttributes bottomCA = new ColoringAttributes(new Color3f(1f, 1f, 0f), ColoringAttributes.NICEST); // Yellow
        bottomBorderApp.setColoringAttributes(bottomCA);
        Box bottomBorder = new Box(5f, 0.5f, 0.05f, bottomBorderApp);
        TransformGroup bottomTG = new TransformGroup();
        Transform3D bottomPos = new Transform3D();
        bottomPos.setTranslation(new Vector3f(0f, -0.45f, -5f));
        bottomTG.setTransform(bottomPos);
        bottomTG.addChild(bottomBorder);
        borderBG.addChild(bottomTG);

        return borderBG;
    }


    private TransformGroup createMorphingAppleTG(float x, float z) {
        TransformGroup appleTG = new TransformGroup();
        appleTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        Transform3D pos = new Transform3D();
        pos.setTranslation(new Vector3f(x, -0.4f, z));
        appleTG.setTransform(pos);

        GeometryArray geom1 = createTetrahedronGeometry(0.05f);
        GeometryArray geom2 = createTetrahedronGeometry(0.075f);
        GeometryArray[] geoms = { geom1, geom2 };

        Appearance morphApp = new Appearance();
        ColoringAttributes redCA = new ColoringAttributes(new Color3f(1f, 0f, 0f), ColoringAttributes.NICEST);
        morphApp.setColoringAttributes(redCA);

        Morph morphApple = new Morph(geoms, morphApp);
        morphApple.setCapability(Morph.ALLOW_WEIGHTS_READ);
        morphApple.setCapability(Morph.ALLOW_WEIGHTS_WRITE);
        double[] weights = {1.0, 0.0};  // Initially show first geometry.
        morphApple.setWeights(weights);

        morphApple.setUserData("appleMorph");

        appleTG.addChild(morphApple);
        return appleTG;
    }

    private GeometryArray createTetrahedronGeometry(float size) {
        TriangleArray ta = new TriangleArray(12, GeometryArray.COORDINATES);
        Point3f v0 = new Point3f(0f, size, 0f);
        Point3f v1 = new Point3f(-size, -size, size);
        Point3f v2 = new Point3f(size, -size, size);
        Point3f v3 = new Point3f(0f, -size, -size);
        ta.setCoordinate(0, v0);
        ta.setCoordinate(1, v1);
        ta.setCoordinate(2, v2);
        ta.setCoordinate(3, v0);
        ta.setCoordinate(4, v2);
        ta.setCoordinate(5, v3);
        ta.setCoordinate(6, v0);
        ta.setCoordinate(7, v3);
        ta.setCoordinate(8, v1);
        ta.setCoordinate(9, v1);
        ta.setCoordinate(10, v3);
        ta.setCoordinate(11, v2);
        return ta;
    }

    
    // Behavior for mouse picking the morphing apple.
    private class AppleMorphPickBehavior extends Behavior {
        private WakeupCondition wakeupCondition;
        private Canvas3D canvas;
        private BranchGroup scene;
        
        public AppleMorphPickBehavior(Canvas3D canvas, BranchGroup scene) {
            this.canvas = canvas;
            this.scene = scene;
        }
        
        @Override
        public void initialize() {
            wakeupCondition = new WakeupOnAWTEvent(MouseEvent.MOUSE_CLICKED);
            wakeupOn(wakeupCondition);
        }
        
        @Override
        public void processStimulus(Iterator<WakeupCriterion> criteria) {
            while (criteria.hasNext()) {
                WakeupCriterion wc = criteria.next();
                if (wc instanceof WakeupOnAWTEvent) {
                    AWTEvent[] events = ((WakeupOnAWTEvent) wc).getAWTEvent();
                    for (AWTEvent event : events) {
                        if (event instanceof MouseEvent) {
                            MouseEvent me = (MouseEvent) event;
                            PickCanvas pickCanvas = new PickCanvas(canvas, scene);
                            pickCanvas.setMode(PickCanvas.BOUNDS);
                            pickCanvas.setShapeLocation(me);
                            PickResult result = pickCanvas.pickClosest();
                            if (result != null) {
                                Node picked = result.getObject();
                                if (picked != null && picked.getUserData() != null &&
                                    picked.getUserData().toString().equals("appleMorph")) {
                                    System.out.println("Apple clicked, toggling morph!");
                                    Morph morph = (Morph) picked;
                                    double[] weights = morph.getWeights();
                                    // Toggle weights with a collision threshold of 0.5.
                                    if (weights[0] > 0.5) {
                                        weights[0] = 0.0;
                                        weights[1] = 1.0;
                                    } else {
                                        weights[0] = 1.0;
                                        weights[1] = 0.0;
                                    }
                                    morph.setWeights(weights);
                                }
                            }
                        }
                    }
                }
            }
            wakeupOn(wakeupCondition);
        }
    }
    
    private class AppleCollisionBehavior extends Behavior {
        private WakeupCondition wakeupCondition;
        private Snake snake;
        private TransformGroup appleTG;
        private boolean appleEaten = false; // Track if apple has been eaten

        public AppleCollisionBehavior(Snake snake, TransformGroup appleTG) {
            this.snake = snake;
            this.appleTG = appleTG;
        }

        @Override
        public void initialize() {
            wakeupCondition = new WakeupOnElapsedTime(100); // You can fine-tune this to check less often
            wakeupOn(wakeupCondition);
        }

        @Override
        public void processStimulus(Iterator<WakeupCriterion> criteria) {
            Vector3f headPos = snake.getHeadPosition();
            Transform3D appleT3d = new Transform3D();
            appleTG.getTransform(appleT3d);
            Vector3f applePos = new Vector3f();
            appleT3d.get(applePos);

            // Check if the snake's head is within a threshold distance of the apple
            Vector3f diff = new Vector3f();
            diff.sub(headPos, applePos);
            if (diff.length() < 0.2f && !appleEaten) {  // Apple eaten check to avoid repetitive actions
                appleEaten = true; // Mark apple as eaten

                // Snake growth logic (add 7 segments)
                for (int i = 0; i < 7; i++) {
                    snake.addSegment();
                }

                // Reposition apple after collision
                repositionApple(appleTG);

                // Play audio once
                AudioLoader audioLoader = new AudioLoader();
                audioLoader.playAudio("apple_eaten.wav");

                System.out.println("Apple eaten! Snake grows.");
            }

            wakeupOn(wakeupCondition); // Continue checking for collisions
        }

        private void repositionApple(TransformGroup appleTG) {
            // Randomize apple's position after being eaten
            float newX = (float)(Math.random() * 8 - 4);
            float newZ = (float)(Math.random() * 8 - 4);
            Transform3D newPos = new Transform3D();
            newPos.setTranslation(new Vector3f(newX, -0.4f, newZ));
            appleTG.setTransform(newPos);

            appleEaten = false; // Reset apple eaten flag after repositioning
        }
    }

    
    private TransformGroup createBillboard() {
        // Create a quad (2 x 2 units) for the billboard.
        QuadArray quad = new QuadArray(4,
                GeometryArray.COORDINATES | GeometryArray.TEXTURE_COORDINATE_2);
        quad.setCoordinate(0, new Point3f(-1f, 1f, 0f));
        quad.setCoordinate(1, new Point3f(1f, 1f, 0f));
        quad.setCoordinate(2, new Point3f(1f, -1f, 0f));
        quad.setCoordinate(3, new Point3f(-1f, -1f, 0f));
        quad.setTextureCoordinate(0, 0, new TexCoord2f(0, 0));
        quad.setTextureCoordinate(0, 1, new TexCoord2f(1, 0));
        quad.setTextureCoordinate(0, 2, new TexCoord2f(1, 1));
        quad.setTextureCoordinate(0, 3, new TexCoord2f(0, 1));
        
        // Create an appearance with a bright red color.
        Appearance app = new Appearance();
        ColoringAttributes ca = new ColoringAttributes(new Color3f(1f, 0f, 0f),
                                                        ColoringAttributes.NICEST);
        app.setColoringAttributes(ca);
        
        // Create the billboard so it always faces the viewer.
        OrientedShape3D billboard = new OrientedShape3D(quad, app,
                OrientedShape3D.ROTATE_ABOUT_POINT, new Point3f(0, 0, 0));
        
        // Create a TransformGroup to position and scale the billboard.
        TransformGroup tg = new TransformGroup();
        Transform3D trans = new Transform3D();
        // Position the billboard at (0, 3, -5)
        trans.setTranslation(new Vector3f(0f, 3f, -5f));
        Transform3D scale = new Transform3D();
        scale.setScale(3.0);
        trans.mul(scale);
        tg.setTransform(trans);
        tg.addChild(billboard);
        return tg;
    }
    
    private class CameraWASDBehavior extends Behavior {
        private WakeupCondition wakeupCondition;
        private TransformGroup viewTG;
        private float zoomSpeed = 0.1f; // Speed of zooming
        private float movementSpeed = 0.1f; // Speed of normal movement

        public CameraWASDBehavior(TransformGroup viewTG) {
            this.viewTG = viewTG;
        }

        @Override
        public void initialize() {
            wakeupCondition = new WakeupOnAWTEvent(java.awt.event.KeyEvent.KEY_PRESSED);
            wakeupOn(wakeupCondition);
        }

        @Override
        public void processStimulus(Iterator<WakeupCriterion> criteria) {
            while (criteria.hasNext()) {
                WakeupCriterion wakeup = criteria.next();
                if (wakeup instanceof WakeupOnAWTEvent) {
                    AWTEvent[] events = ((WakeupOnAWTEvent) wakeup).getAWTEvent();
                    for (AWTEvent event : events) {
                        if (event instanceof java.awt.event.KeyEvent) {
                            java.awt.event.KeyEvent ke = (java.awt.event.KeyEvent) event;
                            Transform3D currTransform = new Transform3D();
                            viewTG.getTransform(currTransform);
                            Vector3f translation = new Vector3f();
                            currTransform.get(translation); // Get current camera position

                            // WASD movement logic (for directional movement)
                            if (ke.getKeyCode() == java.awt.event.KeyEvent.VK_W) {
                                translation.z -= movementSpeed; // Move forward
                            } else if (ke.getKeyCode() == java.awt.event.KeyEvent.VK_S) {
                                translation.z += movementSpeed; // Move backward
                            } else if (ke.getKeyCode() == java.awt.event.KeyEvent.VK_A) {
                                translation.x -= movementSpeed; // Move left
                            } else if (ke.getKeyCode() == java.awt.event.KeyEvent.VK_D) {
                                translation.x += movementSpeed; // Move right
                            }

                            // Zoom in (Z key) -- move the camera forward along its view direction
                            else if (ke.getKeyCode() == java.awt.event.KeyEvent.VK_Z) {
                                // Get the camera's rotation matrix
                                Matrix3f rotation = new Matrix3f();
                                currTransform.getRotationScale(rotation);
                                
                                // Extract the camera's forward direction (typically negative Z in local space)
                                Vector3f forward = new Vector3f();
                                rotation.getColumn(2, forward); // Extract the third column (Z axis)

                                // Move the camera along the forward direction (zoom in)
                                translation.sub(forward); // Move closer to the scene (negative Z direction)
                            }
                            // Zoom out (X key) -- move the camera backward along its view direction
                            else if (ke.getKeyCode() == java.awt.event.KeyEvent.VK_X) {
                                // Get the camera's rotation matrix
                                Matrix3f rotation = new Matrix3f();
                                currTransform.getRotationScale(rotation);
                                
                                // Extract the camera's forward direction (typically negative Z in local space)
                                Vector3f forward = new Vector3f();
                                rotation.getColumn(2, forward); // Extract the third column (Z axis)

                                // Move the camera along the opposite of the forward direction (zoom out)
                                translation.add(forward); // Move away from the scene (positive Z direction)
                            }

                            // Update the camera's transform
                            currTransform.setTranslation(translation);
                            viewTG.setTransform(currTransform);
                        }
                    }
                }
            }
            wakeupOn(wakeupCondition);
        }
    }
}
