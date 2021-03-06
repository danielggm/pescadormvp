/*******************************************************************************
 * Copyright 2013 Instituto de Investigaciones Dr. José María Luis Mora See
 * LICENSE.txt for redistribution conditions. D.R. 2013 Instituto de
 * Investigaciones Dr. José María Luis Mora Véase LICENSE.txt para los términos
 * bajo los cuales se permite la redistribución.
 ******************************************************************************/
package mx.org.pescadormvp.core.client.components;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import mx.org.pescadormvp.core.client.PescadorMVPGinjector;
import mx.org.pescadormvp.core.client.data.DataManager;
import mx.org.pescadormvp.core.client.logging.PescadorMVPLogger;
import mx.org.pescadormvp.core.client.placesandactivities.ActivityManagersFactory;
import mx.org.pescadormvp.core.client.placesandactivities.ActivityMappersFactory;
import mx.org.pescadormvp.core.client.placesandactivities.PescadorMVPActivityMapper;
import mx.org.pescadormvp.core.client.placesandactivities.PAVComponent;
import mx.org.pescadormvp.core.client.placesandactivities.PescadorMVPPlace;
import mx.org.pescadormvp.core.client.placesandactivities.PescadorMVPPlaceActivity;
import mx.org.pescadormvp.core.client.placesandactivities.PescadorMVPPlaceMapper;
import mx.org.pescadormvp.core.client.regionsandcontainers.ForRegionTag;
import mx.org.pescadormvp.core.client.regionsandcontainers.HasRegions;
import mx.org.pescadormvp.core.client.regionsandcontainers.RootHasFixedSetOfRegions;
import mx.org.pescadormvp.core.client.regionsandcontainers.RootRegionManager;
import mx.org.pescadormvp.core.client.regionsandcontainers.NullPanelTools.NullActivity;
import mx.org.pescadormvp.core.client.session.Session;

import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

/**
 * Abstract class for global application setup. All Pescador MVP applications
 * should create a subclass of this class containing things related to global
 * setup. For more information, see {@link mx.org.pescadormvp.core ...core} and
 * {@link mx.org.pescadormvp.examples.jsonp.client ...examples.jsonp.client}.
 */
@SuppressWarnings({ "javadoc" })
public abstract class GlobalSetup implements RootRegionManager {

	// static stuff used for injecting scripts before startup
	private static String[] scriptsToLoad;
	private static Set<String> scriptsFailedToLoad = new HashSet<String>();
	private static Set<String> scriptsLoaded = new HashSet<String>();
	private static boolean loadScriptsInOrder;
	private static int scriptNowLoading = -1;
	private static LoadingPleaseWait loadingPleaseWait;

	private static PescadorMVPGinjectorHolder pendingGinjectorHolder;
	private static List<PendingLog> pendingLogs =
			new ArrayList<GlobalSetup.PendingLog>();

	private ComponentRegistry componentRegistry;
	private List<Component<?>[]> componentsToAdd = new ArrayList<Component<?>[]>();

	private RootHasFixedSetOfRegions regionsWidget;
	private Set<Class<? extends ForRegionTag>> regions;

	private PlaceController placeController;
	private EventBus eventBus;
	private PlaceHistoryHandler historyHandler;
	private ActivityMappersFactory activityMappersFactory;
	private ActivityManagersFactory activityManagersFactory;

	private PescadorMVPPlaceMapper placeMapper;
	private PescadorMVPLogger logger;
	private NullActivity nullActivity;

	/**
	 * A mini interface that allows us to pass around a specific
	 * {@link PescadorMVPGinjector} while delaying its actual instantiation.
	 * Necessary because {@link GWT#create(Class) GWT.create()} only takes class
	 * literals.
	 */
	public interface PescadorMVPGinjectorHolder {
		public PescadorMVPGinjector getPescadorMVPGinjector();
	}

	/**
	 * Start up the framework. This method is static so we can use it before DI
	 * boots up. That way the framework to take care of booting up DI.
	 * 
	 * @param ginjector
	 *            A {@link PescadorMVPGinjector} to use to boot up DI.
	 */
	public static void startUp(PescadorMVPGinjector ginjector) {
		// The Ginjector provides the active GlobalSetup instance, which
		// we use to start the app. This will initialize the UI
		// and go to the default place.
		ginjector.getGlobalSetup().start();
	}

	/**
	 * <p>
	 * Load JS scripts and then start up the framework. Scripts are
	 * injected in the top window.
	 * </p>
	 * <p>
	 * This method is static so it can be accessed before DI boots up. That way the
	 * framework takes care of starting DI, and we don't have to worry about DI
	 * bringing in Java classes that rely on external JS before it's loaded.
	 * </p>
	 * 
	 * @param ginjectorHolder
	 *            A holder for the {@link PescadorMVPGinjector} to use to boot
	 *            up DI.
	 * @param loadingPleaseWait
	 *            An object that does something when scriptloading starts (like
	 *            show a "please wait" message) and when it finishes (like
	 *            remove the message)
	 * @param loadScriptsInOrder
	 *            If more than one script is requested, make sure they are
	 *            loaded sequentially. (Some libraries need this.)
	 * @param scriptsToLoad
	 *            The URLs of scripts to load.
	 */
	public static void loadJSthenStartUp(
			PescadorMVPGinjectorHolder ginjectorHolder,
			LoadingPleaseWait loadingPleaseWait,
			boolean loadScriptsInOrder,
			String... scriptsToLoad) {

		GlobalSetup.pendingGinjectorHolder = ginjectorHolder;
		GlobalSetup.loadingPleaseWait = loadingPleaseWait;
		GlobalSetup.loadScriptsInOrder = loadScriptsInOrder;
		GlobalSetup.scriptsToLoad = scriptsToLoad;

		// if a loadingPleaseWait has been sent, start it up
		if (loadingPleaseWait != null)
			loadingPleaseWait.start();

		if (loadScriptsInOrder) {
			scriptNowLoading = 0;
			launchScriptInjector(scriptsToLoad[0]);

		} else {
			for (String url : scriptsToLoad)
				launchScriptInjector(url);
		}
	}

	private static void scriptInjectReturned() {
		if (allScriptsLoaded()) {
			// if a loadingPleaseWait has been sent, finish it
			if (loadingPleaseWait != null)
				loadingPleaseWait.finish();

			startUp(pendingGinjectorHolder.getPescadorMVPGinjector());

		} else if (loadScriptsInOrder) {
			scriptNowLoading++;
			launchScriptInjector(scriptsToLoad[scriptNowLoading]);
		}
	}

	private static boolean allScriptsLoaded() {
		return scriptsToLoad.length == scriptsLoaded.size()
				+ scriptsFailedToLoad.size();
	}

	private static void launchScriptInjector(final String url) {

		ScriptInjector
				.fromUrl(url)
				.setWindow(ScriptInjector.TOP_WINDOW)
				.setCallback(
						new Callback<Void, Exception>() {

							public void onFailure(Exception reason) {
								pendingLogs.add(new PendingLog(Level.SEVERE,
										"Failed to load JS " + url));

								scriptsFailedToLoad.add(url);
								scriptInjectReturned();
							}

							public void onSuccess(Void result) {
								pendingLogs.add(new PendingLog(Level.INFO,
										"Successfully loaded JS " + url));

								scriptsLoaded.add(url);
								scriptInjectReturned();
							}

						}).inject();
	}

	// TODO look into using multibindings and/or mapbindings
	/**
	 * Add components to the framework. Normally this method should be called
	 * from the constructor of a subclass of this class. It's expected that it
	 * will not be called after that.
	 * 
	 * @param components
	 *            The components to add.
	 */
	public void addComponents(
			Component<?>... components) {
		// Only really add components if the componentRegistry has been injected
		if (componentRegistry != null)
			reallyAddComponents(components);
		else
			componentsToAdd.add(components);
	}

	private void reallyAddComponents(Component<?>... components) {
		componentRegistry.addComponents(components);
	}

	/**
	 * Internal Pescador MVP use. Here we get some basic stuff via method
	 * injection, to keep subclasses' constructors simpler.
	 */
	@Inject
	public void setBasicComponents(
			ComponentRegistry componentRegistry,
			RootHasFixedSetOfRegions regionsWidget,
			PlaceController placeController,
			EventBus eventBus,
			PlaceHistoryHandler historyHandler,
			ActivityMappersFactory activityMappersFactory,
			ActivityManagersFactory activityManagersFactory,
			NullActivity nullActivity,

			DataManager dataManager,
			PescadorMVPPlaceMapper placeMapper,
			Session session,
			PescadorMVPLogger logger) {

		this.componentRegistry = componentRegistry;
		this.placeController = placeController;
		this.eventBus = eventBus;
		this.historyHandler = historyHandler;
		this.nullActivity = nullActivity;
		this.activityMappersFactory = activityMappersFactory;
		this.activityManagersFactory = activityManagersFactory;

		// set regions widget
		setRootRegionsWidget(regionsWidget);

		// Tell the component registry what the regions are, so it can check
		// that components handle regions that are actually available
		componentRegistry.setRegions(regions);

		reallyAddComponents(new Component<?>[] {
				dataManager,
				placeMapper,
				session,
				logger });

		for (Component<?>[] componentsArray : componentsToAdd)
			reallyAddComponents(componentsArray);

		this.placeMapper = placeMapper;
		this.logger = logger;
	}

	@Override
	public Set<Class<? extends ForRegionTag>> getRegions() {
		return regions;
	}

	@Override
	public RootHasFixedSetOfRegions getRootRegionsWidget() {
		return regionsWidget;
	}

	@Override
	public void setRootRegionsWidget(RootHasFixedSetOfRegions regionsWidget) {
		this.regionsWidget = regionsWidget;

		// a reference, not a copy
		regions = regionsWidget.getRegions();
	}

	@Override
	public HasRegions getRegionsWidget() {
		return regionsWidget;
	}

	@Override
	public void setRegionsWidget(HasRegions regionsWidget) {
		if (!(regionsWidget instanceof RootHasFixedSetOfRegions))
			throw new IllegalArgumentException("RootHasRegions widget required");

		setRootRegionsWidget((RootHasFixedSetOfRegions) regionsWidget);
	}

	/**
	 * <p>
	 * Internal Pescador MVP use (unless, for some reason, you don't use one of
	 * the static startup methods, {@link #startUp startUp()} or
	 * {@link #loadJSthenStartUp loadJSthenStartUp()}). Start up the framework:
	 * attach the layout widget to the viewport, create activity managers and
	 * attach them to the region they're concerned with, start history handling,
	 * and go to the default place (if no place is specified in the fragment
	 * identifier in the URL).
	 * </p>
	 */
	public void start() {

		// it is assumed that all constructor and method injection for
		// this class and components will have taken place by the time we get
		// here; so run through all components and call finalizeSetup,
		// which should be called after all components are loaded
		componentRegistry.callFinalizeSetup();

		for (PendingLog pendingLog : pendingLogs)
			logger.log(pendingLog.getLevel(), pendingLog.getText());

		logger.log(Level.INFO, "Components loaded, starting up framework");

		// Check if any requested scripts failed to load
		if (scriptsFailedToLoad.size() > 0) {

			for (String failedSriptURL : scriptsFailedToLoad)
				logger.log(Level.WARNING, "Couldn't inject script "
						+ failedSriptURL);
		}

		// create activity mappers, and
		// create and set display widgets for activity managers
		for (Class<? extends ForRegionTag> region : regions) {
			PescadorMVPActivityMapper mpr =
					activityMappersFactory.create(region);

			ActivityManager mgr =
					activityManagersFactory.create(mpr);

			mgr.setDisplay(regionsWidget.getContainer(region));
		}

		regionsWidget.attach();

		historyHandler.register(
				placeController,
				eventBus,
				placeMapper.defaultPlace().asGWTPlace());

		historyHandler.handleCurrentHistory();
	}

	private <P extends PescadorMVPPlace> PAVComponent<?, P>
			getCastPAVComponent(P place) {

		PAVComponent<?, ?> pavComponent =
				componentRegistry.getPAVComponent(place.getMainToken());

		// GWT reflection doesn't provide for finding implemented interfaces
		@SuppressWarnings("unchecked")
		PAVComponent<?, P> castPAVComponent =
				(PAVComponent<?, P>) pavComponent;

		return castPAVComponent;
	}

	/**
	 * Internal Pescador MVP use. Get a new activity for the specified region
	 * and place.
	 */
	public <P extends PescadorMVPPlace,
			PS extends P> PescadorMVPPlaceActivity<?, ?, ?>
			getActivityForRegionAndPlace(
					Class<? extends ForRegionTag> region,
					P place) {

		PAVComponent<?, P> pavComponent =
				getCastPAVComponent(place);

		PescadorMVPPlaceActivity<?, ?, ?> activity =
				pavComponent.getActivity(region, place);

		if (activity != null)
			return activity;
		else
			return nullActivity;
	}

	private static class PendingLog {

		private final Level level;
		private final String message;

		PendingLog(Level level, String message) {
			this.level = level;
			this.message = message;
		}

		Level getLevel() {
			return level;
		}

		String getText() {
			return message;
		}
	}

	/**
	 * Implement this interface on a class that does something when JS
	 * scriptloading starts (like show a "please wait" message) and when it
	 * finishes (like remove the message). Then pass an instance to
	 * {@link GlobalSetup#loadJSthenStartUp loadJSthenStartUp(...)}. See, for
	 * example,
	 * {@link mx.org.pescadormvp.examples.jsonp.client.InitialLoadingTimer}.
	 * 
	 * @author Andrew Green
	 */
	public interface LoadingPleaseWait {
		/**
		 * If passed to {@link GlobalSetup#loadJSthenStartUp
		 * loadJSthenStartUp(...)}, this method will be called before JS loading
		 * starts.
		 */
		public void start();

		/**
		 * If passed to {@link GlobalSetup#loadJSthenStartUp
		 * loadJSthenStartUp(...)}, this method will be called after JS loading
		 * finishes.
		 */
		public void finish();
	}
}
