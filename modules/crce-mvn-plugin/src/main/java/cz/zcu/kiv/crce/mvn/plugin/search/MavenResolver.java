package cz.zcu.kiv.crce.mvn.plugin.search;

import java.util.Collection;

/**
 * Interface for resolving artifacts from a maven repo.
 *
 * Created by Zdenek Vales on 9.4.2017.
 */
// TODO: resolve methods will probably have to return URI of resolved artifacts or something like that
public interface MavenResolver {

    /**
     * Resolves the artifact from repository.
     * @param artifact Artifact to be resolved.
     * @return Resolved artifact or null if the artifact cannot be resolved.
     */
    FoundArtifact resolve(FoundArtifact artifact);

    /**
     * Resolves the collection of artifacts from repository.
     * @param artifacts Artifacts to be resolved.
     * @return Resolved artifacts or empty list if artifacts cannot be resolved.
     */
    Collection<FoundArtifact> resolveArtifacts(Collection<FoundArtifact> artifacts);
}
