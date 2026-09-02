package org.cytoscape.webservice.psicquic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.webservice.psicquic.mapper.CyNetworkBuilder;
import org.junit.Test;

import psidev.psi.mi.tab.PsimiTabReader;
import psidev.psi.mi.tab.model.BinaryInteraction;
import uk.ac.ebi.enfin.mi.cluster.InteractionCluster;

/**
 * Exercises the "Automatic Network Merge" path (PSIMITAB parsing, MiCluster
 * merging and network building) against the libraries this bundle embeds.
 * Regression test for https://github.com/cytoscape/webservice-psicquic-client/issues/5
 */
public class MergedImportTest {

	private final NetworkTestSupport support = new NetworkTestSupport();

	@Test
	public void mergeResultsFromTwoServicesIntoOneNetwork() throws Exception {
		// Same steps as PSICQUICRestClient.ImportNetworkAsMitabTask + importMerged()
		final List<BinaryInteraction> all = new ArrayList<>();
		for (String service : new String[] { "intact", "mint" }) {
			try (InputStream in = getClass().getResourceAsStream("/mitab/" + service + ".mitab")) {
				all.addAll(new PsimiTabReader().read(in));
			}
		}
		assertEquals(24, all.size());

		final InteractionCluster iC = new InteractionCluster();
		iC.setBinaryInteractionIterator(all.iterator());
		iC.setMappingIdDbNames("uniprotkb,chebi,ddbj/embl/genbank,ensembl,irefindex");
		iC.runService();

		final int clusters = iC.getInteractionMapping().size();
		assertFalse(iC.getInteractionMapping().isEmpty());
		assertTrue("duplicate interactions across services should be merged", clusters < all.size());

		// Same as PSICQUICRestClient.importMergedNetwork()
		final CyServiceRegistrar registrar = mock(CyServiceRegistrar.class);
		when(registrar.getService(CyNetworkFactory.class)).thenReturn(support.getNetworkFactory());
		final CyNetwork network = new CyNetworkBuilder(registrar).buildNetwork(iC);

		assertTrue(network.getNodeCount() > 0);
		assertEquals(clusters, network.getEdgeCount());
	}
}
