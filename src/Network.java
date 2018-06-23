import java.util.List;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.io.IOException;

/**
 * The Network class represents an ecosystem of species and all of the genomes within that ecosystem.
 */
public class Network {

    public static final int INPUT_LEN = 8, OUTPUT_LEN = 5;
    private static final int maxPopulationSize = 75;
    private static final double CHANCE_MUTATE = 0.15, CHANCE_NEW_CONNECTION = 0.20, CHANCE_NEW_NEURON = 0.20;

    List<Genome> totalPopulation = new ArrayList<Genome>();
    List<Species> totalSpecies = new ArrayList<Species>();

    /**
     * The network constructor inits the first generation.
     */
    public Network() {
        for(int i = 0; i < maxPopulationSize; i++) {
            totalPopulation.add(new Genome(INPUT_LEN, OUTPUT_LEN));
        }
    }

    /**
     * A whole lot goes on here. The next population is created and grouped into species.
     * Then adjusted fitness is calculated along with the average species fitness. The
     * best performing critters are added to the next population. Then similiar beasts
     * with near adjusted fitnesses mate together.
     */
    public void evolve() {

        List<Genome> nextPopulation = new ArrayList<Genome>();

        for(Genome g : totalPopulation) {
            boolean speciesExists = false;
            for(Species s : totalSpecies) {
                if(g.distance(g, s.getRepresentative()) < Genome.DT) {
                    s.addGenome(g);
                    speciesExists = true;
                }
            }

            if(!speciesExists)
                totalSpecies.add(new Species(g));
        }

        for(int i = totalSpecies.size()-1; i >= 0; i--) {
            Species s = totalSpecies.get(i);
            if(s.getPopulation().isEmpty()) {
                totalSpecies.remove(i);
            }
        }

        for(Genome g : totalPopulation) {
            Species s = getSpeciesFromGenome(g);
            if(s != null) {
                g.calcAjustedFitness(totalPopulation);
                s.addFitness(g.getAdjustedFitness());
            }
        }

        for(Species s : totalSpecies) {
            s.averageFitness();
        }

        for(Species s : totalSpecies) {
            Genome best = s.getBestGenome();
            nextPopulation.add(best);
        }

        while(nextPopulation.size() < maxPopulationSize) {
            Species s = getRandomSpeciesWithBias();

            Genome g1 = getRadomGenomeWithBias(s);
            Genome g2 = getRadomGenomeWithBias(s);

            if(g1.getAdjustedFitness() >= g2.getAdjustedFitness()) {
                ///
                Genome temp = g1;
                g1 = g2;
                g2 = temp;
            }

            Genome child = g1.crossover(g1, g2);

            if(Math.random() < CHANCE_MUTATE) {
                child.mutateWeight();
            }
            if(Math.random() < CHANCE_NEW_CONNECTION) {
                child.addNewConnection();
            }
            if(Math.random() < CHANCE_NEW_NEURON) {
                child.addNewNeuron();
            }

            nextPopulation.add(child);
        }

        totalPopulation = nextPopulation;
    }

    /**
     * This method saves all genomes in the network into a text file.
     * 
     * @param fileName The name of the file to save to
     */
    public void saveBrains(String fileName) {
        try {
            PrintWriter writer = new PrintWriter(fileName, "UTF-8");
            for(Genome g : totalPopulation) {
                writer.println(g.getSaveForm());
            }
            writer.close();
        } catch(IOException ex) {
            System.err.println("Failed to write to file " + fileName);
        }
    }

    private Species getRandomSpeciesWithBias() {
        float sigma = 0.0f;
        for(Species s : totalSpecies) {
            sigma += s.getFitness();
        }
        float abj = ((float)(Math.random())) * sigma;
        sigma = 0.0f;
        for(Species s : totalSpecies) {
            sigma += s.getFitness();
            if(sigma >= abj) {
                return s;
            }
        }
        throw new RuntimeException("Failed to find species in population of " + totalPopulation.size() + " with fitness as " + abj);
    }

    private Genome getRadomGenomeWithBias(Species s) {
        float sigma = 0.0f;
        for(Genome g : s.getPopulation()) {
            sigma += g.getAdjustedFitness();
        }
        float abj = ((float)(Math.random())) * sigma;
        sigma = 0.0f;
        for(Genome g : s.getPopulation()) {
            sigma += g.getAdjustedFitness();
            if(sigma > abj) {
                return g;
            }
        }
        return s.getPopulation().get((int)(Math.random()*s.getPopulation().size()));
    }

    private Species getSpeciesFromGenome(Genome g) {
        for(Species s : totalSpecies) {
            if(s.getPopulation().contains(g)) {
                return s;
            }
        }
        return null;
    }

    /**
     * The network is returned in a way that is print friendly
     * 
     * @return The network in string form
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Network\n");
        for(Genome g : totalPopulation) {
            sb.append(g.toString());
        }
        return sb.toString();
    }

}