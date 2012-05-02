package cz.zcu.kiv.crce.plugin.internal;

import java.util.ArrayList;
import java.util.Dictionary;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import cz.zcu.kiv.crce.plugin.MetadataIndexingResultService;


/**
 * Implementation of MetadataIndexingResultService interface.
 */
public class MetadataIndexingResultServiceImpl implements MetadataIndexingResultService, ManagedService {

	/** This instance holds information about result of indexing process. */
	private ArrayList<String> messages;
	

	/** Constructor of MetadataIndexingResultServiceImpl class. */
	public MetadataIndexingResultServiceImpl() {
		messages = new ArrayList<String>();
	}

	/**
	 * Getting information.
	 *
	 * @return Informations about indexing result for user.
	 */
	@Override
	public final String[] getMessages() {
		if (!isEmpty()) {
			String[] stringArray = new String[messages.size()];
			int index = 0;
			for (String indexerMessage : messages) {
				stringArray[index++] = indexerMessage;
			}
			return stringArray;
		} else
			return null;
	}

	/**
	 * Setting information.
	 *
	 * @param nextMessage - Information about indexing result.
	 */
	@Override
	public final void addMessage(final String nextMessage) {
		messages.add(nextMessage);
	}

	@Override
	public final void resetMessages() {
		messages.clear();
	}
	
	@Override
	public final boolean isEmpty() {
		return messages.isEmpty();
	}

	@Override
	public final void updated(final Dictionary properties) throws ConfigurationException {
		return;
	}
}
