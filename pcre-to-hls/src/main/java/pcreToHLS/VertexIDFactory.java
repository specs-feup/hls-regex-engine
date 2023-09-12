package pcreToHLS;

public enum VertexIDFactory {
    INSTANCE();
    private static int counter = 0;

    public static String getNewVertexID()
    {
        return "v" + counter++;
    }

    public static void reset()
    {
        counter = 0;
    }
}