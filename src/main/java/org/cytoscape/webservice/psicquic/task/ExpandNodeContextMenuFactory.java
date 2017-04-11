package org.cytoscape.webservice.psicquic.task;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractNodeViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.webservice.psicquic.PSICQUICRestClient;
import org.cytoscape.webservice.psicquic.RegistryManager;
import org.cytoscape.webservice.psicquic.mapper.CyNetworkBuilder;
import org.cytoscape.work.TaskIterator;

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
 * Display "Expand" context menu.
 */
public class ExpandNodeContextMenuFactory extends AbstractNodeViewTaskFactory {

	private final RegistryManager manager;
	private final PSICQUICRestClient client;
	private final CyNetworkBuilder builder;

	private final CyServiceRegistrar serviceRegistrar;

	public ExpandNodeContextMenuFactory(
			final PSICQUICRestClient client,
			final RegistryManager manager,
			final CyNetworkBuilder builder,
			final CyServiceRegistrar serviceRegistrar
	) {
		this.client = client;
		this.manager = manager;
		this.builder = builder;
		this.serviceRegistrar = serviceRegistrar;
	}
	
	@Override
	public TaskIterator createTaskIterator(View<CyNode> nodeView, CyNetworkView netView) {
		if (manager == null)
			throw new NullPointerException("RegistryManager is null");

		// Create query
		String query = netView.getModel().getDefaultNodeTable().getRow(nodeView.getModel().getSUID())
				.get(CyNetwork.NAME, String.class);
		
		if (query == null)
			throw new NullPointerException("Query object is null.");
		else
			return new TaskIterator(new BuildQueryTask(netView, nodeView, client, manager, builder, serviceRegistrar));
	}
}
