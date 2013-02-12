package cz.zcu.kiv.crce.metadata;

/**
 * Object of this interface type can evaluate dependencies between added resources.
 *
 * @author Jiri Kucera (jiri.kucera@kalwi.eu)
 */
public interface Resolver {

    public boolean isSatisfied(Capability capability);

    // 
    
    void add(Resource resource);

    void clean();

    Reason[] getUnsatisfiedRequirements();

    Resource[] getOptionalResources();

    Reason[] getReason(Resource resource);

    Resource[] getResources(Requirement requirement);

    Resource[] getRequiredResources();

    Resource[] getAddedResources();

    boolean resolve();
}
