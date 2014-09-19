package org.arachnidium.core;

import java.net.URL;

import org.arachnidium.core.bean.MainBeanConfiguration;
import org.arachnidium.core.components.ComponentFactory;
import org.arachnidium.core.components.WebdriverComponent;
import org.arachnidium.core.interfaces.IDestroyable;
import org.arachnidium.core.settings.CapabilitySettings;
import org.arachnidium.core.settings.WebDriverSettings;
import org.arachnidium.core.settings.supported.ESupportedDrivers;
import org.arachnidium.util.configuration.Configuration;
import org.arachnidium.util.configuration.interfaces.IConfigurable;
import org.arachnidium.util.configuration.interfaces.IConfigurationWrapper;
import org.arachnidium.util.logging.Log;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.internal.WrapsDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * This class creates an instance of required {@link WebDriver} implementor,
 * wraps it and creates related components ({@link WebdriverComponent})
 *
 */
public class WebDriverEncapsulation implements IDestroyable, IConfigurable,
		WrapsDriver, IConfigurationWrapper {

	private static void prelaunch(ESupportedDrivers supporteddriver,
			Configuration config, Capabilities capabilities) {
		supporteddriver.launchRemoteServerLocallyIfWasDefined();
		supporteddriver.setSystemProperty(config, capabilities);
	}

	// get tests started with FireFoxDriver by default.
	private static ESupportedDrivers defaultSupportedDriver = ESupportedDrivers.FIREFOX;
	private RemoteWebDriver enclosedDriver;

	private Configuration configuration = Configuration.byDefault;
	final AbstractApplicationContext context = new AnnotationConfigApplicationContext(
			MainBeanConfiguration.class);
	private final DestroyableObjects destroyableObjects = new DestroyableObjects();

	/**
	 * Creates and wraps an instance of {@link RemoteWebDriver} by the given
	 * {@link Configuration}
	 * 
	 * @param {@link Configuration}
	 */
	public WebDriverEncapsulation(Configuration configuration) {
		this.configuration = configuration;
		ESupportedDrivers supportedDriver = this.configuration.getSection(
				WebDriverSettings.class).getSupoortedWebDriver();
		if (supportedDriver == null)
			supportedDriver = defaultSupportedDriver;

		Capabilities capabilities = this.configuration
				.getSection(CapabilitySettings.class);
		if (capabilities == null)
			capabilities = supportedDriver.getDefaultCapabilities();

		if (capabilities.asMap().size() == 0)
			capabilities = supportedDriver.getDefaultCapabilities();

		URL remoteAdress = this.configuration.getSection(
				WebDriverSettings.class).getRemoteAddress();
		if (remoteAdress == null) {// local starting
			prelaunch(supportedDriver, this.configuration, capabilities);
			constructorBody(supportedDriver, capabilities, (URL) null);
			return;
		}

		try {
			constructorBody(supportedDriver, capabilities, remoteAdress);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * Creates and wraps an instance of required {@link RemoteWebDriver}
	 * subclass
	 * 
	 * @param supporteddriver
	 *            Is the one element from {@link ESupportedDrivers} enumeration
	 *            which contains the class of required {@link RemoteWebDriver}
	 *            subclass
	 */
	public WebDriverEncapsulation(ESupportedDrivers supporteddriver) {
		this(supporteddriver, supporteddriver.getDefaultCapabilities());
	}

	/**
	 * Creates and wraps an instance of required {@link RemoteWebDriver}
	 * subclass with given {@link Capabilities}
	 * 
	 * @param supporteddriver
	 *            Is the one element from {@link ESupportedDrivers} enumeration
	 *            which contains the class of required {@link RemoteWebDriver}
	 *            subclass
	 * 
	 * @param capabilities
	 *            in an instance of {@link Capabilities}
	 */
	public WebDriverEncapsulation(ESupportedDrivers supporteddriver,
			Capabilities capabilities) {
		prelaunch(supporteddriver, this.configuration, capabilities);
		constructorBody(supporteddriver, capabilities, (URL) null);
	}

	/**
	 * Creates and wraps an instance of required {@link RemoteWebDriver}
	 * subclass with given {@link Capabilities}. It should be launched on the
	 * remote host.
	 * 
	 * @param supporteddriver
	 *            Is the one element from {@link ESupportedDrivers} enumeration
	 *            which contains the class of required {@link RemoteWebDriver}
	 *            subclass
	 * 
	 * @param capabilities
	 *            in an instance of {@link Capabilities}
	 * 
	 * @param remoteAddress
	 *            is the URL of the required remote host
	 */
	public WebDriverEncapsulation(ESupportedDrivers supporteddriver,
			Capabilities capabilities, URL remoteAddress) {
		constructorBody(supporteddriver, capabilities, remoteAddress);
	}

	/**
	 * Creates and wraps an instance of required {@link RemoteWebDriver}
	 * subclass. It should be launched on the remote host.
	 * 
	 * @param supporteddriver
	 *            Is the one element from {@link ESupportedDrivers} enumeration
	 *            which contains the class of required {@link RemoteWebDriver}
	 *            subclass
	 * 
	 * @param remoteAddress
	 *            is the URL of the required remote host
	 */
	public WebDriverEncapsulation(ESupportedDrivers supporteddriver,
			URL remoteAddress) {
		this(supporteddriver, supporteddriver.getDefaultCapabilities(),
				remoteAddress);
	}

	/**
	 * Wraps an instance of required {@link RemoteWebDriver} subclass which is
	 * already instantiated
	 * 
	 * @param explicitlyInitiatedWebDriver
	 *            it is already instantiated {@link RemoteWebDriver}
	 */
	public WebDriverEncapsulation(RemoteWebDriver explicitlyInitiatedWebDriver) {
		this(explicitlyInitiatedWebDriver, Configuration.byDefault);
	}

	/**
	 * Wraps an instance of required {@link RemoteWebDriver} subclass which is
	 * already instantiated and applies given {@link Configuration}
	 * 
	 * @param explicitlyInitiatedWebDriver
	 *            it is already instantiated {@link RemoteWebDriver}
	 * @param {@link Configuration}
	 */
	public WebDriverEncapsulation(RemoteWebDriver explicitlyInitiatedWebDriver,
			Configuration configuration) {
		this.configuration = configuration;
		enclosedDriver = (RemoteWebDriver) context.getBean(
				MainBeanConfiguration.WEBDRIVER_BEAN, context, this,
				destroyableObjects, explicitlyInitiatedWebDriver);
		Log.message("Getting started with already instantiated "
				+ explicitlyInitiatedWebDriver.getClass());
		resetAccordingTo(configuration);
	}

	// other methods:
	private void constructorBody(ESupportedDrivers supporteddriver,
			Capabilities capabilities, URL remoteAddress) {
		if (supporteddriver.startsRemotely() & remoteAddress != null)
			createWebDriver(supporteddriver.getUsingWebDriverClass(),
					new Class[] { URL.class, Capabilities.class },
					new Object[] { remoteAddress, capabilities });
		else {
			if (remoteAddress == null & supporteddriver.requiresRemoteURL())
				throw new RuntimeException(
						"Defined driver '"
								+ supporteddriver.toString()
								+ "' requires remote address (URL)! Please, define it in settings.json "
								+ "or use suitable constructor");
			if (remoteAddress != null)
				Log.message("Remote address " + String.valueOf(remoteAddress)
						+ " has been ignored");
			createWebDriver(supporteddriver.getUsingWebDriverClass(),
					new Class[] { Capabilities.class },
					new Object[] { capabilities });
		}
	}

	// it makes objects of any WebDriver and navigates to specified URL
	private void createWebDriver(Class<? extends WebDriver> driverClass,
			Class<?>[] paramClasses, Object[] values) {
		try {
			enclosedDriver = (RemoteWebDriver) context.getBean(
					MainBeanConfiguration.WEBDRIVER_BEAN, context, this,
					destroyableObjects, driverClass, paramClasses, values);
			Log.message("Getting started with " + driverClass.getSimpleName());
			resetAccordingTo(configuration);
		} catch (Exception e) {
			Log.error(
					"Attempt to create a new web driver instance has been failed! "
							+ e.getMessage(), e);
			destroy();
			throw e;
		}
	}

	/**
	 * Attempts to shut down {@link RemoteWebDriver} and destroys all related
	 * information
	 */
	@Override
	public void destroy() {
		if (enclosedDriver == null)
			return;
		try {
			enclosedDriver.quit();
		} catch (WebDriverException e) { // it may be already dead
			return;
		}
	}

	/**
	 * adds an object which related to {@link Webdriver} and has to be "destroyed"
	 * after quit
	 */
	public void addDestroyable(IDestroyable destroyable) {
		destroyableObjects.add(destroyable);
	}

	/**
	 * @param required {@link WebdriverComponent} subclass
	 * @return The instance of required {@link WebdriverComponent} subclass
	 */
	public <T extends WebdriverComponent> T getComponent(Class<T> required) {
		T result = ComponentFactory.getComponent(required, enclosedDriver);
		if (IConfigurable.class.isAssignableFrom(required)){
			((IConfigurable) result).resetAccordingTo(configuration);
		}
		return result;
	}

	/**
	 * 
	 * @param required {@link WebdriverComponent} subclass
	 * 
	 * @param params is a Class[] which excludes {@link WebDriver}.class
	 * {@link WebDriver} + given Class[] should match to {@link WebdriverComponent} subclass
	 * constructor parameters
	 *   
	 * @param values is a Object[] which excludes {@link WebDriver} instance
	 * {@link WebDriver} instance + given Object[] should match to {@link WebdriverComponent} subclass
	 * constructor 
	 * 
	 * @return The instance of required {@link WebdriverComponent} subclass
	 */
	public <T extends WebdriverComponent> T getComponent(Class<T> required,
			Class<?>[] params, Object[] values) {
		T result = ComponentFactory.getComponent(required, enclosedDriver, params,
				values);
		if (IConfigurable.class.isAssignableFrom(required)){
			((IConfigurable) result).resetAccordingTo(configuration);
		}
		return result;		
	}

	/**
	 * @see org.openqa.selenium.internal.WrapsDriver#getWrappedDriver()
	 */
	@Override
	public WebDriver getWrappedDriver() {
		return enclosedDriver;
	}

	/**
	 * This method replaces previous {@link Configuration}
	 * and applies new given parameters 
	 * 
	 * @see org.arachnidium.util.configuration.interfaces.IConfigurable#resetAccordingTo(org.arachnidium.util.configuration.Configuration)
	 */
	@Override
	public synchronized void resetAccordingTo(Configuration config) {
		configuration = config;
	}

	/**
	 * Returns {@link Configuration}
	 * 
	 * @see org.arachnidium.util.configuration.interfaces.IConfigurationWrapper#getWrappedConfiguration()
	 */
	@Override
	public Configuration getWrappedConfiguration() {
		return configuration;
	}
}