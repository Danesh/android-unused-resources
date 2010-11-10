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

    public abstract boolean doesFileDeclareResource(File parent, File file, String resourceName);
}
