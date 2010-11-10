package ca.skennedy.androidunusedresources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
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
    
    private static final Pattern sResourceTypePattern = Pattern.compile("^\\s*public static final class (\\w+)\\s*\\{$");
    private static final Pattern sResourceNamePattern = Pattern.compile("^\\s*public static final int (\\w+)=0x[0-9A-Fa-f]+;$");
    
    private static final String USAGE_PREFIX = "{type}";
    private static final FileType sJavaFileType = new FileType("java", "R." + USAGE_PREFIX + ".");
    private static final FileType sXmlFileType = new FileType("xml", "@" + USAGE_PREFIX + "/");
    
    private static final Map<String, ResourceType> sResourceTypes = new HashMap<String, ResourceType>();
    
    static {
        // TODO: find declarations of these resources
        // anim
        // array
        // bool
        // color
        // dimen
        // drawable
        // integer
        // menu
        // plurals
        // string
        // style
        
        // id
        sResourceTypes.put("id", new ResourceType("id") {
            @Override
            public boolean doesFileDeclareResource(final File parent, final File file, final String resourceName) {
                // Check if we're in a valid directory
                if (!parent.isDirectory()) {
                    return false;
                }
                
                final String directoryType = parent.getName().split("-")[0];
                if (!directoryType.equals("values") && !directoryType.equals("layout")) {
                    return false;
                }
                
                // Check if the resource is declared here
                // TODO: test the valuesPattern regular expressions
                final Pattern valuesPattern0 = Pattern.compile("<item.*?type\\s*=\\s*\"id\".*?name\\s*=\\s*\"" + resourceName + "\".*?/>");
                final Pattern valuesPattern1 = Pattern.compile("<item.*?name\\s*=\\s*\"" + resourceName + "\".*?type\\s*=\\s*\"id\".*?/>");
                final Pattern layoutPattern = Pattern.compile(":id\\s*=\\s*\"@\\+id/" + resourceName + "\"");
                
                final String fileContents;
                try {
                    fileContents = FileUtilities.getFileContents(file);
                } catch (final IOException e) {
                    e.printStackTrace();
                    return false;
                }
                
                Matcher matcher = valuesPattern0.matcher(fileContents);
                
                if (matcher.find()) {
                    return true;
                }
                
                matcher = valuesPattern1.matcher(fileContents);
                
                if (matcher.find()) {
                    return true;
                }
                
                matcher = layoutPattern.matcher(fileContents);
                
                if (matcher.find()) {
                    return true;
                }
                
                return false;
            }
        });
        
        // layout
        sResourceTypes.put("layout", new ResourceType("layout") {
            @Override
            public boolean doesFileDeclareResource(final File parent, final File file, final String resourceName) {
                // Check if we're in a valid directory
                if (!parent.isDirectory()) {
                    return false;
                }
                
                final String directoryType = parent.getName().split("-")[0];
                if (!directoryType.equals(getType())) {
                    return false;
                }
                
                // Check if the resource is declared here
                final String fileName = file.getName().split("\\.")[0];
                
                return fileName.equals(resourceName);
            }
        });
    }
    
    public ResourceScanner() {
        super();
        final String baseDirectory = System.getProperty("user.dir");
        mBaseDirectory = new File(baseDirectory);
    }
    
    /**
     * This constructor is only used for debugging.
     * @param baseDirectory The project directory to use.
     */
    protected ResourceScanner(final String baseDirectory) {
        super();
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
        
        searchFiles(mSrcDirectory, sJavaFileType);
        searchFiles(mResDirectory, sXmlFileType);
        searchFiles(mManifestFile, sXmlFileType);
        
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
        
        // Ensure we only try to find resource types we're using
        final Map<String, ResourceType> resourceTypes = new HashMap<String,ResourceType>(resources.size());
        
        for (final String type : resources.keySet()) {
            final ResourceType resourceType = sResourceTypes.get(type);
            if (resourceType != null) {
                resourceTypes.put(type, resourceType);
            }
        }
        
        findDeclaredPaths(null, mResDirectory, resourceTypes, resources);
        
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
                final Matcher typeMatcher = sResourceTypePattern.matcher(line);
                final Matcher nameMatcher = sResourceNamePattern.matcher(line);
                
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
        final Set<Resource> foundResources = new HashSet<Resource>();
        
        final String fileContents = FileUtilities.getFileContents(file);
        
        for (final Resource resource : mResources) {
            if (fileContents.contains(usagePrefix.replace(USAGE_PREFIX, resource.getType()) + resource.getName())) {
                foundResources.add(resource);
            }
        }
        
        for (final Resource resource : foundResources) {
            mUsedResources.add(resource);
            mResources.remove(resource);
        }
    }
    
    private void findDeclaredPaths(final File parent, final File file, final Map<String, ResourceType> resourceTypes,
            final Map<String, SortedMap<String, Resource>> resources) {
        if (file.isDirectory()) {
            for (final File child : file.listFiles()) {
                if (!child.isHidden()) {
                    findDeclaredPaths(file, child, resourceTypes, resources);
                }
            }
        } else {
            if (!file.isHidden()) {
                for (final ResourceType resourceType : resourceTypes.values()) {
                    final Map<String, Resource> typeMap = resources.get(resourceType.getType());
                    
                    if (typeMap != null) {
                        for (final Resource resource : typeMap.values()) {
                            if (resourceType.doesFileDeclareResource(parent, file, resource.getName())) {
                                resource.addDeclaredPath(file.getAbsolutePath());
                            }
                        }
                    }
                }
            }
        }
    }
}
