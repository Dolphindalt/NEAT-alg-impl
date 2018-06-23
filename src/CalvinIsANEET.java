import javax.swing.JFrame;
import java.awt.Canvas;
import java.awt.image.BufferStrategy;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.ArrayList;

/**
 * Thank you my friend William for having me do this.
 * 
 * @author Dalton C
 * @version Saturday, June 22, 2018
 */
public class CalvinIsANEET extends JFrame implements Runnable {

    private static final long serialVersionUID = 1L;
    private static final long gameLength = 10000;

    public static void main(String[] args) {
        new CalvinIsANEET();
    }

    public static final int WIDTH = 500, HEIGHT = 500;

    private Canvas canvas;
    private KeyKeeper keyman;
    private BufferStrategy bs;

    private Network brain1, brain2;
    private List<Game> games = new ArrayList<Game>();

    /**
     * Init jframe and components. Prepare to init networks and games.
     */
    public CalvinIsANEET() {
        setTitle("El calvo diablo");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        keyman = new KeyKeeper();
        canvas = new Canvas();
        canvas.setSize(getSize());
        canvas.setFocusable(true);
        canvas.setVisible(true);
        canvas.addKeyListener(keyman);
        add(canvas);

        init();
    }

    private void init() {
        brain1 = new Network();
        brain2 = new Network();

        run();
    }

    private void createGames() {
        games.clear();
        assert(brain1.totalPopulation.size() == brain2.totalPopulation.size());
        for(int i = 0; i < brain1.totalPopulation.size(); i++) {
            games.add(new Game(brain1.totalPopulation.get(i), brain2.totalPopulation.get(i)));
        }
    }

    /**
     * The main run loop creates the games and begins simulating them. After the game
     * is over, the networks are instructed to evolve, then this is called again.
     */
    public void run() {
        createGames();
        long start = System.currentTimeMillis();
        long elapsed = 0;
        float delta = 0;
        long lastTime = System.nanoTime();
        double amountOfTicks = 144;
        double ns = 1000000000 / amountOfTicks;
        long timer = System.currentTimeMillis();
        int frames = 0;
        while(true) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            while(delta >= 1) {
                if(keyman.keys['e'] == true) 
                    saveBrainsAndExit();
                tick(delta);
                delta--;
            }

            render();
            elapsed = System.currentTimeMillis() - start;
            if(elapsed > gameLength) {
                break;
            }

            frames++;

            if (System.currentTimeMillis() - timer > 1000) {
                timer += 1000;
                //System.out.println("FPS: " + frames);
                frames = 0;
            }
        }

        brain1.evolve();
        brain2.evolve();

        run();
    }

    private void saveBrainsAndExit() {
        brain1.saveBrains("brain1.txt");
        brain2.saveBrains("brain2.txt");
        System.exit(0);
    }

    private void tick(double delta_time) {
        
        for(int i = games.size()-1; i >= 0; i--) {
            Game game = games.get(i);
            game.tick(delta_time);
        }
    }

    private void render() {
        if(canvas.getBufferStrategy() == null) {
            canvas.createBufferStrategy(3);
        }
        bs = canvas.getBufferStrategy();
        Graphics2D g = (Graphics2D) bs.getDrawGraphics();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        for(int i = 0; i < games.size(); i++) {
            games.get(i).render(g);
        }

        bs.show();
        g.dispose();
    }

    public static double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    public static double getAngle(double x1, double y1, double x2, double y2) {
        double angle = Math.atan2(y2, x2) - Math.atan2(y1, x1);
        if(angle < 0) angle += 2 * Math.PI;
        return angle;
    }

    private class KeyKeeper implements KeyListener {
        public boolean[] keys;

        public KeyKeeper() {
            keys = new boolean[256];
        }

        public void keyTyped(KeyEvent e) {}

        public void keyPressed(KeyEvent e) {
            keys[e.getKeyChar()] = true;
        }

        public void keyReleased(KeyEvent e) {
            keys[e.getKeyChar()] = false;
        }
    }

}