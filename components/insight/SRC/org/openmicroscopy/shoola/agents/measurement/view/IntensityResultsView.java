/*
 * org.openmicroscopy.shoola.agents.measurement.view.NewIntensityResultsView 
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2007 University of Dundee. All rights reserved.
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
package org.openmicroscopy.shoola.agents.measurement.view;


//Java imports
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

//Third-party libraries
import org.jhotdraw.draw.Figure;

//Application-internal dependencies
import org.openmicroscopy.shoola.agents.measurement.IconManager;
import org.openmicroscopy.shoola.agents.measurement.MeasurementAgent;
import org.openmicroscopy.shoola.agents.measurement.util.TabPaneInterface;
import org.openmicroscopy.shoola.agents.measurement.util.model.AnalysisStatsWrapper;
import org.openmicroscopy.shoola.agents.measurement.util.model.AnnotationDescription;
import org.openmicroscopy.shoola.agents.measurement.util.model.AnalysisStatsWrapper.StatsType;
import org.openmicroscopy.shoola.env.config.Registry;
import org.openmicroscopy.shoola.env.log.Logger;
import org.openmicroscopy.shoola.env.ui.UserNotifier;
import org.openmicroscopy.shoola.util.file.ExcelWriter;
import org.openmicroscopy.shoola.util.image.geom.Factory;
import org.openmicroscopy.shoola.util.roi.figures.MeasureTextFigure;
import org.openmicroscopy.shoola.util.roi.figures.ROIFigure;
import org.openmicroscopy.shoola.util.roi.model.ROIShape;
import org.openmicroscopy.shoola.util.roi.model.annotation.MeasurementAttributes;
import org.openmicroscopy.shoola.util.roi.model.util.Coord3D;
import org.openmicroscopy.shoola.util.ui.UIUtilities;
import org.openmicroscopy.shoola.util.ui.filechooser.FileChooser;
import pojos.ChannelData;

/** 
 * Displays the intensity results.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 	<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author	Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * 	<a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since OME3.0
 */
class IntensityResultsView
	extends JPanel 
	implements ActionListener, TabPaneInterface
{
	
	/** Index to identify tab */
	public final static int		INDEX = 
		MeasurementViewerUI.INTENSITYRESULTVIEW_INDEX;
	
	/** The add button name. */
	private final static String ADD_NAME = "Add";
	
	/** Tooltip for the add button. */
	private final static String ADD_DESCRIPTION = "Add Intensities for " +
							"the selected ROIs to results table.";

	/** The addAll button name. */
	private final static String ADDALL_NAME = "Add Selected";
	
	/** Tooltip for the add button. */
	private final static String ADDALL_DESCRIPTION = "Add Intensities for " +
						"all the shapes of the selected ROIs to results table.";
	
	/** The remove button name. */
	private final static String REMOVE_NAME = "Remove";
	
	/** Tooltip for the remove button. */
	private final static String REMOVE_DESCRIPTION = "Remove Results in " +
			"selected row from table.";
	
	/** The remove button name. */
	private final static String REMOVE_ALL_NAME = "Remove All";
	
	/** Tooltip for the remove button. */
	private final static String REMOVE_ALL_DESCRIPTION = 
		"Remove all the Results from table.";
	
	/** The save button name. */
	private final static String SAVE_NAME = "Export to Excel";
	
	/** Tooltip for the save button. */
	private final static String SAVE_DESCRIPTION = "Save Intensities " +
			"to Excel File.";
	
	/** Reference to the view. */
	private MeasurementViewerUI view;
	
	/** The results table. */
	private JTable results;
	
	/** The results model for the results table. */
	private ResultsTableModel resultsModel;
	
	/** The remove button. */
	private JButton removeButton;
	
	/** The save button. */
	private JButton saveButton;
	
	/** The add button. */
	private JButton addButton;

	/** The addAll button. */
	private JButton addAllButton;
	
	/** The add button. */
	private JButton removeAllButton;
	
	/** The state of the Intensity View. */
	static enum State 
	{
		/** Analysing data. */
		ANALYSING,
		/** Ready to analyse. */
		READY
	}
	
	/** 
	 * Intensity view state, if Analysing we should not all the user to 
	 * change combobox or save. 
	 */
	private State						state = State.READY;
	
	/** The name of the panel. */
	private static final String			NAME = "Intensity Results View";
	
	/** Action command id indicating to remove the selected rows. */
	private static final int			REMOVE = 0;
	
	/** Action command id indicating to add new row. */
	private static final int			ADD = 1;

	/** Action command id indicating to add new row. */
	private static final int			ADDALL = 4;
	
	/** Action command id indicating to remove all the rows. */
	private static final int			REMOVE_ALL = 2;
	
	/** Action command id indicating to save the results. */
	private static final int			SAVE = 3;
	
	/** Reference to the model. */
	private MeasurementViewerModel		model;
	
	/** The map of <ROIShape, ROIStats> .*/
	private Map							ROIStats;
	
	/** list of the channel names. */
	private Map<Integer, String> channelName = new TreeMap<Integer, String>();
	
	/** List of the channel colours. */
	private Map<Integer, Color> channelColour = new TreeMap<Integer, Color>();
	
	/** Map of the channel sums, for each selected channel. */
	private Map<Integer, Double> channelSum = new TreeMap<Integer, Double>();
	
	/** Map of the channel mins, for each selected channel. */
	private Map<Integer, Double> channelMin = new TreeMap<Integer, Double>();
	
	/** Map of the channel Max, for each selected channel. */
	private Map<Integer, Double> channelMax = new TreeMap<Integer, Double>();
	
	/** Map of the channel Mean, for each selected channel. */
	private Map<Integer, Double> channelMean = new TreeMap<Integer, Double>();
	
	/** Map of the channel std. dev., for each selected channel. */
	private Map<Integer, Double> channelStdDev = new TreeMap<Integer, Double>();
	
	/** Map of the channel name to channel number .*/
	private Map<String, Integer> nameMap = new HashMap<String, Integer>();

	/** Map of the min channel intensity values to coord. */
	private Map<Coord3D, Map<Integer, Double>> minStats;
	
	/** Map of the max channel intensity values to coord. */
	private Map<Coord3D, Map<Integer, Double>> maxStats;
	
	/** Map of the mean channel intensity values to coord. */
	private Map<Coord3D, Map<Integer, Double>> meanStats;
	
	/** Map of the std dev channel intensity values to coord. */
	private Map<Coord3D, Map<Integer, Double>> stdDevStats;
	
	/** Map of the sum channel intensity values to coord. */
	private Map<Coord3D, Map<Integer, Double>> sumStats;
	
	/** Map of the coordinate to a shape. */
	private Map<Coord3D, ROIShape> shapeMap;
	
	/** The current coordinate of the ROI being depicted in the slider. */
	private Coord3D coord;
	
	/** Current ROIShape. */
	private 	ROIShape shape;
	
	/** The collection of rois that have been removed. */
	private Set<Long> remove = new HashSet<Long>();
	
	/**
	 * Implemented as specified by the I/F {@link TabPaneInterface}
	 * @see TabPaneInterface#getIndex()
	 */
	public int getIndex() { return INDEX; }
	
	/** Initializes the component composing the display. */
	private void initComponents()
	{
		state = State.READY;
		removeButton = new JButton(REMOVE_NAME);
		removeButton.setToolTipText(
				UIUtilities.formatToolTipText(REMOVE_DESCRIPTION));
		removeButton.setActionCommand(""+REMOVE);
		removeButton.addActionListener(this);
		saveButton = new JButton(SAVE_NAME);
		saveButton.setToolTipText(
				UIUtilities.formatToolTipText(SAVE_DESCRIPTION));
		saveButton.setActionCommand(""+SAVE);
		saveButton.addActionListener(this);
		setButtonsEnabled(false);
		addButton = new JButton(ADD_NAME);
		addButton.setToolTipText(
				UIUtilities.formatToolTipText(ADD_DESCRIPTION));
		addButton.setActionCommand(""+ADD);
		addButton.addActionListener(this);
		addAllButton = new JButton(ADDALL_NAME);
		addAllButton.setToolTipText(
				UIUtilities.formatToolTipText(ADDALL_DESCRIPTION));
		addAllButton.setActionCommand(""+ADDALL);
		addAllButton.addActionListener(this);
		removeAllButton = new JButton(REMOVE_ALL_NAME);
		removeAllButton.setToolTipText(
				UIUtilities.formatToolTipText(REMOVE_ALL_DESCRIPTION));
		removeAllButton.setActionCommand(""+REMOVE_ALL);
		removeAllButton.addActionListener(this);
		addAllButton.setEnabled(false);
		removeAllButton.setEnabled(false);
		removeButton.setEnabled(false);
	}
	
	/**
	 * Sets the <code>enabled</code> flag of the {@link #saveButton}.
	 * 
	 * @param enabled The value to set.
	 */
	private void setButtonsEnabled(boolean enabled)
	{
		saveButton.setEnabled(enabled);
	}
	
	/** Builds and lays out the UI. */
	private void buildGUI()
	{
		resultsModel = new ResultsTableModel();
		resultsModel.addColumn(AnnotationDescription.ROIID_STRING);
		resultsModel.addColumn("Z");
		resultsModel.addColumn("T");
		resultsModel.addColumn("Channel");
		resultsModel.addColumn("Text");
		resultsModel.addColumn("Min");
		resultsModel.addColumn("Max");
		resultsModel.addColumn("Sum");
		resultsModel.addColumn("Mean");
		resultsModel.addColumn("stdDev");
		results = new JTable(resultsModel);
		results.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				onFigureSelected();
			}
		});
		JPanel centrePanel = new JPanel();
		centrePanel.setLayout(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane(results);
		centrePanel.add(scrollPane, BorderLayout.CENTER);
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new FlowLayout());
		//bottomPanel.add(addButton);
		bottomPanel.add(addAllButton);
		bottomPanel.add(removeButton);
		bottomPanel.add(removeAllButton);
		bottomPanel.add(saveButton);
		JPanel containerPanel = new JPanel();
		containerPanel.setLayout(new BorderLayout());
		containerPanel.add(centrePanel, BorderLayout.CENTER);
		containerPanel.add(bottomPanel, BorderLayout.SOUTH);
		
		setLayout(new BorderLayout());
		add(containerPanel, BorderLayout.CENTER);
	}
	
	/** 
	 * Populates the table with the data. 
	 * 
	 * @param shape The analyzed shape. 
	 */
	private void getResults(ROIShape shape)
	{
		Vector<Vector> rows = new Vector<Vector>();
		
		Iterator<String> channelIterator = channelName.values().iterator();
		channelMin = minStats.get(coord);
		channelMax = maxStats.get(coord);
		channelMean = meanStats.get(coord);
		channelStdDev = stdDevStats.get(coord);
		channelSum = sumStats.get(coord);	
		String cName;
		int channel;
		Vector rowData;
		while (channelIterator.hasNext())
		{
			cName = channelIterator.next();
			channel = nameMap.get(cName);
			rowData = new Vector();
			rowData.add(shape.getID());
			rowData.add(shape.getCoord3D().getZSection()+1);
			rowData.add(shape.getCoord3D().getTimePoint()+1);
			rowData.add(cName);
			rowData.add(MeasurementAttributes.TEXT.get(shape.getFigure()));
			rowData.add(channelMin.get(channel));
			rowData.add(channelMax.get(channel));
			rowData.add(channelSum.get(channel));
			rowData.add(channelMean.get(channel));
			rowData.add(channelStdDev.get(channel));
			rows.add(rowData);
		}
		for (Vector data : rows) {
			resultsModel.addRow(data);
		}
		results.repaint();
	}
	
	/** Saves the results of the table to an Excel file. */
	private void saveResults()
	{
		FileChooser chooser = view.createSaveToExcelChooser();
		int option = chooser.showDialog();
		if (option != JFileChooser.APPROVE_OPTION) return;
		File  file = chooser.getFormattedSelectedFile();
		try
		{
			String filename = file.getAbsolutePath();
			ExcelWriter writer = new ExcelWriter(filename);
			writer.openFile();
			writer.createSheet("Intensity Results");
			writer.writeTableToSheet(0, 0, resultsModel);
			BufferedImage originalImage = model.getRenderedImage();
			BufferedImage image = Factory.copyBufferedImage(originalImage);
			// Add the ROI for the current plane to the image.
			//TODO: Need to check that.
			model.setAttributes(MeasurementAttributes.SHOWID, true);
			model.setAttributes(MeasurementAttributes.SHOWID, false);
			try {
				if (image != null) {
					model.getDrawingView().print(image.getGraphics());
					writer.addImageToWorkbook("ThumbnailImage", image); 
					int col = writer.getMaxColumn(0);
					writer.writeImage(0, col+1, 256, 256,	"ThumbnailImage");
				}
			} catch (Exception e) {
				//no image available
			}
			writer.close();
		
		} catch (IOException e) {
			Logger logger = MeasurementAgent.getRegistry().getLogger();
			logger.error(this, "Cannot save ROI results: "+e.toString());
			
			UserNotifier un = MeasurementAgent.getRegistry().getUserNotifier();
			un.notifyInfo("Save Results", "An error occurred while trying to" +
				" save the data.\nPlease try again.");
			return;
		}
		Registry reg = MeasurementAgent.getRegistry();
		UserNotifier un = reg.getUserNotifier();
		un.notifyInfo("Save ROI results", "The ROI results have been " +
											"successfully saved.");
	}
	
	/** Removes the selected results from the table. */
	private void removeResults()
	{
		int [] rows = results.getSelectedRows();
		for (int i = rows.length-1 ; i >= 0 ; i--) {
			remove.add((Long) resultsModel.getValueAt(rows[i], 0));
			resultsModel.removeRow(rows[i]);
		}
		setButtonsEnabled(results.getRowCount() > 0);
	}
	
	/**
	 * Check to see if the selected figure contains textFigure
	 * @param selectedFigures see above.
	 * @return see above.
	 */
	private boolean validFigures(Set<Figure> selectedFigures)
	{
		if (selectedFigures == null || selectedFigures.size() == 0)
			return false;
		for (Figure figure : selectedFigures)
			if (figure instanceof MeasureTextFigure)
				return false;
		return true;
	}
	
	/**
	 * Adds the statistics from the selected ROI to the table.
	 */
	private void addResults()
	{
		Set<Figure> selectedFigures = 
			view.getDrawingView().getSelectedFigures();
		if (!validFigures(selectedFigures))
				return;
		if (selectedFigures.size() == 0 || state == State.ANALYSING) return;
		state = State.ANALYSING;
		List<ROIShape> shapeList = new ArrayList<ROIShape>();
		Iterator<Figure> iterator =  selectedFigures.iterator();
		ROIFigure fig;
		Map map = model.getAnalysisResults();
		Collection shapes = new HashSet();
		if (map != null) shapes = map.keySet();
		ROIShape shape;
		while (iterator.hasNext()) {
			fig = (ROIFigure) iterator.next();
			shape = fig.getROIShape();
			if (shapes.contains(shape)) {
				if (remove.contains(shape.getID())) {
					removeShape(shape.getID());
					shapeList.add(shape);
				}
			} else shapeList.add(shape);
		}
		if (shapeList.size() > 0) {
			view.calculateStats(shapeList);
			onFigureSelected();
		}
		state = State.READY;
	}
	
	/**
	 * Removes the shape from the table.
	 * 
	 * @param shapeID The identifier of the shape.
	 */
	private void removeShape(long shapeID)
	{
		remove.remove(shapeID);
		List<Integer> indexes = new ArrayList<Integer>();
		long id;
		for (int i = 0; i < resultsModel.getRowCount(); i++) {
			id = (Long) resultsModel.getValueAt(i, 0);
			if (id == shapeID)
				indexes.add(i);
		}
		Iterator<Integer> j = indexes.iterator();
		while (j.hasNext()) {
			resultsModel.removeRow(j.next());
		}
	}
	
	/**
	 * Adds the statistics from the ROIShapes of the select ROI to the table.
	 */
	private void addAllResults()
	{
		Set<Figure> selectedFigures = 
			view.getDrawingView().getSelectedFigures();
		if (selectedFigures.size() == 0 || state == State.ANALYSING) return;
		state = State.ANALYSING;
		List<ROIShape> shapeList = new ArrayList<ROIShape>();
		
		Iterator<Figure> i =  selectedFigures.iterator();
		ROIFigure fig;
		TreeMap<Coord3D, ROIShape> treeMap;
		Iterator<Coord3D> j;
		ROIShape shape;
		Map map = model.getAnalysisResults();
		Collection shapes = new HashSet();
		if (map != null) shapes = map.keySet();
		while (i.hasNext()) {
			fig = (ROIFigure) i.next();
			if (!(fig instanceof MeasureTextFigure)) {
				treeMap = fig.getROI().getShapes();
				j = treeMap.keySet().iterator();
				while (j.hasNext()) {
					shape = treeMap.get(j.next());
					if (!shapes.contains(shape))
						shapeList.add(shape);		
				}
			}
		}
		if (shapeList.size() > 0) {
			view.calculateStats(shapeList);
		}
		removeAllButton.setEnabled(true);
		state = State.READY;
	}
	
	/**
	 * Creates a new instance.
	 * 
	 * @param view		 Reference to the View. Mustn't be <code>null</code>.
	 * @param model		 Reference to the Model. Mustn't be <code>null</code>.
	 */
	IntensityResultsView(MeasurementViewerUI view, MeasurementViewerModel model)
	{
		if (view == null)
			throw new IllegalArgumentException("No view.");
		if (model == null)
			throw new IllegalArgumentException("No model.");
		this.view = view;
		this.model = model;
		initComponents();
		buildGUI();
	}
	
	/**
	 * Returns the name of the component.
	 * 
	 * @return See above.
	 */
	String getComponentName() { return NAME; }
	
	/**
	 * Returns the icon of the component.
	 * 
	 * @return See above.
	 */
	Icon getComponentIcon()
	{
		IconManager icons = IconManager.getInstance();
		return icons.getIcon(IconManager.INTENSITYVIEW);
	}
	
	/**
	 * Get the analysis results from the model and convert to the 
	 * necessary array. data types using the ROIStats wrapper then
	 * create the appropriate table data and summary statistics.  
	 */
	void displayAnalysisResults()
	{
		this.ROIStats = model.getAnalysisResults();
		if (ROIStats == null || ROIStats.size() == 0) return;
		
		shapeMap = new TreeMap<Coord3D, ROIShape>(new Coord3D());
		minStats = new TreeMap<Coord3D, Map<Integer, Double>>(new Coord3D());
		maxStats = new TreeMap<Coord3D, Map<Integer, Double>>(new Coord3D());
		meanStats = new TreeMap<Coord3D, Map<Integer, Double>>(new Coord3D());
		sumStats = new TreeMap<Coord3D, Map<Integer, Double>>(new Coord3D());
		stdDevStats = new TreeMap<Coord3D, Map<Integer, Double>>(new Coord3D());
		
		Entry entry;
		Iterator j  = ROIStats.entrySet().iterator();
		channelName = new TreeMap<Integer, String>();
		nameMap = new HashMap<String, Integer>();
		Map<StatsType, Map> shapeStats;
		Coord3D c3D;
		ChannelData channelData;
		int channel;
		List<ChannelData> metadata = model.getMetadata();
		Iterator<ChannelData> i;
		while (j.hasNext())
		{
			entry = (Entry) j.next();
			shape = (ROIShape) entry.getKey();
			//shapeMap.put(shape.getCoord3D(), shape);
			if (shape.getFigure() instanceof MeasureTextFigure)
			{
				state = State.READY;
				return;
			}
			c3D = shape.getCoord3D();
			shapeStats = AnalysisStatsWrapper.convertStats(
					(Map) entry.getValue());
				
			minStats.put(c3D, shapeStats.get(StatsType.MIN));
			maxStats.put(c3D, shapeStats.get(StatsType.MAX));
			meanStats.put(c3D, shapeStats.get(StatsType.MEAN));
			sumStats.put(c3D, shapeStats.get(StatsType.SUM));
			stdDevStats.put(c3D, shapeStats.get(StatsType.STDDEV));
			
			channelName.clear();
			nameMap.clear();
			channelColour.clear();

			i = metadata.iterator();
			while (i.hasNext()) {
				channelData = i.next();
				channel = channelData.getIndex();
				if (model.isChannelActive(channel)) 
				{
					channelName.put(channel, channelData.getChannelLabeling());
					nameMap.put(channelName.get(channel), channel);
					channelColour.put(channel, 
						(Color) model.getActiveChannels().get(channel));
				}
			}
			
			if (channelName.size() == 0 || nameMap.size() == 0 || 
				channelColour.size() == 0)
			{
				state = State.READY;
				return;
			}
		
			coord = c3D;
			getResults(shape);
		}

		setButtonsEnabled(true);
		state = State.READY;
	}

	/** Invokes when figures are selected. */
	void onFigureSelected()
	{
		Set<Figure> selectedFigures = 
			view.getDrawingView().getSelectedFigures();
		boolean valid = validFigures(selectedFigures);
		addButton.setEnabled(valid);
		addAllButton.setEnabled(valid);// && selectedFigures.size() == 1);
		if (results != null) {
			int count = results.getRowCount();
			int[] rows = results.getSelectedRows();
			removeButton.setEnabled(rows != null && rows.length > 0);
			removeAllButton.setEnabled(count > 0);
		} else {
			removeButton.setEnabled(false);
			removeAllButton.setEnabled(false);
		}
	}
	
	/** Removes the results from the table. */
	void removeAllResults()
	{
		int count = results.getRowCount();
		for (int i = count-1 ; i >= 0 ; i--)
			resultsModel.removeRow(i);
		model.setAnalysisResults(null);
		remove.clear();
		setButtonsEnabled(false);
		onFigureSelected();
	}
	
	/**
	 * Listens to the controls.
	 * @see ActionListener#actionPerformed(ActionEvent)
	 * 
	 */
	public void actionPerformed(ActionEvent e)
	{
		int index = Integer.parseInt(e.getActionCommand());
		switch (index) {
			case ADD:
				addResults();
				break;
			case ADDALL:
				addAllResults();
				break;
			case SAVE:
				saveResults();	
				break;
			case REMOVE:
				removeResults();
				break;
			case REMOVE_ALL:
				removeAllResults();
		}
	}

	/** 
	 * The table model for the results table, only overridden to make it read 
	 * only.
	 */
	class ResultsTableModel
		extends DefaultTableModel
	{
		
		/**
		 * Overridden to make sure that the cell cannot be edited.s
		 * @see DefaultTableModel#isCellEditable(int, int)
		 */
		public boolean isCellEditable(int row, int col) { return false; }
	}
	
}

