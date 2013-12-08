package org.deri.vocidex.describers;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.deri.vocidex.JSONHelper;
import org.deri.vocidex.LovConstants;
import org.deri.vocidex.SPARQLRunner;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Provides String Literals for resources,
 * either by querying a SPARQL source for
 * rdfs:label and other properties, or if that fails, then by synthesizing
 * a label from the URI.
 * 
 * @author Pierre-Yves Vandenbussche
 */
public class StrLiteralDescriber extends SPARQLDescriber {
	
	public StrLiteralDescriber(SPARQLRunner source) {
		super(source);
	}

	@Override
	public void describe(Resource term, ObjectNode descriptionRoot) {
		
		//fetch the String and untyped literals along with properties associated to the term
		ResultSet rs = getSource().getResultSet("term-strLiterals.sparql", "term", term);
		ArrayNode arrayPrim = JSONHelper.createArray();
		ArrayNode arraySec = JSONHelper.createArray();
		ArrayNode arrayTer = JSONHelper.createArray();
		while(rs.hasNext()){
			QuerySolution qs2 = rs.next();
			
			String property = qs2.get("p").asResource().getURI();
			ObjectNode node = JSONHelper.createObject();
			putString(node, "prop", property);
			putString(node, "value", qs2.get("literal").asLiteral().getLexicalForm());
			putString(node, "lang", qs2.get("literal").asLiteral().getLanguage());
			
			//primary label
			if(property.equals(LovConstants.RDFS_FULL_LABEL)
				|| property.equals(LovConstants.DC_TERMS_FULL_TITLE)
				|| property.equals(LovConstants.DC_ELEMENT_FULL_TITLE)
				|| property.equals(LovConstants.SKOS_FULL_PREF_LABEL)){
				arrayPrim.add(node);
			}
			else{
				//secondary Label
				if(property.equals(LovConstants.RDFS_FULL_COMMENT)
					|| property.equals(LovConstants.DC_TERMS_FULL_DESCRIPTION)
					|| property.equals(LovConstants.DC_ELEMENT_FULL_DESCRIPTION)
					|| property.equals(LovConstants.SKOS_FULL_ALT_LABEL)){
					arraySec.add(node);
				}
				else{//all other cases -> tertiary label
					arrayTer.add(node);
				}
			}
		}
		
		//if no primary label, synthesize one from the URI localName
		if(arrayPrim.size()==0){
			ObjectNode node = JSONHelper.createObject();
			putString(node, "value", synthesizeLabelFromURI(term.getURI()));
			arrayPrim.add(node);
		}
		putURIArray(descriptionRoot, "primLabels", arrayPrim);
		putURIArray(descriptionRoot, "secLabels", arraySec);
		putURIArray(descriptionRoot, "terLabels", arrayTer);
	}

	
	/**
	 * Generates a label from the URI. Currently simply takes the local name.
	 */
	public String synthesizeLabelFromURI(String uri) {
		try {
			uri = URLDecoder.decode(uri, "utf-8");
		} catch (UnsupportedEncodingException ex) {
			// Can't happen, UTF-8 is always supported
		}
		uri = uri.
				replaceFirst("^.*[#:/](.+?)[#:/]*$", "$1").
				replaceAll("_+", " ").
				replaceAll("(\\p{Ll})(\\p{Lu})", "$1 $2");
		Matcher matcher = uppercaseWordPattern.matcher(uri);
		StringBuffer result = new StringBuffer();
		while (matcher.find()) {
			matcher.appendReplacement(result, matcher.group().toLowerCase());
		}
		matcher.appendTail(result);
		return result.toString();
	}
	private final static Pattern uppercaseWordPattern =	Pattern.compile("(?<!\\p{Lu})(\\p{Lu})(?!\\p{Lu})");

}
