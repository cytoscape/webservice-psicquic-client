package org.cytoscape.webservice.psicquic.ui;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;

import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.LookAndFeelUtil;
import org.cytoscape.webservice.psicquic.PSICQUICRestClient;
import org.cytoscape.webservice.psicquic.PSICQUICRestClient.SearchMode;
import org.cytoscape.webservice.psicquic.PSIMI25VisualStyleBuilder;
import org.cytoscape.webservice.psicquic.RegistryManager;
import org.cytoscape.webservice.psicquic.task.SearchRecordsTask;
import org.cytoscape.webservice.psicquic.ui.SelectorBuilder.Species;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.swing.DialogTaskManager;

/*
 * #%L
 * Cytoscape PSIQUIC Web Service Impl (webservice-psicquic-client-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2017 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

/**
 * Custom Search UI for PSICQUIC services.
 */
public class PSICQUICSearchUI extends JPanel {

	private static final long serialVersionUID = 3163269742016489767L;

	private static final Dimension PANEL_SIZE = new Dimension(680, 500);

	// Property name for saving selection
	static final String PROP_NAME = "psiqcuic.datasource.selection";

	// Fixed messages
	private static final String MIQL_MODE = "Search by Query Language (MIQL)";
	private static final String INTERACTOR_ID_LIST = "Search by ID (gene/protein/compound ID)";
	private static final String BY_SPECIES = "Import Interactome (this may take long time)";

	private final RegistryManager regManager;
	private final PSICQUICRestClient client;
	
	private JEditorPane queryArea;
	private SourceStatusPanel statusPanel;
	private JScrollPane queryScrollPane;

	private JButton searchButton;

	private JPanel speciesPanel;

	private JComboBox<String> searchModeSelector;
	private JComboBox<Species> speciesSelector;

	private JPanel searchConditionPanel;
	private JPanel buttonPanel;
	
	private JButton cancelButton;
	private JButton importButton;

	private SearchMode mode = SearchMode.MIQL;

	private boolean firstClick = true;
	private boolean defKeyStrokesAdded;

	private Set<String> sourceSet = new HashSet<>();
	
	private final PSIMI25VisualStyleBuilder vsBuilder;
	private final PSIMITagManager tagManager;
	
	private final CyServiceRegistrar serviceRegistrar;

	@SuppressWarnings("unchecked")
	public PSICQUICSearchUI(
			final RegistryManager regManager,
			final PSICQUICRestClient client,
			final PSIMI25VisualStyleBuilder vsBuilder,
			final PSIMITagManager tagManager,
			final CyServiceRegistrar serviceRegistrar
	) {
		this.regManager = regManager;
		this.client = client;
		this.vsBuilder = vsBuilder;
		this.tagManager = tagManager;
		this.serviceRegistrar = serviceRegistrar;
		
		// Load Property if available.
		CyProperty<Properties> cyProp = 
				serviceRegistrar.getService(CyProperty.class, "(cyPropertyName=cytoscape3.props)");
		final Properties props = cyProp.getProperties();
		final String selectionListProp = props.getProperty(PROP_NAME);
		
		if (selectionListProp == null) // Create new one if there is no defaults.
			props.setProperty(PROP_NAME, "");
		else
			setSelected(selectionListProp);

		init();

		this.addAncestorListener(new AncestorListener() {
			@Override
			public void ancestorAdded(AncestorEvent ae) {
				// Set focus to query area.
				getQueryArea().requestFocus();
				
				// Set ESC/ENTER default key strokes
				if (getRootPane() != null) {
					getRootPane().setDefaultButton(getImportButton());
					
					if (!defKeyStrokesAdded) {
						LookAndFeelUtil.setDefaultOkCancelKeyStrokes(getRootPane(), getImportButton().getAction(),
								getCancelButton().getAction());
					}
				}
			}
			@Override
			public void ancestorRemoved(AncestorEvent ae) {}
			@Override
			public void ancestorMoved(AncestorEvent ae) {}
		});
	}

	private void init() {
		// Background (Base Panel) settings
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		createDBlistPanel();
		queryModeChanged();

		setSize(PANEL_SIZE);
		setPreferredSize(PANEL_SIZE);

		getQueryArea().addCaretListener(evt -> {
			getSearchButton().setEnabled(!getQueryArea().getText().isEmpty());
		});

		add(getSearchConditionPanel());
		add(statusPanel);
		add(getButtonPanel());
	}
	
	private JPanel getSearchConditionPanel() {
		if (searchConditionPanel == null) {
			searchConditionPanel = new JPanel();
			searchConditionPanel.setBorder(LookAndFeelUtil.createTitledBorder("1. Enter Search Conditions"));
			
			final JLabel modeLabel = new JLabel("Search Mode:");
			
			final GroupLayout layout = new GroupLayout(searchConditionPanel);
			searchConditionPanel.setLayout(layout);
			layout.setAutoCreateContainerGaps(true);
			layout.setAutoCreateGaps(true);
			
			layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER, true)
					.addGroup(Alignment.LEADING, layout.createSequentialGroup()
							.addComponent(modeLabel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
							.addComponent(getSearchModeSelector(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					)
					.addComponent(getQueryScrollPane(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(getSpeciesPanel(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(getSearchButton(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
			);
			layout.setVerticalGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(Alignment.CENTER, true)
							.addComponent(modeLabel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
							.addComponent(getSearchModeSelector(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					)
					.addComponent(getQueryScrollPane(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(getSpeciesPanel(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					.addComponent(getSearchButton(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
			);
		}
		
		return searchConditionPanel;
	}
	
	private JScrollPane getQueryScrollPane() {
		if (queryScrollPane == null) {
			queryScrollPane = new JScrollPane();
			queryScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			queryScrollPane.setPreferredSize(new Dimension(500, 150));
			queryScrollPane.setViewportView(getQueryArea());
		}
		
		return queryScrollPane;
	}
	
	private JEditorPane getQueryArea() {
		if (queryArea == null) {
			queryArea = new JEditorPane();
			queryArea.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent arg0) {
					if (firstClick) {
						firstClick = false;
						getSearchButton().setEnabled(true);
					}
				}
			});
		}
		
		return queryArea;
	}
	
	private JPanel getSpeciesPanel() {
		if (speciesPanel == null) {
			speciesPanel = new JPanel();
			
			if (LookAndFeelUtil.isAquaLAF())
				speciesPanel.setOpaque(false);
			
			final JLabel speciesLabel = new JLabel("Select Species:");
			final SelectorBuilder speciesBuilder = new SelectorBuilder();
			speciesSelector = speciesBuilder.getComboBox();
			getSearchButton().setEnabled(true);
			
			final GroupLayout layout = new GroupLayout(speciesPanel);
			speciesPanel.setLayout(layout);
			layout.setAutoCreateContainerGaps(false);
			layout.setAutoCreateGaps(true);
			
			layout.setHorizontalGroup(layout.createSequentialGroup()
					.addComponent(speciesLabel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					.addComponent(speciesSelector, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
			);
			layout.setVerticalGroup(layout.createParallelGroup(Alignment.CENTER, false)
					.addComponent(speciesLabel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					.addComponent(speciesSelector, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
			);
			
			speciesSelector.addActionListener(evt -> getSearchButton().setEnabled(true) );
		}
		
		return speciesPanel;
	}
	
	private JPanel getButtonPanel() {
		if (buttonPanel == null) {
			buttonPanel = LookAndFeelUtil.createOkCancelPanel(getImportButton(), getCancelButton());
		}
		
		return buttonPanel;
	}
	
	@SuppressWarnings("serial")
	private JButton getImportButton() {
		if (importButton == null) {
			importButton = new JButton(new AbstractAction("Import") {
				@Override
				public void actionPerformed(ActionEvent e) {
					statusPanel.doImport();
				}
			});
		}
		
		return importButton;
	}
	
	@SuppressWarnings("serial")
	private JButton getCancelButton() {
		if (cancelButton == null) {
			cancelButton = new JButton(new AbstractAction("Cancel") {
				@Override
				public void actionPerformed(ActionEvent e) {
					((Window)getRootPane().getParent()).dispose();
				}
			});
		}
		
		return cancelButton;
	}
	
	private JComboBox<String> getSearchModeSelector() {
		if (searchModeSelector == null) {
			searchModeSelector = new JComboBox<>();
			searchModeSelector.addItem(BY_SPECIES);
			searchModeSelector.addItem(INTERACTOR_ID_LIST);
			searchModeSelector.addItem(MIQL_MODE);
			searchModeSelector.setSelectedItem(INTERACTOR_ID_LIST);

			searchModeSelector.addActionListener(evt -> queryModeChanged() );
		}
		
		return searchModeSelector;
	}
	
	private JButton getSearchButton() {
		if (searchButton == null) {
			searchButton = new JButton("Search");
			searchButton.addActionListener(evt -> {
				search();
				enableComponents(true);
			});
			searchButton.setEnabled(false);
		}
		
		return searchButton;
	}
	
	private final void setSelected(final String selected) {
		final String[] sources = selected.split(",");
		
		for (String source:sources)
			sourceSet.add(source);
	}

	public final void setSelected() {
		this.sourceSet = statusPanel.getSelected();
	}

	private void enableComponents(final boolean enable) {
		statusPanel.enableComponents(enable);
		getImportButton().getAction().setEnabled(enable);
	}
	
	private final void createDBlistPanel() {
		// Source Status - list of remote databases
		statusPanel = new SourceStatusPanel("", client, regManager, null, mode, vsBuilder, tagManager,
				serviceRegistrar);
		enableComponents(false);
		statusPanel.setSelected(sourceSet);
	}

	private void search() {
		final SearchRecordsTask searchTask = new SearchRecordsTask(client, mode);
		final Map<String, String> activeSource = regManager.getActiveServices();
		String query = getQueryArea().getText();

		// Query by species
		if (mode == SearchMode.SPECIES)
			query = buildSpeciesQuery();

		this.setSelected();
		statusPanel.setQuery(query);
		searchTask.setQuery(query);
		searchTask.setTargets(activeSource.values());

		DialogTaskManager taskManager = serviceRegistrar.getService(DialogTaskManager.class);
		taskManager.execute(new TaskIterator(searchTask, new SetTableTask(searchTask)));
	}

	private final String buildSpeciesQuery() {
		mode = SearchMode.SPECIES;
		final Object selectedItem = this.speciesSelector.getSelectedItem();
		final Species species = (Species) selectedItem;

		return "taxidA:\"" + species.toString() + "\" AND taxidB:\"" + species.toString() + "\"";
	}

	/**
	 * Update table based on returned result
	 */
	private final class SetTableTask extends AbstractTask {

		private final SearchRecordsTask searchTask;

		public SetTableTask(final SearchRecordsTask searchTask) {
			this.searchTask = searchTask;
		}

		@Override
		public void run(TaskMonitor taskMonitor) throws Exception {
			final Map<String, Long> result = searchTask.getResult();

			String query;
			// Query by species
			if (mode == SearchMode.SPECIES)
				query = buildSpeciesQuery();
			else
				query = getQueryArea().getText();

			statusPanel = new SourceStatusPanel(query, client, regManager, result, mode, vsBuilder, tagManager,
					serviceRegistrar);
			statusPanel.sort();
			updateGUILayout();
			enableComponents(true);
			statusPanel.setSelected(sourceSet);
		}
	}

	public final void updateGUILayout() {
		removeAll();

		if (mode == SearchMode.SPECIES) {
			getQueryScrollPane().setVisible(false);
			getSpeciesPanel().setVisible(true);
			getSearchButton().setEnabled(true);
		} else {
			getQueryScrollPane().setVisible(true);
			getSpeciesPanel().setVisible(false);
		}
		
		statusPanel.setBorder(LookAndFeelUtil.createTitledBorder("2. Select Databases"));
		
		// Add to main panel
		this.add(getSearchConditionPanel());
		this.add(statusPanel);
		this.add(getButtonPanel());

		if (getRootPane() != null) {
			Window parentWindow = ((Window) getRootPane().getParent());
			parentWindow.pack();
			repaint();
			parentWindow.toFront();
		}
	}

	private final void queryModeChanged() {
		final Object selectedObject = getSearchModeSelector().getSelectedItem();
		
		if (selectedObject == null)
			return;

		final String modeString = selectedObject.toString();
		final String query;
		
		if (modeString.equals(MIQL_MODE)) {
			mode = SearchMode.MIQL;
			query = getQueryArea().getText();
			getSearchButton().setEnabled(false);
			getQueryArea().requestFocus();
		} else if (modeString.equals(INTERACTOR_ID_LIST)) {
			mode = SearchMode.INTERACTOR;
			query = getQueryArea().getText();
			getSearchButton().setEnabled(false);
			getQueryArea().requestFocus();
		} else {
			mode = SearchMode.SPECIES;
			query = buildSpeciesQuery();
			getSearchButton().setEnabled(true);
		}

		firstClick = true;

		getQueryArea().setText("");
		statusPanel = new SourceStatusPanel(query, client, regManager, null, mode, vsBuilder, tagManager,
				serviceRegistrar);
		statusPanel.sort();

		updateGUILayout();
		enableComponents(false);
		statusPanel.setSelected(sourceSet);
	}
}