package ca.skennedy.androidunusedresources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    private String mPackageName = null;

    private final Set<Resource> mResources = new HashSet<Resource>();
    private final Set<Resource> mUsedResources = new HashSet<Resource>();

    private static final Pattern sResourceTypePattern = Pattern.compile("^\\s*public static final class (\\w+)\\s*\\{$");
    private static final Pattern sResourceNamePattern = Pattern
            .compile("^\\s*public static( final)? int(\\[\\])? (\\w+)\\s*=\\s*(\\{|(0x)?[0-9A-Fa-f]+;)\\s*$");

    private static final FileType sJavaFileType = new FileType("java", "R." + FileType.USAGE_TYPE + "." + FileType.USAGE_NAME + "[^\\w_]");
    private static final FileType sXmlFileType = new FileType("xml", "[\" >]@" + FileType.USAGE_TYPE + "/" + FileType.USAGE_NAME + "[\" <]");

    private static final Map<String, ResourceType> sResourceTypes = new HashMap<String, ResourceType>();

    static {
        // anim
        sResourceTypes.put("anim", new ResourceType("anim") {
            @Override
            public boolean doesFileDeclareResource(final File parent, final String fileName, final String fileContents, final String resourceName) {
                // Check if we're in a valid directory
                if (!parent.isDirectory()) {
                    return false;
                }

                final String directoryType = parent.getName().split("-")[0];
                if (!directoryType.equals(getType())) {
                    return false;
                }

                // Check if the resource is declared here
                final String name = fileName.split("\\.")[0];

                final Pattern pattern = Pattern.compile("^" + resourceName + "$");

                return pattern.matcher(name).find();
            }
        });

        // array
        sResourceTypes.put("array", new ResourceType("array") {
            @Override
            public boolean doesFileDeclareResource(final File parent, final String fileName, final String fileContents, final String resourceName) {
                // Check if we're in a valid directory
                if (!parent.isDirectory()) {
                    return false;
                }

                final String directoryType = parent.getName().split("-")[0];
                if (!directoryType.equals("values")) {
                    return false;
                }

                // Check if the resource is declared here
                final Pattern pattern = Pattern.compile("<([a-z]+\\-)?array.*?name\\s*=\\s*\"" + resourceName + "\".*?/?>");

                final Matcher matcher = pattern.matcher(fileContents);

                if (matcher.find()) {
                    return true;
                }

                return false;
            }
        });

        // attr
        sResourceTypes.put("attr", new ResourceType("attr") {
            @Override
            public boolean doesFileDeclareResource(final File parent, final String fileName, final String fileContents, final String resourceName) {
                // Check if we're in a valid directory
                if (!parent.isDirectory()) {
                    return false;
                }

                final String directoryType = parent.getName().split("-")[0];
                if (!directoryType.equals("values")) {
                    return false;
                }

                // Check if the resource is declared here
                final Pattern pattern = Pattern.compile("<attr.*?name\\s*=\\s*\"" + resourceName + "\".*?/?>");

                final Matcher matcher = pattern.matcher(fileContents);

                if (matcher.find()) {
                    return true;
                }

                return false;
            }

            @Override
            public boolean doesFileUseResource(final File parent, final String fileName, final String fileContents, final String resourceName) {
                if (parent != null) {
                    // Check if we're in a valid directory
                    if (!parent.isDirectory()) {
                        return false;
                    }

                    final String directoryType = parent.getName().split("-")[0];
                    if (!directoryType.equals("layout") && !directoryType.equals("values")) {
                        return false;
                    }
                }

                // Check if the attribute is used here
                // TODO: This will fail to report attrs as unused even when they're never used. Make it better, but don't allow any false positives.
                final Pattern pattern = Pattern.compile("<.+?:" + resourceName + "\\s*=\\s*\".*?\".*?/?>");

                final Matcher matcher = pattern.matcher(fileContents);

                if (matcher.find()) {
                    return true;
                }

                final Pattern itemPattern = Pattern.compile("<item.+?name\\s*=\\s*\"" + resourceName + "\".*?>");
                final Matcher itemMatcher = itemPattern.matcher(fileContents);

                if (itemMatcher.find()) {
                    return true;
                }

                return false;
            }
        });

        // bool
        sResourceTypes.put("bool", new ResourceType("bool") {
            @Override
            public boolean doesFileDeclareResource(final File parent, final String fileName, final String fileContents, final String resourceName) {
                // Check if we're in a valid directory
                if (!parent.isDirectory()) {
                    return false;
                }

                final String directoryType = parent.getName().split("-")[0];
                if (!directoryType.equals("values")) {
                    return false;
                }

                // Check if the resource is declared here
                final Pattern pattern = Pattern.compile("<bool.*?name\\s*=\\s*\"" + resourceName + "\".*?/?>");

                final Matcher matcher = pattern.matcher(fileContents);

                if (matcher.find()) {
                    return true;
                }

                return false;
            }
        });

        // color
        sResourceTypes.put("color", new ResourceType("color") {
            @Override
            public boolean doesFileDeclareResource(final File parent, final String fileName, final String fileContents, final String resourceName) {
                // Check if we're in a valid directory
                if (!parent.isDirectory()) {
                    return false;
                }

                final String directoryType = parent.getName().split("-")[0];
                if (!directoryType.equals("values")) {
                    return false;
                }

                // Check if the resource is declared here
                final Pattern pattern = Pattern.compile("<color.*?name\\s*=\\s*\"" + resourceName + "\".*?/?>");

                final Matcher matcher = pattern.matcher(fileContents);

                if (matcher.find()) {
                    return true;
                }

                return false;
            }
        });

        // dimen
        sResourceTypes.put("dimen", new ResourceType("dimen") {
            @Override
            public boolean doesFileDeclareResource(final File parent, final String fileName, final String fileContents, final String resourceName) {
                // Check if we're in a valid directory
                if (!parent.isDirectory()) {
                    return false;
                }

                final String directoryType = parent.getName().split("-")[0];
                if (!directoryType.equals("values")) {
                    return false;
                }

                // Check if the resource is declared here
                final Pattern pattern = Pattern.compile("<dimen.*?name\\s*=\\s*\"" + resourceName + "\".*?/?>");

                final Matcher matcher = pattern.matcher(fileContents);

                if (matcher.find()) {
                    return true;
                }

                return false;
            }
        });

        // drawable
        sResourceTypes.put("drawable", new ResourceType("drawable") {
            @Override
            public boolean doesFileDeclareResource(final File parent, final String fileName, final String fileContents, final String resourceName) {
                // Check if we're in a valid directory
                if (!parent.isDirectory()) {
                    return false;
                }

                final String directoryType = parent.getName().split("-")[0];
                if (directoryType.equals(getType())) {
                    // We're in a drawable- directory

                    // Check if the resource is declared here
                    final String name = fileName.split("\\.")[0];

                    final Pattern pattern = Pattern.compile("^" + resourceName + "$");

                    return pattern.matcher(name).find();
                }

                if (directoryType.equals("values")) {
                    // We're in a values- directory

                    // Check if the resource is declared here
                    final Pattern pattern = Pattern.compile("<drawable.*?name\\s*=\\s*\"" + resourceName + "\".*?/?>");

                    final Matcher matcher = pattern.matcher(fileContents);

                    if (matcher.find()) {
                        return true;
                    }
                }

                return false;
            }
        });

        // id
        sResourceTypes.put("id", new ResourceType("id") {
            @Override
            public boolean doesFileDeclareResource(final File parent, final String fileName, final String fileContents, final String resourceName) {
                // Check if we're in a valid directory
                if (!parent.isDirectory()) {
                    return false;
                }

                final String directoryType = parent.getName().split("-")[0];
                if (!directoryType.equals("values") && !directoryType.equals("layout")) {
                    return false;
                }

                // Check if the resource is declared here
                final Pattern valuesPattern0 = Pattern.compile("<item.*?type\\s*=\\s*\"id\".*?name\\s*=\\s*\"" + resourceName + "\".*?/?>");
                final Pattern valuesPattern1 = Pattern.compile("<item.*?name\\s*=\\s*\"" + resourceName + "\".*?type\\s*=\\s*\"id\".*?/?>");
                final Pattern layoutPattern = Pattern.compile(":id\\s*=\\s*\"@\\+id/" + resourceName + "\"");

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

        // integer
        sResourceTypes.put("integer", new ResourceType("integer") {
            @Override
            public boolean doesFileDeclareResource(final File parent, final String fileName, final String fileContents, final String resourceName) {
                // Check if we're in a valid directory
                if (!parent.isDirectory()) {
                    return false;
                }

                final String directoryType = parent.getName().split("-")[0];
                if (!directoryType.equals("values")) {
                    return false;
                }

                // Check if the resource is declared here
                final Pattern pattern = Pattern.compile("<integer.*?name\\s*=\\s*\"" + resourceName + "\".*?/?>");

                final Matcher matcher = pattern.matcher(fileContents);

                if (matcher.find()) {
                    return true;
                }

                return false;
            }
        });

        // layout
        sResourceTypes.put("layout", new ResourceType("layout") {
            @Override
            public boolean doesFileDeclareResource(final File parent, final String fileName, final String fileContents, final String resourceName) {
                // Check if we're in a valid directory
                if (!parent.isDirectory()) {
                    return false;
                }

                final String directoryType = parent.getName().split("-")[0];
                if (!directoryType.equals(getType())) {
                    return false;
                }

                // Check if the resource is declared here
                final String name = fileName.split("\\.")[0];

                final Pattern pattern = Pattern.compile("^" + resourceName + "$");

                return pattern.matcher(name).find();
            }
        });

        // menu
        sResourceTypes.put("menu", new ResourceType("menu") {
            @Override
            public boolean doesFileDeclareResource(final File parent, final String fileName, final String fileContents, final String resourceName) {
                // Check if we're in a valid directory
                if (!parent.isDirectory()) {
                    return false;
                }

                final String directoryType = parent.getName().split("-")[0];
                if (!directoryType.equals(getType())) {
                    return false;
                }

                // Check if the resource is declared here
                final String name = fileName.split("\\.")[0];

                final Pattern pattern = Pattern.compile("^" + resourceName + "$");

                return pattern.matcher(name).find();
            }
        });

        // plurals
        sResourceTypes.put("plurals", new ResourceType("plurals") {
            @Override
            public boolean doesFileDeclareResource(final File parent, final String fileName, final String fileContents, final String resourceName) {
                // Check if we're in a valid directory
                if (!parent.isDirectory()) {
                    return false;
                }

                final String directoryType = parent.getName().split("-")[0];
                if (!directoryType.equals("values")) {
                    return false;
                }

                // Check if the resource is declared here
                final Pattern pattern = Pattern.compile("<plurals.*?name\\s*=\\s*\"" + resourceName + "\".*?/?>");

                final Matcher matcher = pattern.matcher(fileContents);

                if (matcher.find()) {
                    return true;
                }

                return false;
            }
        });

        // raw
        sResourceTypes.put("raw", new ResourceType("raw") {
            @Override
            public boolean doesFileDeclareResource(final File parent, final String fileName, final String fileContents, final String resourceName) {
                // Check if we're in a valid directory
                if (!parent.isDirectory()) {
                    return false;
                }

                final String directoryType = parent.getName().split("-")[0];
                if (!directoryType.equals(getType())) {
                    return false;
                }

                // Check if the resource is declared here
                final String name = fileName.split("\\.")[0];

                final Pattern pattern = Pattern.compile("^" + resourceName + "$");

                return pattern.matcher(name).find();
            }
        });

        // string
        sResourceTypes.put("string", new ResourceType("string") {
            @Override
            public boolean doesFileDeclareResource(final File parent, final String fileName, final String fileContents, final String resourceName) {
                // Check if we're in a valid directory
                if (!parent.isDirectory()) {
                    return false;
                }

                final String directoryType = parent.getName().split("-")[0];
                if (!directoryType.equals("values")) {
                    return false;
                }

                // Check if the resource is declared here
                final Pattern pattern = Pattern.compile("<string.*?name\\s*=\\s*\"" + resourceName + "\".*?/?>");

                final Matcher matcher = pattern.matcher(fileContents);

                if (matcher.find()) {
                    return true;
                }

                return false;
            }
        });

        // style
        sResourceTypes.put("style", new ResourceType("style") {
            @Override
            public boolean doesFileDeclareResource(final File parent, final String fileName, final String fileContents, final String resourceName) {
                // Check if we're in a valid directory
                if (!parent.isDirectory()) {
                    return false;
                }

                final String directoryType = parent.getName().split("-")[0];
                if (!directoryType.equals("values")) {
                    return false;
                }

                // Check if the resource is declared here
                final Pattern pattern = Pattern.compile("<style.*?name\\s*=\\s*\"" + resourceName + "\".*?/?>");

                final Matcher matcher = pattern.matcher(fileContents);

                if (matcher.find()) {
                    return true;
                }

                return false;
            }

            @Override
            public boolean doesFileUseResource(final File parent, final String fileName, final String fileContents, final String resourceName) {
                if (parent != null) {
                    // Check if we're in a valid directory
                    if (!parent.isDirectory()) {
                        return false;
                    }

                    final String directoryType = parent.getName().split("-")[0];
                    if (!directoryType.equals("values")) {
                        return false;
                    }
                }

                // Check if the resource is used here as a parent (name="Parent.Child")
                final Pattern pattern = Pattern.compile("<style.*?name\\s*=\\s*\"" + resourceName + "\\.\\w+\".*?/?>");

                final Matcher matcher = pattern.matcher(fileContents);

                if (matcher.find()) {
                    return true;
                }

                // Check if the resource is used here as a parent (parent="Parent")
                final Pattern pattern1 = Pattern.compile("<style.*?parent\\s*=\\s*\"" + resourceName + "\".*?/?>");

                final Matcher matcher1 = pattern1.matcher(fileContents);

                if (matcher1.find()) {
                    return true;
                }

                return false;
            }
        });

        // styleable
        sResourceTypes.put("styleable", new ResourceType("styleable") {
            @Override
            public boolean doesFileDeclareResource(final File parent, final String fileName, final String fileContents, final String resourceName) {
                // Check if we're in a valid directory
                if (!parent.isDirectory()) {
                    return false;
                }

                final String directoryType = parent.getName().split("-")[0];
                if (!directoryType.equals("values")) {
                    return false;
                }

                // Check if the resource is declared here
                final String[] styleableAttr = resourceName.split("\\[_\\\\.\\]");

                if (styleableAttr.length == 1) {
                    // This is the name of the styleable, not one of its attributes
                    final Pattern pattern = Pattern.compile("<declare-styleable.*?name\\s*=\\s*\"" + styleableAttr[0] + "\".*?/?>");
                    final Matcher matcher = pattern.matcher(fileContents);

                    if (matcher.find()) {
                        return true;
                    }

                    return false;
                }

                // It's one of the attributes, like Styleable_attribute
                final Pattern blockPattern = Pattern.compile("<declare-styleable.*?name\\s*=\\s*\"" + styleableAttr[0] + "\".*?>(.*?)</declare-styleable\\s*>");
                final Matcher blockMatcher = blockPattern.matcher(fileContents);

                if (blockMatcher.find()) {
                    final String styleableAttributes = blockMatcher.group(1);

                    // We now have just the attributes for the styleable
                    final Pattern attributePattern = Pattern.compile("<attr.*?name\\s*=\\s*\"" + styleableAttr[1] + "\".*?/?>");
                    final Matcher attributeMatcher = attributePattern.matcher(styleableAttributes);

                    if (attributeMatcher.find()) {
                        return true;
                    }

                    return false;
                }

                return false;
            }
        });

        // xml
        sResourceTypes.put("xml", new ResourceType("xml") {
            @Override
            public boolean doesFileDeclareResource(final File parent, final String fileName, final String fileContents, final String resourceName) {
                // Check if we're in a valid directory
                if (!parent.isDirectory()) {
                    return false;
                }

                final String directoryType = parent.getName().split("-")[0];
                if (!directoryType.equals(getType())) {
                    return false;
                }

                // Check if the resource is declared here
                final String name = fileName.split("\\.")[0];

                final Pattern pattern = Pattern.compile("^" + resourceName + "$");

                return pattern.matcher(name).find();
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
     * 
     * @param baseDirectory
     *            The project directory to use.
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

        mPackageName = findPackageName(mManifestFile);

        if (mPackageName == null || mPackageName.trim().length() == 0) {
            System.err.println("Unable to determine your application's package name from AndroidManifest.xml.  Please ensure it is set.");
            return;
        }

        if (mGenDirectory == null) {
            System.err.println("You must first build your project to generate R.java");
            return;
        }

        mRJavaFile = findRJavaFile(mGenDirectory, mPackageName);

        if (mRJavaFile == null) {
            System.err.println("You must first build your project to generate R.java");
            return;
        }

        mResources.clear();

        try {
            mResources.addAll(getResourceList(mRJavaFile));
        } catch (final IOException e) {
            System.err.println("The R.java found could not be opened.");
            e.printStackTrace();
        }

        System.out.println(mResources.size() + " resources found");
        System.out.println();

        mUsedResources.clear();

        searchFiles(null, mSrcDirectory, sJavaFileType);
        searchFiles(null, mResDirectory, sXmlFileType);
        searchFiles(null, mManifestFile, sXmlFileType);

        /*
         * Find the paths where the unused resources are declared.
         */
        final SortedMap<String, SortedMap<String, Resource>> unusedResources = new TreeMap<String, SortedMap<String, Resource>>();

        for (final Resource resource : mResources) {
            final String type = resource.getType();
            SortedMap<String, Resource> typeMap = unusedResources.get(type);

            if (typeMap == null) {
                typeMap = new TreeMap<String, Resource>();
                unusedResources.put(type, typeMap);
            }

            typeMap.put(resource.getName(), resource);
        }

        // Ensure we only try to find resource types that exist in the map we just built
        final Map<String, ResourceType> unusedResourceTypes = new HashMap<String, ResourceType>(unusedResources.size());

        for (final String type : unusedResources.keySet()) {
            final ResourceType resourceType = sResourceTypes.get(type);
            if (resourceType != null) {
                unusedResourceTypes.put(type, resourceType);
            }
        }

        findDeclaredPaths(null, mResDirectory, unusedResourceTypes, unusedResources);

        /*
         * Find the paths where the used resources are declared.
         */
        final SortedMap<String, SortedMap<String, Resource>> usedResources = new TreeMap<String, SortedMap<String, Resource>>();

        for (final Resource resource : mUsedResources) {
            final String type = resource.getType();
            SortedMap<String, Resource> typeMap = usedResources.get(type);

            if (typeMap == null) {
                typeMap = new TreeMap<String, Resource>();
                usedResources.put(type, typeMap);
            }

            typeMap.put(resource.getName(), resource);
        }

        // Ensure we only try to find resource types that exist in the map we just built
        final Map<String, ResourceType> usedResourceTypes = new HashMap<String, ResourceType>(usedResources.size());

        for (final String type : usedResources.keySet()) {
            final ResourceType resourceType = sResourceTypes.get(type);
            if (resourceType != null) {
                usedResourceTypes.put(type, resourceType);
            }
        }

        findDeclaredPaths(null, mResDirectory, usedResourceTypes, usedResources);

        // Deal with resources from library projects
        final Set<Resource> libraryProjectResources = getLibraryProjectResources();

        /*
         * Since an app can override a library project resource, we cannot simply remove all resources that are defined in library projects. Instead, we must
         * only remove them if we cannot find a declaration of them in the current project.
         */
        for (final Resource libraryResource : libraryProjectResources) {
            final SortedMap<String, Resource> typedResources = unusedResources.get(libraryResource.getType());

            if (typedResources != null) {
                final Resource appResource = typedResources.get(libraryResource.getName());

                if (appResource != null && appResource.hasNoDeclaredPaths()) {
                    typedResources.remove(libraryResource.getName());
                    mResources.remove(appResource);
                }
            }
        }

        final UsageMatrix usageMatrix = new UsageMatrix(mBaseDirectory, usedResources);
        usageMatrix.generateMatrices();

        final int unusedResourceCount = mResources.size();

        if (unusedResourceCount > 0) {
            System.out.println(unusedResourceCount + " unused resources were found:");

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

        if (children == null) {
            return;
        }

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

    private static String findPackageName(final File androidManifestFile) {
        String manifest = "";

        try {
            manifest = FileUtilities.getFileContents(androidManifestFile);
        } catch (final IOException e) {
            e.printStackTrace();
        }

        final Pattern pattern = Pattern.compile("<manifest\\s+.*?package\\s*=\\s*\"([A-Za-z0-9_\\.]+)\".*?>");
        final Matcher matcher = pattern.matcher(manifest);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private static File findRJavaFile(final File baseDirectory, final String packageName) {
        final File rJava = new File(baseDirectory, packageName.replace('.', '/') + "/R.java");

        if (rJava.exists()) {
            return rJava;
        }

        return null;
    }

    /**
     * Removes all resources declared in library projects.
     */
    private Set<Resource> getLibraryProjectResources() {
        final Set<Resource> resources = new HashSet<Resource>();

        // Find the library projects
        final File projectPropertiesFile = new File(mBaseDirectory, "project.properties");

        if (!projectPropertiesFile.exists()) {
            return resources;
        }

        List<String> fileLines = new ArrayList<String>();
        try {
            fileLines = FileUtilities.getFileLines(projectPropertiesFile);
        } catch (final IOException e) {
            e.printStackTrace();
        }

        final Pattern libraryProjectPattern = Pattern.compile("^android\\.library\\.reference\\.\\d+=(.*)$", Pattern.CASE_INSENSITIVE);

        final List<String> libraryProjectPaths = new ArrayList<String>();

        for (final String line : fileLines) {
            final Matcher libraryProjectMatcher = libraryProjectPattern.matcher(line);

            if (libraryProjectMatcher.find()) {
                libraryProjectPaths.add(libraryProjectMatcher.group(1));
            }
        }

        // We have the paths to the library projects, now we need their R.java files
        for (final String libraryProjectPath : libraryProjectPaths) {
            final File libraryProjectDirectory = new File(mBaseDirectory, libraryProjectPath);

            if (libraryProjectDirectory.exists() && libraryProjectDirectory.isDirectory()) {
                final String libraryProjectPackageName = findPackageName(new File(libraryProjectDirectory, "AndroidManifest.xml"));
                final File libraryProjectRJavaFile = findRJavaFile(new File(libraryProjectDirectory, "gen"), libraryProjectPackageName);

                try {
                    resources.addAll(getResourceList(libraryProjectRJavaFile));
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return resources;
    }

    private static Set<Resource> getResourceList(final File rJavaFile) throws IOException {
        final InputStream inputStream = new FileInputStream(rJavaFile);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        boolean done = false;

        final Set<Resource> resources = new HashSet<Resource>();

        String type = "";

        while (!done) {
            final String line = reader.readLine();
            done = (line == null);

            if (line != null) {
                final Matcher typeMatcher = sResourceTypePattern.matcher(line);
                final Matcher nameMatcher = sResourceNamePattern.matcher(line);

                if (nameMatcher.find()) {
                    resources.add(new Resource(type, nameMatcher.group(3)));
                } else if (typeMatcher.find()) {
                    type = typeMatcher.group(1);
                }
            }
        }

        reader.close();
        inputStream.close();

        return resources;
    }

    private void searchFiles(final File parent, final File file, final FileType fileType) {
        if (file.isDirectory()) {
            for (final File child : file.listFiles()) {
                searchFiles(file, child, fileType);
            }
        } else if (file.getName().endsWith(fileType.getExtension())) {
            try {
                searchFile(parent, file, fileType);
            } catch (final IOException e) {
                System.err.println("There was a problem reading " + file.getAbsolutePath());
                e.printStackTrace();
            }
        }
    }

    private void searchFile(final File parent, final File file, final FileType fileType) throws IOException {
        final Set<Resource> foundResources = new HashSet<Resource>();

        final String fileContents = FileUtilities.getFileContents(file);

        for (final Resource resource : mResources) {
            final Matcher matcher = fileType.getPattern(resource.getType(), resource.getName().replace("_", "[_\\.]")).matcher(fileContents);

            if (matcher.find()) {
                foundResources.add(resource);
            } else {
                final ResourceType type = sResourceTypes.get(resource.getType());

                if (type != null && type.doesFileUseResource(parent, file.getName(), fileContents, resource.getName().replace("_", "[_\\.]"))) {
                    foundResources.add(resource);
                }
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
                final String fileName = file.getName();

                String fileContents = "";
                try {
                    fileContents = FileUtilities.getFileContents(file);
                } catch (final IOException e) {
                    e.printStackTrace();
                }

                for (final ResourceType resourceType : resourceTypes.values()) {
                    final Map<String, Resource> typeMap = resources.get(resourceType.getType());

                    if (typeMap != null) {
                        for (final Resource resource : typeMap.values()) {
                            if (resourceType.doesFileDeclareResource(parent, fileName, fileContents, resource.getName().replace("_", "[_\\.]"))) {
                                resource.addDeclaredPath(file.getAbsolutePath());

                                final String configuration = parent.getName();
                                resource.addConfiguration(configuration);
                            }
                        }
                    }
                }
            }
        }
    }
}
