package org.cytoscape.webservice.psicquic;

import java.util.Map;

import org.cytoscape.io.webservice.NetworkImportWebServiceClient;
import org.cytoscape.io.webservice.SearchWebServiceClient;
import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.webservice.psicquic.PSICQUICRestClient.SearchMode;
import org.cytoscape.webservice.psicquic.mapper.CyNetworkBuilder;
import org.cytoscape.webservice.psicquic.task.ImportNetworkFromPSICQUICTask;
import org.cytoscape.webservice.psicquic.task.SearchRecordsTask;
import org.cytoscape.webservice.psicquic.ui.PSICQUICSearchUI;
import org.cytoscape.webservice.psicquic.ui.PSIMITagManager;
import org.cytoscape.work.TaskIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class PSICQUICWebServiceClient extends AbstractWebServiceGUIClient implements NetworkImportWebServiceClient,
		SearchWebServiceClient {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(PSICQUICWebServiceClient.class);

	private PSICQUICRestClient client;
	private RegistryManager regManager;

	private ImportNetworkFromPSICQUICTask networkTask;
	private SearchRecordsTask searchTask;

	private final PSIMI25VisualStyleBuilder vsBuilder;
	private final PSIMITagManager tagManager;

	private final CyServiceRegistrar serviceRegistrar;

	public PSICQUICWebServiceClient(
			final String uri,
			final String displayName,
			final String description,
			final CyNetworkBuilder builder,
			final PSIMI25VisualStyleBuilder vsBuilder,
			final PSIMITagManager tagManager,
			final CyServiceRegistrar serviceRegistrar
	) {
		super(uri, displayName, description);

		this.vsBuilder = vsBuilder;
		this.tagManager = tagManager;
		this.serviceRegistrar = serviceRegistrar;
		
		regManager = new RegistryManager();
		client = new PSICQUICRestClient(regManager, builder, serviceRegistrar);
	}

	public RegistryManager getRegManager() {
		return regManager;
	}
	
	@Override
	public TaskIterator createTaskIterator(Object query) {
		if (regManager == null)
			throw new NullPointerException("RegistryManager is null");

		if (query == null) {
			throw new NullPointerException("Query object is null.");
		} else {
			searchTask = new SearchRecordsTask(client, SearchMode.MIQL);
			final Map<String, String> activeSource = regManager.getActiveServices();
			final String query2 = query.toString();
			searchTask.setQuery(query2);
			searchTask.setTargets(activeSource.values());

			networkTask = new ImportNetworkFromPSICQUICTask(query2, client, searchTask, SearchMode.MIQL, vsBuilder,
					serviceRegistrar);

			return new TaskIterator(searchTask, networkTask);
		}
	}

	@Override
	public PSICQUICSearchUI getQueryBuilderGUI() {
		return new PSICQUICSearchUI(regManager, client, vsBuilder, tagManager, serviceRegistrar);
	}

	public PSICQUICRestClient getRestClient() {
		return client;
	}

	public RegistryManager getRegistryManager() {
		return regManager;
	}
}
