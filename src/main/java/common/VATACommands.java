package common;

public final class VATACommands
{
    static {
        System.loadLibrary("vata");
    }

    private VATACommands()
    {
    }

    public static native String reduce(String target);

    public static native boolean checkInclusion(String subsumer, String includer);
}
