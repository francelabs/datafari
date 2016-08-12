package com.francelabs.datafari.updateprocessor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;

public class OntologyUpdateProcessorFactory extends UpdateRequestProcessorFactory {

	private static final String ENABLED_PARAM = "enabled";

	/*
	 * Field configuration parameters
	 */
	private static final String ANNOTATION_FIELD_PARAM = "annotationField";
	private static final String INCLUDE_INDIRECT_PARAM = "includeIndirect";
	private static final String USE_LANGUAGES = "useLanguages";
	private static final String ONTOLOGY_URI_PARAM = "ontologyURI";
	private static final String LABEL_FIELD_PARAM = "labelField";
	private static final String CHILD_FIELD_PARAM = "childField";
	private static final String PARENT_FIELD_PARAM = "parentField";
	private static final String DESCENDANT_FIELD_PARAM = "descendantsField";
	private static final String ANCESTOR_FIELD_PARAM = "ancestorsField";
	private static final String CHILD_LABEL_PARAM = "childLabel";
	private static final String PARENT_LABEL_PARAM = "parentLabel";
	private static final String DESCENDANT_LABEL_PARAM = "descendantsLabel";
	private static final String ANCESTOR_LABEL_PARAM = "ancestorsLabel";

	/*
	 * Default field values
	 */
	private static final String LABEL_FIELD_DEFAULT = "ontology_labels";
	private static final String CHILD_FIELD_DEFAULT = "ontology_children";
	private static final String PARENT_FIELD_DEFAULT = "ontology_parents";
	private static final String DESCENDANT_FIELD_DEFAULT = "ontology_descendants";
	private static final String ANCESTOR_FIELD_DEFAULT = "ontology_ancestors";
	private static final String CHILD_LABEL_DEFAULT = "ontology_children_labels";
	private static final String PARENT_LABEL_DEFAULT = "ontology_parents_labels";
	private static final String DESCENDANT_LABEL_DEFAULT = "ontology_descendants_labels";
	private static final String ANCESTOR_LABEL_DEFAULT = "ontology_ancestors_labels";

	private boolean enabled;
	private String annotationField;
	private boolean includeIndirect;
	private boolean useLanguages;
	private String ontologyUri;
	private String labelField;
	private String parentField;
	private String childField;
	private String descendentField;
	private String ancestorField;
	private String parentLabel;
	private String childLabel;
	private String descendantLabel;
	private String ancestorLabel;
	private OntModel model;

	@Override
	public void init(@SuppressWarnings("rawtypes") final NamedList args) {
		if (args != null) {
			final SolrParams params = SolrParams.toSolrParams(args);
			this.enabled = params.getBool(ENABLED_PARAM, true);

			this.annotationField = params.get(ANNOTATION_FIELD_PARAM);
			this.includeIndirect = params.getBool(INCLUDE_INDIRECT_PARAM, false);
			this.useLanguages = params.getBool(USE_LANGUAGES, false);
			this.ontologyUri = params.get(ONTOLOGY_URI_PARAM);
			this.labelField = params.get(LABEL_FIELD_PARAM, LABEL_FIELD_DEFAULT);
			this.parentField = params.get(PARENT_FIELD_PARAM, PARENT_FIELD_DEFAULT);
			this.childField = params.get(CHILD_FIELD_PARAM, CHILD_FIELD_DEFAULT);
			this.descendentField = params.get(DESCENDANT_FIELD_PARAM, DESCENDANT_FIELD_DEFAULT);
			this.ancestorField = params.get(ANCESTOR_FIELD_PARAM, ANCESTOR_FIELD_DEFAULT);
			this.parentLabel = params.get(PARENT_LABEL_PARAM, PARENT_LABEL_DEFAULT);
			this.childLabel = params.get(CHILD_LABEL_PARAM, CHILD_LABEL_DEFAULT);
			this.descendantLabel = params.get(DESCENDANT_LABEL_PARAM, DESCENDANT_LABEL_DEFAULT);
			this.ancestorLabel = params.get(ANCESTOR_LABEL_PARAM, ANCESTOR_LABEL_DEFAULT);

			if (this.enabled) {
				loadOntologyModel();
			}
		}
	}

	/**
	 * loadOntologyModel
	 *
	 * Load the Ontology model from the Ontology URI provided in the conf
	 */
	private void loadOntologyModel() {
		// create an empty model
		this.model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_TRANS_INF);

		// use the FileManager to find the input file
		final InputStream in = FileManager.get().open(getOntologyUri());
		if (in == null) {
			throw new IllegalArgumentException("Ontology " + getOntologyUri() + " not found");
		}

		// read the RDF/XML file
		this.model.read(in, null);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public String getAnnotationField() {
		return annotationField;
	}

	public boolean isIncludeIndirect() {
		return includeIndirect;
	}

	public String getOntologyUri() {
		return ontologyUri;
	}

	public String getLabelField() {
		return labelField;
	}

	public String getParentField() {
		return parentField;
	}

	public String getChildField() {
		return childField;
	}

	public String getDescendentField() {
		return descendentField;
	}

	public String getAncestorField() {
		return ancestorField;
	}

	public String getParentLabel() {
		return parentLabel;
	}

	public String getChildLabel() {
		return childLabel;
	}

	public String getDescendantLabel() {
		return descendantLabel;
	}

	public String getAncestorLabel() {
		return ancestorLabel;
	}

	public boolean isUseLanguages() {
		return useLanguages;
	}

	@Override
	public UpdateRequestProcessor getInstance(final SolrQueryRequest req, final SolrQueryResponse resp, final UpdateRequestProcessor next) {
		return new OntologyUpdateProcessor(next);
	}

	class OntologyUpdateProcessor extends UpdateRequestProcessor {

		public OntologyUpdateProcessor(final UpdateRequestProcessor next) {
			super(next);
		}

		@Override
		public void processAdd(final AddUpdateCommand cmd) throws IOException {
			if (isEnabled()) {
				// retrieve the Solr doc
				final SolrInputDocument doc = cmd.getSolrInputDocument();

				final String resourceUri = (String) cmd.getSolrInputDocument().getFieldValue(getAnnotationField());

				// retrieve the resource from the ontology model
				final OntClass oc = model.getOntClass(resourceUri);

				// retrieve the resource labels
				final List<String> labels = new ArrayList<>();
				ExtendedIterator<RDFNode> labelsIterator = oc.listLabels(null);
				while (labelsIterator.hasNext()) {
					final Node label = labelsIterator.next().asNode();
					if (label.isLiteral()) {
						labels.add(label.getLiteralValue().toString());
						if (isUseLanguages() && label.getLiteralLanguage() != null && !label.getLiteralLanguage().isEmpty()) {
							doc.addField(getLabelField() + "_" + label.getLiteralLanguage().toLowerCase(), label.getLiteralValue().toString());
						}
					}
				}
				doc.addField(getLabelField(), labels);

				// retrieve the parent URIs & labels
				final List<String> parentUris = new ArrayList<String>();
				final List<String> parentLabels = new ArrayList<>();
				ExtendedIterator<OntClass> iterator = oc.listSuperClasses(true);
				while (iterator.hasNext()) {
					final OntClass parent = iterator.next();
					parentUris.add(parent.getURI());

					// retrieve the parent labels
					labelsIterator = parent.listLabels(null);
					while (labelsIterator.hasNext()) {
						final Node label = labelsIterator.next().asNode();
						if (label.isLiteral()) {
							parentLabels.add(label.getLiteralValue().toString());
							// Create a specific label field for each language
							// found
							if (isUseLanguages() && label.getLiteralLanguage() != null && !label.getLiteralLanguage().isEmpty()) {
								doc.addField(getParentLabel() + "_" + label.getLiteralLanguage().toLowerCase(), label.getLiteralValue().toString());
							}
						}
					}
				}
				doc.addField(getParentField(), parentUris);
				doc.addField(getParentLabel(), parentLabels);

				// retrieve the child URIs
				final List<String> childUris = new ArrayList<String>();
				final List<String> childLabels = new ArrayList<>();
				iterator = oc.listSubClasses(true);
				while (iterator.hasNext()) {
					final OntClass child = iterator.next();
					childUris.add(child.getURI());

					// retrieve the child labels
					labelsIterator = child.listLabels(null);
					while (labelsIterator.hasNext()) {
						final Node label = labelsIterator.next().asNode();
						if (label.isLiteral()) {
							childLabels.add(label.getLiteralValue().toString());
							// Create a specific label field for each language
							// found
							if (isUseLanguages() && label.getLiteralLanguage() != null && !label.getLiteralLanguage().isEmpty()) {
								doc.addField(getChildLabel() + "_" + label.getLiteralLanguage().toLowerCase(), label.getLiteralValue().toString());
							}
						}
					}
				}
				doc.addField(getChildField(), childUris);
				doc.addField(getChildLabel(), childLabels);

				// If include indirect retrieve the ancestors and descendants
				if (isIncludeIndirect()) {
					final List<String> descendantUris = new ArrayList<>();
					final List<String> descendantLabels = new ArrayList<>();
					iterator = oc.listSubClasses(false);
					while (iterator.hasNext()) {
						final OntClass descendant = iterator.next();
						descendantUris.add(descendant.getURI());

						// retrieve the descendant labels
						labelsIterator = descendant.listLabels(null);
						while (labelsIterator.hasNext()) {
							final Node label = labelsIterator.next().asNode();
							if (label.isLiteral()) {
								descendantLabels.add(label.getLiteralValue().toString());
								// Create a specific label field for each
								// language
								// found
								if (isUseLanguages() && label.getLiteralLanguage() != null && !label.getLiteralLanguage().isEmpty()) {
									doc.addField(getDescendantLabel() + "_" + label.getLiteralLanguage().toLowerCase(),
											label.getLiteralValue().toString());
								}
							}
						}
					}
					doc.addField(getDescendentField(), descendantUris);
					doc.addField(getDescendantLabel(), descendantLabels);

					final List<String> ancestorUris = new ArrayList<>();
					final List<String> ancestorLabels = new ArrayList<>();
					iterator = oc.listSuperClasses(false);
					while (iterator.hasNext()) {
						final OntClass ancestor = iterator.next();
						ancestorUris.add(ancestor.getURI());

						// retrieve the ancestor labels
						labelsIterator = ancestor.listLabels(null);
						while (labelsIterator.hasNext()) {
							final Node label = labelsIterator.next().asNode();
							if (label.isLiteral()) {
								ancestorLabels.add(label.getLiteralValue().toString());
								// Create a specific label field for each
								// language
								// found
								if (isUseLanguages() && label.getLiteralLanguage() != null && !label.getLiteralLanguage().isEmpty()) {
									doc.addField(getAncestorLabel() + "_" + label.getLiteralLanguage().toLowerCase(),
											label.getLiteralValue().toString());
								}
							}
						}
					}
					doc.addField(getAncestorField(), ancestorUris);
					doc.addField(getAncestorLabel(), ancestorLabels);
				}
			}

			// Run the next processor in the chain
			if (next != null) {
				next.processAdd(cmd);
			}
		}

	}

}
