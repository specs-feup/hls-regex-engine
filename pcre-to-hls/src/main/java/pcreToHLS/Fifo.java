package pcreToHLS;

public class Fifo {
    private String id;
    private static int id_no = 0;

    public Fifo()
    {
        this.id = "fifo" + Fifo.id_no++;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString()
    {
        return this.id;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
            return true;

        if (other == null || getClass() != other.getClass())
            return false;

        Fifo other_fifo = (Fifo) other;
        return this.id.equals(other_fifo.id);
    }
}
