package org.mqnaas.extensions.odl.capabilities.impl;

/*
 * #%L
 * MQNaaS :: ODL Capabilities
 * %%
 * Copyright (C) 2007 - 2015 Fundació Privada i2CAT, Internet i
 * 			Innovació a Catalunya
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mqnaas.client.cxf.ICXFAPIProvider;
import org.mqnaas.clientprovider.api.apiclient.IAPIClientProviderFactory;
import org.mqnaas.clientprovider.exceptions.EndpointNotFoundException;
import org.mqnaas.clientprovider.exceptions.ProviderNotFoundException;
import org.mqnaas.core.api.Endpoint;
import org.mqnaas.core.api.IAttributeStore;
import org.mqnaas.core.api.IResource;
import org.mqnaas.core.api.IResourceManagementListener;
import org.mqnaas.core.api.IRootResource;
import org.mqnaas.core.api.IRootResourceProvider;
import org.mqnaas.core.api.IServiceProvider;
import org.mqnaas.core.api.RootResourceDescriptor;
import org.mqnaas.core.api.Specification;
import org.mqnaas.core.api.Specification.Type;
import org.mqnaas.core.api.exceptions.ApplicationActivationException;
import org.mqnaas.core.api.exceptions.CapabilityNotFoundException;
import org.mqnaas.core.api.exceptions.ResourceNotFoundException;
import org.mqnaas.core.impl.AttributeStore;
import org.mqnaas.core.impl.RootResource;
import org.mqnaas.extensions.odl.client.helium.topology.api.IOpenDaylightTopologyNorthbound;
import org.mqnaas.extensions.odl.client.switchnorthbound.ISwitchNorthboundAPI;
import org.mqnaas.extensions.odl.helium.switchmanager.model.Node;
import org.mqnaas.extensions.odl.helium.switchmanager.model.NodeConnector;
import org.mqnaas.extensions.odl.helium.switchmanager.model.NodeConnectorProperties;
import org.mqnaas.extensions.odl.helium.switchmanager.model.NodeConnectors;
import org.mqnaas.extensions.odl.helium.switchmanager.model.NodeProperties;
import org.mqnaas.extensions.odl.helium.switchmanager.model.Nodes;
import org.mqnaas.extensions.odl.helium.switchmanager.model.PropertyValue;
import org.mqnaas.extensions.odl.helium.switchmanager.model.Node.NodeType;
import org.mqnaas.extensions.odl.helium.topology.model.Edge;
import org.mqnaas.extensions.odl.helium.topology.model.EdgeProperty;
import org.mqnaas.extensions.odl.helium.topology.model.Topology;
import org.mqnaas.general.test.helpers.reflection.ReflectionTestHelper;
import org.mqnaas.network.api.topology.link.ILinkAdministration;
import org.mqnaas.network.api.topology.link.ILinkManagement;
import org.mqnaas.network.api.topology.port.IPortManagement;
import org.mqnaas.network.impl.topology.link.LinkAdministration;
import org.mqnaas.network.impl.topology.link.LinkManagement;
import org.mqnaas.network.impl.topology.link.LinkResource;
import org.mqnaas.network.impl.topology.port.PortManagement;
import org.powermock.api.mockito.PowerMockito;

/**
 * <p>
 * Unitary tests for the {@link ODLRootResourceProvider} implementation
 * </p>
 * 
 * @author Adrián Roselló Rey (i2CAT)
 *
 */
public class ODLRootResourceProviderTest {

	private static final String	OFSWITCH_NODE_1_ID	= "00:00:00:00:00:00:00:01";
	private static final String	OFSWITCH_NODE_2_ID	= "00:00:00:00:00:00:00:02";
	private static final String	PE_NODE_ID			= "00:00:00:00:00:00:00:03";

	private static final String	PORT1_EXTERNAL_ID	= "0";
	private static final String	PORT2_EXTERNAL_ID	= "1";
	private static final String	PORT1_EXTERNAL_NAME	= "eth0";
	private static final String	PORT2_EXTERNAL_NAME	= "eth1";

	IRootResource				fakeOdlResource;

	IRootResourceProvider		odlIRootResourceProvider;

	Nodes						odlNodes;
	NodeConnectors				resource1NodeConnectors;
	NodeConnectors				resource2NodeConnectors;
	NodeConnectors				resource3NodeConnectors;

	Topology					odlTopology;

	Endpoint					fakeOdlEndpoint;

	IResourceManagementListener	mockedRmListener;

	IServiceProvider			mockedServiceProvider;

	CapabilityFactory			capabilityFactory;

	/**
	 * This method initialize the {@link ODLRootResourceProvider} capability, and create all required structures used on its activation method.
	 */
	@Before
	public void prepareTest() throws SecurityException, IllegalArgumentException, IllegalAccessException, InstantiationException, URISyntaxException,
			CapabilityNotFoundException, ApplicationActivationException, EndpointNotFoundException, ProviderNotFoundException {

		capabilityFactory = new CapabilityFactory();

		odlIRootResourceProvider = new ODLRootResourceProvider();

		createFakeOdlResponse();
		createFakeOdlTopologyResponse();
		mockRequiredFeatures();

		odlIRootResourceProvider.activate();

	}

	/**
	 * This method tests the {@link IRootResourceProvider#getRootResource(String)}, {@link IRootResourceProvider#getRootResources()} and the
	 * {@link IRootResourceProvider#getRootResources(Type, String, String)} implemented in the {@link ODLRootResourceProvider} class. The capability
	 * should contain 2 Openflow switches and 1 "other" resource, representing the Opendaylight devices returned by the mocked client.
	 * 
	 * @throws ApplicationActivationException
	 * @throws InstantiationException
	 * 
	 */
	@Test
	public void capabilityImplementationTest() throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, ResourceNotFoundException, CapabilityNotFoundException, InstantiationException, ApplicationActivationException {

		// test getResources() method
		List<IRootResource> odlResources = odlIRootResourceProvider.getRootResources();

		Assert.assertNotNull("ODLIRootResourceProvider capability should contain three resources.", odlResources);
		Assert.assertEquals("ODLIRootResourceProvider capability should contain three resources.", 3, odlResources.size());

		IRootResource switch1Resource = odlResources.get(0);
		IRootResource switch2Resource = odlResources.get(1);
		IRootResource peResource = odlResources.get(2);

		Assert.assertNotNull("Switch1 resource could not be null.", switch1Resource);
		Assert.assertFalse("Switch1 resource should contain a valid id.", switch1Resource.getId().isEmpty());
		Assert.assertNotNull("Switch1 resource descriptor should have been created.", switch1Resource.getDescriptor());
		Assert.assertNotNull("Switch1 resource specification should have been created.", switch1Resource.getDescriptor()
				.getSpecification());
		Assert.assertEquals("Switch1 resource should be of type " + Type.OF_SWITCH, Type.OF_SWITCH, switch1Resource.getDescriptor()
				.getSpecification().getType());
		Assert.assertNotNull("Switch1 resource should contain ODL network endpoint.", switch1Resource.getDescriptor().getEndpoints());
		Assert.assertEquals("Switch1 resource should contain ODL network endpoint.", 1, switch1Resource.getDescriptor().getEndpoints().size());
		Assert.assertTrue("Switch1 resource should contain ODL network endpoint.",
				switch1Resource.getDescriptor().getEndpoints().contains(fakeOdlEndpoint));

		IAttributeStore attributeStoreResource1 = mockedServiceProvider.getCapability(switch1Resource, IAttributeStore.class);
		Assert.assertNotNull("AttributeStore of Switch1 should contain the external port id",
				attributeStoreResource1.getAttribute(AttributeStore.RESOURCE_EXTERNAL_ID));
		Assert.assertEquals("AttributeStore of Switch1 should contain the external port id of the device it represents.", OFSWITCH_NODE_1_ID,
				attributeStoreResource1.getAttribute(AttributeStore.RESOURCE_EXTERNAL_ID));

		IPortManagement resource1PortMgm = mockedServiceProvider.getCapability(switch1Resource, IPortManagement.class);
		Assert.assertEquals("First resource should contain two ports", 2, resource1PortMgm.getPorts().size());

		IAttributeStore attributeStorePort1 = mockedServiceProvider.getCapability(resource1PortMgm.getPorts().get(0), IAttributeStore.class);
		Assert.assertNotNull("Port1 should contain the mapped external resource id.",
				attributeStorePort1.getAttribute(IAttributeStore.RESOURCE_EXTERNAL_ID));
		Assert.assertNotNull("Port1 should contain the mapped external resource name",
				attributeStorePort1.getAttribute(IAttributeStore.RESOURCE_EXTERNAL_NAME));
		Assert.assertEquals("Port1 should contain the external port id " + PORT1_EXTERNAL_ID, PORT1_EXTERNAL_ID,
				attributeStorePort1.getAttribute(IAttributeStore.RESOURCE_EXTERNAL_ID));
		Assert.assertEquals("Port1 should contain the external port name " + PORT1_EXTERNAL_NAME, PORT1_EXTERNAL_NAME,
				attributeStorePort1.getAttribute(IAttributeStore.RESOURCE_EXTERNAL_NAME));

		IAttributeStore attributeStorePort2 = mockedServiceProvider.getCapability(resource1PortMgm.getPorts().get(1), IAttributeStore.class);
		Assert.assertNotNull("Port2 should contain the mapped external resource id.",
				attributeStorePort2.getAttribute(IAttributeStore.RESOURCE_EXTERNAL_ID));
		Assert.assertNotNull("Port2 should contain the mapped external resource name",
				attributeStorePort2.getAttribute(IAttributeStore.RESOURCE_EXTERNAL_NAME));
		Assert.assertEquals("Port2 should contain the external port id " + PORT2_EXTERNAL_ID, PORT2_EXTERNAL_ID,
				attributeStorePort2.getAttribute(IAttributeStore.RESOURCE_EXTERNAL_ID));
		Assert.assertEquals("Port2 should contain the external port name " + PORT2_EXTERNAL_NAME, PORT2_EXTERNAL_NAME,
				attributeStorePort2.getAttribute(IAttributeStore.RESOURCE_EXTERNAL_NAME));

		Assert.assertNotNull("Switch2 resource could not be null", switch2Resource);
		Assert.assertFalse("Switch2 resource should contain a valid id.", switch2Resource.getId().isEmpty());
		Assert.assertNotNull("Switch2 resource descriptor should have been created.", switch2Resource.getDescriptor());
		Assert.assertEquals("Switch2 resource should be of type " + Type.OF_SWITCH, Type.OF_SWITCH, switch2Resource.getDescriptor()
				.getSpecification().getType());
		Assert.assertNotNull("Switch2 resource should contain ODL network endpoint.", switch2Resource.getDescriptor().getEndpoints());
		Assert.assertEquals("Switch2 resource should contain ODL network endpoint.", 1, switch2Resource.getDescriptor().getEndpoints().size());
		Assert.assertTrue("Switch2 resource should contain ODL network endpoint.",
				switch2Resource.getDescriptor().getEndpoints().contains(fakeOdlEndpoint));

		IAttributeStore attributeStoreResource2 = mockedServiceProvider.getCapability(switch2Resource, IAttributeStore.class);
		Assert.assertNotNull("AttributeStore of Switch2 should contain the external port id",
				attributeStoreResource2.getAttribute(AttributeStore.RESOURCE_EXTERNAL_ID));
		Assert.assertEquals("AttributeStore of Switch2 should contain the external port id of the device it represents.", OFSWITCH_NODE_2_ID,
				attributeStoreResource2.getAttribute(AttributeStore.RESOURCE_EXTERNAL_ID));

		Assert.assertNotNull("EP resource could not be null.", peResource);
		Assert.assertFalse("EP resource should contain a valid id.", peResource.getId().isEmpty());
		Assert.assertNotNull("EP resource descriptor should have been created.", peResource.getDescriptor());
		Assert.assertEquals("EP resource should be of type " + Type.OTHER, Type.OTHER, peResource.getDescriptor()
				.getSpecification().getType());
		Assert.assertNotNull("EP resource should contain ODL network endpoint.", peResource.getDescriptor().getEndpoints());
		Assert.assertEquals("EP resource should contain ODL network endpoint.", 1, peResource.getDescriptor().getEndpoints().size());
		Assert.assertTrue("EP resource should contain ODL network endpoint.",
				peResource.getDescriptor().getEndpoints().contains(fakeOdlEndpoint));

		IAttributeStore attributeStoreResource3 = mockedServiceProvider.getCapability(peResource, IAttributeStore.class);
		Assert.assertNotNull("AttributeStore of EP should contain the external port id",
				attributeStoreResource3.getAttribute(AttributeStore.RESOURCE_EXTERNAL_ID));
		Assert.assertEquals("AttributeStore of EP should contain the external port id of the device it represents.", PE_NODE_ID,
				attributeStoreResource3.getAttribute(AttributeStore.RESOURCE_EXTERNAL_ID));

		// test getRootResources(type,model,version)
		List<IRootResource> switches = odlIRootResourceProvider.getRootResources(Type.OF_SWITCH, null, null);
		Assert.assertNotNull("OdlRootResourceprovider should contain two openflow switches.", switches);
		Assert.assertEquals("OdlRootResourceprovider should contain two openflow switches.", 2, switches.size());
		Assert.assertTrue("OdlRootResourceProvider should contain switch1.", switches.contains(switch1Resource));
		Assert.assertTrue("OdlRootResourceProvider should contain switch2.", switches.contains(switch2Resource));

		List<IRootResource> pes = odlIRootResourceProvider.getRootResources(Type.OTHER, null, null);
		Assert.assertNotNull("OdlRootResourceprovider should contain one EP resource.", pes);
		Assert.assertEquals("OdlRootResourceprovider should contain tone EP resource.", 1, pes.size());
		Assert.assertTrue("OdlRootResourceProvider should contain EP resource.", pes.contains(peResource));

		// test getRootResource(id)
		IRootResource resource = odlIRootResourceProvider.getRootResource(switch1Resource.getId());
		Assert.assertEquals("OdlRootResourceProvier capability did not retreive the requested resource.", switch1Resource, resource);
		resource = odlIRootResourceProvider.getRootResource(switch2Resource.getId());
		Assert.assertEquals("OdlRootResourceProvier capability did not retreive the requested resource.", switch2Resource, resource);
		resource = odlIRootResourceProvider.getRootResource(peResource.getId());
		Assert.assertEquals("OdlRootResourceProvier capability did not retreive the requested resource.", peResource, resource);

		// test links are in place
		ILinkManagement linkManagement = capabilityFactory.getCapability(fakeOdlResource, LinkManagement.class);
		Assert.assertNotNull(linkManagement.getLinks());
		Assert.assertFalse(linkManagement.getLinks().isEmpty());
		Assert.assertEquals(odlTopology.getEdgeProperties().size(), linkManagement.getLinks().size());

		for (EdgeProperty edgeProperty : odlTopology.getEdgeProperties()) {

			String name = edgeProperty.getProperties().get("name").getValue();
			String srcPortId = edgeProperty.getEdge().getHeadNodeConnector().getNodeConnectorID();
			String srcNodeId = edgeProperty.getEdge().getHeadNodeConnector().getNode().getNodeID();
			String dstPortId = edgeProperty.getEdge().getTailNodeconnector().getNodeConnectorID();
			String dstNodeId = edgeProperty.getEdge().getTailNodeconnector().getNode().getNodeID();

			// getLinkByExternalName(name)
			LinkResource link = null;
			for (IResource alink : linkManagement.getLinks()) {
				if (mockedServiceProvider.getCapability(alink, IAttributeStore.class).getAttribute(AttributeStore.RESOURCE_EXTERNAL_NAME)
						.equals(name)) {
					link = (LinkResource) alink;
					break;
				}
			}
			Assert.assertNotNull("There must be a link named " + name, link);

			ILinkAdministration linkAdmin = mockedServiceProvider.getCapability(link, ILinkAdministration.class);
			Assert.assertEquals(
					"Link source port should match edge src port",
					srcPortId,
					mockedServiceProvider.getCapability(linkAdmin.getSrcPort(), IAttributeStore.class).getAttribute(
							AttributeStore.RESOURCE_EXTERNAL_ID));
			Assert.assertEquals(
					"Link dst port should match edge dst port",
					dstPortId,
					mockedServiceProvider.getCapability(linkAdmin.getDestPort(), IAttributeStore.class).getAttribute(
							AttributeStore.RESOURCE_EXTERNAL_ID));

			// check the resource containing linkAdmin.getSrcPort() has external id == srcNodeId
			// check the resource containing linkAdmin.getDestPort() has external id == dstNodeId
			boolean srcNodeFound = false;
			boolean dstNodeFound = false;
			for (IRootResource rootResource : odlResources) {
				for (IResource port : mockedServiceProvider.getCapability(rootResource, IPortManagement.class).getPorts()) {
					if (port.equals(linkAdmin.getSrcPort())) {
						srcNodeFound = true;
						Assert.assertEquals(
								srcNodeId,
								mockedServiceProvider.getCapability(rootResource, IAttributeStore.class).getAttribute(
										AttributeStore.RESOURCE_EXTERNAL_ID));
					}
					if (port.equals(linkAdmin.getDestPort())) {
						dstNodeFound = true;
						Assert.assertEquals(
								dstNodeId,
								mockedServiceProvider.getCapability(rootResource, IAttributeStore.class).getAttribute(
										AttributeStore.RESOURCE_EXTERNAL_ID));
					}
				}
			}
			Assert.assertTrue(srcNodeFound);
			Assert.assertTrue(dstNodeFound);
		}

	}

	/**
	 * This method tests the {@link ODLRootResourceProvider#isSupporting(IRootResource)} method. It checks that this capability implementation would
	 * only be bound to ODL networks.
	 *
	 */
	@Test
	public void isSupportingTest() throws InstantiationException, IllegalAccessException {
		IRootResource rootResource = new RootResource(RootResourceDescriptor.create(new Specification(Type.NETWORK, "odl")));
		Assert.assertTrue(ODLRootResourceProvider.isSupporting(rootResource));

		rootResource = new RootResource(RootResourceDescriptor.create(new Specification(Type.NETWORK)));
		Assert.assertFalse(ODLRootResourceProvider.isSupporting(rootResource));

	}

	/**
	 * Create the {@link Nodes} object that the OpenDaylight instance would answer, containing 2 openflow switches and one PE.
	 */
	private void createFakeOdlResponse() {

		odlNodes = new Nodes();

		NodeProperties switch1NodeProperties = generateNodeProperties(OFSWITCH_NODE_1_ID, NodeType.OF);
		NodeProperties switch2NodeProperties = generateNodeProperties(OFSWITCH_NODE_2_ID, NodeType.OF);
		NodeProperties peNodeProperties = generateNodeProperties(PE_NODE_ID, NodeType.PE);

		odlNodes.setNodeProperties(Arrays.asList(switch1NodeProperties, switch2NodeProperties, peNodeProperties));

		resource1NodeConnectors = new NodeConnectors();

		NodeConnectorProperties port0Properties = generateNodeConnectorProperties(PORT1_EXTERNAL_ID, PORT1_EXTERNAL_NAME,
				switch1NodeProperties.getNode());
		NodeConnectorProperties port1Properties = generateNodeConnectorProperties(PORT2_EXTERNAL_ID, PORT2_EXTERNAL_NAME,
				switch1NodeProperties.getNode());

		resource1NodeConnectors.setNodeConnectorProperties(Arrays.asList(port0Properties, port1Properties));

		resource2NodeConnectors = new NodeConnectors();
		NodeConnectorProperties port2Properties = generateNodeConnectorProperties(PORT1_EXTERNAL_ID + 2, PORT1_EXTERNAL_NAME,
				switch2NodeProperties.getNode());
		NodeConnectorProperties port3Properties = generateNodeConnectorProperties(PORT1_EXTERNAL_ID + 3, PORT2_EXTERNAL_NAME,
				switch2NodeProperties.getNode());
		resource2NodeConnectors.setNodeConnectorProperties(Arrays.asList(port2Properties, port3Properties));

		resource3NodeConnectors = new NodeConnectors();
		NodeConnectorProperties port4Properties = generateNodeConnectorProperties(PORT1_EXTERNAL_ID + 4, PORT1_EXTERNAL_NAME,
				peNodeProperties.getNode());
		NodeConnectorProperties port5Properties = generateNodeConnectorProperties(PORT1_EXTERNAL_ID + 5, PORT2_EXTERNAL_NAME,
				peNodeProperties.getNode());
		resource3NodeConnectors.setNodeConnectorProperties(Arrays.asList(port4Properties, port5Properties));

	}

	private NodeConnectorProperties generateNodeConnectorProperties(String ncId, String ncName, Node node) {

		NodeConnectorProperties ncProperties = new NodeConnectorProperties();
		NodeConnector nc = new NodeConnector();
		nc.setNodeConnectorID(ncId);
		nc.setNode(node);

		ncProperties.setNodeConnector(nc);
		Map<String, PropertyValue> ncPropertiesMap = new HashMap<String, PropertyValue>();
		ncPropertiesMap.put("name", new PropertyValue(ncName, null));
		ncProperties.setProperties(ncPropertiesMap);

		return ncProperties;

	}

	/**
	 * Creates an instance of the {@link NodeProperties} object, with the specified id and type.
	 * 
	 * @param nodeId
	 *            Id of the node.
	 * @param nodeType
	 *            Type of the node.
	 * @return {@link NodeProperties} containing a node with id <code>nodeId</code> and type <code>nodeType</node>
	 */
	private NodeProperties generateNodeProperties(String nodeId, NodeType nodeType) {

		Node node = new Node();
		node.setNodeID(nodeId);
		node.setNodeType(nodeType);

		NodeProperties nodeProperties = new NodeProperties();
		nodeProperties.setNode(node);

		return nodeProperties;
	}

	private void createFakeOdlTopologyResponse() {
		// creates a following topology:
		// 1|switch2|0 <-----> 0|switch1|1 <-----> 1|PE|0

		Topology topology = new Topology();

		List<EdgeProperty> edgeProperties = new ArrayList<EdgeProperty>(0);

		// Create bidirectional link between switch1 and switch2

		EdgeProperty first = new EdgeProperty();
		first.setEdge(new Edge(resource1NodeConnectors.getNodeConnectorProperties().get(0).getNodeConnector(),
				resource2NodeConnectors.getNodeConnectorProperties().get(0).getNodeConnector()));
		Map<String, PropertyValue> firstProperties = new HashMap<String, PropertyValue>();
		firstProperties.put("name", new PropertyValue("first", null));
		first.setProperties(firstProperties);
		edgeProperties.add(first);

		// Edges are uni-directional
		// second is the reverse of first
		EdgeProperty second = new EdgeProperty();
		second.setEdge(new Edge(resource2NodeConnectors.getNodeConnectorProperties().get(0).getNodeConnector(),
				resource1NodeConnectors.getNodeConnectorProperties().get(0).getNodeConnector()));
		Map<String, PropertyValue> secondProperties = new HashMap<String, PropertyValue>();
		secondProperties.put("name", new PropertyValue("second", null));
		second.setProperties(secondProperties);
		edgeProperties.add(second);

		// Create bidirectional link between switch1 and PE
		EdgeProperty third = new EdgeProperty();
		third.setEdge(new Edge(resource1NodeConnectors.getNodeConnectorProperties().get(1).getNodeConnector(),
				resource3NodeConnectors.getNodeConnectorProperties().get(1).getNodeConnector()));
		Map<String, PropertyValue> thirdProperties = new HashMap<String, PropertyValue>();
		thirdProperties.put("name", new PropertyValue("third", null));
		third.setProperties(thirdProperties);
		edgeProperties.add(third);

		// fourth is the reverse of third
		EdgeProperty fourth = new EdgeProperty();
		fourth.setEdge(new Edge(resource3NodeConnectors.getNodeConnectorProperties().get(1).getNodeConnector(),
				resource1NodeConnectors.getNodeConnectorProperties().get(1).getNodeConnector()));
		Map<String, PropertyValue> fourthProperties = new HashMap<String, PropertyValue>();
		fourthProperties.put("name", new PropertyValue("fourth", null));
		fourth.setProperties(fourthProperties);
		edgeProperties.add(fourth);

		topology.setEdgeProperties(edgeProperties);
		odlTopology = topology;
	}

	/**
	 * Mock all capabilities and resources used by the {@link ODLRootResourceProvider} implementation.
	 * 
	 * @throws ApplicationActivationException
	 */
	private void mockRequiredFeatures() throws SecurityException, IllegalArgumentException, IllegalAccessException, InstantiationException,
			URISyntaxException, CapabilityNotFoundException, EndpointNotFoundException, ProviderNotFoundException, ApplicationActivationException {

		// mock switch client
		ISwitchNorthboundAPI mockedOdlClient = PowerMockito.mock(ISwitchNorthboundAPI.class);
		PowerMockito.when(mockedOdlClient.getNodes(Mockito.eq("default"))).thenReturn(odlNodes);
		PowerMockito.when(
				mockedOdlClient.getNodeConnectors(Mockito.eq("default"), Mockito.eq(NodeType.OF.toString()), Mockito.eq(OFSWITCH_NODE_1_ID)))
				.thenReturn(resource1NodeConnectors);
		PowerMockito.when(
				mockedOdlClient.getNodeConnectors(Mockito.eq("default"), Mockito.eq(NodeType.OF.toString()), Mockito.eq(OFSWITCH_NODE_2_ID)))
				.thenReturn(resource2NodeConnectors);
		PowerMockito.when(
				mockedOdlClient.getNodeConnectors(Mockito.eq("default"), Mockito.eq(NodeType.PE.toString()), Mockito.eq(PE_NODE_ID))).thenReturn(
				resource3NodeConnectors);

		// mock topology client
		IOpenDaylightTopologyNorthbound mockedTopologyClient = PowerMockito.mock(IOpenDaylightTopologyNorthbound.class);
		PowerMockito.when(mockedTopologyClient.getTopology(Mockito.eq("default"))).thenReturn(odlTopology);

		// mock and inject apiclientprovider

		ICXFAPIProvider mockedCxfApiProvider = PowerMockito.mock(ICXFAPIProvider.class);
		PowerMockito.when(mockedCxfApiProvider.getAPIClient(Mockito.any(IResource.class), Mockito.eq(ISwitchNorthboundAPI.class))).thenReturn(
				mockedOdlClient);
		PowerMockito.when(mockedCxfApiProvider.getAPIClient(Mockito.any(IResource.class), Mockito.eq(IOpenDaylightTopologyNorthbound.class)))
				.thenReturn(
						mockedTopologyClient);

		IAPIClientProviderFactory mockedApiProviderFactory = PowerMockito.mock(IAPIClientProviderFactory.class);
		PowerMockito.when(mockedApiProviderFactory.getAPIProvider(Mockito.eq(ICXFAPIProvider.class))).thenReturn(mockedCxfApiProvider);
		ReflectionTestHelper.injectPrivateField(odlIRootResourceProvider, mockedApiProviderFactory, "apiProviderFactory");

		// inject fake resource in capability
		fakeOdlEndpoint = new Endpoint(new URI("http://www.myfakeodl.com/"));
		fakeOdlResource = new RootResource(RootResourceDescriptor.create(new Specification(Type.NETWORK, "odl"),
				Arrays.asList(fakeOdlEndpoint)));
		ReflectionTestHelper.injectPrivateField(odlIRootResourceProvider, fakeOdlResource, "resource");

		// mock and inject rmListener
		mockedRmListener = PowerMockito.mock(IResourceManagementListener.class);
		ReflectionTestHelper.injectPrivateField(odlIRootResourceProvider, mockedRmListener, "rmListener");

		// inject linkManagement
		ReflectionTestHelper.injectPrivateField(odlIRootResourceProvider, capabilityFactory.getCapability(fakeOdlResource, LinkManagement.class),
				"linkManagement");

		// mock and inject serviceProvider

		mockedServiceProvider = PowerMockito.mock(IServiceProvider.class);

		// config mockedServiceProvider to return IAttributeStore(s)
		PowerMockito.when(mockedServiceProvider.getCapability(Mockito.any(IResource.class), Mockito.eq(IAttributeStore.class)))
				.thenAnswer(new Answer<IAttributeStore>() {

					@Override
					public IAttributeStore answer(InvocationOnMock invocation) throws Throwable {
						IResource resource = (IResource) invocation.getArguments()[0];

						return capabilityFactory.getCapability(resource, AttributeStore.class);
					}
				});

		// config mockedServiceProvider to return IPortManagement(s)
		PowerMockito.when(mockedServiceProvider.getCapability(Mockito.any(IRootResource.class), Mockito.eq(IPortManagement.class)))
				.thenAnswer(new Answer<IPortManagement>() {

					@Override
					public IPortManagement answer(InvocationOnMock invocation) throws Throwable {
						IResource resource = (IResource) invocation.getArguments()[0];

						return capabilityFactory.getCapability(resource, PortManagement.class);
					}
				});

		// config mockedServiceProvider to return ILinkAdministration for each LinkResource
		PowerMockito.when(mockedServiceProvider.getCapability(Mockito.any(LinkResource.class), Mockito.eq(ILinkAdministration.class)))
				.thenAnswer(new Answer<ILinkAdministration>() {

					@Override
					public ILinkAdministration answer(InvocationOnMock invocation) throws Throwable {
						IResource resource = (IResource) invocation.getArguments()[0];

						return capabilityFactory.getCapability(resource, LinkAdministration.class);
					}
				});

		ReflectionTestHelper.injectPrivateField(odlIRootResourceProvider, mockedServiceProvider, "serviceProvider");

	}
}
