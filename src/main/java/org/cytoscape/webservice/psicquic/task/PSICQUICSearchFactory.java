package org.cytoscape.webservice.psicquic.task;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.GroupLayout.Alignment.CENTER;

import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingUtilities;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.search.AbstractNetworkSearchTaskFactory;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.LookAndFeelUtil;
import org.cytoscape.webservice.psicquic.PSICQUICRestClient.SearchMode;
import org.cytoscape.webservice.psicquic.PSICQUICWebServiceClient;
import org.cytoscape.webservice.psicquic.PSIMI25VisualStyleBuilder;
import org.cytoscape.webservice.psicquic.ui.PSIMITagManager;
import org.cytoscape.webservice.psicquic.ui.SourceStatusPanel;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;

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

public class PSICQUICSearchFactory extends AbstractNetworkSearchTaskFactory {

	public static String ID = "org.cytoscape.PSICQUIC";
	public static String NAME = "PSICQUIC";
	public static String DESCRIPTION = "Search PSICQUIC-compliant databases.";
	public static String WEBSITE_URL = "http://psicquic.github.io/";
	
	private static final Icon icon = new ImageIcon(
			PSICQUICSearchFactory.class.getResource("/images/psicquic-logo-32.png"));
	private URL url;
	
	private final SearchTaskObserver taskObserver;
	
	private final PSICQUICWebServiceClient client;
	private final PSIMI25VisualStyleBuilder vsBuilder;
	private final PSIMITagManager tagManager;
	private final CyServiceRegistrar serviceRegistrar;
	
	public PSICQUICSearchFactory(
			PSICQUICWebServiceClient client,
			PSIMI25VisualStyleBuilder vsBuilder,
			PSIMITagManager tagManager, 
			CyServiceRegistrar serviceRegistrar
	) {
		super(ID, NAME, DESCRIPTION, icon);
		
		try {
			url = new URL(WEBSITE_URL);
		} catch (MalformedURLException e) {
		}
		
		this.client = client;
		this.vsBuilder = vsBuilder;
		this.tagManager = tagManager;
		this.serviceRegistrar = serviceRegistrar;
		
		taskObserver = new SearchTaskObserver();
	}

	@Override
	public URL getWebsite() {
		return url;
	}
	
	@Override
	public TaskObserver getTaskObserver() {
		return taskObserver;
	}
	
	@Override
	public TaskIterator createTaskIterator() {
		String query = getQuery();
		
		if (query == null) {
			throw new NullPointerException("Query string is null.");
		} else {
			final SearchRecordsTask searchTask = new SearchRecordsTask(client.getRestClient(), SearchMode.INTERACTOR);
			searchTask.setQuery(query);
			
			final Map<String, String> activeSource = client.getRegistryManager().getActiveServices();
			searchTask.setTargets(activeSource.values());

			taskObserver.setTask(searchTask);
			
			return new TaskIterator(searchTask);
		}
	}
	
	private class SearchTaskObserver implements TaskObserver {

		private SearchRecordsTask task;
		
		public void setTask(SearchRecordsTask task) {
			this.task = task;
		}
		
		@Override
		public void taskFinished(ObservableTask task) {
		}

		@Override
		@SuppressWarnings("serial")
		public void allFinished(FinishStatus finishStatus) {
			final Map<String, Long> result = task.getResult();

			SwingUtilities.invokeLater(() -> {
				SourceStatusPanel statusPanel = new SourceStatusPanel(task.getQuery(), client.getRestClient(),
						client.getRegistryManager(), result, SearchMode.INTERACTOR, vsBuilder, tagManager,
						serviceRegistrar);
				statusPanel.setBorder(LookAndFeelUtil.createPanelBorder());
				statusPanel.sort();
				statusPanel.enableComponents(true);
				// TODO
//				importButton.getAction().setEnabled(enable);
				statusPanel.setSelected(result.keySet());
				statusPanel.setPreferredSize(new Dimension(680, 360));
				
				JButton cancelButton = new JButton(new AbstractAction("Cancel") {
					@Override
					public void actionPerformed(ActionEvent e) {
						((Window)statusPanel.getRootPane().getParent()).dispose();
					}
				});
				JButton importButton = new JButton(new AbstractAction("Import") {
					@Override
					public void actionPerformed(ActionEvent e) {
						statusPanel.doImport();
					}
				});
				JPanel buttonPanel = LookAndFeelUtil.createOkCancelPanel(importButton, cancelButton);
				
				JFrame parent = serviceRegistrar.getService(CySwingApplication.class).getJFrame();
				
				JDialog dialog = new JDialog(parent, "Select Databases", ModalityType.APPLICATION_MODAL);
				
				final GroupLayout layout = new GroupLayout(dialog.getContentPane());
				dialog.getContentPane().setLayout(layout);
				layout.setAutoCreateContainerGaps(true);
				layout.setAutoCreateGaps(true);
				
				layout.setHorizontalGroup(layout.createParallelGroup(CENTER, true)
						.addComponent(statusPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(buttonPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
				);
				layout.setVerticalGroup(layout.createSequentialGroup()
						.addComponent(statusPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addComponent(buttonPanel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
				);
				
				dialog.getRootPane().setDefaultButton(importButton);
				LookAndFeelUtil.setDefaultOkCancelKeyStrokes(dialog.getRootPane(), importButton.getAction(),
						cancelButton.getAction());
				
				dialog.pack();
				dialog.setLocationRelativeTo(parent);
				dialog.setVisible(true);
			});
		}
	}
}
