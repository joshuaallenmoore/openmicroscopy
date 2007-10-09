/*
 * org.openmicroscopy.shoola.agents.iviewer.view.ImViewerModel
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006 University of Dundee. All rights reserved.
 *
 *
 * 	This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */

package org.openmicroscopy.shoola.agents.imviewer.view;



//Java imports
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

//Third-party libraries

//Application-internal dependencies
import ome.model.core.Pixels;
import omeis.providers.re.data.PlaneDef;
import org.openmicroscopy.shoola.agents.events.iviewer.CopyRndSettings;
import org.openmicroscopy.shoola.agents.imviewer.CategoryLoader;
import org.openmicroscopy.shoola.agents.imviewer.CategorySaver;
import org.openmicroscopy.shoola.agents.imviewer.DataLoader;
import org.openmicroscopy.shoola.agents.imviewer.ImViewerAgent;
import org.openmicroscopy.shoola.agents.imviewer.RenderingControlLoader;
import org.openmicroscopy.shoola.agents.imviewer.browser.Browser;
import org.openmicroscopy.shoola.agents.imviewer.browser.BrowserFactory;
import org.openmicroscopy.shoola.agents.imviewer.rnd.Renderer;
import org.openmicroscopy.shoola.agents.imviewer.rnd.RendererFactory;
import org.openmicroscopy.shoola.agents.imviewer.util.HistoryItem;
import org.openmicroscopy.shoola.agents.imviewer.util.player.ChannelPlayer;
import org.openmicroscopy.shoola.agents.imviewer.util.player.Player;
import org.openmicroscopy.shoola.env.LookupNames;
import org.openmicroscopy.shoola.env.data.DSAccessException;
import org.openmicroscopy.shoola.env.data.DSOutOfServiceException;
import org.openmicroscopy.shoola.env.data.OmeroImageService;
import org.openmicroscopy.shoola.env.data.model.ChannelMetadata;
import org.openmicroscopy.shoola.env.event.EventBus;
import org.openmicroscopy.shoola.env.rnd.RenderingControl;
import org.openmicroscopy.shoola.env.rnd.RenderingServiceException;
import org.openmicroscopy.shoola.env.rnd.RndProxyDef;
import org.openmicroscopy.shoola.util.image.geom.Factory;
import pojos.CategoryData;
import pojos.CategoryGroupData;
import pojos.ExperimenterData;

/** 
* The Model component in the <code>ImViewer</code> MVC triad.
* This class tracks the <code>ImViewer</code>'s state and knows how to
* initiate data retrievals. It also knows how to store and manipulate
* the results. This class provides a suitable data loader.
* The {@link ImViewerComponent} intercepts the results of data loadings, feeds
* them back to this class and fires state transitions as appropriate.
* 
* @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
* 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
* @author	Andrea Falconi &nbsp;&nbsp;&nbsp;&nbsp;
* 				<a href="mailto:a.falconi@dundee.ac.uk">a.falconi@dundee.ac.uk</a>
* @author	Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
* 				<a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
* @version 3.0
* <small>
* (<b>Internal version:</b> $Revision: $ $Date: $)
* </small>
* @since OME2.2
*/
class ImViewerModel
{

	/** Indicates the displayed image has one star. */
	static final int     		RATING_ONE = 3;

	/** Indicates the displayed image has two star. */
	static final int     		RATING_TWO = 4;

	/** Indicates the displayed image has three star. */
	static final int     		RATING_THREE = 5;

	/** Indicates the displayed image has four star. */
	static final int     		RATING_FOUR = 6;

	/** Indicates the displayed image has five star. */
	static final int     		RATING_FIVE = 7;

	/** The maximum width of the thumbnail. */
	private static final int    THUMB_MAX_WIDTH = 48; 

	/** The maximum height of the thumbnail. */
	private static final int    THUMB_MAX_HEIGHT = 48;

	/** The id of the set of pixels. */
	private long                pixelsID;

	/** The id of the image. */
	private long                imageID;

	/** The name of the image. */
	private String              imageName;

	/** Holds one of the state flags defined by {@link ImViewer}. */
	private int                 state;

	/** Reference to the component that embeds this model. */
	private ImViewer            component;

	/** 
	 * Will either be a data loader or
	 * <code>null</code> depending on the current state. 
	 */
	private DataLoader          currentLoader;

	/** The sub-component that hosts the display. */
	private Browser             browser;

	/** Reference to the rendering control. */
	private RenderingControl    rndControl;

	/** Reference to the {@link Renderer}. */
	private Renderer            renderer;

	/** Reference to the current player. */
	private ChannelPlayer       player;

	/** The width of the thumbnail if the window is iconified. */
	private int                 sizeX;

	/** The height of the thumbnail if the window is iconified. */
	private int                 sizeY;

	/** The magnification factor for the thumbnail. */
	private double              factor;

	/** The image icon. */
	private BufferedImage       imageIcon;

	/** The bounds of the component requesting the viewer. */
	private Rectangle           requesterBounds;

	/** Fit the image to the size of window, on resize. */
	private boolean				zoomFitToWindow; 

	/** The index of the selected tabbed. */
	private int					tabbedIndex;

	/** 
	 * Flag indicating to paint or not some textual information on top
	 * of the grid image.
	 */
	private boolean				textVisible;

	/** Flag indicating that a movie is played. */
	private boolean				playingMovie;

	/** Collection of history item. */
	private List<HistoryItem>	historyItems;

	/** 
	 * Flag indicating that a previous item replaced an existing one. 
	 * In that case, a new element is not added to the history list.
	 */
	private boolean				historyItemReplacement;

	/** Collection of categories the image belongs to. */
	private List				categories;

	/** Collection of available categories.*/
	private List				availableCategories;

	/** Collection of category Group. */
	private List				categoryGroups;

	/** The pixels set to copy the rendering settings from. */
	private Pixels 				pixels;

	private boolean				reverse;
	
	/** Computes the values of the {@link #sizeX} and {@link #sizeY} fields. */
	private void computeSizes()
	{
		if (sizeX == -1 && sizeY == -1) {
			sizeX = THUMB_MAX_WIDTH;
			sizeY = THUMB_MAX_HEIGHT;
			double x = sizeX/(double) getMaxX();
			double y =  sizeY/(double) getMaxY();
			if (x > y) factor = x;
			else factor = y;
			double ratio =  (double) getMaxX()/getMaxY();
			if (ratio < 1) sizeX *= ratio;
			else if (ratio > 1 && ratio != 0) sizeY *= 1/ratio;
		}
	}

	/**
	 * Creates a new object and sets its state to {@link ImViewer#NEW}.
	 * 
	 * @param pixelsID  The id of the pixels set.
	 * @param imageID   The id of the image.
	 * @param name      The image's name.
	 * @param bounds    The bounds of the component invoking the 
	 *                  {@link ImViewer}.
	 */
	ImViewerModel(long pixelsID, long imageID, String name, Rectangle bounds)
	{
		this.pixelsID = pixelsID;
		this.imageID = imageID;
		imageName = name;
		requesterBounds = bounds;
		state = ImViewer.NEW;
		sizeX = sizeY = -1;
		zoomFitToWindow = false; 
		tabbedIndex = ImViewer.VIEW_INDEX;
		textVisible = true;
		reverse = false;
		/*
		if (name != null) {
			if (name.endsWith(".dv") || name.endsWith(".DV"))
				reverse = true;
		}
		*/
	}

	/**
	 * Called by the <code>ImViewer</code> after creation to allow this
	 * object to store a back reference to the embedding component.
	 * 
	 * @param component The embedding component.
	 */
	void initialize(ImViewer component)
	{ 
		this.component = component;
		browser = BrowserFactory.createBrowser(component, imageID);
	}

	/**
	 * Returns <code>true</code> if we need to flip along the X-axis the image,
     * <code>false</code> otherwise.
	 * This is only valid for dv file.
	 * 
	 * @return See above.
	 */
	boolean isReverse() { return reverse; }
	
	/**
	 * Returns the current user's details.
	 * 
	 * @return See above.
	 */
	ExperimenterData getUserDetails()
	{ 
		return (ExperimenterData) ImViewerAgent.getRegistry().lookup(
				LookupNames.CURRENT_USER_DETAILS);
	}

	/**
	 * Compares another model to this one to tell if they would result in
	 * having the same display.
	 *  
	 * @param other The other model to compare.
	 * @return <code>true</code> if <code>other</code> would lead to a viewer
	 *          with the same display as the one in which this model belongs;
	 *          <code>false</code> otherwise.
	 */
	boolean isSameDisplay(ImViewerModel other)
	{
		if (other == null) return false;
		return ((other.pixelsID == pixelsID) && (other.imageID == imageID));
	}

	/**
	 * Returns the name of the image.
	 * 
	 * @return See above.
	 */
	String getImageName() { return imageName; }

	/**
	 * Returns the current state.
	 * 
	 * @return One of the flags defined by the {@link ImViewer} interface.  
	 */
	int getState() { return state; }

	/**
	 * Sets the object in the {@link ImViewer#DISCARDED} state.
	 * Any ongoing data loading will be cancelled.
	 */
	void discard()
	{
		state = ImViewer.DISCARDED;
		//Shut down the service
		ImViewerAgent.getRegistry().getImageService().shutDown(pixelsID);
		if (currentLoader != null) {
			currentLoader.cancel();
			currentLoader = null;
		}
		if (renderer != null) renderer.discard();
		if (player == null) return;
		player.setPlayerState(Player.STOP);
		player = null;

	}

	/**
	 * Returns the sizeX.
	 * 
	 * @return See above.
	 */
	int getMaxX() { return rndControl.getPixelsDimensionsX(); }

	/**
	 * Returns the sizeY.
	 * 
	 * @return See above.
	 */
	int getMaxY() { return rndControl.getPixelsDimensionsY(); }

	/**
	 * Returns the maximum number of z-sections.
	 * 
	 * @return See above.
	 */
	int getMaxZ() { return rndControl.getPixelsDimensionsZ()-1; }

	/**
	 * Returns the maximum number of timepoints.
	 * 
	 * @return See above.
	 */
	int getMaxT() { return rndControl.getPixelsDimensionsT()-1; }

	/**
	 * Returns the currently selected z-section.
	 * 
	 * @return See above.
	 */
	int getDefaultZ() { return rndControl.getDefaultZ(); }

	/**
	 * Returns the currently selected timepoint.
	 * 
	 * @return See above.
	 */
	int getDefaultT() { return rndControl.getDefaultT(); }

	/**
	 * Returns the currently selected color model.
	 * 
	 * @return See above.
	 */
	String getColorModel() { return rndControl.getModel(); }

	/**
	 * Returns the rate image level. One of the constants defined by this class.
	 * 
	 * @return See above.
	 */
	int getRatingLevel()
	{
		return RATING_TWO;
	}

	/**
	 * Returns an array of <code>ChannelData</code> object.
	 * 
	 * @return See above.
	 */
	ChannelMetadata[] getChannelData() { return rndControl.getChannelData(); }

	/**
	 * Returns the <code>ChannelData</code> object corresponding to the
	 * given index.
	 * 
	 * @param index The index of the channel.
	 * @return See above.
	 */
	ChannelMetadata getChannelData(int index)
	{ 
		return rndControl.getChannelData(index);
	}

	/**
	 * Returns the color associated to a channel.
	 * 
	 * @param w The OME index of the channel.
	 * @return See above.
	 */
	Color getChannelColor(int w) { return rndControl.getRGBA(w); }

	/**
	 * Returns <code>true</code> if the channel is mapped, <code>false</code>
	 * otherwise.
	 * 
	 * @param w	The channel's index.
	 * @return See above.
	 */
	boolean isChannelActive(int w) { return rndControl.isActive(w); }

	/** Fires an asynchronous retrieval of the rendering control. */
	void fireRenderingControlLoading()
	{
		currentLoader = new RenderingControlLoader(component, pixelsID, false);
		currentLoader.load();
		state = ImViewer.LOADING_RENDERING_CONTROL;
	}

	/** Fires an asynchronous retrieval of the rendering control. */
	void reloadRenderingControl()
	{
		currentLoader = new RenderingControlLoader(component, pixelsID, true);
		currentLoader.load();
		state = ImViewer.LOADING_RENDERING_CONTROL;
	}

	/** Fires an asynchronous retrieval of the rendered image. */
	void fireImageRetrieval()
	{
		PlaneDef pDef = new PlaneDef(PlaneDef.XY, getDefaultT());
		pDef.setZ(getDefaultZ());
		state = ImViewer.LOADING_IMAGE;
		OmeroImageService os = ImViewerAgent.getRegistry().getImageService();
		try {
			component.setImage(os.renderImage(pixelsID, pDef));
		} catch (Exception e) {
			component.reload(e);
		}
		/*
      currentLoader = new ImageLoader(component, pixelsID, pDef);
      currentLoader.load();
		 */
	}

	/**
	 * This method should only be invoked when we save the displayed image
	 * and split its components.
	 * 
	 * @return See above.
	 * @throws RenderingServiceException 	If an error occured while setting 
	 * 										the value.
	 */
	BufferedImage getSplitComponentImage()
		throws RenderingServiceException
	{
		PlaneDef pDef = new PlaneDef(PlaneDef.XY, getDefaultT());
		pDef.setZ(getDefaultZ());
		//state = ImViewer.LOADING_IMAGE;
		OmeroImageService os = ImViewerAgent.getRegistry().getImageService();
		return os.renderImage(pixelsID, pDef);
	}

	/**
	 * Sets the rendering control.
	 * 
	 * @param rndControl 	The object to set.
	 */
	void setRenderingControl(RenderingControl rndControl)
	{
		this.rndControl = rndControl;
		if (renderer == null) {
			renderer = RendererFactory.createRenderer(component, rndControl);
			state = ImViewer.RENDERING_CONTROL_LOADED;
		} else {
			renderer.setRenderingControl(rndControl);
		}
	} 

	/**
	 * Returns the {@link Browser}.
	 * 
	 * @return See above.
	 */
	Browser getBrowser() { return browser; }

	/**
	 * Sets the zoom factor.
	 * 
	 * @param factor The factor to set.
	 */
	void setZoomFactor(double factor) { browser.setZoomFactor(factor); }

	/**
	 * Returns the zoom factor.
	 * 
	 * @return The factor to set.
	 */
	double getZoomFactor() { return browser.getZoomFactor(); }

	/**
	 * This method determines if the browser image should be resized to fit 
	 * the window size if the window is resized. 
	 * 
	 * @param option see above.
	 */
	void setZoomFitToWindow(boolean option) { zoomFitToWindow = option; }

	/**
	 * This method determines if the browser image should be resized to fit 
	 * the window size if the window is resized.
	 *  
	 * @return <code>true</code> if image should resize on window resize. 
	 */ 
	boolean getZoomFitToWindow() { return zoomFitToWindow; }

	/**
	 * Sets the retrieved image.
	 * 
	 * @param image The image to set.
	 */
	void setImage(BufferedImage image)
	{
		state = ImViewer.READY; 
		browser.setRenderedImage(image);
		//update image icon
		computeSizes();
		imageIcon = Factory.magnifyImage(factor, image);
	}

	/**
	 * Sets the color model.
	 * 
	 * @param colorModel	The color model to set.
	 * @throws RenderingServiceException 	If an error occured while setting 
	 * 										the value.
	 * @throws DSOutOfServiceException  	If the connection is broken.
	 */
	void setColorModel(String colorModel)
		throws RenderingServiceException, DSOutOfServiceException
	{
		if (ImViewer.GREY_SCALE_MODEL.equals(colorModel))
			rndControl.setModel(colorModel);
		else if (ImViewer.RGB_MODEL.equals(colorModel) ||
				(ImViewer.HSB_MODEL.equals(colorModel)))
			rndControl.setModel(ImViewer.HSB_MODEL);
	}

	/**
	 * Sets the selected plane.
	 * 
	 * @param z The z-section to set.
	 * @param t The timepoint to set.
	 * @throws RenderingServiceException 	If an error occured while setting 
	 * 										the value.
	 * @throws DSOutOfServiceException  	If the connection is broken.
	 */
	void setSelectedXYPlane(int z, int t)
		throws RenderingServiceException, DSOutOfServiceException
	{
		rndControl.setDefaultT(t);
		rndControl.setDefaultZ(z);
	}

	/**
	 * Sets the color for the specified channel.
	 * 
	 * @param index The channel's index.
	 * @param c     The color to set.
	 * @throws RenderingServiceException 	If an error occured while setting 
	 * 										the value.
	 * @throws DSOutOfServiceException  	If the connection is broken.
	 */
	void setChannelColor(int index, Color c)
		throws RenderingServiceException, DSOutOfServiceException
	{
		rndControl.setRGBA(index, c);
	}

	/**
	 * Sets the channel active.
	 * 
	 * @param index The channel's index.
	 * @param b     Pass <code>true</code> to select the channel,
	 *              <code>false</code> otherwise.
	 * @throws RenderingServiceException 	If an error occured while setting 
	 * 										the value.
	 * @throws DSOutOfServiceException  	If the connection is broken.
	 */
	void setChannelActive(int index, boolean b)
		throws RenderingServiceException, DSOutOfServiceException
	{
		rndControl.setActive(index, b);
	}  

	/**
	 * Returns the number of channels.
	 * 
	 * @return See above.
	 */
	int getMaxC() { return rndControl.getPixelsDimensionsC(); }

	/** 
	 * Returns the number of active channels.
	 * 
	 * @return See above.
	 */
	int getActiveChannelsCount()
	{
		int active = 0;
		for (int i = 0; i < getMaxC(); i++) {
			if (rndControl.isActive(i)) active++;
		}
		return active;
	}

	/**
	 * Returns a list of active channels.
	 * 
	 * @return See above.
	 */
	List getActiveChannels()
	{
		ArrayList<Integer> active = new ArrayList<Integer>();
		for (int i = 0; i < getMaxC(); i++) {
			if (rndControl.isActive(i)) active.add(new Integer(i));
		}
		return active;
	}

	/** 
	 * Starts the channels movie player, invokes in the event-dispatcher 
	 * thread for safety reason.
	 * 
	 * @param play  Pass <code>true</code> to play the movie, <code>false</code>
	 *              to stop it.
	 * @throws RenderingServiceException 	If an error occured while setting 
	 * 										the value.
	 * @throws DSOutOfServiceException  	If the connection is broken.
	 */
	void playMovie(boolean play)
		throws RenderingServiceException, DSOutOfServiceException
	{
		if (player != null && !play) {
			player.setPlayerState(Player.STOP);
			List l = player.getChannels();
			if (l != null) {
				Iterator i = l.iterator();
				while (i.hasNext()) 
					setChannelActive( ((Integer) i.next()).intValue(), true);
			}
			player = null;
			state = ImViewer.READY;
			return;
		}
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				player = new ChannelPlayer(component);
				player.setPlayerState(Player.START);
			}
		});
		state = ImViewer.CHANNEL_MOVIE;
	}

	/**
	 * Returns the {@link Renderer}.
	 * 
	 * @return See above.
	 */
	Renderer getRenderer() { return renderer; }

	/**
	 * Returns the displayed image.
	 * 
	 * @return See above.
	 */
	BufferedImage getDisplayedImage() { return browser.getDisplayedImage(); }

	/**
	 * Returns the original image returned by the image service.
	 * 
	 * @return See above.
	 */
	BufferedImage getOriginalImage() { return browser.getRenderedImage(); }

	/**
	 * Returns the image displayed in the grid view.
	 * 
	 * @return See above.
	 */
	BufferedImage getGridImage() { return browser.getGridImage(); }

	/**
	 * Returns the size in microns of a pixel along the X-axis.
	 * 
	 * @return See above.
	 */
	float getPixelsSizeX() { return rndControl.getPixelsSizeX(); }

	/**
	 * Returns the size in microns of a pixel along the Y-axis.
	 * 
	 * @return See above.
	 */
	float getPixelsSizeY() { return rndControl.getPixelsSizeY(); }

	/**
	 * Returns the size in microns of a pixel along the Y-axis.
	 * 
	 * @return See above.
	 */
	float getPixelsSizeZ() { return rndControl.getPixelsSizeZ(); }

	/**
	 * Returns <code>true</code> if the unit bar is painted on top of 
	 * the displayed image, <code>false</code> otherwise.
	 * 
	 * @return See above.
	 */
	boolean isUnitBar() { return browser.isUnitBar(); }

	/**
	 * Returns an iconified version of the displayed image.
	 * 
	 * @return See above.
	 */
	BufferedImage getImageIcon() { return imageIcon; }

	/**
	 * Returns the bounds of the component invoking the {@link ImViewer},
	 * or <code>null</code> if not available.
	 * 
	 * @return See above.
	 */
	Rectangle getRequesterBounds() { return requesterBounds; }

	/**
	 * Returns the ID of the pixels set.
	 * 
	 * @return See above.
	 */
	long getPixelsID() { return pixelsID; }

	/**
	 * Returns the index of the selected tabbed.
	 * 
	 * @return See above.
	 */
	int getTabbedIndex() { return tabbedIndex; }

	/**
	 * Sets the tabbed index.
	 * 
	 * @param index The value to set.
	 */
	void setTabbedIndex(int index) { tabbedIndex = index; }

	/**
	 * Returns a 3-dimensional array of boolean value, one per color band.
	 * The first (resp. second, third) element is set to <code>true</code> 
	 * if an active channel is mapped to <code>RED</code> (resp. 
	 * <code>GREEN</code>, <code>BLUE</code>), to <code>false</code> otherwise.
	 * 
	 * @return See above
	 */
	boolean[] hasRGB()
	{
		boolean[] rgb = new boolean[3];
		rgb[0] = rndControl.hasActiveChannelRed();
		rgb[1] = rndControl.hasActiveChannelGreen();
		rgb[2] = rndControl.hasActiveChannelBlue();
		return rgb;
	}

	/**
	 * Returns <code>true</code> if the textual information is painted on 
	 * top of the grid image, <code>false</code> otherwise.
	 * 
	 * @return See above.
	 */
	boolean isTextVisible() { return textVisible; }

	/**
	 * Sets to <code>true</code> if the textual information is painted on 
	 * top of the grid image, <code>false</code> otherwise.
	 * 
	 * @param textVisible The value to set.
	 */
	void setTextVisible(boolean textVisible) { this.textVisible = textVisible; }

	/**
	 * Returns the image displayed in the annotator view.
	 * 
	 * @return See above.
	 */
	BufferedImage getAnnotateImage() { return browser.getAnnotateImage(); }

	/**
	 * Returns the ID of the viewed image.
	 * 
	 * @return See above.
	 */
	long getImageID() { return imageID; }

	/** 
	 * Saves the rendering settings. 
	 * 
	 * @throws RenderingServiceException 	If an error occured while setting 
	 * 										the value.
	 * @throws DSOutOfServiceException  	If the connection is broken.
	 */
	void saveRndSettings()
		throws RenderingServiceException, DSOutOfServiceException
	{
		if (rndControl != null) rndControl.saveCurrentSettings(); 
	}

	/**
	 * Sets to <code>true</code> when the movie is played, to <code>false</code>
	 * otherwise.
	 * 
	 * @param b The value to set.
	 */
	void setPlayingMovie(boolean b) { playingMovie = b; }

	/**
	 * Returns <code>true</code> if a movie is played, to <code>false</code>
	 * otherwise.
	 * 
	 * @return See above.
	 */
	boolean isPlayingMovie() { return playingMovie; }

	/**
	 * Returns <code>true</code> if the channel is mapped
	 * to <code>RED</code>, <code>false</code> otherwise.
	 * 
	 * @param index The index of the channel.
	 * @return See above.
	 */
	boolean isChannelRed(int index)
	{
		return rndControl.isChannelRed(index);
	}

	/**
	 * Returns <code>true</code> if the channel is mapped
	 * to <code>GREEN</code>, <code>false</code> otherwise.
	 * 
	 * @param index The index of the channel.
	 * @return See above.
	 */
	boolean isChannelGreen(int index)
	{
		return rndControl.isChannelGreen(index);
	}

	/**
	 * Returns <code>true</code> if the channel is mapped
	 * to <code>BLUE</code>, <code>false</code> otherwise.
	 * 
	 * @param index The index of the channel.
	 * @return See above.
	 */
	boolean isChannelBlue(int index)
	{
		return rndControl.isChannelBlue(index);
	}

	/**
	 * Returns a collection of pairs 
	 * (active channel's index, active channel's color).
	 * 
	 * @return See above.
	 */
	Map getActiveChannelsMap()
	{
		List l = getActiveChannels();
		Map<Integer, Color> m = new HashMap<Integer, Color>(l.size());
		Iterator i = l.iterator();
		Integer index;
		while (i.hasNext()) {
			index = (Integer) i.next();
			m.put(index, getChannelColor(index.intValue()));
		}
		return m;
	}

	/** 
	 * Creates a new history item and adds it to the list of elements.
	 * Returns the newly created item.
	 * 
	 *  @return See above.
	 */
	HistoryItem createHistoryItem()
	{
		//Make a smaller image
		BufferedImage img = browser.getRenderedImage();
		double ratio = 1;
		int w = img.getWidth();
		int h = img.getHeight();
		if (w < ImViewer.MINIMUM_SIZE || h < ImViewer.MINIMUM_SIZE) ratio = 1;
		else {
			if (w >= h) ratio = (double) ImViewer.MINIMUM_SIZE/w;
			else ratio = (double) ImViewer.MINIMUM_SIZE/h;
		}
		BufferedImage thumb = Factory.magnifyImage(ratio, img);
		HistoryItem i = new HistoryItem(rndControl.getRndSettingsCopy(), 
										thumb, reverse);
		if (historyItems == null) historyItems = new ArrayList<HistoryItem>();
		historyItems.add(i);
		return i;
	}

	/**
	 * Removes the item from the list.
	 * 
	 * @param node The node to remove.
	 */
	void removeHistoryItem(HistoryItem node)
	{
		if (historyItems != null) historyItems.remove(node);
	}

	/** Removes the last item from the list. */
	void removeLastHistoryItem()
	{
		if (historyItems != null || historyItems.size() > 1) {
			//historyItems.remove(historyItems.size()-1);
		}
	}
	
	/** Clears the history. */
	void clearHistory()
	{
		if (historyItems != null) {
			HistoryItem node = historyItems.get(0);
			historyItems.clear();
			historyItems.add(node);
		}
		historyItemReplacement = false;
	}

	/**
	 * Returns <code>true</code> if an history item has been added to the 
	 * list when the user is swapping between rendering settings, 
	 * <code>false</code> otherwise.
	 * 
	 * @return See above.
	 */
	boolean isHistoryItemReplacement() { return historyItemReplacement; }

	/**
	 * Sets the value of the {@link #historyItemReplacement} flag.
	 * 
	 * @param b The value to set.
	 */
	void setHistoryItemReplacement(boolean b)
	{
		historyItemReplacement = b;
	}

	/**
	 * Returns the collection of history items.
	 * 
	 * @return See above.
	 */
	List getHistory() { return historyItems; }

	/**
	 * Partially resets the rendering settings.
	 * 
	 * @param settings The value to set.
	 * @throws RenderingServiceException 	If an error occured while setting 
	 * 										the value.
	 * @throws DSOutOfServiceException  	If the connection is broken.
	 */
	void resetMappingSettings(RndProxyDef settings) 
		throws RenderingServiceException, DSOutOfServiceException
	{
		rndControl.resetMappingSettings(settings);
		renderer.resetRndSettings();
	}

	/**
	 * Resets the rendering settings.
	 * Returns <code>true</code> if it is possible to copy the rendering 
	 * settings, <code>false</code> otherwise.
	 * 
	 * @return See above.
	 * @throws RenderingServiceException 	If an error occured while setting 
	 * 										the value.
	 * @throws DSOutOfServiceException  	If the connection is broken.
	 * @throws DSAccessException  			If the data cannot be retrieved.
	 */
	boolean resetSettings() 
		throws RenderingServiceException, DSOutOfServiceException,
				DSAccessException
	{
		//First check that the pixels are compatible
		if (pixels == null) {
			OmeroImageService 
			os = ImViewerAgent.getRegistry().getImageService();
			pixels = os.loadPixels(ImViewerFactory.getRefPixelsID());
		}
		if (rndControl.validatePixels(pixels)) {
			rndControl.resetSettings(ImViewerFactory.getRenderingSettings());
			renderer.resetRndSettings();
			return true;
		}
		return false;
	}

	/** 
	 * Resets the default settings. 
	 * 
	 * @throws RenderingServiceException 	If an error occured while setting 
	 * 										the value.
	 * @throws DSOutOfServiceException  	If the connection is broken.
	 */
	void resetDefaultRndSettings()
		throws RenderingServiceException, DSOutOfServiceException
	{ 
		rndControl.resetDefaults(); 
	}
	
	/** Sets the reference to the pixels set to <code>null</code>. */
	void copyRndSettings() { pixels = null; }

	/**
	 * Returns <code>true</code> if we have rendering settings to paste,
	 * <code>false</code> otherwise.
	 * 
	 * @return See above.
	 */
	boolean hasRndToPaste() 
	{ 
		return (ImViewerFactory.getRenderingSettings() != null); 
	}

	/** Posts a {@link CopyRndSettings} event. */
	void copyRenderingSettings()
	{
		CopyRndSettings evt = new CopyRndSettings(pixelsID, 
				rndControl.getRndSettingsCopy());
		EventBus bus = ImViewerAgent.getRegistry().getEventBus();
		bus.post(evt);
	}

	/** 
	 * Starts an asynchronous call to retrieve the category the 
	 * image is categorised into.
	 */
	void fireCategoriesLoading()
	{
		state = ImViewer.LOADING_METADATA;
		currentLoader = new CategoryLoader(component, imageID, 
				getUserDetails().getId());
		currentLoader.load();
	}

	/**
	 * Sets the categories the images is categorised into.
	 * 
	 * @param categories 	 The collection to set.
	 * @param available		 The colllection of categories the image can be
	 * 						 categorised into.
	 * @param categoryGroups The category groups.
	 */
	void setCategories(List categories, List available, List categoryGroups)
	{
		this.categories = categories;
		availableCategories = available;
		this.categoryGroups = categoryGroups;
		state = ImViewer.READY;
	}

	/**
	 * Returns the categories the image is categorised into.
	 * 
	 * @return See above.
	 */
	List getCategories() { return categories; }

	/**
	 * Returns the categories the image can be categorised into.
	 * 
	 * @return See above.
	 */
	List getAvailableCategories() { return availableCategories; }

	/**
	 * Returns the category groups available.
	 * 
	 * @return See above.
	 */
	List getCategoryGroups() { return categoryGroups; }

	/**
	 * Returns the category groups containing categories.
	 * 
	 * @return See above.
	 */
	List getPopulatedCategoryGroups()
	{
		List groups = new ArrayList();
		if (categoryGroups == null || categoryGroups.size() == 0)
			return groups;
		Iterator i = categoryGroups.iterator();
		CategoryGroupData data;
		Set categories;
		while (i.hasNext()) {
			data = (CategoryGroupData) i.next();
			categories = data.getCategories();
			if (categories != null && categories.size() > 0)
				groups.add(data);
		}
		return groups;
	}

	/**
	 * Classifies the image into the specified category.
	 * 
	 * @param categoryID	The id of the category.
	 */
	void declassify(long categoryID)
	{
		state = ImViewer.CLASSIFICATION;
		CategoryData data = new CategoryData();
		data.setId(categoryID);
		Set<CategoryData> categories = new HashSet<CategoryData>(1);
		categories.add(data);
		currentLoader = new CategorySaver(component, imageID, categories, 
				CategorySaver.DECLASSIFY);
		currentLoader.load();
	}

	/**
	 * Classifies the image into the specified categories.
	 * 
	 * @param categories	The categories to add the image to.
	 */
	void classify(Set<CategoryData> categories)
	{
		state = ImViewer.CLASSIFICATION;
		currentLoader = new CategorySaver(component, imageID, categories, 
				CategorySaver.CLASSIFY);
		currentLoader.load();
	}

	/**
	 * Creates new categories and adds the image to the categories.
	 * 
	 * @param categories	The categories to create.
	 */
	void createAndClassify(Set<CategoryData> categories)
	{
		state = ImViewer.CLASSIFICATION;
		currentLoader = new CategorySaver(component, imageID, categories, 
				CategorySaver.CREATE);
		currentLoader.load();
	}

	/**
	 * Creates new categories and adds the image to the categories,
	 * then adds the image to the existing categories.
	 * 
	 * @param toCreate	The categories to create.
	 * @param toUpdate	The categories to update.
	 */
	void createAndClassify(Set<CategoryData> toCreate, 
			Set<CategoryData> toUpdate)
	{
		state = ImViewer.CLASSIFICATION;
		currentLoader = new CategorySaver(component, imageID, toCreate, 
				toUpdate);
		currentLoader.load();
	}
	
}
