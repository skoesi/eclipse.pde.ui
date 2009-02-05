/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAccess;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiField;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchReporter;
import org.eclipse.pde.api.tools.internal.util.Signatures;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Search reporter that outputs results to an XML file
 * 
 * @since 1.0.1
 */
public class XMLApiSearchReporter implements IApiSearchReporter {

	/**
	 * file names for the outputted reference files
	 */
	public static final String TYPE_REFERENCES = "type_references"; //$NON-NLS-1$
	public static final String METHOD_REFERENCES = "method_references"; //$NON-NLS-1$
	public static final String FIELD_REFERENCES = "field_references"; //$NON-NLS-1$
	
	private String fLocation = null;
	private HashMap fReferenceMap = null;
	private IApiDescription fDescription = null;
	private DocumentBuilder parser = null;
	
	/**
	 * Constructor
	 */
	public XMLApiSearchReporter(String location) {
		fLocation = location;
		try {
			parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			parser.setErrorHandler(new DefaultHandler());
		}
		catch(FactoryConfigurationError fce) {} 
		catch (ParserConfigurationException e) {}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchReporter#reportResults(org.eclipse.pde.api.tools.internal.provisional.builder.IReference[])
	 */
	public void reportResults(IApiElement element, final IReference[] references) {
		if(fLocation != null) {
			try {
				File parent = new File(fLocation);
				if(!parent.exists()) {
					parent.mkdirs();
				}
				collateResults(references);
				writeXML(parent);
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
			finally {
				if(fReferenceMap != null) {
					fReferenceMap.clear();
					fReferenceMap = null;
				}
			}
		}
	}
	
	/**
	 * Collates the results into like reference kinds
	 * @param references
	 */
	private void collateResults(IReference[] references) throws CoreException {
		if(fReferenceMap == null) {
			fReferenceMap = new HashMap();
		}
		Integer type = null;
		Integer kind = null;
		Integer visibility = null;
		String id = null;
		HashMap rmap = null;
		HashMap mmap = null;
		HashMap vmap = null;
		HashMap tmap = null;
		HashSet reflist = null;
		IApiAnnotations annot = null;
		IApiComponent rcomponent = null;
		IApiComponent mcomponent = null;
		for (int i = 0; i < references.length; i++) {
			rcomponent = references[i].getResolvedReference().getApiComponent(); 
			id = rcomponent.getId();
			rmap = (HashMap) fReferenceMap.get(id);
			if(rmap == null) {
				rmap = new HashMap();
				fReferenceMap.put(id, rmap);
			}
			mcomponent = references[i].getMember().getApiComponent(); 
			id = mcomponent.getId();
			mmap = (HashMap) rmap.get(id);
			if(mmap == null) {
				mmap = new HashMap();
				rmap.put(id, mmap);
			}
			fDescription = rcomponent.getApiDescription();
			annot = fDescription.resolveAnnotations(references[i].getResolvedReference().getHandle());
			if(annot != null) {
				visibility = new Integer(annot.getVisibility());
				if(annot.getVisibility() == VisibilityModifiers.PRIVATE) {
					IApiAccess access = fDescription.resolveAccessLevel(
							mcomponent.getHandle(), 
							getPackageDescriptor(references[i].getResolvedReference()));
					if(access != null && access.getAccessLevel() == IApiAccess.FRIEND) {
						visibility = new Integer(VisibilityModifiers.PRIVATE_PERMISSIBLE);
					}
				}
			}
			else {
				//overflow for those references that cannot be resolved
				visibility = new Integer(VisibilityModifiers.ALL_VISIBILITIES);
			}
			vmap = (HashMap) mmap.get(visibility);
			if(vmap == null) {
				vmap = new HashMap();
				mmap.put(visibility, vmap);
			}
			type = new Integer(references[i].getReferenceType());
			kind = new Integer(references[i].getReferenceKind());
			tmap = (HashMap) vmap.get(type);
			if(tmap == null) {
				tmap = new HashMap();
				vmap.put(type, tmap);
			}
			reflist = (HashSet) tmap.get(kind);
			if(reflist == null) {
				reflist = new HashSet();
				tmap.put(kind, reflist);
			}
			reflist.add(references[i]);
		}
	}
	
	/**
	 * Returns the {@link IPackageDescriptor} for the package that contains the given {@link IApiMember}
	 * @param member
	 * @return a new package descriptor for the given {@link IApiMember}
	 * @throws CoreException
	 */
	private IPackageDescriptor getPackageDescriptor(IApiMember member) throws CoreException {
		IApiType type = null;
		if(member.getType() != IApiElement.TYPE) {
			 type = member.getEnclosingType();
		}
		else {
			type = (IApiType) member;
		}
		return Factory.packageDescriptor(type.getPackageName());
	}
	
	/**
	 * Returns the name for the file of references base on the given type
	 * @param type
	 * @return
	 */
	private String getRefTypeName(int type) {
		switch(type) {
			case IReference.T_TYPE_REFERENCE: return TYPE_REFERENCES;
			case IReference.T_METHOD_REFERENCE: return METHOD_REFERENCES;
			case IReference.T_FIELD_REFERENCE: return FIELD_REFERENCES;
		}
		return "unknown_reference_kinds"; //$NON-NLS-1$
	}
	
	/**
	 * Writes out the XML for the given api element using the collated {@link IReference}s
	 * @param parent
	 * @throws CoreException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void writeXML(File parent) throws CoreException, FileNotFoundException, IOException {
		HashMap vismap = null;
		HashMap typemap = null;
		HashMap rmap = null;
		HashMap mmap = null;
		Integer type = null;
		Integer vis = null;
		String id = null;
		File root = null;
		File location = null;
		for(Iterator iter = fReferenceMap.keySet().iterator(); iter.hasNext();) {
			id = (String) iter.next();
			location = new File(parent, id);
			if(!location.exists()) {
				location.mkdir();
			}
			rmap = (HashMap) fReferenceMap.get(id);
			for(Iterator iter2 = rmap.keySet().iterator(); iter2.hasNext();) {
				id = (String) iter2.next();
				root = new File(location, id);
				if(!root.exists()) {
					root.mkdir();
				}
				mmap = (HashMap) rmap.get(id);
				for(Iterator iter4 = mmap.keySet().iterator(); iter4.hasNext();) {
					vis = (Integer) iter4.next();
					location = new File(root, Util.getVisibilityKind(vis.intValue()));
					if(!location.exists()) {
						location.mkdir();
					}
					vismap = (HashMap) mmap.get(vis);
					for(Iterator iter3 = vismap.keySet().iterator(); iter3.hasNext();) {
						type = (Integer) iter3.next();
						typemap = (HashMap) vismap.get(type);
						writeGroup(location, getRefTypeName(type.intValue()), typemap, vis.intValue());
					}
				}
			}
		}
	}
	
	/**
	 * Writes out a group of references under the newly created element with the given name
	 * @param parent
	 * @param name
	 * @param map
	 * @param visibility
	 */
	private void writeGroup(File parent, String name, HashMap map, int visibility) throws CoreException, FileNotFoundException, IOException {
		if(parent.exists()) {
			BufferedWriter writer = null;
			try {
				Document doc = null;
				Element root = null;
				int count = 0;
				File out = new File(parent, name+".xml"); //$NON-NLS-1$
				if(out.exists()) {
					try {
						doc = this.parser.parse(new FileInputStream(out));
						root = doc.getDocumentElement();
						String value = root.getAttribute(IApiXmlConstants.ATTR_REFERENCE_COUNT);
						count = Integer.parseInt(value);
					}
					catch(SAXException se) {
						se.printStackTrace();
					}
				}
				else {
					doc = Util.newDocument();
					root = doc.createElement(IApiXmlConstants.REFERENCES);
					doc.appendChild(root);
					root.setAttribute(IApiXmlConstants.ATTR_REFERENCE_VISIBILITY, Integer.toString(visibility));
				}
				if(doc == null) {
					return;
				}
				Integer kind = null;
				HashSet refs = null;
				Element kelement = null;
				for(Iterator iter = map.keySet().iterator(); iter.hasNext();) {
					kind = (Integer) iter.next();
					kelement = findKindElement(root, kind);
					if(kelement == null) {
						kelement = doc.createElement(IApiXmlConstants.REFERENCE_KIND);
						kelement.setAttribute(IApiXmlConstants.ATTR_REFERENCE_KIND_NAME, Util.getReferenceKind(kind.intValue()));
						kelement.setAttribute(IApiXmlConstants.ATTR_KIND, kind.toString());
						root.appendChild(kelement);
					}
					refs = (HashSet) map.get(kind);
					if(refs != null) {
						for(Iterator iter2 = refs.iterator(); iter2.hasNext();) {
							count++;
							writeReference(doc, kelement, (IReference) iter2.next());
						}
					}
				}
				root.setAttribute(IApiXmlConstants.ATTR_REFERENCE_COUNT, Integer.toString(count));
				writer = new BufferedWriter(new FileWriter(out));
				writer.write(Util.serializeDocument(doc));
				writer.flush();
			}
			finally {
				if (writer != null) {
					writer.close();
				}
			}
		}
	}
	
	/**
	 * gets the root kind element
	 * @param root
	 * @param kind
	 * @return
	 */
	private Element findKindElement(Element root, Integer kind) {
		Element kelement = null;
		NodeList nodes = root.getElementsByTagName(IApiXmlConstants.REFERENCE_KIND);
		for (int i = 0; i < nodes.getLength(); i++) {
			kelement = (Element) nodes.item(i);
			if(kind.toString().equals(kelement.getAttribute(IApiXmlConstants.ATTR_KIND))) {
				return kelement;
			}
		}
		return null;
	}
	
	/**
	 * Writes the attributes from the given {@link IReference} into a new {@link Element} that is added to 
	 * the given parent.
	 * 
	 * @param document
	 * @param parent
	 * @param reference
	 */
	private void writeReference(Document document, Element parent, IReference reference) throws CoreException {
		Element relement = document.createElement(IApiXmlConstants.ATTR_REFERENCE);
		IApiMember member = reference.getMember();
		relement.setAttribute(IApiXmlConstants.ATTR_ORIGIN, getText(member));
		member = reference.getResolvedReference();
		if(member != null) {
			relement.setAttribute(IApiXmlConstants.ATTR_REFEREE, getText(member));
			relement.setAttribute(IApiXmlConstants.ATTR_LINE_NUMBER, Integer.toString(reference.getLineNumber()));
			String sig = reference.getReferencedSignature();
			if(sig != null) {
				relement.setAttribute(IApiXmlConstants.ATTR_SIGNATURE, sig);
			}
			parent.appendChild(relement);
		}
	}
	
	/**
	 * Returns the text to set in the attribute for the given {@link IApiMember}
	 * @param member
	 * @return
	 * @throws CoreException
	 */
	private String getText(IApiMember member) throws CoreException {
		switch(member.getType()) {
			case IApiElement.TYPE: return Signatures.getQualifiedTypeSignature((IApiType) member);
			case IApiElement.METHOD: return Signatures.getQualifiedMethodSignature((IApiMethod) member);
			case IApiElement.FIELD: return Signatures.getQualifiedFieldSignature((IApiField) member);
		}
		return null;
	}
}