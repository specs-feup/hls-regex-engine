package regexjava;

public class Counter {

    public enum CounterOperation {COMPARE_EQUAL, COMPARE_LESS, COMPARE_MORE, COMPARE_EQUALMORE, COMPARE_EQUALLESS, COMPARE_RANGE};

    private int target_value;
    private int target_value2;
    private String id;
    private static int id_no;
    
    public Counter(int target_value)
    {
        this.target_value = target_value;
        this.id = "c" + Counter.id_no;
        Counter.id_no++;
    }

    public Counter(int target_value1, int target_value2)
    {
        this.target_value = target_value1;
        this.target_value2 = target_value2;
        this.id = "c" + Counter.id_no;
        Counter.id_no++;
    }

    public int getTarget_value() {
        return target_value;
    }

    public void setTarget_value(int target_value) {
        this.target_value = target_value;
    }

    public int getTarget_value2() {
        return target_value2;
    }

    public void setTarget_value2(int target_value2) {
        this.target_value2 = target_value2;
    }

    public String getId() {
        return id;
    }
}
