/*
 * org.openmicroscopy.shoola.env.data.views.calls.DataObjectSaver
 *
 *------------------------------------------------------------------------------
 *
 *  Copyright (C) 2004 Open Microscopy Environment
 *      Massachusetts Institute of Technology,
 *      National Institutes of Health,
 *      University of Dundee
 *
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *------------------------------------------------------------------------------
 */

package org.openmicroscopy.shoola.env.data.views.calls;


//Java imports
import java.util.ArrayList;

//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.env.data.DataManagementService;
import org.openmicroscopy.shoola.env.data.SemanticTypesService;
import org.openmicroscopy.shoola.env.data.model.CategoryData;
import org.openmicroscopy.shoola.env.data.model.CategoryGroupData;
import org.openmicroscopy.shoola.env.data.model.DatasetData;
import org.openmicroscopy.shoola.env.data.model.DatasetSummary;
import org.openmicroscopy.shoola.env.data.model.ImageData;
import org.openmicroscopy.shoola.env.data.model.ProjectData;
import org.openmicroscopy.shoola.env.data.model.ProjectSummary;
import org.openmicroscopy.shoola.env.data.views.BatchCall;
import org.openmicroscopy.shoola.env.data.views.BatchCallTree;
import pojos.DataObject;

/** 
 * 
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @version 2.2
 * <small>
 * (<b>Internal version:</b> $Revision$ $Date$)
 * </small>
 * @since OME2.2
 */
public class DataObjectSaver
    extends BatchCallTree
{

    /** Indicates to create a <code>DataObject</code>. */
    public static final int CREATE = 0;
    
    /** Indicates to update the <code>DataObject</code>. */
    public static final int UPDATE = 1;
    
    /** Indicates to remove the <code>DataObject</code>. */
    public static final int REMOVE = 2;
    
    /** The save call. */
    private BatchCall       saveCall;
    
    /** The result of the call. */
    private DataObject      result;
    
    /** Tempo. */
    private DataObject      objectToSave;
    
    //TODO: remove asap.
    private ProjectData daToProjectData(pojos.ProjectData data)
    {
        ProjectData d = new ProjectData();
        d.setName(data.getName());
        d.setDescription(data.getDescription());
        return d;
    }
    
    private pojos.ProjectData summaryToDataObject(ProjectSummary s)
    {
        pojos.ProjectData data = new pojos.ProjectData();
        data.setId(s.getID());
        data.setName(s.getName());
        data.setDescription(((pojos.ProjectData) objectToSave).getDescription());
        return data;
    }
    
    private DatasetData daToDatasetData(pojos.DatasetData data)
    {
        DatasetData d = new DatasetData();
        d.setName(data.getName());
        d.setDescription(data.getDescription());
        return d;
    }
    
    private ImageData daToImageData(pojos.ImageData data)
    {
        ImageData i = new ImageData();
        i.setName(data.getName());
        i.setDescription(data.getDescription());
        return i;
    }
    
    private CategoryGroupData daToCategoryGroupData(pojos.CategoryGroupData d)
    {
        CategoryGroupData c = new CategoryGroupData();
        c.setName(d.getName());
        c.setDescription(d.getDescription());
        return c;
    }
    
    private CategoryData daToCategoryData(pojos.CategoryData d)
    {
        CategoryData c = new CategoryData();
        c.setName(d.getName());
        c.setDescription(d.getDescription());
        return c;
    }
    
    private pojos.CategoryGroupData reverseCategoryGroup(CategoryGroupData c)
    {
        pojos.CategoryGroupData pojo = new  pojos.CategoryGroupData();
        pojo.setId(c.getID());
        pojo.setName(c.getName());
        pojo.setDescription(c.getDescription());
        return pojo;
    }
    
    private pojos.DatasetData summaryToDataObject(DatasetSummary s)
    {
        pojos.DatasetData data = new pojos.DatasetData();
        data.setId(s.getID());
        data.setName(s.getName());
        data.setDescription(((pojos.DatasetData) objectToSave).getDescription());
        return data;
    }
    
    /**
     * Creates a {@link BatchCall} to create the specified {@link DataObject}.
     * 
     * @param userObject 	The <code>DataObject</code> to create.
     * @param parent 		The parent of the <code>DataObject</code>.
     * @return The {@link BatchCall}.
     */
    private BatchCall create(final DataObject userObject, final Object parent)
    {
        return new BatchCall("Create Data object.") {
            public void doCall() throws Exception
            {
                DataManagementService dms = context.getDataManagementService();
                SemanticTypesService sts = context.getSemanticTypesService();
                //tempo
                if (userObject instanceof pojos.ProjectData) {
                    ProjectData data = 
                        daToProjectData((pojos.ProjectData) userObject);
                    result = summaryToDataObject(dms.createProject(data));
                } else if (userObject instanceof pojos.DatasetData) {
                    DatasetData data = 
                        daToDatasetData((pojos.DatasetData) userObject);
                    ProjectSummary p = new ProjectSummary();
                    p.setID(((pojos.ProjectData) parent).getId());
                    ArrayList l = new ArrayList(1);
                    l.add(parent);
                    result = summaryToDataObject(
                                    dms.createDataset(l, null, data));
                } else if (userObject instanceof pojos.CategoryGroupData) {
                    CategoryGroupData c = daToCategoryGroupData(
                            	(pojos.CategoryGroupData) userObject);
                    result = reverseCategoryGroup(sts.createCategoryGroup(c));
                } else if (userObject instanceof pojos.CategoryData) {
                    
                }
            }
        };
    }
    
    /**
     * Creates a {@link BatchCall} to update the specified {@link DataObject}.
     * 
     * @param userObject The <code>DataObject</code> to update.
     * @return The {@link BatchCall}.
     */
    private BatchCall update(final DataObject userObject)
    {
        return new BatchCall("Update Data object.") {
            public void doCall() throws Exception
            {
                //TODO: REMOVE ASAP
                DataManagementService dms = context.getDataManagementService();
                SemanticTypesService sts = context.getSemanticTypesService();
                if (userObject instanceof pojos.ProjectData) {
                    ProjectData data = 
                        daToProjectData((pojos.ProjectData) userObject);
                    dms.updateProject(data, null, null);
                    result = userObject;
                } else if (userObject instanceof pojos.DatasetData) {
                    DatasetData data = 
                        daToDatasetData((pojos.DatasetData) userObject);
                    dms.updateDataset(data, null, null);
                    result = userObject;
                } else if (userObject instanceof pojos.ImageData) {
                    ImageData data = daToImageData(
                            		(pojos.ImageData) userObject);
                    dms.updateImage(data);
                    result = userObject;
                } else if (userObject instanceof pojos.CategoryGroupData) {
                    CategoryGroupData data = daToCategoryGroupData(
                            	(pojos.CategoryGroupData) userObject);
                    sts.updateCategoryGroup(data, null);
                    result = userObject;
                } else if (userObject instanceof pojos.CategoryData) {
                    CategoryData data = daToCategoryData(
                        		(pojos.CategoryData) userObject);
                    sts.updateCategory(data, null, null);
                    result = userObject;
                }
            }
        };
    }
    
    /**
     * Creates a {@link BatchCall} to update the specified {@link DataObject}.
     * 
     * @param userObject	The <code>DataObject</code> to remove.
     * @param parent		The parent of the <code>DataObject</code>.
     * @return The {@link BatchCall}.
     */
    private BatchCall remove(final DataObject userObject, final Object parent)
    {
        return new BatchCall("Update Data object.") {
            public void doCall() throws Exception
            {
                
            }
        };
    }
    
    /**
     * Adds the {@link #saveCall} to the computation tree.
     * @see BatchCallTree#buildTree()
     */
    protected void buildTree() { add(saveCall); }

    /**
     * TODO: modified code.
     */
    protected Object getResult()
    {
        return Boolean.TRUE;
    }

    /**
     * Creates a new instance.
     * 
     * @param userObject    The {@link DataObject} to create or update.
     *                      Mustn't be <code>null</code>.
     * @param index         One of the constants defined by this class.
     * @param parent     	The parent of the <code>DataObject</code>. 
     * 						The value is <code>null</code> if there 
     * 						is no parent.
     */
    public DataObjectSaver(DataObject userObject, int index, Object parent)
    {
        if (userObject == null)
            throw new IllegalArgumentException("No DataObject.");
        objectToSave = userObject; //tempo
        if (index == CREATE || index == REMOVE) {
            if (userObject instanceof pojos.DatasetData) {
                if (!(parent instanceof pojos.ProjectData))
                throw new IllegalArgumentException("Parent not valid.");
            } else if (userObject instanceof pojos.CategoryData) {
                if (!(parent instanceof pojos.CategoryGroupData))
                    throw new IllegalArgumentException("Parent not valid.");
            }
        }
        switch (index) {
            case CREATE:
                saveCall = create(userObject, parent);
                break;
            case UPDATE:
                saveCall = update(userObject);
                break;
            case REMOVE:
                saveCall = remove(userObject, parent);   
                break;
            default:
                throw new IllegalArgumentException("Operation not supported.");
        }
    }
  
    
}
