/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.io.File;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.pde.internal.launching.IPDEConstants;
import org.eclipse.pde.internal.launching.launcher.LaunchArgumentsHelper;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class WorkspaceDataBlock extends BaseBlock {

	private Button fClearWorkspaceCheck;
	private Button fAskClearCheck;
	private Button fClearWorkspaceRadio;
	private Button fClearWorkspaceLogRadio;

	private String fLastKnownName;
	private String fLastKnownLocation;

	public WorkspaceDataBlock(AbstractLauncherTab tab) {
		super(tab);
	}

	public void createControl(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(PDEUIMessages.WorkspaceDataBlock_workspace);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createText(group, PDEUIMessages.WorkspaceDataBlock_location, 0);

		Composite buttons = new Composite(group, SWT.NONE);
		layout = new GridLayout(7, false);
		layout.marginHeight = layout.marginWidth = 0;
		buttons.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		buttons.setLayoutData(gd);

		fClearWorkspaceCheck = new Button(buttons, SWT.CHECK);
		fClearWorkspaceCheck.setText(PDEUIMessages.WorkspaceDataBlock_clear);
		fClearWorkspaceCheck.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		fClearWorkspaceCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fAskClearCheck.setEnabled(fClearWorkspaceCheck.getSelection());
				fClearWorkspaceRadio.setEnabled(fClearWorkspaceCheck.getSelection());
				fClearWorkspaceLogRadio.setEnabled(fClearWorkspaceCheck.getSelection());
				fTab.updateLaunchConfigurationDialog();
			}
		});

		fClearWorkspaceRadio = new Button(buttons, SWT.RADIO);
		fClearWorkspaceRadio.setText(PDEUIMessages.WorkspaceDataBlock_clearWorkspace);
		fClearWorkspaceRadio.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		fClearWorkspaceRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fTab.updateLaunchConfigurationDialog();
			}
		});
		fClearWorkspaceLogRadio = new Button(buttons, SWT.RADIO);
		fClearWorkspaceLogRadio.setText(PDEUIMessages.WorkspaceDataBlock_clearLog);
		fClearWorkspaceLogRadio.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fClearWorkspaceLogRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fTab.updateLaunchConfigurationDialog();
			}
		});

		createButtons(buttons, new String[] {PDEUIMessages.BaseBlock_workspace, PDEUIMessages.BaseBlock_filesystem, PDEUIMessages.BaseBlock_variables});

		fAskClearCheck = new Button(group, SWT.CHECK);
		fAskClearCheck.setText(PDEUIMessages.WorkspaceDataBlock_askClear);
		gd = new GridData();
		gd.horizontalSpan = 2;
		fAskClearCheck.setLayoutData(gd);
		fAskClearCheck.addSelectionListener(fListener);
	}

	public void performApply(ILaunchConfigurationWorkingCopy config, boolean isJUnit) {
		/*
		 * Goals:
		 * - Update the workspace location when the configuration name changes and ...
		 *   - the workspace location matches the default location
		 *   - the workspace location doesn't exist on the file system
		 * - Renaming the config should not set the workspace location to an already existing location
		 */
		String currentLocation = getLocation();
		String currentName = config.getName();
		if (fLastKnownName != null && !fLastKnownName.equals(currentName)) {
			if (currentLocation.equals(fLastKnownLocation)) {
				try {
					File workspaceDir = new File(LaunchArgumentsHelper.getWorkspaceLocation(config));
					if (!workspaceDir.exists()) {
						currentLocation = getUnusedWorkspaceLocation(currentName, isJUnit);
						fLocationText.setText(currentLocation);
						fLastKnownName = currentName;
						fLastKnownLocation = currentLocation;
					}
				} catch (CoreException e) {
					// don't change workspace location
				}
			}
		}
		config.setAttribute(IPDELauncherConstants.LOCATION, currentLocation);
		config.setAttribute(IPDELauncherConstants.DOCLEAR, fClearWorkspaceCheck.getSelection());
		config.setAttribute(IPDELauncherConstants.ASKCLEAR, fAskClearCheck.getSelection());
		config.setAttribute(IPDEConstants.DOCLEARLOG, fClearWorkspaceLogRadio.getSelection());
	}

	private String getUnusedWorkspaceLocation(String name, boolean isJUnit) {
		String location = LaunchArgumentsHelper.getDefaultWorkspaceLocation(name, isJUnit);
		String resolvedLocation;
		try {
			resolvedLocation = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(location);
		} catch (CoreException e) {
			return location;
		}
		File workspaceDir = new File(resolvedLocation);
		if (!workspaceDir.exists())
			return location;

		String resolvedLocation2;
		int i = 1;
		do {
			i++;
			resolvedLocation2 = resolvedLocation + '-' + i;
		} while (new File(resolvedLocation2).exists());
		return location + '-' + i;
	}

	public void initializeFrom(ILaunchConfiguration configuration) throws CoreException {
		fLastKnownName = configuration.getName();
		fLastKnownLocation = configuration.getAttribute(IPDELauncherConstants.LOCATION, LaunchArgumentsHelper.getDefaultWorkspaceLocation(fLastKnownName));
		fLocationText.setText(fLastKnownLocation);
		fClearWorkspaceCheck.setSelection(configuration.getAttribute(IPDELauncherConstants.DOCLEAR, false));
		fAskClearCheck.setSelection(configuration.getAttribute(IPDELauncherConstants.ASKCLEAR, true));
		fAskClearCheck.setEnabled(fClearWorkspaceCheck.getSelection());
		fClearWorkspaceLogRadio.setEnabled(fClearWorkspaceCheck.getSelection());
		fClearWorkspaceLogRadio.setSelection(configuration.getAttribute(IPDEConstants.DOCLEARLOG, false));
		fClearWorkspaceRadio.setEnabled(fClearWorkspaceCheck.getSelection());
		fClearWorkspaceRadio.setSelection(!configuration.getAttribute(IPDEConstants.DOCLEARLOG, false));
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy configuration, boolean isJUnit) {
		fLastKnownName = configuration.getName();
		fLastKnownLocation = LaunchArgumentsHelper.getDefaultWorkspaceLocation(fLastKnownName, isJUnit);
		configuration.setAttribute(IPDELauncherConstants.LOCATION, fLastKnownLocation);
		configuration.setAttribute(IPDELauncherConstants.DOCLEAR, isJUnit);
		configuration.setAttribute(IPDELauncherConstants.ASKCLEAR, !isJUnit);
		configuration.setAttribute(IPDEConstants.DOCLEARLOG, false);
	}

	protected String getName() {
		return PDEUIMessages.WorkspaceDataBlock_name;
	}

	protected boolean isFile() {
		return false;
	}

	protected void handleBrowseWorkspace() {
		super.handleBrowseWorkspace();
	}

	protected void handleBrowseFileSystem() {
		super.handleBrowseFileSystem();
	}

	public String validate() {
		int length = getLocation().length();
		fClearWorkspaceCheck.setEnabled(length > 0);
		fAskClearCheck.setEnabled(fClearWorkspaceCheck.getSelection() && length > 0);
		if (length == 0)
			fClearWorkspaceCheck.setSelection(false);
		return null;
	}

}
