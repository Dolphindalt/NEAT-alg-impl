import java.util.List;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

/**
 * I do not want to document this. Just figure it out.
 */
public class Game {

    private Player p1, p2;
    private List<Bullet> p1_bullets = new ArrayList<Bullet>(), p2_bullets = new ArrayList<Bullet>();
    private static final int Y_BOUNDS = 100;

    public Game(Genome g1, Genome g2) {
        g1.createPhenotype();
        g2.createPhenotype();

        p1 = new Player(this, g1, true);
        p2 = new Player(this, g2, false);
    }

    public void tick(double delta) {
        p1.tick(delta, p2);
        p2.tick(delta, p1);
        for(int i = p1_bullets.size()-1; i >= 0; i--) {
            Bullet c = p1_bullets.get(i);
            c.tick(delta);
            if(c.remove)
                p1_bullets.remove(i);
            if(p2.checkCollision(c)) {
                p1.brain.incrementUnweightedFitness();
                p2.brain.decrementUnweightedFitness();
                p1_bullets.remove(c);
            }
        }
        for(int i = p2_bullets.size()-1; i >= 0; i--) {
            Bullet c = p2_bullets.get(i);
            c.tick(delta);
            if(c.remove)
                p2_bullets.remove(i);
            if(p1.checkCollision(c)) {
                p2.brain.incrementUnweightedFitness();
                p1.brain.decrementUnweightedFitness();
                p2_bullets.remove(c);
            }
        }
    }

    public void render(Graphics2D g) {
        p1.render(g);
        p2.render(g);
        for(int i = p1_bullets.size()-1; i >= 0; i--) { // you are probably wondering, why backwards?
            p1_bullets.get(i).render(g);
        }
        for(int i = p2_bullets.size()-1; i >= 0; i--) {
            p2_bullets.get(i).render(g);
        }
    }

    private class Bullet {
        private double px, py;
        private double vx, vy;
        private static final double RADIUS = 10;
        private boolean remove = false;

        public Bullet(double rotation, int px, int py) {
            this.px = (double)px;
            this.py = (double)py;

            vx = 3 * Math.cos(rotation);
            vy = 3 * Math.sin(rotation);
        }

        public void tick(double delta) {
            px += vx * delta;
            py += vy * delta;

            if(px < 0 && py < 0 || px > CalvinIsANEET.WIDTH || py > CalvinIsANEET.HEIGHT) {
                remove = true;
            }
        }

        public void render(Graphics2D g) {
            int newx = (int)(px-(RADIUS/2.0));
            int newy = (int)(py-(RADIUS/2.0));
            g.setColor(Color.GRAY);
            g.fillOval(newx, newy, (int)RADIUS, (int)RADIUS);
        }

        public double getPx() {
            return this.px;
        }

        public double getPy() {
            return this.py;
        }
    }

    private class Player {
        private static final double RADIUS = 50.0, LITTLE_RADIUS = 12.0, DELTA_ROTATION = 0.2;
        private static final double DELTA_PY = 0.5;
        private static final long shot_cooldown = 1000;
        private Game game;
        private Genome brain;
        public final int px;
        private double py;
        private double rotation;
        private double[] inputs, outputs;
        private boolean left;
        private long lastShot = 0;

        public Player(Game game, Genome brain, boolean left) {
            this.game = game;
            this.brain = brain;
            this.left = left;
            inputs = new double[Network.INPUT_LEN];
            outputs = new double[Network.OUTPUT_LEN];
            if(left) {
                px = (CalvinIsANEET.WIDTH/2) - (CalvinIsANEET.WIDTH/4);
                rotation = 0;
            } else {
                px = (CalvinIsANEET.WIDTH/2) + (CalvinIsANEET.WIDTH/4);
                rotation = Math.PI;
            }

            py = CalvinIsANEET.HEIGHT/2;
        }

        public boolean checkCollision(Bullet b) {
            return CalvinIsANEET.distance(b.getPx(), b.getPy(), px + (Math.cos(rotation) * RADIUS), py + (Math.sin(rotation) * RADIUS)) <= Bullet.RADIUS + LITTLE_RADIUS;
        }

        public void render(Graphics2D g) {
            int newx = px-(int)(RADIUS/2.0);
            int newy = (int)(py-(RADIUS/2.0));
            g.setColor((left) ? Color.RED : Color.BLUE);
            g.fillOval(newx, newy, (int)RADIUS, (int)RADIUS);

            newx = (int)(Math.cos(rotation) * RADIUS + px);
            newy = (int)(Math.sin(rotation) * RADIUS + py);
            g.setColor(Color.BLACK);
            g.fillOval(newx, newy, (int)LITTLE_RADIUS, (int)LITTLE_RADIUS);
        }

        public void tick(double delta_time, Player en) {
            setInputs(en);
            if(outputs[0] >= 0.5 && py < CalvinIsANEET.HEIGHT-Y_BOUNDS) {
                py += DELTA_PY * delta_time;
            }
            if(outputs[1] <= 0.5 && py > Y_BOUNDS) {
                py -= DELTA_PY * delta_time;
            }
            if(outputs[2] >= 0.5) {
                rotation += DELTA_ROTATION * delta_time;
            }
            if(outputs[3] <= 0.5) {
                rotation -= DELTA_ROTATION * delta_time;
            }
            if(outputs[4] >= 0.5) {
                if(System.currentTimeMillis() - lastShot > shot_cooldown) {
                    fire();
                    lastShot = System.currentTimeMillis();
                }
            }
        }

        private void fire() {
            if(left)
                game.p1_bullets.add(new Bullet(rotation, px, (int)py));
            else
                game.p2_bullets.add(new Bullet(rotation, px, (int)py));
        }

        private void setInputs(Player en) {
            assert(inputs.length == Network.INPUT_LEN);
            assert(outputs.length == Network.OUTPUT_LEN);
            inputs[0] = py;
            inputs[1] = rotation;
            inputs[2] = calculateDistance(en);
            inputs[3] = calculateAngle(en);
            inputs[4] = en.getPy();
            inputs[5] = en.getRotation();
            inputs[6] = en.calculateDistance(this);
            inputs[7] = en.calculateAngle(this);

            outputs = brain.getPhenotype().fireNetwork(inputs);
        }

        private double calculateAngle(Player en) {
            return CalvinIsANEET.getAngle(px, py, en.px, en.getPy());
        }

        private double calculateDistance(Player en) {
            return CalvinIsANEET.distance(px, py, en.px, en.getPy());
        }

        private double getPy() {
            return this.py;
        }

        private double getRotation() {
            return rotation;
        }

    }

}