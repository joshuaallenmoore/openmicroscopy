/*
 * org.openmicroscopy.shoola.agents.browser.heatmap.HeatMapTreeUI
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

/*------------------------------------------------------------------------------
 *
 * Written by:    Jeff Mellen <jeffm@alum.mit.edu>
 *
 *------------------------------------------------------------------------------
 */
 
package org.openmicroscopy.shoola.agents.browser.heatmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * The UI for displaying the semantic type hierarchy for the heat map.
 * 
 * @author Jeff Mellen, <a href="mailto:jeffm@alum.mit.edu">jeffm@alum.mit.edu</a><br>
 * <b>Internal version:</b> $Revision$ $Date$
 * @version 2.2
 * @since OME2.2
 */
public class HeatMapTreeUI extends JPanel
{
    private JTree treeView;
    private SemanticTypeTree tree;
    
    private Set nodeSelectionListeners;
    
    /**
     * The default tree selection handler for the entire heat map tree.
     */
    private TreeSelectionListener defaultListener = new TreeSelectionListener()
    {
        public void valueChanged(TreeSelectionEvent e)
        {
            DefaultMutableTreeNode selectedNode =
                (DefaultMutableTreeNode)treeView.getLastSelectedPathComponent();
            if(selectedNode == null) return;
            
            SemanticTypeTree.TreeNode node =
                (SemanticTypeTree.TreeNode)selectedNode.getUserObject();
            
            for(Iterator iter = nodeSelectionListeners.iterator(); iter.hasNext();)
            {
                HeatMapTreeListener listener = (HeatMapTreeListener)iter.next();
                listener.nodeSelected(node);
            }
        }
    };
    
    /**
     * Compares the two nodes by element name (compareIgnoreCase)
     */
    private Comparator stringComparator = new Comparator()
    {
        public int compare(Object o1, Object o2)
        {
            if(o1 == null)
            {
                return -1;
            }
            else if(o2 == null)
            {
                return 1;
            }
            String st1 = o1.toString();
            String st2 = o2.toString();
            
            return st1.compareToIgnoreCase(st2);
        }
    };
    
    /**
     * Constructs a heat map UI based on the specified tree.
     * @param tree
     */
    public HeatMapTreeUI(SemanticTypeTree tree)
    {
        if(tree == null)
        {
            DefaultMutableTreeNode empty =
                new DefaultMutableTreeNode("(empty)");
            treeView = new JTree(empty);
        }
        else
        {
            this.tree = tree;
            treeView = buildTree(tree);
        }
        nodeSelectionListeners = new HashSet();
        treeView.setCellRenderer(new HeatMapTreeRenderer());
        add(treeView);
    }
    
    /**
     * Sets the backing ST tree to the specified object
     * @param tree The tree to use.
     */
    public void setModel(SemanticTypeTree tree)
    {
        if(tree != null)
        {
            this.tree = tree;
            treeView = buildTree(tree);
        }
    }
    
    /**
     * Adds a selection listener to the tree.
     * @param listener The listener to add.
     */
    public void addListener(HeatMapTreeListener listener)
    {
        if(listener != null)
        {
            nodeSelectionListeners.add(listener);
        }
    }
    
    /**
     * Removes a selection listener from the tree.
     * @param listener The listener to remove.
     */
    public void removeListener(HeatMapTreeListener listener)
    {
        if(listener != null)
        {
            nodeSelectionListeners.remove(listener);
        }
    }
    
    
    
    private JTree buildTree(SemanticTypeTree tree)
    {
        if(tree == null)
        {
            DefaultMutableTreeNode empty =
                new DefaultMutableTreeNode("(empty)");
            return new JTree(empty);
        }
        DefaultMutableTreeNode root =
            new DefaultMutableTreeNode(tree.getRootNode());
        
        List nodeQueue = new ArrayList();
        nodeQueue.add(root);
        
        while(nodeQueue.size() > 0)
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)nodeQueue.get(0);
            nodeQueue.remove(node);
            SemanticTypeTree.TreeNode stNode =
                (SemanticTypeTree.TreeNode)node.getUserObject();
            
            Set children = stNode.getChildren();
            List organizedChildren = new ArrayList(children);
            Collections.sort(organizedChildren,stringComparator);
            
            for(Iterator iter = organizedChildren.iterator(); iter.hasNext();)
            {
                SemanticTypeTree.TreeNode child =
                    (SemanticTypeTree.TreeNode)iter.next();
                
                DefaultMutableTreeNode childNode =
                    new DefaultMutableTreeNode(child);
                node.add(childNode);
                if(child.getChildren() != null && child.getChildren().size() > 0)
                {
                    nodeQueue.add(childNode);
                }
            }
        }
        
        return new JTree(root);
    }
}
