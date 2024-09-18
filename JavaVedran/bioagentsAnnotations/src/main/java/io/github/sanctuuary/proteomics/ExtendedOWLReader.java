package io.github.sanctuuary.proteomics;

import nl.uu.cs.ape.sat.models.AbstractModule;
import nl.uu.cs.ape.sat.models.AllModules;
import nl.uu.cs.ape.sat.models.AllTypes;
import nl.uu.cs.ape.sat.models.Type;
import nl.uu.cs.ape.sat.models.enums.NodeType;
import nl.uu.cs.ape.sat.utils.APEDimensionsException;
import nl.uu.cs.ape.sat.utils.APEDomainSetup;

import org.apache.commons.io.FileExistsException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.SystemOutDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * The {@code ExtendedOWLReader} class is used to extract the classification
 * information regarding the modules and data types from the OWL ontology.
 *
 * @author Vedran Kasalica
 */
public class ExtendedOWLReader {

	/** File cinstianing the ontology */
	private final File ontologyFile;
	/** List of all types in the domain */
	private final AllTypes allTypes;
	/** Mapping from each dimension to the list of the types within it */
	private Map<String, Set<String>> typeDimensions = new HashMap<String, Set<String>>();

	private OWLOntology ontology;
	private OWLDataFactory factory;
	/** OWL logger */
	private Logger logger = Logger.getLogger("ExtendedOWLReader.class");

	/**
	 * Setting up the reader that will populate the provided module and type sets
	 * with objects from the ontology.
	 *
	 * @param domain       Domain information, including all the existing agents and
	 *                     types.
	 * @param ontologyFile Path to the OWL file.
	 */
	public ExtendedOWLReader(APEDomainSetup domain, File ontologyFile) {
		this.ontologyFile = ontologyFile;
		this.allTypes = domain.getAllTypes();
		this.factory = OWLManager.getOWLDataFactory();
	}

	public Map<String, Set<String>> getObjectProperties(String formatIRI, String objectPropertyIRI)
			throws OWLOntologyCreationException {
		/** Mapping from a data type/format to all relevant format/data types. */
		Map<String, Set<String>> dataFormatDependencies = new HashMap<String, Set<String>>();

		final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		if (this.ontologyFile.exists()) {
			this.ontology = manager.loadOntologyFromOntologyDocument(ontologyFile);
		} else {
			this.logger.warning("Provided ontology does not exist.");
			return null;
		}
		OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
		OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);

		/* Get a root of the data format taxonomy. */
		OWLClass formatRootClass = manager.getOWLDataFactory().getOWLClass(IRI.create(formatIRI));
		if (!ontology.containsClassInSignature(IRI.create(formatIRI))) {
			/* Handle scenario when the agent taxonomy root was not defined properly. */
			throw APEDimensionsException.notExistingDimension(
					String.format("Operation root %s does not exist in the ontology.", formatIRI));
		}

		exploreTypeFormatDepencencyRec(objectPropertyIRI, dataFormatDependencies, reasoner, formatRootClass,
				new HashSet<String>());
		
		return dataFormatDependencies;
	}

	/**
	 * Recursively exploring the hierarchy of the data format ontology and defining
	 * relations between data type and data format objects ({@link Type}).
	 * @param objectPropertyIRI 
	 *
	 * @param dataFormatDependencies - Existing data/format dependencies
	 * @param reasoner               - Reasoner used to provide subclasses.
	 * @param currClass              - The class (node) currently explored.
	 * @param superClass             - The superclass of the currClass.
	 */
	private void exploreTypeFormatDepencencyRec(String objectPropertyIRI, Map<String, Set<String>> dataFormatDependencies, OWLReasoner reasoner,
			OWLClass currClass, Set<String> dependantDataTypes) {

		String currTypeID = getIRI(currClass);

		Set<String> dataTypes = findInOWLObjectPropertiesRel(currTypeID, objectPropertyIRI);
		dataTypes.addAll(dependantDataTypes);
		if(!dataTypes.isEmpty()) {
			/**
			 * Adding the dependency from Data Format to Type
			 */
			if(dataFormatDependencies.get(currTypeID) == null) {
				
				dataFormatDependencies.put(currTypeID, dataTypes);
			} else {
				dataFormatDependencies.get(currTypeID).addAll(dataTypes);
			}
			
			/**
			 * Adding the inverse dependency from Data Types to Formats
			 */
//			for(String dataType : dependantDataTypes) {
//				if(dataFormatDependencies.get(dataType) == null) {
//					Set<String> format = new HashSet<String>();
//					format.add(currTypeID);
//					dataFormatDependencies.put(dataType, format);
//				} else {
//					dataFormatDependencies.get(dataType).add(currTypeID);
//				}
//			}
		}
		
		List<OWLClass> subClasses = reasoner.getSubClasses(currClass, true).entities()
				.filter(child -> reasoner.isSatisfiable(child)).collect(Collectors.toList());

		subClasses.forEach(child -> exploreTypeFormatDepencencyRec(objectPropertyIRI, dataFormatDependencies, reasoner, child, dataTypes));

	}

	
	private Set<String> findInOWLObjectPropertiesRel(String currTypeID, String objPropertyIRI) {
		Set<String> dataTypes = new HashSet<String>();

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try {

			// optional, but recommended
			// process XML securely, avoid attacks like XML External Entities (XXE)
			dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

			// parse XML file
			DocumentBuilder db = dbf.newDocumentBuilder();

			Document doc = db.parse(ontologyFile);

			// optional, but recommended
			// http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			// get <staff>
			NodeList list = doc.getElementsByTagName("owl:Class");

			for (int temp = 0; temp < list.getLength(); temp++) {

				Node node = list.item(temp);

//	              System.out.println(node.getNodeName());
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element element = (Element) node;

					// get staff's attribute
					String id = element.getAttribute("rdf:about");
					if (id.equals(currTypeID)) {

						NodeList restrictions = element.getElementsByTagName("owl:Restriction");
						if(restrictions.getLength() == 0) {
							continue;
						}
						System.out.println(restrictions.toString());
						Node onProp = ((Element) restrictions.item(0)).getElementsByTagName("owl:onProperty").item(0);

						// if resource is the expected relation
						if (((Element) onProp).getAttribute("rdf:resource").equals(objPropertyIRI)) {

							for (int j = 0; j < restrictions.getLength(); j++) {
								Node refDataType = ((Element) restrictions.item(j))
										.getElementsByTagName("owl:someValuesFrom").item(0);
								String refDataTypeID = ((Element) refDataType).getAttribute("rdf:resource");
								dataTypes.add(refDataTypeID);
							}
						}
						break;
					}
				}
			}

		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}

		return dataTypes;
	}

	private Type addNewTypeToAllTypes(String classLabel, String classID, String rootID, NodeType currNodeType) {
		Type currType = null;
		/* Generate the Type that corresponds to the taxonomy class. */
		try {
			currType = allTypes.addPredicate(new Type(classLabel, classID, rootID, currNodeType));
			typeDimensions.get(rootID).add(classID);
		} catch (ExceptionInInitializerError e) {
			e.printStackTrace();
		}
		return currType;
	}

	/**
	 * Returning the label of the provided OWL class.
	 *
	 * @param currClass Provided OWL class.
	 * @return String representation of the class name.
	 */
	private String getLabel(OWLClass currClass) {
		if (currClass == null || currClass.isOWLNothing()) {
			return "N/A";
		}
		String label, classID = currClass.toStringID();
		Optional<OWLAnnotation> classLabel = EntitySearcher.getAnnotations(currClass, ontology, factory.getRDFSLabel())
				.findFirst();
		if (classLabel.isPresent()) {
			OWLAnnotationValue val = classLabel.get().getValue();
			if (val instanceof OWLLiteral)
				return ((OWLLiteral) val).getLiteral();
		} else if (classID.contains("#")) {
			label = classID.substring(classID.indexOf('#') + 1);
//			label = label.replace(" ", "_");
			return label;
		}
		logger.fine("Class '" + classID + "' has no label.");
		return classID;

	}

	/**
	 * Returning the IRI of the provided OWL class.
	 *
	 * @param currClass Provided OWL class.
	 * @return String representation of the class name.
	 */
	private String getIRI(OWLClass currClass) {
		if (currClass == null) {
			return null;
		}
		return currClass.toStringID();
	}
}
