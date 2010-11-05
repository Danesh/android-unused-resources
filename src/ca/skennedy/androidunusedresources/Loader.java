package ca.skennedy.androidunusedresources;

public class Loader {
    public static void main(final String[] args) {
        //final ResourceScanner resourceScanner = new ResourceScanner("C:\\Users\\Scott\\Documents\\Homick\\Cineplex Mobile\\android\\trunk");
        final ResourceScanner resourceScanner = new ResourceScanner();
        resourceScanner.run();
    }
}
