package ca.skennedy.androidunusedresources;

public class FileType {
    private final String mExtension;
    private final String mUsagePrefix;
    
    public FileType(final String extension, final String usagePrefix) {
        super();
        mExtension = extension;
        mUsagePrefix = usagePrefix;
    }
    
    public String getExtension() {
        return mExtension;
    }
    
    public String getUsagePrefix() {
        return mUsagePrefix;
    }
}
