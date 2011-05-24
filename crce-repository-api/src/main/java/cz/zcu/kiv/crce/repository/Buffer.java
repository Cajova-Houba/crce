package cz.zcu.kiv.crce.repository;

import cz.zcu.kiv.crce.metadata.Repository;
import cz.zcu.kiv.crce.metadata.Resource;
import cz.zcu.kiv.crce.repository.plugins.Executable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * This interface defines a temporary store for uploading artifacts before putting
 * them to the permanent store.
 * @author Jiri Kucera (kalwi@students.zcu.cz, jiri.kucera@kalwi.eu)
 */
public interface Buffer {

    /**
     * Puts a new resource into resource buffer.
     * 
     * @param name A name of uploaded file.
     * @param resource Uploaded resource.
     * @return 
     * @throws IOException
     * @throws RevokedArtifactException  
     */
    public Resource put(String name, InputStream resource) throws IOException, RevokedArtifactException;
    
    /**
     * Removes the resource from this buffer and returns <code>true</code>, if
     * the buffer contained the given resource before removing, <code>false</code>
     * otherwise.
     * 
     * @param resource Resource to be removed from this buffer.
     * @return <code>true</code>, if the buffer contained the resource before
     * removing.
     * @throws IOException  
     */
    public boolean remove(Resource resource) throws IOException;

    /**
     * Returns resources stored in buffer.
     * @return array of resources.
     */
    public Repository getRepository();
    
    /**
     * TODO analyze requirements for this method:
     * - add an executable plugin
     * @param resources 
     * @param executable 
     * @param properties 
     */
    public void execute(List<Resource> resources, Executable executable, Properties properties);

    /**
     * Commits uploaded resources to the configured store and remove them from
     * the buffer, if <code>move</code> is <code>true</code>. Resources commited
     * to the store are returned in <code>List</code>.
     * 
     * @param move if <code>true</code>, commited resources are removed from the
     * buffer.
     * @return list of commited resources.
     * @throws IOException  
     */
    public List<Resource> commit(boolean move) throws IOException;
}
