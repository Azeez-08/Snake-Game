package Project;

import org.jogamp.java3d.*;
import org.jogamp.vecmath.Point3d;
import java.util.Iterator;

public class SnakeMoveBehavior extends Behavior {
    private Snake snake;
    private WakeupCondition wakeupCondition;
    
    public SnakeMoveBehavior(Snake snake) {
        this.snake = snake;
    }
    
    @Override
    public void initialize() {
        wakeupCondition = new WakeupOnElapsedTime(20);
        wakeupOn(wakeupCondition);
    }
    
    @Override
    public void processStimulus(Iterator<WakeupCriterion> criteria) {
        snake.move();
        wakeupOn(wakeupCondition);
    }
}
