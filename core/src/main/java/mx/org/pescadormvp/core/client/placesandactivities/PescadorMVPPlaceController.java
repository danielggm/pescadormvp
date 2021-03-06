/*******************************************************************************
 * Copyright 2013 Instituto de Investigaciones Dr. José María Luis Mora
 * See LICENSE.txt for redistribution conditions.
 * 
 * D.R. 2013 Instituto de Investigaciones Dr. José María Luis Mora
 * Véase LICENSE.txt para los términos bajo los cuales se permite
 * la redistribución.
 ******************************************************************************/
package mx.org.pescadormvp.core.client.placesandactivities;

import com.google.gwt.place.shared.PlaceController;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

/**
 * Internal Pescador MVP use. Guice-friendly {@link PlaceController}.
 *
 */
public class PescadorMVPPlaceController extends PlaceController {

	@Inject
	public PescadorMVPPlaceController(EventBus eventBus) {
		super(eventBus);
	}
}
