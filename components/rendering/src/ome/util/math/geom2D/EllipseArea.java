/*
 * ome.util.math.geom2D.EllipseArea
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

package ome.util.math.geom2D;

//Java imports
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

//Third-party libraries

//Application-internal dependencies
import ome.util.mem.Handle;

/** 
 * Represents an ellipse in the Euclidean space <b>R</b><sup>2</sup>.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author  <br>Andrea Falconi &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:a.falconi@dundee.ac.uk">
 * 					a.falconi@dundee.ac.uk</a>
 * @version 2.2
 * <small>
 * (<b>Internal version:</b> $Revision: 1.1 $ $Date: 2005/06/09 15:01:32 $)
 * </small>
 * @since OME2.2
 */
public class EllipseArea
    extends Handle
    implements PlaneArea
{

    public EllipseArea(float x, float y, float width, float height)
    {
        super(new EllipseAreaAdapter(x, y, width, height));
    }
    
    /** Implemented as specified in the {@link PlaneArea} I/F. */
    public void setBounds(int x, int y, int width, int height)
    {
        breakSharing();
        ((EllipseAreaAdapter) getBody()).setBounds(x, y, width, height);
    }

    /** Implemented as specified in the {@link PlaneArea} I/F. */
    public void scale(double factor)
    {
        breakSharing();
        EllipseAreaAdapter adapter = (EllipseAreaAdapter) getBody();
        Rectangle r = adapter.getBounds();
        adapter.setBounds((int) (r.x*factor), (int) (r.y*factor), 
                (int) (r.width*factor), (int) (r.height*factor)); 
    }

    /** Implemented as specified in the {@link PlaneArea} I/F. */
    public PlanePoint[] getPoints()
    {
        return ((EllipseAreaAdapter) getBody()).getPoints();
    }

    /** Implemented as specified in the {@link PlaneArea} I/F. */
    public boolean onBoundaries(double x, double y)
    {
        return ((EllipseAreaAdapter) getBody()).onBoundaries(x, y);
    }
    
    /** Required by the {@link java.awt.Shape Shape} I/F. */
    public boolean contains(double x, double y)
    {
        return ((EllipseAreaAdapter) getBody()).contains(x, y);
    }

    /** Required by the {@link java.awt.Shape Shape} I/F. */
    public boolean contains(double x, double y, double w, double h)
    {
        return ((EllipseAreaAdapter) getBody()).contains(x, y, w, h);
    }

    /** Required by the {@link java.awt.Shape Shape} I/F. */
    public boolean intersects(double x, double y, double w, double h)
    {
        EllipseAreaAdapter adapter = (EllipseAreaAdapter) getBody();
        return adapter.intersects(x, y, w, h);
    }

    /** Required by the {@link java.awt.Shape Shape} I/F. */
    public Rectangle getBounds()
    {
        return ((EllipseAreaAdapter) getBody()).getBounds();
    }

    /** Required by the {@link java.awt.Shape Shape} I/F. */
    public boolean contains(Point2D p)
    {
        return ((EllipseAreaAdapter) getBody()).contains(p);
    }

    /** Required by the {@link java.awt.Shape Shape} I/F. */
    public Rectangle2D getBounds2D()
    {
        return ((EllipseAreaAdapter) getBody()).getBounds2D();
    }

    /** Required by the {@link java.awt.Shape Shape} I/F. */
    public boolean contains(Rectangle2D r)
    {
        return ((EllipseAreaAdapter) getBody()).contains(r);
    }

    /** Required by the {@link java.awt.Shape Shape} I/F. */
    public boolean intersects(Rectangle2D r)
    {
        return ((EllipseAreaAdapter) getBody()).intersects(r);
    }

    /** Required by the {@link java.awt.Shape Shape} I/F. */
    public PathIterator getPathIterator(AffineTransform at)
    {
        return ((EllipseAreaAdapter) getBody()).getPathIterator(at);
    }

    /** Required by the {@link java.awt.Shape Shape} I/F. */
    public PathIterator getPathIterator(AffineTransform at, double flatness)
    {
        return ((EllipseAreaAdapter) getBody()).getPathIterator(at, flatness);
    }

}
