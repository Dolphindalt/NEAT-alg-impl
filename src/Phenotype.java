import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * I had a really hard time working with the connection data in the genome, so this class
 * transforms that data into something I want to work with. Thank god for this class.
 */
public class Phenotype {

    private static final double BIAS = 1000.0;

    private class Neuron {
        public float bvalue, tvalue;
        public Neuron()
        {
            bvalue = -1;
            tvalue = -1;
        }
        public void activate() {
            tvalue = 1.0f / (1.0f + (float)Math.pow((float)Math.E, -bvalue));
        }
    }

    private List<Connection>[] connectionMatrix; // neuron, connections
    private Set<Integer> inputs = new HashSet<Integer>(), outputs = new HashSet<Integer>();
    private Neuron[] neurons;
    private float fitness = 0;

    /**
     * The phenotype constructor converts a genome into something I want to use.
     * 
     * @param genome The genome to data transform
     */
    public Phenotype(Genome genome) {
        connectionMatrix = new List[genome.getConnections().size()];
        for(int i = 0; i < connectionMatrix.length; i++)
            connectionMatrix[i] = new ArrayList<>();
        neurons = new Neuron[genome.getTotalNeurons()];
        for(int i = 0; i < neurons.length; i++) {
            neurons[i] = new Neuron();
        }
        for(Connection c : genome.getConnections()) {
            if(c.isInput()) inputs.add(c.getFrom());
            if(c.isOutput()) {
                //System.out.println(c.toString());
                outputs.add(c.getTo());
            }
            connectionMatrix[c.getFrom()].add(c);
        }
        assert(outputs.size() == Network.OUTPUT_LEN);
    }

    /**
     * This is not a recursive call. The next one is.
     * 
     * @param inputs The actual input values as an array, bound by Network.INPUT_LEN
     * @return The actual outputs as an array, bound by Network.OUTPUT_LEN
     */
    public double[] fireNetwork(double[] inputs) {
        double[] result = new double[Network.OUTPUT_LEN];
        for(int i = 0; i < inputs.length; i++)
        {
            neurons[i].bvalue = (float)inputs[i];
            neurons[i].activate();
        }

        int[] input_neurons = new int[Network.INPUT_LEN];
        for(int i = 0; i < Network.INPUT_LEN; i++) {
            input_neurons[i] = i; // Convert the used memory space for another use, harder to read though
        }

        feedForward(input_neurons);

        Iterator<Integer> itr = outputs.iterator();
        //System.out.println(outputs.size());
        int i = 0;
        while(itr.hasNext()) {
            int k = itr.next();
            neurons[k].activate();
            result[i++] = neurons[k].tvalue;
        }
        
        return result;
    }

    /**
     * This is the recursive call you are looking for. This propegates the entire network forward.
     * 
     * @param from An array of neuron indexes, the neurons we are moving forward from
     */
    public void feedForward(int[] from) {
        if(from.length == 0)
            return;
        
        int i, j;

        List<Integer> to = new ArrayList<>();
        for(i = 0; i < from.length; i++)
        {
            for(j = 0; j < connectionMatrix[from[i]].size(); j++)
            {
                Connection forward = connectionMatrix[from[i]].get(j);
                neurons[forward.getTo()].bvalue += (neurons[forward.getFrom()].tvalue / BIAS) * forward.getWeight();
                
                //System.out.println("From: " + forward.getFrom() + " To: " + forward.getTo() + " Type: " + forward.isInput() + " " + forward.isOutput());

                if(!(forward.isOutput()))
                    to.add(forward.getTo());
            }
        }

        int[] result = new int[to.size()];
        for(i = 0; i < to.size(); i++) {
            int nindex = to.get(i);
            neurons[nindex].activate();
            result[i] = nindex;
        }

        feedForward(result);
    }

    /**
     * This method creates a print friendly phenotype.
     * 
     * @return Print friendly string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Raw Values\n");
        for(int i = 0; i < neurons.length; i++)
            sb.append(neurons[i].bvalue + " " + neurons[i].tvalue + "\n");
        sb.append("Raw Inputs\n");
        Iterator<Integer> itr = inputs.iterator();
        while(itr.hasNext()) {
            sb.append(neurons[itr.next()].bvalue).append(' ');
        }
        sb.append('\n');
        sb.append("Raw Outputs\n");
        itr = outputs.iterator();
        while(itr.hasNext()) {
            sb.append(neurons[itr.next()].tvalue).append(' ');
        }
        sb.append('\n');
        return sb.toString();
    }

    public float getFitness() {
        return this.fitness;
    }

    public void setFitness(float fitness) {
        this.fitness = fitness;
    }

    public void incrementFitness() {
        this.fitness++;
    }

}