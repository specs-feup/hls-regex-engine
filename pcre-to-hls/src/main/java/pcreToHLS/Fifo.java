package pcreToHLS;

public class Fifo {
    private static final int ID_NO_START = 0;
    private static final String ID_PREFIX = "fifo";
    private String id;
    private static int id_no = ID_NO_START;

    public Fifo()
    {
        this.id = ID_PREFIX + Fifo.id_no++;
    }

    public Fifo(int id_no)
    {
        this.id = ID_PREFIX + id_no;
    }

    public static void resetIdNo()
    {
        Fifo.id_no = ID_NO_START;
    }

    public String getId() {
        return id;
    }

    public int getId_no() {
        return Integer.parseInt(this.id.substring(ID_PREFIX.length()));
    }

    @Override
    public String toString()
    {
        return this.id;
    }

    @Override
    public int hashCode()
    {
        return this.id.hashCode();
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
