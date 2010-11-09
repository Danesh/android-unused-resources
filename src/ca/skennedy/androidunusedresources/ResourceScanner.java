package ca.skennedy.androidunusedresources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResourceScanner {
    private final File mBaseDirectory;
    
    private File mSrcDirectory = null;
    private File mResDirectory = null;
    private File mGenDirectory = null;
    
    private File mManifestFile = null;
    private File mRJavaFile = null;
    
    private final Set<Resource> mResources = new HashSet<Resource>();
    private final Set<Resource> mUsedResources = new HashSet<Resource>();
    
    private static final Pattern RESOURCE_TYPE_PATTERN = Pattern.compile("^\\s*public static final class (\\w+)\\s*\\{$");
    private static final Pattern RESOURCE_NAME_PATTERN = Pattern.compile("^\\s*public static final int (\\w+)=0x[0-9A-Fa-f]+;$");
    
    private static final String TYPE_USAGE_PREFIX = "{type}";
    private static final FileType JAVA_FILE_TYPE = new FileType("java", "R." + TYPE_USAGE_PREFIX + ".");
    private static final FileType XML_FILE_TYPE = new FileType("xml", "@" + TYPE_USAGE_PREFIX + "/");
    
    public ResourceScanner() {
        final String baseDirectory = System.getProperty("user.dir");
        mBaseDirectory = new File(baseDirectory);
    }
    
    /**
     * This constructor is only used for debugging.
     * @param baseDirectory The project directory to use.
     */
    protected ResourceScanner(final String baseDirectory) {
        mBaseDirectory = new File(baseDirectory);
    }
    
    public void run() {
        System.out.println("Running in: " + mBaseDirectory.getAbsolutePath());
        
        findPaths();
        
        if (mSrcDirectory == null || mResDirectory == null || mManifestFile == null) {
            System.err.println("The current directory is not a valid Android project root.");
            return;
        }
        
        if (mGenDirectory == null || !findRJavaFile(mGenDirectory)) {
            System.err.println("You must first build your project to generate R.java");
            return;
        }
        
        try {
            generateResourceList();
        } catch (final IOException e) {
            System.err.println("The R.java found could not be opened.");
            e.printStackTrace();
        }
        
        System.out.println(mResources.size() + " resources found");
        System.out.println();
        
        mUsedResources.clear();
        
        searchFiles(mSrcDirectory, JAVA_FILE_TYPE);
        searchFiles(mResDirectory, XML_FILE_TYPE);
        searchFiles(mManifestFile, XML_FILE_TYPE);
        
        /*
         * Find the paths where the unused resources are declared.
         */
        final SortedMap<String, SortedMap<String, Resource>> resources = new TreeMap<String, SortedMap<String, Resource>>();
        
        for (final Resource resource : mResources) {
            final String type = resource.getType();
            SortedMap<String, Resource> typeMap = resources.get(type);
            
            if (typeMap == null) {
                typeMap = new TreeMap<String, Resource>();
                resources.put(type, typeMap);
            }
            
            typeMap.put(resource.getName(), resource);
        }
        
        findDeclaredPaths(mResDirectory, resources);
        
        final int unusedResources = mResources.size();
        
        if (unusedResources > 0) {
            System.out.println(unusedResources + " unused resources were found:");
            
            final SortedSet<Resource> sortedResources = new TreeSet<Resource>(mResources);
            
            for (final Resource resource : sortedResources) {
                System.out.println(resource);
            }
            
            System.out.println();
            System.out.println("If any of the above resources are used, please submit your project as a test case so this application can be improved.");
            System.out.println();
            System.out.println("This application does not maintain a dependency graph, so you should run it again after removing the above resources.");
        } else {
            System.out.println("No unused resources were detected.");
            System.out.println("If you know you have some unused resources, please submit your project as a test case so this application can be improved.");
        }
    }
    
    private void findPaths() {
        final File[] children = mBaseDirectory.listFiles();
        
        for (final File file : children) {
            if (file.isDirectory()) {
                if (file.getName().equals("src")) {
                    mSrcDirectory = file;
                } else if (file.getName().equals("res")) {
                    mResDirectory = file;
                } else if (file.getName().equals("gen")) {
                    mGenDirectory = file;
                }                
            } else if (file.getName().equals("AndroidManifest.xml")) {
                mManifestFile = file;
            }
        }
    }
    
    private boolean findRJavaFile(final File baseDirectory) {
        final File[] children = baseDirectory.listFiles();
        
        boolean found = false;
        
        for (final File file : children) {
            if (file.getName().equals("R.java")) {
                mRJavaFile = file;
                return true;
            } else if (file.isDirectory()) {
                found = findRJavaFile(file);
            }
        }
        
        return found;
    }
    
    private void generateResourceList() throws IOException {
        final InputStream inputStream = new FileInputStream(mRJavaFile);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        
        boolean done = false;
        
        mResources.clear();
        
        String type = "";
        
        while (!done) {
            final String line = reader.readLine();
            done = (line == null);
            
            if (line != null) {
                final Matcher typeMatcher = RESOURCE_TYPE_PATTERN.matcher(line);
                final Matcher nameMatcher = RESOURCE_NAME_PATTERN.matcher(line);
                
                if (nameMatcher.find()) {
                    mResources.add(new Resource(type, nameMatcher.group(1)));
                } else if (typeMatcher.find()) {
                    type = typeMatcher.group(1);
                }
            }
        }
        
        reader.close();
        inputStream.close();
    }
    
    private void searchFiles(final File file, final FileType fileType) {
        if (file.isDirectory()) {
            for (final File child : file.listFiles()) {
                searchFiles(child, fileType);
            }
        } else if (file.getName().endsWith(fileType.getExtension())) {
            try {
                searchFile(file, fileType.getUsagePrefix());
            } catch (final IOException e) {
                System.err.println("There was a problem reading " + file.getAbsolutePath());
                e.printStackTrace();
            }
        }
    }
    
    private void searchFile(final File file, final String usagePrefix) throws IOException {
        final InputStream inputStream = new FileInputStream(file);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        
        final Set<Resource> foundResources = new HashSet<Resource>();
        
        final StringBuilder stringBuilder = new StringBuilder();
        
        boolean done = false;
        
        while (!done) {
            final String line = reader.readLine();
            done = (line == null);
            
            if (line != null) {
                stringBuilder.append(line);
            }
        }
        
        final String fileContents = stringBuilder.toString();
        
        for (final Resource resource : mResources) {
            if (fileContents.contains(usagePrefix.replace(TYPE_USAGE_PREFIX, resource.getType()) + resource.getName())) {
                foundResources.add(resource);
            }
        }
        
        for (final Resource resource : foundResources) {
            mUsedResources.add(resource);
            mResources.remove(resource);
        }
        
        reader.close();
        inputStream.close();
    }
    
    private void findDeclaredPaths(final File file, final Map<String, SortedMap<String, Resource>> resources) {
        if (file.isDirectory()) {
            for (final File child : file.listFiles()) {
                final String fileName = child.getName();
                if (!fileName.startsWith(".")) {
                    final String type = fileName.split("-")[0];
                    findDeclaredPaths(child, type, resources);
                }
            }
        }
    }
    
    private void findDeclaredPaths(final File file, final String type, final Map<String, SortedMap<String, Resource>> resources) {
        if (file.isDirectory()) {
            for (final File child : file.listFiles()) {
                final String fileName = child.getName();
                if (!fileName.startsWith(".")) {
                    final String resourceName = fileName.split("\\.")[0];
                    final Map<String, Resource> typeMap = resources.get(type);
                    
                    if (typeMap != null) {
                        final Resource resource = typeMap.get(resourceName);
                        
                        if (resource != null) {
                            resource.addDeclaredPath(child.getAbsolutePath());
                        }
                    }
                }
            }
        }
    }
}
