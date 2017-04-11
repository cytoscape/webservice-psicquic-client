package org.cytoscape.webservice.psicquic.task;

import java.util.HashMap;
import java.util.Map;

import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.webservice.psicquic.PSICQUICRestClient;
import org.cytoscape.webservice.psicquic.RegistryManager;
import org.cytoscape.webservice.psicquic.mapper.CyNetworkBuilder;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

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
 * Add new edges to the existing network
 */
public class ProcessSearchResultTask extends AbstractTask {
	
	private final PSICQUICRestClient client;
	private final String query;
	private final SearchRecordsTask searchTask;

	private final CyNetworkView netView;
	private final View<CyNode> nodeView;

	private volatile boolean canceled = false;

	private final RegistryManager registryManager;
	private final CyNetworkBuilder builder;

	private final CyServiceRegistrar serviceRegistrar;

	public ProcessSearchResultTask(
			final String query,
			final PSICQUICRestClient client,
			final SearchRecordsTask searchTask,
			final CyNetworkView parentNetworkView,
			final View<CyNode> nodeView,
			final RegistryManager registryManager,
			final CyNetworkBuilder builder,
			final CyServiceRegistrar serviceRegistrar
	) {
		this.client = client;
		this.query = query;
		this.netView = parentNetworkView;
		this.nodeView = nodeView;
		this.searchTask = searchTask;
		this.registryManager = registryManager;
		this.builder = builder;
		this.serviceRegistrar = serviceRegistrar;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setTitle("Expanding by PSICQUIC Services");
		taskMonitor.setStatusMessage("Loading interaction from remote service...");
		taskMonitor.setProgress(0.01d);
		
		if (canceled)
			return;
		
		Map<String, String> result = processSearchResult();
		insertTasksAfterCurrentTask(
				new ExpandFromSelectedSourcesTask(query, client, result, netView, nodeView, builder, serviceRegistrar));
		taskMonitor.setProgress(1.0d);
	}
	
	@Override
	public void cancel() {
		this.canceled = true;
		client.cancel();
	}

	private final Map<String, String> processSearchResult() {
		final Map<String, String> sourceMap = new HashMap<>();
		final Map<String, Long> rs = searchTask.getResult();
		
		for (final String sourceURL : rs.keySet()) {
			final Long interactionCount = rs.get(sourceURL);
			if (interactionCount <= 0)
				continue;

			sourceMap.put(registryManager.getSource2NameMap().get(sourceURL) + ": " + interactionCount, sourceURL);
		}
		
		return sourceMap;
	}
}
