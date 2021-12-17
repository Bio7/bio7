/*package com.eco.bio7.rcp;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.eclipse.osgi.internal.loader.classpath.ClasspathEntry;
import org.eclipse.osgi.internal.loader.classpath.ClasspathManager;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.storage.bundlefile.ZipBundleFile;

@SuppressWarnings("restriction")
public final class Internals
{
    public static void moduleClassLoader_addClasspathEntry(Class<?> klass, File jarFile)
    {
        try
        {
            ModuleClassLoader moduleClassLoader = (ModuleClassLoader) klass.getClassLoader();
            ClasspathManager classpathManager = moduleClassLoader.getClasspathManager();
            // Get Configuration
            Method getConfigurationMethod = ModuleClassLoader.class.getDeclaredMethod("getConfiguration");
            getConfigurationMethod.setAccessible(true);
            EquinoxConfiguration config = (EquinoxConfiguration) getConfigurationMethod.invoke(moduleClassLoader);
            // Get ClasspathManager.entries
            Field entriesField = ClasspathManager.class.getDeclaredField("entries");
            entriesField.setAccessible(true);
            ClasspathEntry[] oldEntries = (ClasspathEntry[]) entriesField.get(classpathManager);
            // Make ClasspathManager.entries writable (it's final field!)
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(entriesField, entriesField.getModifiers() & ~Modifier.FINAL);
            // Create ClasspathEntry for the given jarFile
            BundleFile jarBundleFile = new ZipBundleFile(jarFile, null, null, config.getDebug());
            ClasspathEntry jarEntry = new ClasspathEntry(jarBundleFile, oldEntries[0].getDomain(), classpathManager.getGeneration());
            // Prepare new entries
            ClasspathEntry[] newEntries = new ClasspathEntry[oldEntries.length + 1];
            System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
            newEntries[oldEntries.length] = jarEntry;
            // Set new entries to replace the old one
            entriesField.set(classpathManager, newEntries);
            System.out.println("added classpath: " + jarFile);
        }
        catch (IOException | ReflectiveOperationException e)
        {
            e.printStackTrace();
        }
    }
}*/