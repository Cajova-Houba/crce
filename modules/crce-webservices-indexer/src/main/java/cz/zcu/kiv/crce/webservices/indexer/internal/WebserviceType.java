package cz.zcu.kiv.crce.webservices.indexer.internal;

import cz.zcu.kiv.crce.metadata.Resource;

/**
 * This interface serves as a basic API for processing of IDLs of different types of web services. Any class implementing this interface should represent one
 * particular type of web service standard (e.g. SOAP, JSON-WSP, etc...).
 *
 * @author David Pejrimovsky (maxidejf@gmail.com)
 */
public interface WebserviceType {
    
    /**
     * Determines whether given IDL satisfies all conditions in order to be valid representation of a web service. For example if SOAP was implemented as web
     * service type of this interface. This method would check whether given IDL is a valid WSDL document.
     *
     * @param idl Textual representation of webservice IDL.
     * @return Returns <code>true</code> if given IDL was recognized as the type of implemented web service standard. Returns <code>false</code> otherwise.
     */
    boolean recognizeIDL(String idl);
    
    /**
     * Processes given IDL into CRCE {@link cz.zcu.kiv.crce.metadata.Resource} representation of a webservices capabilities represented by the given
     * IDL.
     *
     * @param idl Textual representation of webservice IDL.
     * @param resource  CRCE {@link cz.zcu.kiv.crce.metadata.Resource} to which the meta-data will be stored to.
     * @return Returns number of successfully parsed representations of webservice descriptions in given IDL. In case of any error -1 is returned instead.
     */
    int parseIDL(String idl, Resource resource);
    
    /**
     * Generates IDL from CRCE {@link cz.zcu.kiv.crce.metadata.Resource} representation of a webservices capabilities.
     *
     * <p>Please note, that due to the webservice descriptions being stored at CRCE repository in a unified way, some information that is specific for a
     * particular webservice schema might not be (and often is not) stored at the moment. As a result webservice schema document that is being generated by this
     * method often does not result in a valid document according to the corresponding webservice schema specification. All the information in a generated
     * document are correctly formatted according to relevant webservice schema specs., but that does not ensure it's validity (e.g. document might contain
     * references to missing parts).
     * 
     * @param resource  CRCE {@link cz.zcu.kiv.crce.metadata.Resource} from which the meta-data will be read.
     * @return Textual representation of webservice IDL.
     */
    String generateIDL(Resource resource);
    
    /**
     * Returns name of specific IDL of which class that implements this interface deals with. E.g. "json-wsp", "soap", "wadl", etc...
     *
     * @return Name of specific IDL of which class that implements this interface deals with.
     */
    String getSpecificIdlCategory();
    
}
