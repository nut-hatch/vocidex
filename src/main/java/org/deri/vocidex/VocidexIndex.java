package org.deri.vocidex;

import java.io.Closeable;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

/**
 * A connection to a specific named index on an ElasticSearch cluster
 * 
 * @author Richard Cyganiak
 */
public class VocidexIndex implements Closeable {
	private final String clusterName;
	private final String hostName;
	private final String indexName;
	private final String mappingType;
	private Client client = null;
	
	public VocidexIndex(String clusterName, String hostName, String indexName, String mappingType) {
		this.clusterName = clusterName;
		this.hostName = hostName;
		this.indexName = indexName;
		this.mappingType = mappingType;
	}

	/**
	 * Connects to the cluster if not yet connected. Is called implicitly by
	 * all operations that require a connection. 
	 */
	public void connect() {
		if (client != null) return;
		Settings settings = Settings.builder().put("cluster.name", clusterName).build();
		try {
			client = new PreBuiltTransportClient(settings).addTransportAddress(new TransportAddress(InetAddress.getByName(hostName), 9300));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	public void close() {
		if (client == null) return;
		client.close();
		client = null;
	}
	
	public boolean exists() {
		connect();
		return client.admin().indices().exists(Requests.indicesExistsRequest(indexName)).actionGet().isExists();
	}
	
	public void delete() {
		connect();
		client.admin().indices().prepareDelete(indexName).execute();		
	}
	
	public boolean create() {
		connect();
		if (!client.admin().indices().create(Requests.createIndexRequest(indexName).settings(JSONHelper.readFile("mappings/settings.json"), XContentType.JSON)).actionGet().isAcknowledged()) {
			return false;
		}

		String mappingFile = "mappings/"+mappingType+".json";
		if (!client.admin().indices().preparePutMapping().setIndices(indexName).setType(mappingType).setSource(JSONHelper.readFile(mappingFile), XContentType.JSON).execute().actionGet().isAcknowledged()) {
			return false;
		}

//		//create index with specific settings
//		if (!client.admin().indices().create(Requests.createIndexRequest(indexName).settings(JSONHelper.readFile("mappings/settings.json"))).actionGet().isAcknowledged()) {
//			return false;
//		}
//		// TODO: Add mappings/common.json for the shared stuff
//		if (!setMapping("class", "mappings/class.json")) return false;
//		if (!setMapping("property", "mappings/property.json")) return false;
//		if (!setMapping("datatype", "mappings/datatype.json")) return false;
//		if (!setMapping("instance", "mappings/instance.json")) return false;
//		if (!setMapping("vocabulary", "mappings/vocabulary.json")) return false;
		
		return true;
	}
	
//	public boolean setMapping(String type, String jsonConfigFile) {
//		String json = JSONHelper.readFile(jsonConfigFile);
//		if (!client.admin().indices().preparePutMapping().setIndices(indexName).setType(type).setSource(json).execute().actionGet().isAcknowledged()) {
//			return false;
//		}
//		return true;
//	}
	
	/**
	 * Adds a document (that is, a JSON structure) to the index.
	 * @return The document's id
	 */
	public String addDocument(VocidexDocument document) {
//		System.out.println(document.getId());
//		System.out.println(document.getJSONContents());
		return client
				.prepareIndex(indexName, document.getType(), document.getId())
				.setSource(document.getJSONContents(), XContentType.JSON)
				.execute().actionGet().getId();
	}
}
