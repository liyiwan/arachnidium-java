package org.arachnidium.core.interfaces;

/**
 * @author s.tihomirov
 * Implementors should take pictures of itself 
 */
public interface ITakesPictureOfItSelf {
	public void takeAPictureOfAnInfo(String Comment);
	public void takeAPictureOfAFine(String Comment);	
	public void takeAPictureOfAWarning(String Comment);
	public void takeAPictureOfASevere(String Comment);
}
