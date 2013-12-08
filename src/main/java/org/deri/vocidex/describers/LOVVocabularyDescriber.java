package org.deri.vocidex.describers;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.deri.vocidex.JSONHelper;
import org.deri.vocidex.SPARQLRunner;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Produces a JSON description of a vocabulary, using the metadata present in LOV.
 * 
 * @author Richard Cyganiak, Pierre-Yves Vandenbussche
 */
public class LOVVocabularyDescriber extends SPARQLDescriber {
	public final static String TYPE = "vocabulary";

	public LOVVocabularyDescriber(SPARQLRunner source) {
		super(source);
	}
	
	public void describe(Resource vocabulary, ObjectNode descriptionRoot) {
		QuerySolution qs = getSource().getOneSolution("describe-lov-vocab.sparql", "vocab", vocabulary);
		descriptionRoot.put("type", TYPE);
		putString(descriptionRoot, "uri", vocabulary.getURI());
		putString(descriptionRoot, "prefix", qs.get("prefix").asLiteral().getLexicalForm()); 
		
		ResultSet rsTitles = getSource().getResultSet("describe-lov-vocab-titles.sparql", "vocab", vocabulary);
		ArrayNode arrayTitle = JSONHelper.createArray();
		while(rsTitles.hasNext()){
			QuerySolution qs2 = rsTitles.next();
			ObjectNode node = JSONHelper.createObject();
			putString(node, "value", qs2.get("title").asLiteral().getLexicalForm()); 
			putString(node, "lang", qs2.get("title").asLiteral().getLanguage()); 
			arrayTitle.add(node);
		}
		putURIArray(descriptionRoot, "titles", arrayTitle);
		
		ResultSet rsComments = getSource().getResultSet("describe-lov-vocab-descs.sparql", "vocab", vocabulary);
		ArrayNode arrayComments = JSONHelper.createArray();
		while(rsComments.hasNext()){
			QuerySolution qs2 = rsComments.next();
			ObjectNode node = JSONHelper.createObject();
			putString(node, "value", qs2.get("description").asLiteral().getLexicalForm()); 
			putString(node, "lang", qs2.get("description").asLiteral().getLanguage()); 
			arrayComments.add(node);
		}
		putURIArray(descriptionRoot, "descriptions", arrayComments);
	}	
}
