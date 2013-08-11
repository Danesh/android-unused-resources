package ca.skennedy.androidunusedresources;

public class Loader {
    private Loader() {
        super();
    }

    public static void main(final String[] args) {
        final ResourceScanner resourceScanner = new ResourceScanner();
        resourceScanner.run(args);
    }
}
