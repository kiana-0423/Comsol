/**
 * Unique default-package entry point for COMSOL batch's class-file loader.
 *
 * <p>The application remains in the com.nfm.comsol package. A unique bridge
 * name avoids any ambiguity with the packaged application class.</p>
 */
public final class NfmComsolEntry {
    private NfmComsolEntry() {}

    public static void main(String[] args) {
        // The Unix launcher passes application options through NFM_COMSOL_ARGS;
        // do not forward any COMSOL-owned batch arguments to the application.
        com.nfm.comsol.Main.main(new String[0]);
    }
}
