package ca.skennedy.androidunusedresources;

import java.io.File;

public abstract class ResourceType {
    private final String mType;
    
    public ResourceType(final String type) {
        super();
        mType = type;
    }
    
    public String getType() {
        return mType;
    }

    public abstract boolean doesFileDeclareResource(File parent, String fileName, String fileContents, String resourceName);
    
    /**
     * Scans a file for special uses of the resource (i.e. not a simple string match on the resource name).
     * @param parent
     * @param fileName
     * @param fileContents
     * @param resourceName
     * @return true if used, false otherwise
     */
    public boolean doesFileUseResource(final File parent, final String fileName, final String fileContents, final String resourceName) {
        return false;
    }
}
