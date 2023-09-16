package pcreToHLS;

import pcreToHLS.Counter.CounterOperation;

public class CounterInfo 
{
    public Counter counter;
    public CounterOperation operation;

    public Counter getCounter() {
        return counter;
    }

    public void setCounter(Counter counter) {
        this.counter = counter;
    }

    public CounterOperation getOperation() {
        return operation;
    }

    public void setOperation(CounterOperation operation) {
        this.operation = operation;
    }

    public CounterInfo(Counter counter, CounterOperation operation) 
    {
        this.counter = counter;
        this.operation = operation;
    }

    @Override
    public String toString()
    {
        return this.counter.getId() + " " + this.operation + " " + this.counter.getTarget_value();
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
            return true;

        if (other == null || getClass() != other.getClass())
            return false;

        CounterInfo other_counter_info = (CounterInfo) other;
        return this.counter.equals(other_counter_info.counter) && this.operation.equals(other_counter_info.operation);
    }
}
