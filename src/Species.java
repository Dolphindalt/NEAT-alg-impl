import java.util.List;
import java.util.ArrayList;

public class Species {

    private Genome representative;
    private List<Genome> population = new ArrayList<Genome>();
    private float fitness;

    public Species(Genome representative) {
        this.representative = representative;
        population.add(representative);
    }

    public Genome getBestGenome() {
        Genome best = representative;
        for(Genome g : population) {
            if(g.getRawFitness() > best.getRawFitness()) {
                best = g;
            }
        }
        return best;
    }

    public void addGenome(Genome genome) {
        population.add(genome);
    }

    public List<Genome> getPopulation() {
        return this.population;
    }

    public Genome getRepresentative() {
        return this.representative;
    }

    public void addFitness(float fitnessDelta) {
        fitness += fitnessDelta;
    }

    public void averageFitness() {
        fitness /= (float)population.size();
    }

    public float getFitness() {
        return this.fitness;
    }

}