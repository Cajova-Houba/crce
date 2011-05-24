package cz.zcu.kiv.crce.metadata.combined.internal;

import cz.zcu.kiv.crce.metadata.ResourceCreator;
import cz.zcu.kiv.crce.plugin.Plugin;
import cz.zcu.kiv.crce.plugin.PluginManager;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

/**
 * Activator of this bundle.
 * @author Jiri Kucera (kalwi@students.zcu.cz, jiri.kucera@kalwi.eu)
 */
public class Activator extends DependencyActivatorBase {

    public static final String PID = "cz.zcu.kiv.crce.metadata.combined";
    
    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        
        manager.add(createComponent()
                .setInterface(Plugin.class.getName(), null)
                .setImplementation(CombinedResourceDAO.class)
                .add(createServiceDependency().setRequired(true).setService(PluginManager.class))
                .add(createServiceDependency().setRequired(true).setService(ResourceCreator.class))
                .add(createConfigurationDependency().setPid(PID))
                );
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {
        // nothing to do
    }
}
