package ca.skennedy.androidunusedresources;

public class Resource implements Comparable<Resource> {
    private final String mType;
    private final String mName;
    
    private static final String sStringFormat = "%-10s: %s";
    
    public Resource(final String type, final String name) {
        mType = type;
        mName = name;
    }
    
    public String getType() {
        return mType;
    }
    
    public String getName() {
        return mName;
    }

    @Override
    public int compareTo(final Resource another) {
        final int typeComparison = mType.compareTo(another.getType());
        
        if (typeComparison != 0) {
            return typeComparison;
        }
        
        return mName.compareTo(another.getName());
    }
    
    @Override
    public String toString() {
        return String.format(sStringFormat, mType, mName);
    }
}
