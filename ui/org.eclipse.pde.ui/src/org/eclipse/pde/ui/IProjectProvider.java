package org.eclipse.pde.ui;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
/**
 * This interface is used to insulate
 * the client's wizards from the
 * master wizard that is responsible for
 * creating the new project.
 * Clients use this interface
 * to ask for the new project's name
 * (without forcing the project creation)
 * and the project handle itself. Content wizards 
 * can use the project name to construct default
 * values for other name properties before
 * the project resource is being created.
 * <p>
 * <b>Note:</b> This interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */
public interface IProjectProvider {
	/**
	 * Returns the new plug-in project handle. This
	 * method will cause project creation if not created
	 * already.
	 *
	 * @return the handle of the new plug-in project
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	IProject getProject();
	/**
	 * Returns the name of the plug-in project that
	 * will be created. This method can be called
	 * at any time without forcing the project resource creation.
	 *
	 * @return new project name
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	String getProjectName();

	/**
	 * Returns an absolute path of the new plug-in 
	 * project that will be created. This method can
	 * be called at any time without forcing the project
	 * resource creation.
	 * 
	 * @return absolute project location path
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	IPath getLocationPath();
}