package regexjava;

public class Counter {

    public enum CounterOperation {COMPARE_EQUAL, COMPARE_LESS};

    private int target_value;
    private String id;
    private static int id_no;
    
    public Counter(int target_value)
    {
        this.target_value = target_value;
        this.id = "c" + Counter.id_no;
        Counter.id_no++;
    }

    public int getTarget_value() {
        return target_value;
    }

    public void setTarget_value(int target_value) {
        this.target_value = target_value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
