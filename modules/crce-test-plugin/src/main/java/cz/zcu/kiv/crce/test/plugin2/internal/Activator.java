package cz.zcu.kiv.crce.test.plugin2.internal;

import cz.zcu.kiv.crce.metadata.MetadataFactory;
import cz.zcu.kiv.crce.metadata.service.MetadataService;
import cz.zcu.kiv.crce.plugin.Plugin;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;


/**
 * Activator class required by crce.
 */
public class Activator extends DependencyActivatorBase {

    @Override
    public void init(BundleContext bundleContext, DependencyManager dependencyManager) throws Exception {
        dependencyManager.add(createComponent()
                .setInterface(Plugin.class.getName(), null)
                .setImplementation(ExamplePlugin.class)
                .add(createServiceDependency().setRequired(true).setService(MetadataService.class))
                .add(createServiceDependency().setRequired(true).setService(MetadataFactory.class))
        );
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {
//        super.destroy(context, manager);
    }
}