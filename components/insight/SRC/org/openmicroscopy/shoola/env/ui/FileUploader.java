/*
 * org.openmicroscopy.shoola.env.ui.FileSubmit
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2010 University of Dundee. All rights reserved.
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
package org.openmicroscopy.shoola.env.ui;



//Java imports
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.agents.dataBrowser.DataBrowserLoader;
import org.openmicroscopy.shoola.env.config.Registry;
import org.openmicroscopy.shoola.env.data.events.DSCallFeedbackEvent;
import org.openmicroscopy.shoola.env.data.views.CallHandle;
import org.openmicroscopy.shoola.util.file.ImportErrorObject;
import org.openmicroscopy.shoola.util.ui.FileTableNode;
import org.openmicroscopy.shoola.util.ui.MessengerDetails;
import org.openmicroscopy.shoola.util.ui.MessengerDialog;

/**
 * Uploads files to the QA system.
 *
 * @author Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since 3.0-Beta4
 */
class FileUploader
	extends UserNotifierLoader
{

    /** Handle to the asynchronous call so that we can cancel it. */
    private CallHandle	handle;
    
    /** Object hosting the data to upload. */
    private MessengerDetails details;
    
    /** The source dialog. */
    private MessengerDialog src;
    
    /** The files to upload. */
    private Map<ImportErrorObject, FileTableNode> nodes;
    
    /**
     * Creates a new instance.
     * 
     * @param viewer 	Reference to the parent.
     * @param reg    	Reference to the registry.
     * @param src 		The source object. 
     * @param details	Object hosting the data to upload.
     */
    FileUploader(UserNotifier viewer, Registry reg, MessengerDialog src,
    		MessengerDetails details)
	{
		super(viewer, reg, null);
		if (details == null)
			throw new IllegalArgumentException("No files to upload.");
		this.details = details;
		this.src = src;
		nodes = new HashMap<ImportErrorObject, FileTableNode>();
		List l = (List) details.getObjectToSubmit();
		if (l != null) {
			Iterator i = l.iterator();
			FileTableNode node;
			while (i.hasNext()) {
				node = (FileTableNode) i.next();
				nodes.put(node.getFailure(), node);
			}
		}
	}
	
	/** 
	 * Uploads the file. 
	 * @see UserNotifierLoader#cancel()
	 */
	public void load()
	{
		handle = mhView.submitFiles(details, this);
	}
    
	/** 
	 * Cancels the data uploading. 
	 * @see UserNotifierLoader#cancel()
	 */
	public void cancel()
	{ 
		handle.cancel();
	}
	
	/** 
     * Feeds the results back. 
     * @see DataBrowserLoader#update(DSCallFeedbackEvent)
     */
    public void update(DSCallFeedbackEvent fe) 
    {
    	ImportErrorObject f = (ImportErrorObject) fe.getPartialResult();
        if (f != null) {
        	FileTableNode node = nodes.get(f);
        	if (node != null) node.setStatus(false);
        	nodes.remove(f);
        }
        if (nodes.size() == 0) {
        	if (details.isExceptionOnly()) {
        		viewer.notifyInfo("Submit Exceptions", "The exceptions " +
        				"have been submitted.");
        	} else {
        		viewer.notifyInfo("Submit Files", "The files have been " +
    			"successfully submitted.");
        	}
        	if (src != null) {
        		src.setVisible(false);
            	src.dispose();
        	}
        }
    }
    
    /**
     * Does nothing as the asynchronous call returns <code>null</code>.
     * The actual pay-load is delivered progressively
     * during the updates.
     * @see DataBrowserLoader#handleNullResult()
     */
    public void handleNullResult() {}
    
    /**
     * Notifies the user that an error has occurred.
     * @see DataBrowserLoader#handleException(Throwable)
     */
    public void handleException(Throwable exc) 
    {
        String s = "File Upload Failure: ";
        registry.getLogger().error(this, s+exc);
        registry.getUserNotifier().notifyError("File Upload failure", s, exc);
    }
    
}
