package cz.zcu.kiv.crce.test.plugin2.search.impl.central.aether;

import cz.zcu.kiv.crce.test.plugin2.search.FoundArtifact;
import cz.zcu.kiv.crce.test.plugin2.search.MavenLocator;
import cz.zcu.kiv.crce.test.plugin2.search.impl.central.SimpleFoundArtifact;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.*;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.version.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * This locator uses the Eclipse Aether to search through central maven repo for artifacts.
 *
 * @author Zdendek Vales
 */
public class CentralMavenAetherLocator implements MavenLocator {

    private static final Logger logger = LoggerFactory.getLogger(CentralMavenAetherLocator.class);

    private static final String REPOSITORY_ID = "central";
    private static final String REPOSITORY_TYPE = "default";
    private static final String REPOSITORY_URL = "http://repo1.maven.org/maven2/";


    /**
     * Creates a list of repositories containing central repository.
     * @return
     */
    public static List<RemoteRepository> newRepositories()
    {
        return new ArrayList<RemoteRepository>( Arrays.asList( newCentralRepository() ) );
    }

    /**
     * Creates a new central repository object.
     * @return
     */
    private static RemoteRepository newCentralRepository()
    {
        return new RemoteRepository.Builder( REPOSITORY_ID, REPOSITORY_TYPE, REPOSITORY_URL).build();
    }


    /**
     * Initializes a new repository system for aether.
     * @return Repository system.
     */
    private RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService( TransporterFactory.class, FileTransporterFactory.class );
        locator.addService( TransporterFactory.class, HttpTransporterFactory.class );

        return locator.getService(RepositorySystem.class);
    }

    /**
     * Initializes a new repository session for aether. This is used to keep common settings for artifact
     * resolutions.
     * @param system Initialized repository system.
     * @return Repository session.
     */
    private RepositorySystemSession newSession(RepositorySystem system )
    {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        LocalRepository localRepo = new LocalRepository( "target/local-repo" );
        session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepo ) );

        return session;
    }


    @Override
    public FoundArtifact locate(String groupId, String artifactId, String version) {

        RepositorySystem repositorySystem = newRepositorySystem();
        RepositorySystemSession session = newSession(repositorySystem);

//        Artifact artifact = new DefaultArtifact(groupId+":"+artifactId+":"+VersionRangeBuilder.singleVersion(version));
        Artifact artifact = new DefaultArtifact(groupId+":"+artifactId+":"+version);

        ArtifactDescriptorRequest artifactDescriptorRequest = new ArtifactDescriptorRequest();
        artifactDescriptorRequest.setArtifact(artifact);
        artifactDescriptorRequest.setRepositories(newRepositories());

        ArtifactDescriptorResult artifactDescriptorResult = null;
        try {
            artifactDescriptorResult = repositorySystem.readArtifactDescriptor(session, artifactDescriptorRequest);
        } catch (ArtifactDescriptorException e) {
            logger.error("Unexpected error occurred while resolving artifact: "+e.getMessage());
            e.printStackTrace();
            return null;
        }

        FoundArtifact foundArtifact = new SimpleFoundArtifact(artifactDescriptorResult.getArtifact());

        return foundArtifact;
    }

    @Override
    public Collection<FoundArtifact> locate(String groupId, String artifactId) {
        RepositorySystem repositorySystem = newRepositorySystem();
        RepositorySystemSession session = newSession(repositorySystem);

        Artifact artifact = new DefaultArtifact(groupId+":"+artifactId+":"+VersionRangeBuilder.allVersions());

        VersionRangeRequest versionRangeRequest = new VersionRangeRequest();
        versionRangeRequest.setArtifact(artifact);
        versionRangeRequest.setRepositories(newRepositories());

        VersionRangeResult versionRangeResult = null;
        try {
            versionRangeResult = repositorySystem.resolveVersionRange(session, versionRangeRequest);
        } catch (VersionRangeResolutionException e) {
            logger.error("Unexpected error occurred while resolving artifact: "+e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }


        List<FoundArtifact> res = new ArrayList<>();
        for (Version v : versionRangeResult.getVersions()) {
            res.add(new SimpleFoundArtifact(groupId, artifactId, v.toString(), "", ""));
        }

        return res;
    }

    @Override
    public Collection<FoundArtifact> locate(String groupId, String artifactId, String fromVersion, String toVersion) {
        return null;
    }
}
