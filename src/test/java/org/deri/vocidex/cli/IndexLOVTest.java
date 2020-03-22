package org.deri.vocidex.cli;

import com.hp.hpl.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

public class IndexLOVTest {

    @Test
    public void loadDataset() {
        Dataset dataset = RDFDataMgr.loadDataset("./dumps/2020-03-22_lov.nq", Lang.NQUADS);
        long graphCount = 1;
        long tripleCount = dataset.getDefaultModel().size();
        Iterator<String> it = dataset.listNames();
        while (it.hasNext()) {
            graphCount++;
            tripleCount += dataset.getNamedModel(it.next()).size();
        }
        System.out.println("Read " + tripleCount + " triples in " + graphCount + " graphs");
        assertEquals(906946, tripleCount);
        assertEquals(696, graphCount);
    }

}