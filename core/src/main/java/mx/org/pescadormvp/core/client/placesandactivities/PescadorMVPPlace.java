/*******************************************************************************
 * Copyright 2013 Instituto de Investigaciones Dr. José María Luis Mora
 * See LICENSE.txt for redistribution conditions.
 * 
 * D.R. 2013 Instituto de Investigaciones Dr. José María Luis Mora
 * Véase LICENSE.txt para los términos bajo los cuales se permite
 * la redistribución.
 ******************************************************************************/
package mx.org.pescadormvp.core.client.placesandactivities;

import java.util.Map;

import com.google.gwt.place.shared.Place;

import mx.org.pescadormvp.core.client.session.StatePointer;
import mx.org.pescadormvp.core.shared.PescadorMVPLocale;

public interface PescadorMVPPlace extends StatePointer {

	String getMainToken();

	String[] getPropertyKeys();

	void setProperties(Map<String, String> properties);

	Map<String, String> getProperties();

	/**
	 * Set the text used to produce a user-friendly reference to this place
	 * to display somewhere in the UI. May not be set when not needed.
	 * 
	 */
	void setPresentationText(String presentationText);

	/**
	 * Returns presentation text. Warning: may return null.
	 * 
	 * @return presentation text
	 */
	String getPresentationText();

	/**
	 * Full history token to use in URLs in UI. Convenience field that may or may not
	 * be set in any given case.
	 * 
	 * @return full history token
	 */
	String getHistoryToken();

	void setHistoryToken(String historyToken);

	/**
	 * If set to non-null, then activating this place results in reloading
	 * the whole app in the new locale. Null is used to indicate the same
	 * locale as is currently active, whatever that may be. 
	 */
	void setNewLocale(PescadorMVPLocale newLocale);
	
	PescadorMVPLocale getNewLocale();

	String getURL();

	void setURL(String uRL);

	boolean requiresReload();

	void setRequiresReload(boolean requiresReload);
	
	public Place asGWTPlace();
}
