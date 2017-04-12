package org.cytoscape.webservice.psicquic;

import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.NODE_APPS_MENU;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.util.Properties;

import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.webservice.psicquic.mapper.CyNetworkBuilder;
import org.cytoscape.webservice.psicquic.task.ExpandNodeContextMenuFactory;
import org.cytoscape.webservice.psicquic.task.PSICQUICSearchFactory;
import org.cytoscape.webservice.psicquic.ui.PSIMITagManager;
import org.osgi.framework.BundleContext;

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

public class CyActivator extends AbstractCyActivator {
	
	private static final String WEB_SERVICE_URL = "http://www.ebi.ac.uk/Tools/webservices/psicquic/registry/registry";
	private static final String MIQL_URL = "http://psicquic.github.io/MiqlDefinition.html";
	private static final String REGISTRY_URL = "http://www.ebi.ac.uk/Tools/webservices/psicquic/registry/registry?action=STATUS";
	
	private static final String CLIENT_DISCRIPTION = 
			"<p>This is a web service client for <a href=\"" + PSICQUICSearchFactory.WEBSITE_URL + "\">PSICQUIC</a>-compliant databases.</p>" +
			"<ul><li><a href=\"" + MIQL_URL + "\">Query language (MIQL) Syntax</a></li>" +
			"<li><a href=\"" + REGISTRY_URL + "\">List of Supported Databases</a></li></ul>";
	
	@Override
	public void start(BundleContext bc) {
		CyServiceRegistrar serviceRegistrar = getService(bc, CyServiceRegistrar.class);

		PSIMITagManager tagManager = new PSIMITagManager();
		PSIMI25VisualStyleBuilder vsBuilder = new PSIMI25VisualStyleBuilder(serviceRegistrar);
		CyNetworkBuilder builder = new CyNetworkBuilder(serviceRegistrar);

		PSICQUICWebServiceClient psicquicClient = new PSICQUICWebServiceClient(
				WEB_SERVICE_URL,
				"Universal Interaction Database Client",
				CLIENT_DISCRIPTION, 
				builder, vsBuilder, tagManager,
				serviceRegistrar);
		{
			Properties props = new Properties();
			props.put("id", "PSICQUICWebServiceClient");
			registerAllServices(bc, psicquicClient, props);
		}
		{
			ExpandNodeContextMenuFactory factory = new ExpandNodeContextMenuFactory(
					psicquicClient.getRestClient(), psicquicClient.getRegistryManager(), builder, serviceRegistrar);
			Properties props = new Properties();
			props.setProperty("preferredTaskManager", "menu");
			props.setProperty(PREFERRED_MENU, NODE_APPS_MENU);
			props.setProperty(MENU_GRAVITY, "10.0");
			props.setProperty(TITLE, "Extend Network by public interaction database...");
			registerService(bc, factory, NodeViewTaskFactory.class, props);
		}
		{
			PSICQUICSearchFactory factory = new PSICQUICSearchFactory(psicquicClient, vsBuilder, tagManager, serviceRegistrar);
			registerAllServices(bc, factory);
		}
	}
}
