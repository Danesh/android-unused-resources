package ca.skennedy.androidunusedresources;

import java.util.SortedSet;
import java.util.TreeSet;

public class Resource implements Comparable<Resource> {
    private final String mType;
    private final String mName;
    
    private final SortedSet<String> mDeclaredPaths = new TreeSet<String>();
    
    private static final String sStringFormat = "%-10s: %s";
    private static final String sPathFormat = "    %s";
    
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
    
    public SortedSet<String> getDeclaredPaths() {
        return mDeclaredPaths;
    }
    
    public void addDeclaredPath(final String path) {
        mDeclaredPaths.add(path);
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
        final StringBuilder string = new StringBuilder(String.format(sStringFormat, mType, mName));

        for (final String path : mDeclaredPaths) {
            string.append('\n');
            string.append(String.format(sPathFormat, path));
        }
        
        return string.toString();
    }
}
