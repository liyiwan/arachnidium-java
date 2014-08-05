package org.arachnidium.core.components.mobile;

import io.appium.java_client.MultiTouchAction;
import io.appium.java_client.TouchAction;

import org.arachnidium.core.components.WebdriverComponent;
import org.openqa.selenium.WebDriver;

/**
 * Performs {@link TouchAction} and {@link MultiTouchAction}
 *
 */
public abstract class TouchActionsPerformer extends WebdriverComponent {
	public TouchActionsPerformer(WebDriver driver) {
		super(driver);
		delegate = this.driver;
	}	
    public abstract void performMultiTouchAction(MultiTouchAction multiAction);
	public abstract TouchAction performTouchAction(TouchAction touchAction);
}