import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Scanner;
import java.util.Set;
import java.util.HashSet;
import java.util.Comparator;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * The DNA of a single individual. A lot of time was spent here.
 * 
 * There is some todo stuff below, but I am lazy.
 */
public class Genome {

    /// STATIC STUFF BELOW

    public static final float DT = 10.0f;
    private static final float C1 = 1.0f, C2 = 1.0f, C3 = 0.4f;
    private static final float NEW_WEIGHT_OR_AUGMENT = 0.9f,
    MAX_WEIGHT_DELTA = 1.0f, MIN_WEIGHT_DELTA = -1.0f;
    private static final float MAX_RANDOM_WEIGHT = 1.0f, MIN_RANDOM_WEIGHT = -1.0f;

    private static int innovation_count = 0;
    private static Map<String, Integer> innovation_set = new HashMap<String, Integer>();

    /**
     * Is the innovation number in use already?
     * 
     * @return true or false
     */
    public static boolean checkInnovtionNumber(int to, int from) {
        String hash = from + "->" + to;
        return innovation_set.containsKey(hash);
    }

    /**
     * Assuming an innovation number exists for said hash, we can get it.
     * 
     * @return The innovation number
     */
    public static int getOldInnovationNumber(int to, int from) {
        String hash = from + "->" + to;
        return innovation_set.get(hash);
    }

    /**
     * Use this to get a brand new, unused innovation number.
     * 
     * @return A new innovation number
     */
    public static int getNewInnovationNumber() {
        return innovation_count++;
    }

    /// STATIC STUFF ABOVE

    public final int NUM_INPUTS, NUM_OUTPUTS;
    private int total_neurons = 0;
    private List<Connection> connections = new ArrayList<Connection>();
    private Phenotype phenotype;
    private float adjustedFitness = 0;

    /**
     * Constructs a default genome, hooks all inputs into outputs.
     */
    public Genome(final int NUM_INPUTS, final int NUM_OUTPUTS) {
        this.NUM_INPUTS = NUM_INPUTS;
        this.NUM_OUTPUTS = NUM_OUTPUTS;

        for(int i = 0; i < NUM_INPUTS; i++) {
            for(int j = NUM_INPUTS; j < NUM_INPUTS+NUM_OUTPUTS; j++) {
                connections.add(new Connection(j, i, getNewInnovationNumber(), false, randomWeight(), true, true));
            }
        }

        total_neurons = NUM_INPUTS + NUM_OUTPUTS;

        sort();
    }

    /**
     * Constructs a genome from a provided file.
     * 
     * TODO: Make this work
     * 
     * @param fileName The name of the file to open
     */
    public Genome(String fileName) {

        Scanner sc = null;
        try {
            sc = new Scanner(new File(fileName));
        } catch(IOException e) {
            System.err.println("Failed to open file: " + fileName);
            System.exit(1);
        }

        NUM_INPUTS = sc.nextInt();
        NUM_OUTPUTS = sc.nextInt();

        while(sc.hasNextInt()) {
            int to = sc.nextInt();
            int from = sc.nextInt();
            int inno = sc.nextInt();
            int disabled = sc.nextInt();
            float weight = sc.nextFloat();
            int input = sc.nextInt();
            int output = sc.nextInt();
            total_neurons = Math.max(total_neurons, Math.max(to, from));
            connections.add(new Connection(to, from, inno, (disabled > 0) ? true : false, weight, (input > 0) ? true : false, (output > 0) ? true : false));
        }

        innovation_count = connections.size();
        sort();
    }

    /**
     * Inits the phenotype for play testing.
     */
    public void createPhenotype() {
        phenotype = new Phenotype(this);
    }

    /**
     * The mating process matches matching genes and passes disjoint and excess genes to the child.
     * 
     * TODO: Implement re-enabling of disabled genes
     * 
     * @param g1 Parent number one
     * @param g2 Parent number two
     * @return The newly created child
     */
    public Genome crossover(Genome g1, Genome g2) {
        Genome child = new Genome(Network.INPUT_LEN, Network.OUTPUT_LEN);
        int total_neurons = 0;

        List<MatchingGene> matching = getMatchingGenes(g1, g2);
        for(int i = 0; i < matching.size(); i++) {
            Connection con1 = matching.get(i).g1, con2 = matching.get(i).g2;
            child.getConnections().add((Math.random() > 0.5) ? con1 : con2);
        }
        for(int i = 0; i < g1.getConnections().size(); i++)
        {
            Connection current = g1.getConnections().get(i);
            total_neurons = Math.max(total_neurons, current.getTo());
            if(!(g2.getConnections().contains(current))) {
                child.getConnections().add(current);
            }
        }
        for(int i = 0; i < g2.getConnections().size(); i++)
        {
            Connection current = g2.getConnections().get(i);
            total_neurons = Math.max(total_neurons, current.getTo());
            if(!(g1.getConnections().contains(current))) {
                child.getConnections().add(current);
            }
        }


        child.setNumberOfNeurons(++total_neurons); // estimate, but good enough

        return child;
    }

    /**
     * Adjusted fitness, or f prime, is used to tell how related a genome is compared to all other genomes.
     * 
     * @param fitness The raw fitness value from the phenotype
     * @param g1 The genome to compare against all the others
     * @param fall All of the other genomes
     * @return The adjusted fitness value
     */
    public double getAdjustedFitness(double fitness, Genome g1, List<Genome> fall) {
        double sum = 0;
        for(int j = 0; j < fall.size(); j++) {
            sum += (distance(g1, fall.get(j)) > DT) ? 0 : 1;
        }
        fitness /= sum;
        return fitness;
    }

    /**
     * The distance between two genomes is one of genetic difference, not an actual physical distance.
     * It is nice to have a function like this to tell how closely related two genomes are.
     * 
     * @param Genome The larger genome
     * @param Genome The smaller genome
     * @return Some distance value
     */
    public double distance(Genome g1, Genome g2) {
        Genome temp = g1;
        if(g1.getConnections().size() < g2.getConnections().size()) {
            g2 = g1;
            g1 = temp;
        }

        final double N = (g1.getConnections().size() <= 20) ? 1 : g1.getConnections().size();
        final int E = excessCount(g1, g2);
        final int D = disjointCount(g1, g2);
        final double W = getAverageWeightDifferencesOfMatchingGenes(g1, g2);

        double delta = ((C1 * E) / N) + ((C2 * D) / N) + (C3 * W);
        return delta;
    }

    /**
     * Get the amount of excess genes between two genomes.
     *
     * @param Genome The larger genome
     * @param Genome The smaller genome
     * @return The amount of excess genes
     */
    public int excessCount(Genome g1, Genome g2) {
        if(g1.getConnections().size() < g2.getConnections().size()) {
            Genome temp = g2;
            g2 = g1;
            g1 = temp;
        }

        int excessCount = 0;
        for(int i = g2.getConnections().size(); i < g1.getConnections().size(); i++) {
            excessCount++;
        }

        return excessCount;
    }

    /**
     * A disjoint gene has an innovation number that is contained in only one genome and is not excess.
     * 
     * @param Genome The larger genome
     * @param Genome The smaller genome
     * @return The amount of disjoint genes
     */
    public int disjointCount(Genome g1, Genome g2) {

        Set<Connection> disjoint = new HashSet<Connection>();
        int disjointCount = 0;
        for(int i = 0; i < g1.getConnections().size(); i++) {
            disjoint.add(g1.getConnections().get(i));
        }

        for(int i = 0; i < g2.getConnections().size(); i++) {
            if(!(disjoint.contains(g2.getConnections().get(i)))) {
                disjointCount++;
            }
        }

        return disjointCount;
    }

    /**
     * Matching genes from two seperate genomes have the same innovation number.
     * 
     * @param Genome The larger genome
     * @param Genome The smaller genome
     * @return A list of MatchingGene objects, which just contain the two connections that match
     */
    public List<MatchingGene> getMatchingGenes(Genome g1, Genome g2) {

        List<MatchingGene> matchingGenes = new ArrayList<>();
        for(int i = 0; i < g1.getConnections().size(); i++) {
            Connection current = g1.getConnections().get(i);
            if(g2.getConnections().contains(current)) {
                Connection matchingGene = null;
                for(int j = 0; j < g2.getConnections().size(); j++) {
                    if(g2.getConnections().get(j).equals(current)) {
                        matchingGene = g2.getConnections().get(j);
                        break;
                    }
                }
                matchingGenes.add(new MatchingGene(current, matchingGene));
            }
        }
        return matchingGenes;
    }

    /**
     * The average weight differences of matching genes is used to help compute the distance value.
     * 
     * @param Genome The larger genome
     * @param Genome The smaller genome
     * @return The average weight difference of matching genes, or W
     */
    public double getAverageWeightDifferencesOfMatchingGenes(Genome g1, Genome g2) {
        List<MatchingGene> matchingGenes = getMatchingGenes(g1, g2);
        if(matchingGenes.size() == 0) 
            return 0;

        double awdomg = 0;
        for(int i = 0; i < matchingGenes.size(); i++) {
            MatchingGene match = matchingGenes.get(i);
            awdomg += Math.abs(match.g1.getWeight() - match.g2.getWeight());
        }
        awdomg /= (double)matchingGenes.size();
        return awdomg;
    }

    /**
     * Sorts the connections by innovation number.
     */
    public void sort() {
        Collections.sort(connections, new Comparator<Connection>() {
            public int compare(Connection c1, Connection c2) {
                return c1.getInnovationNumber() - c2.getInnovationNumber();
            }
        });
    }

    @Override
    public Genome clone() {
        Genome genome = new Genome(NUM_INPUTS, NUM_OUTPUTS);
        for(Connection c : connections)
            genome.getConnections().add(c);
        genome.setNumberOfNeurons(total_neurons);
        genome.sort();
        return genome;
    }

    /**
     * Stimulate the weights in such a way the paper told me to.
     */
    public void mutateWeight() {
        for(Connection c : connections) {
            if(Math.random() < NEW_WEIGHT_OR_AUGMENT) {
                c.setWeight(c.getWeight() * ((float)(Math.random() * (MAX_WEIGHT_DELTA - MIN_WEIGHT_DELTA) + MIN_WEIGHT_DELTA)));
            } else {
                c.setWeight(randomWeight());
            }
        }
    }

    /**
     * Adds a random new connection somewhere.
     */
    public void addNewConnection() {

        int con1 = (int) (Math.random() * connections.size());
        int con2 = (int) (Math.random() * connections.size());

        Connection acon1 = connections.get(con1);
        Connection acon2 = connections.get(con2);
        float weight = randomWeight();

        if(acon2.isInput()) {
            Connection temp = acon2;
            acon2 = acon1;
            acon1 = temp;
        }
        if(acon1.isOutput()) {
            Connection temp = acon1;
            acon1 = acon2;
            acon2 = temp;
        }

        final int new_to = acon1.getTo();
        final int new_from = acon2.getFrom();

        int inno_num = getInnovationNumber(new_to, new_from);

        boolean disabled = (Math.random() > 0.2) ? false : true;

        Connection new_connection = new Connection(new_to, new_from, inno_num, disabled, weight, acon2.isInput(), acon1.isOutput());

        connections.add(new_connection);

        sort();
    }

    /**
     * Neurons are only added between existing connections. A neuron is really just two connections.
     * Two new connections are created, but the old connection is disabled for later maybe.
     */
    public void addNewNeuron() {
        int con1 = (int) (Math.random() * getConnections().size());

        Connection acon1 = connections.get(con1);

        acon1.setDisabled(true);

        int new_neuron = total_neurons++;

        Connection replacement_connection1 = new Connection(new_neuron, acon1.getFrom(), getInnovationNumber(new_neuron, acon1.getFrom()), false, randomWeight(), acon1.isInput(), false);
        Connection replacement_connection2 = new Connection(acon1.getTo(), new_neuron, getInnovationNumber(acon1.getTo(), new_neuron), false, randomWeight(), false, acon1.isOutput());

        acon1.setInput(false);
        acon1.setOutput(false);

        connections.add(replacement_connection1);
        connections.add(replacement_connection2);

        sort();
    }

    /**
     * This is how the paper told me to do it, lol.
     */
    public void adjustWeightsRandomly() {
        for(int i = 0; i < connections.size(); i++) {
            Connection current = connections.get(i);
            if(Math.random() < NEW_WEIGHT_OR_AUGMENT) {
                current.setWeight(current.getWeight() * randomWeight());
            } else {
                current.setWeight(randomWeight());
            }
        }
    }

    /**
     * Generates a random weight decided by variables at the top of this file.
     * 
     * @return The random weight
     */
    public float randomWeight() {
        return ((float)Math.random()) * (MAX_RANDOM_WEIGHT - MIN_RANDOM_WEIGHT) + MIN_RANDOM_WEIGHT;
    }

    private int getInnovationNumber(final int to, final int from) {
        int inno_num;
        if(checkInnovtionNumber(to, from)) {
            inno_num = getOldInnovationNumber(to, from);
        } else {
            inno_num = getNewInnovationNumber();
        }
        return inno_num;
    }

    private void setNumberOfNeurons(int total_neurons) {
        this.total_neurons = total_neurons;
    }

    private class MatchingGene {
        public Connection g1, g2;
        public MatchingGene(Connection g1, Connection g2) {
            this.g1 = g1;
            this.g2 = g2;
        }
    }

    /**
     * Creates a print friendly genome. Not for saving!
     * 
     * @return Print friendly string of the genome
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Genome\n");
        for(Connection c : connections) {
            sb.append(c.toString() + "\n");
        }
        if(phenotype != null) {
            sb.append("Phenotype\n");
            sb.append(phenotype.toString());
        }
        return sb.toString();
    }

    /**
     * Creates a string representing an entire genome. Used for save files.
     * 
     * @return The save string
     */
    public String getSaveForm() {
        StringBuilder sb = new StringBuilder();
        sb.append(NUM_INPUTS).append(' ').append(NUM_OUTPUTS).append('\n');
        sb.append(total_neurons).append('\n');
        sb.append(getConnections().size()).append('\n');
        for(Connection c : getConnections()) {
            sb.append(c.getTo()).append(' ').append(c.getFrom()).append(' ').append(c.getInnovationNumber()).append(' ')
            .append(c.isDisabled()).append(' ').append(c.getWeight()).append(' ').append(c.isInput()).append(' ')
            .append(c.isOutput()).append('\n');
        }
        return sb.toString();
    }

    public float getRawFitness() {
        return phenotype.getFitness();
    }

    public void setRawFitness(float rawFitness) {
        this.phenotype.setFitness(rawFitness);
    }

    public float getAdjustedFitness() {
        return this.adjustedFitness;
    }

    public void calcAjustedFitness(List<Genome> population) {
        getAdjustedFitness(getRawFitness(), this, population);
    }

    public Phenotype getPhenotype() {
        return this.phenotype;
    }

    public void incrementUnweightedFitness() {
        this.phenotype.setFitness(this.getRawFitness() + 1);
    }

    public void decrementUnweightedFitness() {
        this.phenotype.setFitness(this.getRawFitness() - 1);
    }

    public int getTotalNeurons() {
        return this.total_neurons;
    }

    public List<Connection> getConnections() {
        return this.connections;
    }

}