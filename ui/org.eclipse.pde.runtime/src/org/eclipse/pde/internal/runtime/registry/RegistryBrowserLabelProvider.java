/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.resource.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.runtime.*;
import org.eclipse.swt.graphics.*;

public class RegistryBrowserLabelProvider extends LabelProvider {
	private Image pluginImage;
	private Image activePluginImage;
	private Image libraryImage;
	private Image runtimeImage;
	private Image genericTagImage;
	private Image extensionImage;
	private Image extensionsImage;
	private static final String KEY_IMPORTS = "RegistryView.folders.imports";
	private static final String KEY_LIBRARIES = "RegistryView.folders.libraries";
	private static final String KEY_EXPOINTS =
		"RegistryView.folders.extensionPoints";
	private static final String KEY_EXTENSIONS = "RegistryView.folders.extensions";
	private Image extensionPointImage;
	private Image extensionPointsImage;
	private Image requiresImage;
	private Image reqPluginImage;
	private TreeViewer viewer;
	
	public RegistryBrowserLabelProvider(TreeViewer viewer) {
		this.viewer = viewer;
		pluginImage = PDERuntimePluginImages.DESC_PLUGIN_OBJ.createImage();
		reqPluginImage = PDERuntimePluginImages.DESC_REQ_PLUGIN_OBJ.createImage();
		extensionPointImage = PDERuntimePluginImages.DESC_EXT_POINT_OBJ.createImage();
		extensionPointsImage = PDERuntimePluginImages.DESC_EXT_POINTS_OBJ.createImage();
		extensionImage = PDERuntimePluginImages.DESC_EXTENSION_OBJ.createImage();
		extensionsImage = PDERuntimePluginImages.DESC_EXTENSIONS_OBJ.createImage();
		requiresImage = PDERuntimePluginImages.DESC_REQ_PLUGINS_OBJ.createImage();
		libraryImage = PDERuntimePluginImages.DESC_JAVA_LIB_OBJ.createImage();
		genericTagImage = PDERuntimePluginImages.DESC_GENERIC_XML_OBJ.createImage();
		runtimeImage = PDERuntimePluginImages.DESC_RUNTIME_OBJ.createImage();

		ImageDescriptor activePluginDesc =
			new OverlayIcon(
				PDERuntimePluginImages.DESC_PLUGIN_OBJ,
				new ImageDescriptor[][] { { PDERuntimePluginImages.DESC_RUN_CO }
		});
		activePluginImage = activePluginDesc.createImage();
	}
	public void dispose() {
		pluginImage.dispose();
		activePluginImage.dispose();
		reqPluginImage.dispose();
		extensionPointImage.dispose();
		extensionPointsImage.dispose();
		extensionImage.dispose();
		extensionsImage.dispose();
		requiresImage.dispose();
		libraryImage.dispose();
		genericTagImage.dispose();
		runtimeImage.dispose();
	}
	public Image getImage(Object element) {
		if (element instanceof PluginObjectAdapter)
			element = ((PluginObjectAdapter) element).getObject();
		if (element instanceof IPluginDescriptor) {
			IPluginDescriptor pd = (IPluginDescriptor) element;
			boolean active = pd.isPluginActivated();
			return active ? activePluginImage : pluginImage;
		}
		if (element instanceof IPluginFolder) {
			int id = ((IPluginFolder) element).getFolderId();
			switch (id) {
				case IPluginFolder.F_EXTENSIONS :
					return extensionsImage;
				case IPluginFolder.F_EXTENSION_POINTS :
					return extensionPointsImage;
				case IPluginFolder.F_IMPORTS :
					return requiresImage;
				case IPluginFolder.F_LIBRARIES :
					return runtimeImage;
			}
			return null;
		}
		if (element instanceof IExtension) {
			return extensionImage;
		}
		if (element instanceof IExtensionPoint) {
			return extensionPointImage;
		}
		if (element instanceof IPluginPrerequisite) {
			return reqPluginImage;
		}
		if (element instanceof ILibrary) {
			return libraryImage;
		}
		if (element instanceof IConfigurationElement) {
			return genericTagImage;
		}
		return null;
	}
	public String getText(Object element) {
		if (element instanceof PluginObjectAdapter)
			element = ((PluginObjectAdapter) element).getObject();
		if (element instanceof IPluginDescriptor) {
			return ((IPluginDescriptor)element).getUniqueIdentifier();
		}
		if (element instanceof IPluginFolder) {
			switch (((IPluginFolder) element).getFolderId()) {
				case IPluginFolder.F_IMPORTS :
					return PDERuntimePlugin.getResourceString(KEY_IMPORTS);
				case IPluginFolder.F_LIBRARIES :
					return PDERuntimePlugin.getResourceString(KEY_LIBRARIES);
				case IPluginFolder.F_EXTENSION_POINTS :
					return PDERuntimePlugin.getResourceString(KEY_EXPOINTS);
				case IPluginFolder.F_EXTENSIONS :
					return PDERuntimePlugin.getResourceString(KEY_EXTENSIONS);
			}
		}
		if (element instanceof IExtension) {
			if (((RegistryBrowserContentProvider)viewer.getContentProvider()).isInExtensionSet)
				return ((IExtension) element).getExtensionPointUniqueIdentifier();

			IPluginDescriptor desc = ((IExtension) element).getDeclaringPluginDescriptor();
			return "contributed by: "+ desc.getUniqueIdentifier();
			
		}
		if (element instanceof IExtensionPoint) {
			String pluginId = ((IExtensionPoint)element).getDeclaringPluginDescriptor().getUniqueIdentifier();//getParentIdentifier();
			String extPointId = ((IExtensionPoint) element).getUniqueIdentifier();
			return extPointId.substring(pluginId.length()+1);
		}
		if (element instanceof IPluginPrerequisite) {
			return ((IPluginPrerequisite) element).getUniqueIdentifier();
		}
		if (element instanceof ILibrary) {
			return ((ILibrary) element).getPath().toString();
		}
		if (element instanceof IConfigurationElement) {
			String label = ((IConfigurationElement) element).getAttribute("label");
			if (label == null){
				label = ((IConfigurationElement) element).getAttribute("name");
			}
			if (label == null && ((IConfigurationElement) element).getAttribute("id") != null){
				String[] labelSplit = ((IConfigurationElement) element).getAttribute("id").split("\\.");
				label = labelSplit.length == 0 ? null: labelSplit[labelSplit.length-1];
			} 
			if (label == null){
				label = ((IConfigurationElement) element).getName();
			}
				
			return label;
		}
		return super.getText(element);
	}
}
