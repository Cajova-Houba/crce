package cz.zcu.kiv.crce.rest.internal.rest.convertor;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import cz.zcu.kiv.crce.rest.internal.rest.generated.Tattribute;
import cz.zcu.kiv.crce.rest.internal.rest.generated.Tcapability;
import cz.zcu.kiv.crce.rest.internal.rest.generated.Tresource;

/**
 * 
 * Test {@link ConvertorToBeans}.
 * @author Jan Reznicek
 *
 */
public class ConvertorToBeansTest {
	
	private static final String TEST_ID_1 = "testid-1.0.0";
	private static final String EXPECTED_DELETED_STATUS = "deleted";
	private static final String CRCE_ID_CAP = "crce.identity";
	private static final String CRCE_STATUS = "crce.status";
	
	
	public Tcapability getCapability(List<Tcapability> caps, String namespace) {
		for(Tcapability cap: caps) {
			if(namespace.equals(cap.getNamespace())) {
				return cap;
			}
		}
		
		return null;
	}
	
	public Tattribute getAttribute(List<Object> list, String attrName) {
		for(Object obj: list) {
			if(obj instanceof Tattribute) {
				Tattribute atr = (Tattribute) obj;
				
				if(attrName.equals(atr.getName())) {
					return atr;
				}
				
			}
			
		}
		
		return null;
	}
	
	/**
	 * Test {@link ConvertorToBeans#getDeletedResource(String)}.
	 */
	@Test
	public void testGetDeletedResource() {
		
		ConvertorToBeans conv = new ConvertorToBeans();
		
		Tresource res = conv.getDeletedResource(TEST_ID_1);
		
		assertTrue("Wrong id", TEST_ID_1.equals(res.getId()));
		
		Tcapability crceIdentityCap = getCapability(res.getCapability(), CRCE_ID_CAP);
		
		assertNotNull("Capabily " + CRCE_ID_CAP + " is missing.", crceIdentityCap);
		
		Tattribute crceStatusAtr = getAttribute(crceIdentityCap.getDirectiveOrAttributeOrCapability(), CRCE_STATUS);
		
		assertTrue("Wrong status (" + crceStatusAtr.getValue() + "), expected status is: " + EXPECTED_DELETED_STATUS, EXPECTED_DELETED_STATUS.equals(crceStatusAtr.getValue()));
		
	}

}