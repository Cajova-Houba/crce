package cz.zcu.kiv.crce.rest.internal.rest.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import cz.zcu.kiv.crce.metadata.Capability;
import cz.zcu.kiv.crce.metadata.Resource;
import cz.zcu.kiv.crce.rest.internal.rest.PostProviderOfCapability;
import cz.zcu.kiv.crce.rest.internal.rest.convertor.ConvertorToBeans;
import cz.zcu.kiv.crce.rest.internal.rest.convertor.IncludeMetadata;
import cz.zcu.kiv.crce.rest.internal.rest.generated.ObjectFactory;
import cz.zcu.kiv.crce.rest.internal.rest.generated.Tattribute;
import cz.zcu.kiv.crce.rest.internal.rest.generated.Trepository;
import cz.zcu.kiv.crce.rest.internal.rest.generated.Trequirement;
import cz.zcu.kiv.crce.rest.internal.rest.structures.VersionDemand;

@Path("/provider-of-capability")
public class ProviderOfCapabilityResource extends ResourceParent implements PostProviderOfCapability {
	
	private static final Logger log = LoggerFactory.getLogger(PostProviderOfCapability.class);
	
	/**
     * Unmarshal requirement.
     * 
     * @param requiremnt XML representation of requirement
     * @return requirement
     * @throws WebApplicationException unmarshal of xml failed.
     */
	private Trequirement unmarshalRequirent(String requirement) throws WebApplicationException {

		try {
			ClassLoader cl = ObjectFactory.class.getClassLoader();
			JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class
					.getPackage().getName(), cl);

			Unmarshaller unmarshaller = jc.createUnmarshaller();

			InputStream requirementStream = new ByteArrayInputStream(
					requirement.getBytes(DEF_ENCODING));
			
			 DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			 dbf.setNamespaceAware(true);
			 DocumentBuilder db = dbf.newDocumentBuilder();
			 Document doc = db.parse(requirementStream);
			 Node requirementSubtree = doc.getFirstChild();
			 log.info("node: " + requirementSubtree.getLocalName());
			
			
			Object obj = unmarshaller.unmarshal(requirementSubtree, Trequirement.class);			
			

			JAXBElement<?> jxbE = (JAXBElement<?>) obj;

			Trequirement req = (Trequirement) jxbE.getValue();

			return req;
			
		} catch (UnsupportedEncodingException e) {
			log.warn("Request ({}) - Unsuported encoding {}", requestId, DEF_ENCODING);
			log.debug(e.getMessage(), e);			
			throw new WebApplicationException(500);
			
		} catch (JAXBException e) {
			log.info("Request ({}) - Post request XML unmarshal failed.", requestId);
			log.debug(e.getMessage(), e);	
			throw new WebApplicationException(400);
			
		} catch (ParserConfigurationException e) {
			log.warn("Request ({}) - ParserConfigurationException during unmarshal", requestId);
			log.debug(e.getMessage(),e);
			throw new WebApplicationException(500);
			
		} catch (IOException e) {
			log.warn("Request ({}) - IOException during unmarshal", requestId);
			log.debug(e.getMessage(),e);
			throw new WebApplicationException(500);
			
		} catch (SAXException e) {
			log.info("Request ({}) - XMLParser exception during unmarshal - {}", requestId, e.getMessage());
			log.debug(e.getMessage(),e);
			throw new WebApplicationException(400);
		}

	}
	
	/**
	 * Get name attribute of the requirement.
	 * @param requirement the requirement
	 * @return name attribute of the requirement
	 */
	private String getRequirementName(Trequirement requirement) {
		List<Object> dirAtrReq = requirement.getDirectiveOrAttributeOrRequirement();
		
		for(Object obj: dirAtrReq) {
			if(obj instanceof Tattribute) {
				Tattribute atr = (Tattribute) obj;
				if(atr.getName().equals("name")) {
					return atr.getValue();
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Return list of resources, that has capability, that matches the requirement.
	 * @param requirement the requirement
	 * @return list of resources, that has capability, that matches the requirement.
	 */
	private Resource[] matchingResources(Trequirement requirement) {
		String reqName = getRequirementName(requirement);
		if(reqName == null) {
			log.info("Request ({}) - No name found in the input requierment.", requestId);
			throw new WebApplicationException(400);
		}
		
		Resource[] allResources = findBundlesByFilter("");
		//log.info("All resources size " + allResources.length);
		
		List<Resource> matchingResources = new ArrayList<>();
	
		for(Resource res: allResources) {
			if(checkIfMatchRequirement(res, reqName, requirement)) {
				log.debug("Request ({}) - Matching resource found - {}", requestId , res.getId());
				matchingResources.add(res);
			}
		}
		
		return matchingResources.toArray(new Resource[matchingResources.size()]);
	}
	
	/**
	 * Prepare list of version demands from the requirement
	 * @param requirement the requirement
	 * @return list of version demands
	 */
	private List<VersionDemand> prepareVersionDemandsList(Trequirement requirement) {
		
		List<VersionDemand> versionDemandsList =  new ArrayList<>();
		
		List<Object> dirAtrReq = requirement.getDirectiveOrAttributeOrRequirement();		
		for(Object obj: dirAtrReq) {
			if(obj instanceof Tattribute) {
				Tattribute atr = (Tattribute) obj;
				if(atr.getName().equals("version")) {
					if(atr.getValue()!=null && atr.getType()!=null) {
					
						VersionDemand verDemand = new VersionDemand();
						verDemand.setVersion(new Version(atr.getValue()));
						verDemand.setOperation(atr.getType());
						versionDemandsList.add(verDemand);
					} else {
						log.warn("Request ({}) - Requirement version attribute has not set value or type and will be skipped", requestId);
					}
				}
			}
		}
		
		return versionDemandsList;
	}
	
	/**
	 * Check if the version demand match the capability version.
	 * @param verDemand the version demand
	 * @param capabilityVersion the capability version
	 * @return boolean, if the version demand match the capability version.
	 */
	private boolean checkVersionDemand(VersionDemand verDemand, Version capabilityVersion) {
		String operation = verDemand.getOperation();
		
		int compareVersions = capabilityVersion.compareTo(verDemand.getVersion());
		
		switch (operation) {
		case VersionDemand.EQUEAL:
			if(compareVersions == 0) return true;
			break;
		case VersionDemand.GREATER_THAN:
			if(compareVersions >= 0) return true;
			break;
		case VersionDemand.LESS_THAN:
			if(compareVersions <= 0) return true;
			break;
		case VersionDemand.GREATER_EQUEAL:
			if(compareVersions > 0) return true;
			break;
		case VersionDemand.LESS_EQUEAL:
			if(compareVersions < 0) return true;
			break;
		case VersionDemand.NOT_EQUEAL:
			if(compareVersions != 0) return true;
			break;
		default:
			return false;
		}
		
		return false;
	}
	
	/**
	 * Check if the capability matches the version demands of the requirement.
	 * Requirement can have more version demands and all must match.
	 * 
	 * @param capability the capability
	 * @param requirement the requirement
	 * @return boolean, if the capability matches the version demands of the requirement.
	 */
	private boolean checkVersionMatching(Capability capability, Trequirement requirement) {
		Version capabilityVersion = new Version(capability.getPropertyString("version"));
		List<VersionDemand> verDemands = prepareVersionDemandsList(requirement);
		//log.info("version demands size " + verDemands.size());
		
		for(VersionDemand verDemand : verDemands) {
			
			//if one of versionCriterium do not match, return false;
			if(!checkVersionDemand(verDemand, capabilityVersion)) {
				//log.info("Ver demand do not match - " + verDemand.getVersion().toString() + " " + verDemand.getOperation() );
				return false;
			}
		}
		
		//log.info("All verison demands matching");
		return true;
	}
	
	/**
	 * Check if one of the resource's capabilities matches requirements.
	 * @param resource the resource
	 * @param reqName the name of requirement
	 * @param requirement the requirement
	 * @return boolean, if one of the resource's capabilities matches requirements
	 */
	private boolean checkIfMatchRequirement(Resource resource, String reqName, Trequirement requirement) {
		
		Capability[] capabilities = resource.getCapabilities("package");
		for(Capability cap: capabilities) {
			//check if name matches
			if(cap.getPropertyString("package").equals(reqName)) {
				//check if version demands matches
				if(checkVersionMatching(cap, requirement)) {
					//one of capabilities matches
					return true;
				}
			}			
			
		}
		
		return false;
		
	}
	
	/**
	 * Find all resources, that have a capability, that matches the requirement
	 * @param requirement the requirement
	 * @return repository of resources, that have a capability, that matches the requirement
	 */
	private Trepository findProviders(Trequirement requirement) {
		
		Resource[] matchingResources = matchingResources(requirement);
		
		//log.info("Matching resources size " + matchingResources.length);
		
		ConvertorToBeans conv = new ConvertorToBeans();
		
		IncludeMetadata include = new IncludeMetadata();
		include.includeAll();
		
		Trepository repo = conv.convertRepository(matchingResources, include, null);
		
		return repo;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@POST
	@Consumes(MediaType.APPLICATION_XML)
	@Produces({MediaType.APPLICATION_XML })
	public Response providerOfCapability(String requirement) {
		requestId++;
		log.debug("Request ({}) - Provider of capability request was received.", requestId);
		log.debug("Request ({}) - Requirement received: {}", requestId, requirement);
		try {
			Trequirement req = unmarshalRequirent(requirement);
			log.debug("Request ({}) - Requirement was unmashaled.", requestId);
			
			Trepository repositoryBean = findProviders(req);

			Response response = Response.ok(createXML(repositoryBean)).build();
			log.debug("Request ({}) - Response was successfully created.", requestId);
			return response;
		} catch (WebApplicationException e) {
			
			return e.getResponse();
		}
	}

}
