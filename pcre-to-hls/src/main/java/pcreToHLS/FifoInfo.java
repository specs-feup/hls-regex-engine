package pcreToHLS;

public class FifoInfo {
    private Fifo fifo;
    private boolean clear;

    public FifoInfo(Fifo fifo, boolean clear)
    {
        this.fifo = fifo;
        this.clear = clear;
    }

    @Override
    public String toString()
    {
        return this.fifo.toString() + this.clear;
    }

    public Fifo getFifo() {
        return fifo;
    }

    public void setFifo(Fifo fifo) {
        this.fifo = fifo;
    }

    public boolean isClear() {
        return clear;
    }

    public void setClear(boolean clear) {
        this.clear = clear;
    }
}
