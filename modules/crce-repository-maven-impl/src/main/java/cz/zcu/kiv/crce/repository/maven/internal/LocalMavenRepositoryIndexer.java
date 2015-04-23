package cz.zcu.kiv.crce.repository.maven.internal;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.FSDirectory;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.ArtifactInfoFilter;
import org.apache.maven.index.ArtifactInfoGroup;
import org.apache.maven.index.DefaultScannerListener;
import org.apache.maven.index.FlatSearchRequest;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.GroupedSearchRequest;
import org.apache.maven.index.GroupedSearchResponse;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.IndexerEngine;
import org.apache.maven.index.IteratorSearchRequest;
import org.apache.maven.index.IteratorSearchResponse;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.Scanner;
import org.apache.maven.index.ScanningRequest;
import org.apache.maven.index.context.DefaultIndexingContext;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexUtils;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.apache.maven.index.search.grouping.GAGrouping;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdateResult;
import org.apache.maven.index.updater.IndexUpdater;
import org.apache.maven.index.updater.ResourceFetcher;
import org.apache.maven.index.updater.WagonHelper;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.observers.AbstractTransferListener;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.zcu.kiv.crce.concurrency.model.Task;
import cz.zcu.kiv.crce.repository.maven.internal.aether.RepositoryFactory;
import cz.zcu.kiv.crce.repository.maven.internal.metadata.MetadataIndexerCallback;

/**
 *
 * @author Miroslav Brožek
 */
public class LocalMavenRepositoryIndexer extends Task<Object> {

    private static final Logger logger = LoggerFactory.getLogger(LocalMavenRepositoryIndexer.class);

    private final URI uri;
    private final MetadataIndexerCallback metadataIndexerCallback;
    private CloseableIndexingContext closeableIndexingContext;
    private static final String INDEXING_CONTEXT = MavenStoreConfig.getIndexingContextURI();
    
    
    public LocalMavenRepositoryIndexer(URI uri, MetadataIndexerCallback metadataIndexerCallback) {
        super(uri.toString(), "Indexes local maven repository.", "crce-repository-maven-impl");
        this.uri = uri;
        this.metadataIndexerCallback = metadataIndexerCallback;
    }

	@Override
	protected Object run() throws Exception {
		logger.info("Indexing Maven repository metadata started: {}", uri);
		logger.debug("Updating  index started.");

		try {

			if (!MavenStoreConfig.isRemoteRepoDefault()) {
				closeableIndexingContext = createLocalRepoIndexingContext(MavenStoreConfig.getStoreName(), new File(uri), new File(
						INDEXING_CONTEXT), MavenStoreConfig.isUpdateRepository());
			}

			else {
				closeableIndexingContext = createRemoteRepositoryIndexingContext(MavenStoreConfig.getStoreName(), uri, new File(
						INDEXING_CONTEXT), MavenStoreConfig.isUpdateRepository());
			}

			Indexer indexer = closeableIndexingContext.getIndexer();
			Set<ArtifactInfo> results = indexAll(indexer);

			ArtifactResolve ar = MavenStoreConfig.getArtifactResolve();
			switch (ar) {
			case ALL:
				logger.debug("All Artifact's versions will be processed");				
				break;

			case NEWEST:
				logger.debug("Only the latest Artifact's versions will be processed");
				//results = filterNewest(results);
				results = latestVersionMI(indexer);
				break;

			case HIGHEST_MAJOR:
				logger.debug("Only the highest major Artifact's versions will be processed");
				results = highestMajorMI(indexer, results);
				break;
			case HIGHEST_MINOR:
				logger.debug("Only the highest minor Artifact's versions will be processed");
				break;
			case HIGHEST_MICRO:
				logger.debug("Only the highest micro Artifact's versions will be processed");
				break;
			default:
				// index all??
				// index none?
				break;
			}

			logger.debug("Indexing artifacts (amount: {}).", results.size());
			indexResults(results);

		} catch (Exception e) {
			logger.error("Error updating Maven repository index. STOPPING INDEXING artifact's metadata !!", e);
			return null;
		}

		logger.info("Indexing Maven repository metadata finished: {}", uri);
		closeableIndexingContext.close();
		return null;
	}

	private void indexResults(Set<ArtifactInfo> results) {
		RepositorySystem system = RepositoryFactory.newRepositorySystem(); // Aether
		RepositorySystemSession session = RepositoryFactory.newRepositorySystemSession(system);
		ArtifactRequest artifactRequest = new ArtifactRequest();
		artifactRequest.setRepositories(RepositoryFactory.newRepositories());

		for (ArtifactInfo ai : results) {
			artifactRequest.setArtifact(new DefaultArtifact(ai.groupId + ":" + ai.artifactId + ":" + ai.version));
			logger.debug("Processing artifact {} from indexingContext.", ai.toString());		

			try {

				if (MavenStoreConfig.isResolveArtifacts() == false) {
					indexByPom(system, session, artifactRequest, ai);
				}

				else {
					indexByJar(system, session, artifactRequest);
				}
			}

			catch (ArtifactResolutionException e) {
				logger.error("Artifact {} couldnt be resolved, could be old indexing context: ", ai);
				// optionally try download the artifact from a another remote
				// repository or local .m2
				continue;

			} catch (ArtifactDescriptorException e) {
				logger.error("Failed to read ArtifactDescriptor...", e);
				continue;
			}
		}
	}

	private void indexByPom(RepositorySystem system, RepositorySystemSession session, ArtifactRequest artifactRequest, ArtifactInfo ai)
			throws ArtifactDescriptorException, ArtifactResolutionException {
		Artifact artifact = new DefaultArtifact(ai.groupId + ":" + ai.artifactId + ":" + ai.version);

		ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
		descriptorRequest.setArtifact(artifact);
		descriptorRequest.setRepositories(artifactRequest.getRepositories());
		ArtifactDescriptorResult descriptorResult;
		descriptorResult = system.readArtifactDescriptor(session, descriptorRequest);
		Artifact a = descriptorResult.getArtifact();

		a = setPOMfileToArtifact(a, system, session, artifactRequest);
		metadataIndexerCallback.index(a, this);
	}

	private Artifact setPOMfileToArtifact(Artifact a, RepositorySystem system, RepositorySystemSession session, ArtifactRequest artifactRequest) throws ArtifactResolutionException {
		File pom = new File(getPathForArtifact(a, true, true));
		
		if (pom.getAbsoluteFile().exists()) {
			a = a.setFile(pom);			
		}
		
		else{
			String g = a.getGroupId().split("\\.")[0];
			String pomS = a.getArtifactId() + "-" + a.getVersion()+".pom";	
			File root = new File(MavenStoreConfig.getLocalRepoURI().toString() + "\\" + g) ;
			String  newPath = findPOM(pomS, root);
			
			if(newPath== null){
				logger.debug("Can't find POM file...trying resolve whole JAR file... " + a);
				a = system.resolveArtifact(session, artifactRequest).getArtifact();				
			}
			else{
				logger.debug("POM file found in repository on different place"); 
				a=a.setFile(new File(newPath));
			}
		}		

		return a;
	}

	//fallback
	private String findPOM(String name, File dir) {
		String found = null;		

		File[] list = dir.listFiles();
		if (list != null)
			for (File fil : list) {
				if (fil.isDirectory()) {
					found = findPOM(name, fil);
					if(found!=null){
						break;
					}
				} else if (name.equalsIgnoreCase(fil.getName())) {
					return fil.getAbsolutePath();
				}
			}
		return found;
	}
	
	private String getPathForArtifact(Artifact artifact, boolean local, boolean searchPOM) {
	    StringBuilder path = new StringBuilder(128);
	    path.append(MavenStoreConfig.getLocalRepoURI().toString()+"\\");
	    path.append(artifact.getGroupId().replace('.', '\\')).append('\\');
	    path.append(artifact.getArtifactId()).append('\\');
	    path.append(artifact.getBaseVersion()).append('\\');
	    path.append(artifact.getArtifactId()).append('-');
	    if (local) {
	      path.append(artifact.getBaseVersion());
	    } else {
	      path.append(artifact.getVersion());
	    }
	    if (artifact.getClassifier().length() > 0) {
	      path.append('-').append(artifact.getClassifier());
	    }
	    
	    if(searchPOM){
	    	path.append('.').append("pom");
	    }
	    else if (artifact.getExtension().length() > 0) {
	      path.append('.').append(artifact.getExtension());
	    }
	    return path.toString();
	  }

	private void indexByJar(RepositorySystem system, RepositorySystemSession session, ArtifactRequest artifactRequest)
			throws ArtifactResolutionException {
		ArtifactResult result;
		result = system.resolveArtifact(session, artifactRequest);
		metadataIndexerCallback.index(result.getArtifact(), this);
	}

	/**
	 * Main method to get all 'bundles' from indexingContext
	 * 
	 * @param indexer  Indexer from context
	 * @return FlatSearchResponse result due query setting
	 * @throws IOException
	 */
	private Set<ArtifactInfo> indexAll(Indexer indexer) throws IOException {
		FlatSearchResponse response;
		BooleanQuery query = new BooleanQuery();
		query.add(indexer.constructQuery(MAVEN.PACKAGING, new SourcedSearchExpression("bundle")), BooleanClause.Occur.MUST);
		response = indexer.searchFlat(new FlatSearchRequest(query, closeableIndexingContext));
		return response.getResults();
	}

	/**
	 * Filter to search latest version of bundle recieved by MavenIndexer 
	 * 
	 * @param indexer maven indexer
	 * @return Set of artifacts
	 * @throws IOException
	 */
	private Set<ArtifactInfo> latestVersionMI(Indexer indexer) throws IOException {		
		BooleanQuery query = new BooleanQuery();
		query.add(indexer.constructQuery(MAVEN.PACKAGING, new SourcedSearchExpression("bundle")), BooleanClause.Occur.MUST);
		GroupedSearchResponse response = indexer.searchGrouped(new GroupedSearchRequest(query, new GAGrouping(), closeableIndexingContext));
		Set<ArtifactInfo> res = new LinkedHashSet<ArtifactInfo>();
		
		//debug
		for (Map.Entry<String, ArtifactInfoGroup> entry : response.getResults().entrySet()) {
			ArtifactInfo ai = entry.getValue().getArtifactInfos().iterator().next();
			res.add(ai);
			logger.debug("* Entry " + ai);
			logger.debug("{} artifact atest version:  {}",ai, ai.version);
		}		 
		return res;
	}

	/**
	 * Filter to search only artifact with highest major in version
	 * @param indexer i maven indexer
	 * @param ai is artifact info
	 * @throws IOException
	 * @throws InvalidVersionSpecificationException 
	 */
	private Set<ArtifactInfo> highestMajorMI(Indexer indexer, Set<ArtifactInfo> results) throws IOException,
			InvalidVersionSpecificationException {
		for (ArtifactInfo ai : results) {

			getVersions(indexer, ai);

		}
		return results;
	}

	private void getVersions(Indexer indexer, ArtifactInfo ai) throws InvalidVersionSpecificationException, IOException {
		Query gidQ = indexer.constructQuery(MAVEN.GROUP_ID, new SourcedSearchExpression(ai.groupId));
		Query aidQ = indexer.constructQuery(MAVEN.ARTIFACT_ID, new SourcedSearchExpression(ai.artifactId));		
		Query pckQ = indexer.constructQuery(MAVEN.PACKAGING, new SourcedSearchExpression("bundle"));

		BooleanQuery bq = new BooleanQuery();
		bq.add(gidQ, Occur.MUST);
		bq.add(aidQ, Occur.MUST);	
		bq.add(pckQ, Occur.MUST);

		final GenericVersionScheme versionScheme = new GenericVersionScheme();
		final String versionString = "0.0.0";
		final Version version = versionScheme.parseVersion(versionString);

		// construct the filter to express "V greater than"
		final ArtifactInfoFilter versionFilter = new ArtifactInfoFilter() {
			public boolean accepts(final IndexingContext ctx, final ArtifactInfo ai) {
				try {
					final Version aiV = versionScheme.parseVersion(ai.version);
					// Use ">=" if you are INCLUSIVE
					return aiV.compareTo(version) > 0;
				} catch (InvalidVersionSpecificationException e) {
					// do something here? be safe and include?
					return true;
				}
			}
		};

		System.out.println("Searching for all GAVs with G=org.sonatype.nexus and nexus-api and having V greater than 1.5.0");
		final IteratorSearchRequest request = new IteratorSearchRequest(bq,
				Collections.singletonList((IndexingContext) closeableIndexingContext), versionFilter);
		final IteratorSearchResponse response = indexer.searchIterator(request);
		for (ArtifactInfo a : response) {
			System.out.println(a.toString());
		}
	}
	

	
//	//Aether artifacts latest version filter NEEDS rework
//	private Set<ArtifactInfo> filterLatestByAether(Set<ArtifactInfo> all) throws VersionRangeResolutionException {
//		  RepositorySystem system = RepositoryFactory.newRepositorySystem(); // Aether
//			RepositorySystemSession session = RepositoryFactory.newRepositorySystemSession(system);
//			ArtifactRequest artifactRequest = new ArtifactRequest();
//			artifactRequest.setRepositories(RepositoryFactory.newRepositories());
//		
//		for (Iterator<ArtifactInfo> i = all.iterator(); i.hasNext();) {
//		    ArtifactInfo ai = i.next();			
//			Artifact artifact = new DefaultArtifact(ai.groupId + ":" + ai.artifactId + ":[0,)");
//
//			VersionRangeRequest rangeRequest = new VersionRangeRequest();
//			rangeRequest.setArtifact(artifact);
//			rangeRequest.setRepositories(RepositoryFactory.newRepositories());
//			VersionRangeResult rangeResult = system.resolveVersionRange(session, rangeRequest);
//			org.eclipse.aether.version.Version newestVersion = rangeResult.getHighestVersion();
//			List<org.eclipse.aether.version.Version> versions = rangeResult.getVersions();
////			logger.debug("Available versions " + versions);
//			if(newestVersion==null){
//				logger.debug("Removing artifact, which is not available anymore...");
//				i.remove();
//				continue;
//			}
//			Version latest = new MavenArtifactVersion(newestVersion.toString()).convertVersion();
//			Version cand = new MavenArtifactVersion(ai.version).convertVersion();
//
//			if (cand.compareTo(latest) < 0) {
//				i.remove();
//			}
//		}
//		
//		return all;
//	}

	private CloseableIndexingContext createLocalRepoIndexingContext(String name, File repository, File indexParentDir, boolean update)
            throws PlexusContainerException, ComponentLookupException, IOException {
        logger.debug("Updating index '{}' at '{}' for local repo '{}', update: {}", name, indexParentDir, repository, update);
        if (repository == null || indexParentDir == null) {
        	logger.debug("Mvn repository '{}' or index parent dir '{}' is null. Indexing could not be started!", repository, indexParentDir);
            return null;
        }
 
        if (!repository.exists()) {
            throw new IOException("Repository directory " + repository + " does not exist");
        }

        if (!indexParentDir.exists() && !indexParentDir.mkdirs()) {
            throw new IOException("Cannot create parent directory for indices: " + indexParentDir);
        }

        logger.debug("Initializing Plexus container.");

        PlexusContainer plexusContainer;
        Indexer indexer;
        try {
            ContainerConfiguration config = new DefaultContainerConfiguration();
            
            ClassWorld world = new ClassWorld();
            ClassRealm classRealm = new ClassRealm(world, "crce-maven-repo-indexer", getClass().getClassLoader());
            config.setRealm(classRealm);

            plexusContainer = new DefaultPlexusContainer(config);
            indexer = plexusContainer.lookup(Indexer.class);
//            final IndexUpdater indexUpdater = plexusContainer.lookup(IndexUpdater.class);
        } catch (Exception e) {
            logger.error("Error initializing Plexus container.", e);
            throw new IllegalStateException(e);
        }

        List<IndexCreator> indexers = new ArrayList<>();
        indexers.add(plexusContainer.lookup(IndexCreator.class, "min"));
        indexers.add( plexusContainer.lookup( IndexCreator.class, "maven-archetype" ) );
        indexers.add( plexusContainer.lookup( IndexCreator.class, "osgi-metadatas" ) );       

        logger.info("Creating indexing context of local maven store.");

        IndexingContext indexingContext = indexer.createIndexingContext(
                        name + "-context",
                        name,
                        repository,
                        new File(indexParentDir, name),
                        null,
                        null,
                        true,
                        true,
                        indexers
                );

        // always use temporary context when reindexing
        final File tmpFile = File.createTempFile(indexingContext.getId(), "-tmp", indexParentDir);
        final File tmpDir = new File(indexParentDir, tmpFile.getName() + ".dir");
        if (!tmpDir.mkdirs()) {
            throw new IOException("Cannot create temporary directory: " + tmpDir);
        }

        logger.debug("Temporary dir '{}' created.", tmpDir);


        try {
            Scanner scanner = plexusContainer.lookup(Scanner.class);
            IndexerEngine indexerEngine = plexusContainer.lookup(IndexerEngine.class);

            final FSDirectory directory = FSDirectory.open(tmpDir);
            if (update) {
                IndexUtils.copyDirectory(indexingContext.getIndexDirectory(), directory);
            }

            logger.debug("Creating temporary indexing context.");

            try(@SuppressWarnings("deprecation")
			CloseableIndexingContext tmpContext = new CloseableIndexingContext(
                    new DefaultIndexingContext(
                        indexingContext.getId() + "-tmp",
                        indexingContext.getRepositoryId(),
                        indexingContext.getRepository(),
                        directory,
                        indexingContext.getRepositoryUrl(),
                        indexingContext.getIndexUpdateUrl(),
                        indexingContext.getIndexCreators(),
                        true
                    ),
                    null)
            ) {

                logger.debug("Maven local store scanning started.");

                ScanningRequest scanningRequest = new ScanningRequest(
                        tmpContext,
                        new DefaultScannerListener(tmpContext, indexerEngine, update, null),
                        null
                );

                long start_time = System.nanoTime();  
                scanner.scan(scanningRequest);
                long end_time = System.nanoTime();
                double difference = (end_time - start_time)/1e6;
                logger.debug("{} nanoseconds to index local maven repository", difference);
                
                tmpContext.updateTimestamp(true);

                logger.debug("Replacing contexts from temporary to origin.");

                indexingContext.replace(tmpContext.getIndexDirectory());
            } catch (Throwable t) {
                logger.error("Error indexing local Maven repository 1.", t);
            }
        } catch (IOException | ComponentLookupException ex) {
            logger.error("Error indexing local Maven repository 2.", ex);
            throw new IOException("Error scanning context " + indexingContext.getId() + ": " + ex, ex);
        } catch (Throwable t) {
                logger.error("Error indexing local Maven repository 3.", t);
        } finally {
            try {
                if (tmpFile.exists()) {
                    tmpFile.delete();
                }
            } finally {
                FileUtils.deleteDirectory(tmpDir); // TODO replace plexus utils
            }
        }

        logger.debug("Indexing local maven store '{}' finished.", repository);

        return new CloseableIndexingContext(indexingContext, indexer);
    }
	
	
	private CloseableIndexingContext createRemoteRepositoryIndexingContext(String storeName, URI uri, File indexParentDir, boolean update)
			throws IOException, PlexusContainerException, ComponentLookupException {
		if (!indexParentDir.exists() && !indexParentDir.mkdirs()) {
			throw new IOException("Cannot create parent directory for indices: " + indexParentDir);
		}

		logger.debug("Initializing Plexus container.");

		PlexusContainer plexusContainer;
		Indexer indexer;
		IndexUpdater indexUpdater;
		IndexingContext indexingContext;
		Wagon httpWagon;

		try {
			DefaultContainerConfiguration config = new DefaultContainerConfiguration();
			config.setClassPathScanning(PlexusConstants.SCANNING_INDEX);
			plexusContainer = new DefaultPlexusContainer(config);

			ClassWorld world = new ClassWorld();
			ClassRealm classRealm = new ClassRealm(world, "crce-maven-repo-indexer", getClass().getClassLoader());
			config.setRealm(classRealm);

			plexusContainer = new DefaultPlexusContainer(config);
			indexer = plexusContainer.lookup(Indexer.class);
			indexUpdater = plexusContainer.lookup(IndexUpdater.class);
			httpWagon = plexusContainer.lookup(Wagon.class, "http");

		} catch (Exception e) {
			logger.error("Error initializing Plexus container.", e);
			throw new IllegalStateException(e);
		}
		

		// Creators we want to use (search for fields it defines)
		List<IndexCreator> indexers = new ArrayList<IndexCreator>();
		indexers.add(plexusContainer.lookup(IndexCreator.class, "min"));
		// indexers.add( plexusContainer.lookup( IndexCreator.class,"maven-archetype" ) );
		// indexers.add( plexusContainer.lookup( IndexCreator.class,"osgi-metadatas" ) );

		// Create context for remote repository index
		indexingContext = indexer.createIndexingContext(storeName + "-context", storeName, new File(storeName + "cache"), new File(indexParentDir, storeName),
				uri.toString(), null, true, true, indexers);

		//TODO: replace 'update' for some trigger ...eg once in week after midnight
		if (update) {

			logger.info("Updating Index...");
			logger.info("This might take a while on first run, so please be patient! ... It could take 5 minutes and more");

			TransferListener listener = new AbstractTransferListener() {
				public void transferStarted(TransferEvent transferEvent) {
					logger.info("  Downloading " + transferEvent.getResource().getName());
				}

				public void transferProgress(TransferEvent transferEvent, byte[] buffer, int length) {
				}

				public void transferCompleted(TransferEvent transferEvent) {
					logger.info(" - DONE");
				}
			};

			// always use temporary context when reindexing
			final File tmpFile = File.createTempFile(indexingContext.getId(), "-tmp", indexParentDir);
			final File tmpDir = new File(indexParentDir, tmpFile.getName() + ".dir");
			if (!tmpDir.mkdirs()) {
				throw new IOException("Cannot create temporary directory: " + tmpDir);
			}

			logger.debug("Temporary dir '{}' created.", tmpDir);

			try {

				final FSDirectory directory = FSDirectory.open(tmpDir);
				IndexUtils.copyDirectory(indexingContext.getIndexDirectory(), directory);

				logger.debug("Creating temporary indexing context.");

				try (@SuppressWarnings("deprecation")
				CloseableIndexingContext tmpContext = new CloseableIndexingContext(new DefaultIndexingContext(indexingContext.getId()
						+ "-tmp", indexingContext.getRepositoryId(), indexingContext.getRepository(), directory,
						indexingContext.getRepositoryUrl(), indexingContext.getIndexUpdateUrl(), indexingContext.getIndexCreators(), true),
						null)) {

					logger.debug("Remote maven store indexing started.");
					long start_time = System.nanoTime();

					ResourceFetcher resourceFetcher = new WagonHelper.WagonFetcher(httpWagon, listener, null, null);
					Date indexingContextCurrentTimestamp = tmpContext.getTimestamp();
					IndexUpdateRequest updateRequest = new IndexUpdateRequest(tmpContext, resourceFetcher);
					IndexUpdateResult updateResult = indexUpdater.fetchAndUpdateIndex(updateRequest);
					
					if(updateResult.getTimestamp() == null){
						logger.debug("Index is up to date"); //as it is in DefaultIndexUpdater.class
						updateResult.setTimestamp(indexingContextCurrentTimestamp);
					}
					
					else if (updateResult.isFullUpdate()) {
						logger.debug("Full update happened!");
					}
					
					else if (updateResult.getTimestamp().equals(indexingContextCurrentTimestamp)) {
						logger.debug("No update needed, index is up to date!");
					}
					
					else {
						logger.debug("Incremental update happened, change covered " + indexingContextCurrentTimestamp + " - "
								+ updateResult.getTimestamp() + " period.");
					}

					logger.info("Indexing remote repository finished succesfully!!!");
					long end_time = System.nanoTime();
					double difference = (end_time - start_time) / 1e6;
					logger.debug("Indexing remote repository took {} nanoseconds ", difference);

					tmpContext.updateTimestamp(true);
					
					//TODO: decide if use tempContext
					logger.debug("Replacing contexts from temporary to origin.");
					indexingContext.replace(tmpContext.getIndexDirectory());
					
				} catch (Throwable t) {
					logger.error("Error indexing remote Maven repository 1.", t);
				}
			} catch (IOException ex) {
				logger.error("Error indexing remote Maven repository 2.", ex);
				throw new IOException("Error scanning context " + indexingContext.getId() + ": " + ex, ex);
			} catch (Throwable t) {
				logger.error("Error indexing remote Maven repository 3.", t);
			} finally {
				try {
					if (tmpFile.exists()) {
						tmpFile.delete();
					}
				} finally {
					FileUtils.deleteDirectory(tmpDir); // TODO replace plexus utils														
				}
			}
		}
		
		logger.debug("Indexing remote maven store '{}' finished.", uri);
		return new CloseableIndexingContext(indexingContext, indexer);
	}
}
