/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.StateHelper;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class RequiredPluginsClasspathContainer extends PDEClasspathContainer implements IClasspathContainer {
	private IPluginModelBase fModel;
	
	private HashMap fVisiblePackages = new HashMap();
	
	private static boolean DEBUG = false;
	
	static {
		DEBUG  = PDECore.getDefault().isDebugging() 
					&& "true".equals(Platform.getDebugOption("org.eclipse.pde.core/classpath")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * Constructor for RequiredPluginsClasspathContainer.
	 */
	public RequiredPluginsClasspathContainer(IPluginModelBase model) {
		fModel = model;
	}

	
	public void reset() {
		fEntries = null;
	}

	/**
	 * @see org.eclipse.jdt.core.IClasspathContainer#getKind()
	 */
	public int getKind() {
		return K_APPLICATION;
	}

	/**
	 * @see org.eclipse.jdt.core.IClasspathContainer#getPath()
	 */
	public IPath getPath() {
		return new Path(PDECore.CLASSPATH_CONTAINER_ID);
	}
	
	/**
	 * @see org.eclipse.jdt.core.IClasspathContainer#getDescription()
	 */
	public String getDescription() {
		return PDECoreMessages.RequiredPluginsClasspathContainer_description; //$NON-NLS-1$
	}
	
	/**
	 * @see org.eclipse.jdt.core.IClasspathContainer#getClasspathEntries()
	 */
	public IClasspathEntry[] getClasspathEntries() {
		if (fModel == null) {
			if (DEBUG) {
				System.out.println("********Returned an empty container"); //$NON-NLS-1$
				System.out.println();
			}
			return new IClasspathEntry[0];
		}
		if (fEntries == null) {
			computePluginEntries();
		}
		if (DEBUG) {
			System.out.println("Dependencies for plugin '" + fModel.getPluginBase().getId() + "':"); //$NON-NLS-1$ //$NON-NLS-2$
			for (int i = 0; i < fEntries.size(); i++) {
				System.out.println(fEntries.get(i).toString());
			}
			System.out.println();
		}
		return (IClasspathEntry[])fEntries.toArray(new IClasspathEntry[fEntries.size()]);
	}

	private void computePluginEntries() {
		fEntries = new ArrayList();
		try {			
			BundleDescription desc = fModel.getBundleDescription();
			if (desc == null)
				return;
			
			retrieveVisiblePackagesFromState(desc);
			
			HashSet added = new HashSet();

			HostSpecification host = desc.getHost();
			if (desc.isResolved() && host != null) {
				addHostPlugin(host, added);
			}

			// add dependencies
			BundleSpecification[] required = desc.getRequiredBundles();
			for (int i = 0; i < required.length; i++) {
				addDependency(getSupplier(required[i]),
							required[i].isExported(), 
							added);
			}
			
			// add Import-Package
			Iterator iter = fVisiblePackages.keySet().iterator();
			while (iter.hasNext()) {
				BundleDescription dep = (BundleDescription)iter.next();
				if (added.add(dep.getSymbolicName())) {
					addPlugin(dep, false, true);
				}
			}

			ClasspathUtilCore.addExtraClasspathEntries(fModel, fEntries);

			// add implicit dependencies
			addImplicitDependencies(added);
			
			fVisiblePackages.clear();
		} catch (CoreException e) {
		}
	}
	
	private BundleDescription getSupplier(BundleSpecification spec) {
		if (spec.isResolved())
			return (BundleDescription)spec.getSupplier();
		
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(spec.getName());
		return model == null ? null : model.getBundleDescription();	
	}
	
	private void retrieveVisiblePackagesFromState(BundleDescription bundle) {
		fVisiblePackages.clear();
		if (bundle.isResolved()) {
			BundleDescription desc = bundle;
			if (desc.getHost() != null)
				desc = desc.getHost().getHosts()[0];
			
			StateHelper helper = Platform.getPlatformAdmin().getStateHelper();
			ExportPackageDescription[] exports = helper.getVisiblePackages(desc);
			for (int i = 0; i < exports.length; i++) {
				BundleDescription exporter = exports[i].getExporter();
				if (exporter == null)
					continue;
				ArrayList list = (ArrayList)fVisiblePackages.get(exporter);
				if (list == null) 
					list = new ArrayList();
				list.add(new Path(exports[i].getName().replaceAll("\\.", "/") + "/*"));
				fVisiblePackages.put(exporter, list);
			}
		}		
	}

	private void addDependency(BundleDescription desc, boolean isExported, HashSet added) throws CoreException {
		if (desc == null || !added.add(desc.getSymbolicName()))
			return;

		boolean inWorkspace = addPlugin(desc, isExported, true);

		if (hasExtensibleAPI(desc)) {
			BundleDescription[] fragments = desc.getFragments();
			for (int i = 0; i < fragments.length; i++) {
				if (fragments[i].isResolved())
					addDependency(fragments[i], isExported, added);
			}
		}

		if (!inWorkspace) {
			BundleSpecification[] required = desc.getRequiredBundles();
			for (int i = 0; i < required.length; i++) {
				if (required[i].isExported()) {
					addDependency(getSupplier(required[i]), isExported, added);
				}
			}
		}
	}


	private boolean addPlugin(BundleDescription desc, boolean isExported, boolean useInclusions)
			throws CoreException {		
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(desc);
		IResource resource = model.getUnderlyingResource();
		IPath[] inclusions = useInclusions ? getInclusions(model) : null;
		if (resource != null) {
			addProjectEntry(resource.getProject(), isExported, inclusions);
		} else {
			addExternalPlugin(model, isExported, inclusions);
		}
		return resource != null;
	}
	
	private IPath[] getInclusions(IPluginModelBase model) {
		BundleDescription desc = model.getBundleDescription();
		if (desc == null)
			return null;
		
		IPath[] inclusions;
		if (desc.isResolved() && desc.getHost() != null)
			inclusions = getInclusions(desc.getHost().getHosts()[0]);
		else
			inclusions = getInclusions(desc);
		
		return (inclusions.length == 0 && !ClasspathUtilCore.isBundle(model)) ? null : inclusions;

	}
	
	private IPath[] getInclusions(BundleDescription desc) {
		ArrayList list = (ArrayList)fVisiblePackages.get(desc);
		if (list == null) {
			list = new ArrayList();
			ExportPackageDescription[] exports = desc.getExportPackages();
			for (int i = 0; i < exports.length; i++) {
				list.add(new Path(exports[i].getName().replaceAll("\\.", "/") + "/*"));
			}
		}
		return (IPath[])list.toArray(new IPath[list.size()]);		
	}

	private void addImplicitDependencies(HashSet added) throws CoreException {

		String id = fModel.getPluginBase().getId();
		String schemaVersion = fModel.getPluginBase().getSchemaVersion();
		boolean isOSGi = TargetPlatform.isOSGi();
		
		if ((isOSGi && schemaVersion != null)
				|| id.equals("org.eclipse.core.boot") //$NON-NLS-1$
				|| id.equals("org.apache.xerces") //$NON-NLS-1$
				|| id.startsWith("org.eclipse.swt")) //$NON-NLS-1$
			return;

		PluginModelManager manager = PDECore.getDefault().getModelManager();

		if (schemaVersion == null && isOSGi) {
			if (!id.equals("org.eclipse.core.runtime")) { //$NON-NLS-1$
				IPluginModelBase plugin = manager.findModel(
						"org.eclipse.core.runtime.compatibility"); //$NON-NLS-1$
				if (plugin != null)
					addDependency(plugin.getBundleDescription(), false, added);
			}
		} else {
			IPluginModelBase plugin = manager.findModel("org.eclipse.core.boot"); //$NON-NLS-1$
			if (plugin != null)
				addDependency(plugin.getBundleDescription(), false, added);
			
			if (!id.equals("org.eclipse.core.runtime")) { //$NON-NLS-1$
				plugin = manager.findModel("org.eclipse.core.runtime"); //$NON-NLS-1$
				if (plugin != null)
					addDependency(plugin.getBundleDescription(), false, added);
			}
		}
	}

	private void addHostPlugin(HostSpecification hostSpec, HashSet added) throws CoreException {
		BaseDescription desc = hostSpec.getSupplier();
		
		if (desc instanceof BundleDescription && added.add(desc.getName())) {
			BundleDescription host = (BundleDescription)desc;
			// add host plug-in
			boolean inWorkspace = addPlugin(host, false, false);
			
			BundleSpecification[] required = host.getRequiredBundles();
			for (int i = 0; i < required.length; i++) {
				// if the plug-in is a project in the workspace, only add
				// non-reexported dependencies since the fragment will
				// automatically get the reexported dependencies.
				// if the plug-in is in the target, then you need to explicitly add
				// all the parent plug-in's non-reexported dependencies.
				if ((!inWorkspace || !required[i].isExported())) {
					desc = getSupplier(required[i]);
					if (desc instanceof BundleDescription) {
						addDependency((BundleDescription)desc, false, added);
					}
				}
			}
		}
	}
	
	private boolean hasExtensibleAPI(BundleDescription desc) {
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(desc);
		return (model instanceof IPluginModel) 
					? ClasspathUtilCore.hasExtensibleAPI(((IPluginModel)model).getPlugin()) 
					: false;
	}
	
}
