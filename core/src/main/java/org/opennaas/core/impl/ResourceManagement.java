package org.opennaas.core.impl;

import java.util.ArrayList;
import java.util.List;

import org.opennaas.core.api.IResource;
import org.opennaas.core.api.IResourceManagement;

public class ResourceManagement implements IResourceManagement {

	private List<IResource>	resources	= new ArrayList<IResource>();

	public static boolean isSupporting(IResource resource) {
		return resource instanceof OpenNaaS;
	}

	@Override
	public void addResource(IResource resource) {
		resources.add(resource);
	}

	@Override
	public void removeResource(IResource resource) {
		resources.remove(resource);
	}

	@Override
	public List<IResource> getResources() {
		return new ArrayList<IResource>(resources);
	}

	@Override
	public <R extends IResource> R getResource(Class<R> clazz) {
		for (IResource resource : resources) {
			if (clazz.isInstance(resource))
				return (R) resource;
		}

		return null;
	}
}