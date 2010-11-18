package ca.skennedy.androidunusedresources;

import java.util.regex.Pattern;

public class FileType {
    private final String mExtension;
    private final String mUsage;
    
    public static final String USAGE_TYPE = "{type}";
    public static final String USAGE_NAME = "{name}";
    
    public FileType(final String extension, final String usage) {
        super();
        mExtension = extension;
        mUsage = usage;
    }
    
    public String getExtension() {
        return mExtension;
    }
    
    public Pattern getPattern(final String type, final String name) {
        return Pattern.compile(mUsage.replace(USAGE_TYPE, type).replace(USAGE_NAME, name));
    }
}
