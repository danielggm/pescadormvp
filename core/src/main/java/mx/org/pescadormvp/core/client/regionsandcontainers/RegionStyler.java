/*******************************************************************************
 * Copyright 2013 Instituto de Investigaciones Dr. José María Luis Mora
 * See LICENSE.txt for redistribution conditions.
 * 
 * D.R. 2013 Instituto de Investigaciones Dr. José María Luis Mora
 * Véase LICENSE.txt para los términos bajo los cuales se permite
 * la redistribución.
 ******************************************************************************/
package mx.org.pescadormvp.core.client.regionsandcontainers;

import java.util.List;

import com.google.gwt.user.client.ui.IsWidget;

/**
 * A class used for complex styling of regions.
 */
public interface RegionStyler {

	List<IsWidget> getAbsPositionedWidgets();
	
	void setRegionDimensions(Class<? extends ForRegionTag> region,
			int x, int y, int width, int height);
}
