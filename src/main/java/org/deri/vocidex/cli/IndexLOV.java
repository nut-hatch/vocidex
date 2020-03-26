package org.deri.vocidex.cli;

import java.util.Iterator;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.deri.vocidex.JSONHelper;
import org.deri.vocidex.SPARQLRunner;
import org.deri.vocidex.VocidexDocument;
import org.deri.vocidex.VocidexException;
import org.deri.vocidex.VocidexIndex;
import org.deri.vocidex.describers.LOVTermMetricsDescriber;
import org.deri.vocidex.extract.LOVExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import arq.cmdline.CmdGeneral;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.shared.NotFoundException;

/**
 * A command line tool that indexes an LOV dump, adding all vocabularies
 * and their terms to the index. Uses {@link LOVExtractor}.
 * 
 * @author Richard Cyganiak
 */
public class IndexLOV extends CmdGeneral {
	private final static Logger log = LoggerFactory.getLogger(IndexLOV.class);
	
	public static void main(String... args) {
		new IndexLOV(args).mainRun();
	}

	private String clusterName;
	private String hostName;
	private String vocabularyIndexName;
	private String classIndexName;
	private String propertyIndexName;
	private String lovDumpFile;
	
	public IndexLOV(String[] args) {
		super(args);
		getUsage().startCategory("Arguments");
		getUsage().addUsage("clusterName", "ElasticSearch cluster name (e.g., elasticsearch)");
		getUsage().addUsage("hostname", "ElasticSearch hostname (e.g., localhost)");
		getUsage().addUsage("vocabularyIndexName", "Target ElasticSearch VOCABULARY index (e.g., vocabularies)");
		getUsage().addUsage("classIndexName", "Target ElasticSearch CLASS index (e.g., classes)");
		getUsage().addUsage("propertyIndexName", "Target ElasticSearch PROPERTY index (e.g., properties)");
		getUsage().addUsage("lov.nq", "Filename or URL of the LOV N-Quads dump");
	}
	
	@Override
    protected String getCommandName() {
		return "index-lov";
	}
	
	@Override
	protected String getSummary() {
		return getCommandName() + " clusterName hostname vocabularyIndexName classIndexName propertyIndexName indexName lov.nq";
	}

	@Override
	protected void processModulesAndArgs() {
		if (getPositional().size() < 6) {
			doHelp();
		}
		clusterName = getPositionalArg(0);
		hostName = getPositionalArg(1);
		vocabularyIndexName = getPositionalArg(2);
		classIndexName = getPositionalArg(3);
		propertyIndexName = getPositionalArg(4);
		lovDumpFile = getPositionalArg(5);
	}

	@Override
	protected void exec() {
		try {
			log.info("Loading LOV dump: " + lovDumpFile);
			Dataset dataset = RDFDataMgr.loadDataset(lovDumpFile, Lang.NQUADS);
			long graphCount = 1;
			long tripleCount = dataset.getDefaultModel().size();
			Iterator<String> it = dataset.listNames();
			while (it.hasNext()) {
				graphCount++;
				tripleCount += dataset.getNamedModel(it.next()).size();
			}
			log.info("Read " + tripleCount + " triples in " + graphCount + " graphs");

//			String[] types = {"class","property","vocabulary"};

			extractType(dataset,"vocabulary",vocabularyIndexName);
			extractType(dataset,"class",classIndexName);
			extractType(dataset,"property",propertyIndexName);

//			VocidexIndex vocabularyIndex = new VocidexIndex(clusterName, hostName, vocabularyIndexName, "vocabulary");
//
//			for (String type : types) {
//				VocidexIndex index = new VocidexIndex(clusterName, hostName, indexName, type);
//				try {
//					if (!index.exists()) {
//						throw new VocidexException("Index '" + indexName + "' does not exist on the cluster. Create the index first!");
//					}
//					LOVExtractor lovTransformer = new LOVExtractor(dataset);
//					for (VocidexDocument document: lovTransformer) {
//						log.info("Indexing " + document.getId());
//						String resultId = index.addDocument(document);
//						log.debug("Added new " + document.getType() + ", id " + resultId);
//					}
//					log.info("Done!");
//				} finally {
//					index.close();
//				}
//			}
		} catch (NotFoundException ex) {
			cmdError("Not found: " + ex.getMessage());
		} catch (VocidexException ex) {
			cmdError(ex.getMessage());
		}
	}

	private void extractType(Dataset dataset, String mappingType, String indexName) {
		VocidexIndex index = new VocidexIndex(clusterName, hostName, indexName, mappingType);
		try {
			if (!index.exists()) {
				throw new VocidexException("Index '" + indexName + "' does not exist on the cluster. Create the index first!");
			}
			LOVExtractor lovTransformer = new LOVExtractor(dataset);
			for (VocidexDocument document: lovTransformer) {
			    if (document.getType().equals(mappingType)) {
                    log.info("Indexing " + document.getId());
                    String resultId = index.addDocument(document);
                    log.debug("Added new " + document.getType() + ", id " + resultId);
                }
			}
			log.info("Done!");
		} finally {
			index.close();
		}
	}
}
