package regexjava;

public enum VertexIDFactory {
    INSTANCE();
    private static int counter = 0;

    public static String getNewVertexID()
    {
        return "v" + counter++;
    }
}