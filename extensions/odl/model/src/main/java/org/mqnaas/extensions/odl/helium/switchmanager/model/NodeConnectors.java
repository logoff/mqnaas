package org.mqnaas.extensions.odl.helium.switchmanager.model;

/*
 * #%L
 * MQNaaS :: ODL Model
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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * {@link NodeConnectorProperties} wrapper.
 * 
 * Based on OpenDaylight Helium release (<a href=
 * "https://github.com/opendaylight/controller/blob/stable/helium/opendaylight/northbound/switchmanager/src/main/java/org/opendaylight/controller/switchmanager/northbound/NodeConnectors.java"
 * >reference</a>).
 * 
 * @author Julio Carlos Barrera
 *
 */
@XmlRootElement(name = "list")
@XmlAccessorType(XmlAccessType.FIELD)
public class NodeConnectors {

	private List<NodeConnectorProperties>	nodeConnectorProperties;

	public NodeConnectors() {
	}

	public List<NodeConnectorProperties> getNodeConnectorProperties() {
		return nodeConnectorProperties;
	}

	public void setNodeConnectorProperties(List<NodeConnectorProperties> nodeConnectorProperties) {
		this.nodeConnectorProperties = nodeConnectorProperties;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((nodeConnectorProperties == null) ? 0 : nodeConnectorProperties.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NodeConnectors other = (NodeConnectors) obj;
		if (nodeConnectorProperties == null) {
			if (other.nodeConnectorProperties != null)
				return false;
		} else if (!nodeConnectorProperties.equals(other.nodeConnectorProperties))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "NodeConnectors [nodeConnectorProperties=" + nodeConnectorProperties + "]";
	}

}
