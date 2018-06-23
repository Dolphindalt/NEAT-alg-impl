/**
 * The connection class could be aliased to a gene. It operates as such.
 */
public class Connection {
    private final int to, from, innovationNumber;
    private boolean disabled;
    private float weight;
    private boolean input, output;

    public Connection(int to, int from, int innovationNumber, boolean disabled, float weight) {
        this.to = to;
        this.from = from;
        this.innovationNumber = innovationNumber;
        this.disabled = disabled;
        this.weight = weight;
        this.input = false;
        this.output = false;
    }

    public Connection(int to, int from, int innovationNumber, boolean disabled, float weight, boolean input, boolean output) {
        this.to = to;
        this.from = from;
        this.innovationNumber = innovationNumber;
        this.disabled = disabled;
        this.weight = weight;
        this.input = input;
        this.output = output;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Connection && ((Connection)o).getInnovationNumber() == this.getInnovationNumber();
    }

    @Override
    public Connection clone() {
        return new Connection(to, from, innovationNumber, disabled, weight);
    }

    @Override
    public String toString() {
        return from + "->" + to + " | " + input + " " + output;
    }

    public boolean isInput() {
        return this.input;
    }

    public boolean isOutput() {
        return this.output;
    }

    public int getTo() {
        return this.to;
    }

    public int getFrom() {
        return this.from;
    }

    public float getWeight() {
        return this.weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public int getInnovationNumber() {
        return this.innovationNumber;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public void setInput(boolean input) {
        this.input = input;
    }

    public void setOutput(boolean output) {
        this.output = output;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

}