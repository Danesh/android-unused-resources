package ca.skennedy.androidunusedresources;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

/**
 * Generates a usage matrix that lists the various configurations each resource is defined under.
 */
public class UsageMatrix {
    private final File mBaseDirectory;

    /**
     * <p>
     * ResourceType->(ResourceName->Resource)
     * </p>
     * <p>
     * string->(app_name->Resource)
     * </p>
     */
    private final SortedMap<String, SortedMap<String, Resource>> mResources;

    /**
     * <p>
     * ResourceType->(Configuration)
     * </p>
     * <p>
     * string->(en-rUS)
     * </p>
     */
    private final Map<String, LinkedHashSet<String>> mConfigurations = new HashMap<String, LinkedHashSet<String>>();

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public UsageMatrix(final File baseDirectory, final SortedMap<String, SortedMap<String, Resource>> resources) {
        mBaseDirectory = baseDirectory;
        mResources = resources;
    }

    public void generateMatrices() {
        final File matrixDirectory = new File(mBaseDirectory, "resource-matrices");

        if (!matrixDirectory.exists()) {
            System.out
                    .println("Not generating resource qualifier matrices. If you would like them, create a directory named 'resource-matrices' in the base of your project.");
            System.out.println();
            return;
        }

        System.out.println("Resource qualifier matrices generated.");
        System.out.println();

        generateConfigurationList();

        for (final String resourceType : mResources.keySet()) {
            final File resourceMatrix = new File(matrixDirectory, resourceType + ".csv");

            try {
                final Writer writer = new BufferedWriter(new FileWriter(resourceMatrix));
                writer.write(buildCsv(resourceType));
                writer.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void generateConfigurationList() {
        mConfigurations.clear();

        // Resource types
        for (final String resourceType : mResources.keySet()) {
            // Set up the resource type
            final LinkedHashSet<String> configurations = new LinkedHashSet<String>();
            mConfigurations.put(resourceType, configurations);

            final SortedMap<String, Resource> resources = mResources.get(resourceType);

            // All the resources of this type
            for (final Resource resource : resources.values()) {
                // Paths where it exists
                for (final String configuration : resource.getConfigurations()) {
                    configurations.add(configuration);
                }
            }
        }
    }

    /**
     * Generates the CSV for a given resource type
     * 
     * @param resourceType
     *            The resource type for which to build the CSV
     * @return a {@link String} in the format:
     * 
     *         <pre>
     * ,ldpi,mdpi,hdpi,xhdpi
     * resource-name0,,X,X,
     * resource-name2,,X,X,X
     * resource-name1,,X,,
     * </pre>
     */
    private String buildCsv(final String resourceType) {
        final StringBuilder stringBuilder = new StringBuilder();

        final Set<String> configurations = mConfigurations.get(resourceType);

        // Header row
        for (final String configuration : configurations) {
            stringBuilder.append(',').append(configuration);
        }

        // Resource rows
        for (final Resource resource : mResources.get(resourceType).values()) {
            stringBuilder.append(LINE_SEPARATOR).append(resource.getName());

            final Set<String> resourceConfigurations = resource.getConfigurations();

            for (final String configuration : configurations) {
                stringBuilder.append(',');

                if (resourceConfigurations.contains(configuration)) {
                    stringBuilder.append('X');
                }
            }
        }

        return stringBuilder.toString();
    }
}
