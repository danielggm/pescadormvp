/*******************************************************************************
 * Copyright 2013 Instituto de Investigaciones Dr. José María Luis Mora
 * See LICENSE.txt for redistribution conditions.
 * 
 * D.R. 2013 Instituto de Investigaciones Dr. José María Luis Mora
 * Véase LICENSE.txt para los términos bajo los cuales se permite
 * la redistribución.
 ******************************************************************************/
package mx.org.pescadormvp.core.client.placesandactivities;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import mx.org.pescadormvp.core.client.components.Component;
import mx.org.pescadormvp.core.client.components.ComponentRegistry;
import mx.org.pescadormvp.core.client.logging.PescadorMVPLogger;
import mx.org.pescadormvp.core.shared.PescadorMVPLocale;

import com.google.gwt.http.client.UrlBuilder;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.user.client.Window.Location;
import com.google.inject.Inject;

/**
 * A {@link PlaceHistoryMapper} for {@link PescadorMVPPlace}s. Implemented as a
 * {@link Component}.
 */
public class PescadorMVPPlaceMapperImpl implements PescadorMVPPlaceMapper {

	private static String MAIN_TOKEN_SEPARATOR = ";";
	private static String KV_PAIR_SEPARATOR = "/";
	private static String KV_SEPARATOR = "=";
	
	private ComponentRegistry componentRegistry;
	private RawDefaultPlaceProvider defaultPlaceProvider;
	private PescadorMVPLogger logger;
	
	@Inject
	protected PescadorMVPPlaceMapperImpl(
			ComponentRegistry componentRegistry,
			RawDefaultPlaceProvider defaultPlaceProvider,
			PescadorMVPLogger logger) { 
		
		this.componentRegistry = componentRegistry;
		this.defaultPlaceProvider = defaultPlaceProvider;
		this.logger = logger;
	}
	
	@Override
	public Place getPlace(String fullToken) {
		String[] tokenParts = fullToken.split(MAIN_TOKEN_SEPARATOR);
		
		PAVComponent<?, ?> pavComponent =
				componentRegistry.getPAVComponent(tokenParts[0]);

		// we have an invalid main token if pavComponent is null
		if (pavComponent == null) {
			logger.log(Level.INFO, "Invalid main token in " + fullToken);
			return defaultPlaceProvider.getRawDefaultPlace().asGWTPlace();
		}
			
		PescadorMVPPlace place = pavComponent.getRawPlace();
		if (tokenParts.length > 1) {
			Map<String, String> properties = getKVMap(tokenParts[1]);
			
			// We have invalid key-values if properties is null
			if (properties == null) {
				logger.log(Level.INFO, "Invalid properties in " + fullToken);
				return defaultPlaceProvider.getRawDefaultPlace().asGWTPlace();
			}
				
			place.setProperties(properties);
		}
		
		return place.asGWTPlace();
	}

	@Override
	public <P extends PescadorMVPPlace> P copyPlaceInto(
			P originalPlace,
			P newPlace) {
		
		newPlace.setProperties(originalPlace.getProperties());
		newPlace.setNewLocale(originalPlace.getNewLocale());
		newPlace.setPresentationText(originalPlace.getPresentationText());
		
		return newPlace;
	}
	
	@Override
	public String getToken(Place place) {
		if (!(place instanceof PescadorMVPPlace))
			throw new IllegalArgumentException();
		
		return getToken((PescadorMVPPlace) place);
	}
	
	@Override
	public String getToken(PescadorMVPPlace place) {

		Map<String, String> properties = place.getProperties();

		if ((properties == null) || (properties.keySet().size() == 0))
			return place.getMainToken();
		
		String propertiesString = makeKVString(properties,
				place.getPropertyKeys());
		
		return place.getMainToken() + 
				(propertiesString == null ?
						"" : MAIN_TOKEN_SEPARATOR + propertiesString);
	}
	
	@Override
	public void setupURLInfo(PescadorMVPPlace place) {

		String historyTokenFromPlaceObj = place.getHistoryToken();
		String historyToken = 
				historyTokenFromPlaceObj == null ?
				getToken(place) :
				historyTokenFromPlaceObj;
		
		place.setHistoryToken(historyToken);
				
		PescadorMVPLocale newLocale = place.getNewLocale();
		if (newLocale == null) {
			place.setURL("#" + historyToken);
			
			// instructs Session not to reload the whole app when going here
			place.setRequiresReload(false);
			
		} else {
			// TODO check that the locale is not the same as the current one (?)
			String queryParam = LocaleInfo.getLocaleQueryParam();
		    UrlBuilder builder = Location.createUrlBuilder();
		    builder.setParameter(queryParam, newLocale.getLocaleName());
		    builder.setHash(historyToken);
		    place.setURL(builder.buildString());
		    
		    // in this case, do reload the whole app when going here
		    place.setRequiresReload(true);
		    
		}
	}
	
	private Map<String, String> getKVMap(String token) { 
		String[] pairs = token.split(KV_PAIR_SEPARATOR);
		Map<String, String> propValueMap = new HashMap<String, String>();
		
		for (String kvPair : pairs) {
			String[] kv = kvPair.split(KV_SEPARATOR);
			
			if (kv.length != 2) {
				// This tells the calling method that the string 
				// couldn't be interpreted
				return null;
			}
			
			propValueMap.put(kv[0], kv[1]);
		}
		
		return propValueMap;
	}
	
	private String makeKVString(Map<String, String> map, String[] orderedKeys) {
		
		int size = map.keySet().size();
		if (size == 0)
			return null;
		
		String[] pairs = new String[orderedKeys.length];
		int ctr = 0;
		for (String key : orderedKeys) {
			String value = map.get(key);
			if (value != null)
				pairs[ctr++] = key + KV_SEPARATOR + map.get(key);
		}
		
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < size; i++) {
			String pair = pairs[i];
			if (pair != null) {
				if (sb.length() > 0)
					sb.append(KV_PAIR_SEPARATOR);
				sb.append(pair);
			}
		}			
		
		return sb.length() > 0 ? sb.toString() : null;
	}

	@Override
	public void finalizeSetup() {
		// nothing to do
	}
	
	@Override
	public Class<PescadorMVPPlaceMapper> publicInterface() {
		return PescadorMVPPlaceMapper.class;
	}
	
	@Override
	public PescadorMVPPlace defaultPlace() {
		
		PescadorMVPPlace place = defaultPlaceProvider.getRawDefaultPlace();
		setupURLInfo(place);
		
		// For some reason, it seems necessary to set this to an empty string
		// rather than the real history token. This allows links to the default
		// place in the UI (generated from this very object) to have no
		// history token at all. If they have their normal history token,
		// then clicking on them adds an extra, unwanted entry in the 
		// browser history.
		place.setHistoryToken("");
		
		return place;
	}
}
