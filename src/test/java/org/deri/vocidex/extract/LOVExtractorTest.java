package org.deri.vocidex.extract;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.*;

public class LOVExtractorTest {

    Dataset dataset = RDFDataMgr.loadDataset("./dumps/2020-03-22_lov.nq", Lang.NQUADS);
    LOVExtractor lovTransformer = new LOVExtractor(dataset);


    @Test
    public void listVocabularies() {
        Collection<Resource> vocs = lovTransformer.listVocabularies();
//        System.out.println(vocs.size());
        assertEquals(698, vocs.size());
//        for (Resource voc : vocs) {
//            System.out.println(voc);
//        }
    }

    @Test
    public void listDefinedTerms() {
        Collection<Resource> vocs = lovTransformer.listVocabularies();
        Collection<Resource> terms = lovTransformer.listDefinedTerms(vocs.iterator().next());
//                System.out.println(terms.size());
        assertEquals(47, terms.size());
//        for (Resource term : terms) {
//            System.out.println(term);
//        }
    }
}