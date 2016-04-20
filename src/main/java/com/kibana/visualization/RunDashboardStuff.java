package com.kibana.visualization;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.TreeMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.lucene.store.SleepingLockWrapper;
//import org.apache.logging.log4j.core.Logger;
import org.elasticsearch.index.mapper.object.ObjectMapper;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.hsd.es_url.utils.NetworkUtils;

public class RunDashboardStuff {

	// static Logger log = Logger.getLogger(RunDashboardStuff.class);
	 private static Logger log = LoggerFactory.getLogger(RunDashboardStuff.class);

	private static final String USERNAME = "fenkam";

	private static final String PASSWORD = "mysecret";

	public static void main(String[] args) {
		//String log4jConfPath = "log4j.properties";
		// PropertyConfigurator.configure(log4jConfPath);

		// createVisual("" + 54743, "rio-mcluster*");
		// createSampleVisualization();.
		
		createAllVisualizations("robert-roberta-system-metrics-2016.04.19", "63423");
	}

	public static void createAllVisualizations(String indexPattern, String kibanaPort) {
		String id1 = "Count-by-time";
//		String id2 = "Cpu-usage(idle,-nice,-system,-user,-iowait)";
		String id2 = "Cpu-usage";
//		String id3 = "File-system-(avai,-used,-total)";
		String id3 = "File-System";
		String id4 = "Max-cpu.user";
		String id5 = "Max-fs.used";
		String id6 = "Max-mem.actual_used" ;
		String id7 = "Memory-used-over-total";
		String id8 = "average-of-memory-actualy-used";
		createVisualizationArea("Cpu-usage", indexPattern, kibanaPort, "cpu.nice", "cpu.idle", "cpu.user", "cpu.iowait",
				"cpu.system");
		createVisualizationArea("Memory", indexPattern, kibanaPort, "mem.total", "mem.actual_used", "mem.actual_free");
		createVisualizationArea("File-System", indexPattern, kibanaPort, "fs.total", "fs.avail", "fs.files");

		createVisualizationPie("Count-by-time", indexPattern, kibanaPort, "count");
		createVisualizationPie("CPU-usage-nice-by-time", indexPattern, kibanaPort, "cardinality");
		
		createVisualizationMetric("Max-mem.actual_used", indexPattern, kibanaPort,"mem.actual_used");
		createVisualizationMetric("Max-cpu.user", indexPattern, kibanaPort,"cpu.user");
		createVisualizationMetric("Max-fs.used", indexPattern, kibanaPort,"fs.used");
		
	   createVisualizationLine("average-of-used-memory-over-available",  indexPattern, kibanaPort, "9" , "mem.used", "max", "avg");
	   createVisualizationLine("Memory-used-over-total",  indexPattern, kibanaPort, "13" , "mem.used", "sum", "sum");
	   createVisualizationLine("average-of-memory-actualy-used",  indexPattern, kibanaPort, "7" , "mem.actual_used", "max", "avg");
	   
	   createDashboard("dashboard", kibanaPort, id1, id2, id3, id4, id5, id6, id7, id8);
	}

	public static void createVisualizationPie(String id, String indexPattern, String kibanaPort, String type) {

		try {
			TreeMap<String, String> requestMap = new TreeMap();
			JSONObject body = new JSONObject();
			body = new JSONObject();
			body.put("title", id);
			body.put("type", "visualization");

			JSONObject visState = new JSONObject();
			visState.put("title", id);
			visState.put("type", "pie");
			
			visState.put("params", new JSONObject());
			
			visState.getJSONObject("params").put("shareYAxis", true);String id2 = "Cpu-usage(idle,-nice,-system,-user,-iowait)";
			
			visState.getJSONObject("params").put("addTooltip", true);
			visState.getJSONObject("params").put("addLegend", true);
			visState.getJSONObject("params").put("isDonut", false);
			
			visState.put("aggs", new JSONArray());
			
			JSONObject agg = new JSONObject();

			agg = new JSONObject();
			agg.put("id", "1");
			agg.put("type", "date_histogram");
			agg.put("schema", "split");
			agg.put("params", new JSONObject());
			agg.getJSONObject("params").put("field", "@timestamp");
			agg.getJSONObject("params").put("interval", "m");
			agg.getJSONObject("params").put("customInterval", "2h");
			agg.getJSONObject("params").put("min_doc_count", 1);
			agg.getJSONObject("params").put("extended_bounds", new JSONObject());
			agg.getJSONObject("params").put("row", true);
			visState.getJSONArray("aggs").put(agg);

			if (type.equals("cardinality")) {
				agg = new JSONObject();
				agg.put("id", "2");
				agg.put("type", "cardinality");
				agg.put("schema", "metric");
				agg.put("params", new JSONObject());
				agg.getJSONObject("params").put("field", "cpu.nice");
				visState.getJSONArray("aggs").put(agg);
			} else if (type.equals("count")) {
				agg = new JSONObject();
				agg.put("id", "2");
				agg.put("type", "count");
				agg.put("schema", "metric");
				agg.put("params",new JSONObject());
				visState.getJSONArray("aggs").put(agg);
			}

			visState.put("listeners", new JSONObject());
			body.put("visState", visState.toString());
			body.put("uiStateJSON", "{}");
			body.put("description", "");
			body.put("version", 1);
			body.put("kibanaSavedObjectMeta", new JSONObject());

			JSONObject searchSourceJSON = new JSONObject();
			searchSourceJSON.put("index", indexPattern);
			searchSourceJSON.put("query", new JSONObject());
			searchSourceJSON.getJSONObject("query").put("query_string",  new JSONObject());
			searchSourceJSON.getJSONObject("query").getJSONObject("query_string").put("query", "*");
			searchSourceJSON.getJSONObject("query").getJSONObject("query_string").put("analyze_wildcard", true);
			searchSourceJSON.put("filter", new JSONArray());
			body.getJSONObject("kibanaSavedObjectMeta").put("searchSourceJSON", searchSourceJSON.toString());
			

			NetworkUtils.getUrlMetodText(
					requestMap, body.toString(), "POST", "http://localkibana.awswouri.com:" + kibanaPort
							+ "/elasticsearch/.kibana" + kibanaPort + "/visualization/" + id + "?op_type:create",
					USERNAME, PASSWORD);

		} catch (InvalidKeyException | NoSuchAlgorithmException | IllegalStateException | IOException
				| JSONException e1) {

			log.error("An error occur when creating visualization",e1.getMessage());

		} 
	}

	public static void createVisualizationArea(String id, String indexPattern, String kibanaPort, String... criterias) {
		
		try {
			TreeMap<String, String> requestMap = new TreeMap();
			JSONObject body = new JSONObject();
			body = new JSONObject();
			body.put("title", id);
			// body.put("id", "Memory");
			body.put("type", "visualization");

			JSONObject visState = new JSONObject();
			visState.put("title", id);
			visState.put("type", "area");
			visState.put("params", new JSONObject());
			visState.getJSONObject("params").put("shareYAxis", true);
			visState.getJSONObject("params").put("addTooltip", true);
			visState.getJSONObject("params").put("addLegend", true);
			visState.getJSONObject("params").put("smoothLines", true);
			visState.getJSONObject("params").put("scale", "linear");
			visState.getJSONObject("params").put("interpolate", "linear");
			visState.getJSONObject("params").put("mode", "stacked");
			visState.getJSONObject("params").put("times", new JSONArray());
			visState.getJSONObject("params").put("addTimeMarker", true);
			visState.getJSONObject("params").put("defaultYExtents", false);
			visState.getJSONObject("params").put("setYExtents", false);
			visState.getJSONObject("params").put("yAxis", new JSONObject());

			visState.put("aggs", new JSONArray());

			JSONObject agg = new JSONObject();
			agg = new JSONObject();
			agg.put("id", "1");
			agg.put("type", "date_histogram");
			agg.put("schema", "segment");
			agg.put("params", new JSONObject());
			agg.getJSONObject("params").put("field", "@timestamp");
			agg.getJSONObject("params").put("interval", "m");
			agg.getJSONObject("params").put("customInterval", "2h");
			agg.getJSONObject("params").put("min_doc_count", 1);
			agg.getJSONObject("params").put("extended_bounds", new JSONObject());
			visState.getJSONArray("aggs").put(agg);

			int i = 1;
			for (String criteria : criterias) {
				agg = new JSONObject();
				agg.put("id", "" + (++i));
				agg.put("type", "cardinality");
				agg.put("schema", "metric");
				agg.put("params", new JSONObject());
				agg.getJSONObject("params").put("field", criteria);
				visState.getJSONArray("aggs").put(agg);

			}

			visState.put("listeners", "{}");
			body.put("visState", visState.toString());
			body.put("uiStateJSON", "{}");
			body.put("description", "");
			body.put("version", 1);
			body.put("kibanaSavedObjectMeta", new JSONObject());

			JSONObject searchSourceJSON = new JSONObject();
			searchSourceJSON.put("index", indexPattern);
			searchSourceJSON.put("query", new JSONObject());
			searchSourceJSON.getJSONObject("query").put("query_string", new JSONObject());
			searchSourceJSON.getJSONObject("query").getJSONObject("query_string").put("query", "*");
			searchSourceJSON.getJSONObject("query").getJSONObject("query_string").put("analyze_wildcard", true);
			searchSourceJSON.put("filter", new JSONArray());
			body.getJSONObject("kibanaSavedObjectMeta").put("searchSourceJSON", searchSourceJSON.toString());

			NetworkUtils.getUrlMetodText(
					requestMap, body.toString(), "POST", "http://localkibana.awswouri.com:" + kibanaPort
							+ "/elasticsearch/.kibana" + kibanaPort + "/visualization/" + id + "?op_type=create",
					USERNAME, PASSWORD);
			

		} catch (InvalidKeyException | NoSuchAlgorithmException | IllegalStateException | IOException
				| JSONException e1) {
			log.error("An error occur when creating visualization",e1.getMessage());

		}
	}

	public static void createVisualizationLine(String id, String indexPattern, String kibanaPort, String radius , String field, String type1, String type2) {

		try {
			TreeMap<String, String> requestMap = new TreeMap();
			JSONObject body = new JSONObject();
			body = new JSONObject();
			body.put("title", id);
			body.put("type", "visualization");

			JSONObject visState = new JSONObject();
			visState.put("title", id);
			visState.put("type", "line");
			visState.put("params", new JSONObject());
			visState.getJSONObject("params").put("shareYAxis", true);
			visState.getJSONObject("params").put("addTooltip", true);
			visState.getJSONObject("params").put("addLegend", true);
			visState.getJSONObject("params").put("showCircles", true);
			visState.getJSONObject("params").put("smoothLines", true);
			visState.getJSONObject("params").put("scale", "linear");
			visState.getJSONObject("params").put("interpolate", "linear");
			visState.getJSONObject("params").put("drawLinesBetweenPoints", true);
			visState.getJSONObject("params").put("radiusRatio", radius);
			visState.getJSONObject("params").put("addTimeMarker", true);
			visState.getJSONObject("params").put("defaultYExtents", false);
			visState.getJSONObject("params").put("setYExtents", false);
			visState.getJSONObject("params").put("yAxis", new JSONObject());
			
			visState.put("aggs", new JSONArray());

			JSONObject agg = new JSONObject();
			agg = new JSONObject();
			agg.put("id", "1");
			agg.put("type", type1);
			agg.put("schema", "metric");
			agg.put("params", new JSONObject());
			agg.getJSONObject("params").put("field", "mem.total");
			agg.getJSONObject("params").put("id", "3");
			agg.getJSONObject("params").put("type", "date_histogram");
			agg.getJSONObject("params").put("schema", "segment");
			agg.getJSONObject("params").put("params", new JSONObject());
			agg.getJSONObject("params").getJSONObject("params").put("field", "@timestamp");
			agg.getJSONObject("params").getJSONObject("params").put("interval", "m");
			agg.getJSONObject("params").getJSONObject("params").put("customInterval", "2h");
			agg.getJSONObject("params").getJSONObject("params").put("min_doc_count", "1");
			agg.getJSONObject("params").getJSONObject("params").put("extended_bounds", "{}");
			visState.getJSONArray("aggs").put(agg);

			agg = new JSONObject();
			agg.put("id", "4");
			agg.put("type", type2);
			agg.put("schema", "metric");
			agg.put("params", new JSONObject());
			agg.getJSONObject("params").put("field", field);
			visState.getJSONArray("aggs").put(agg);

			visState.put("listeners", "{}");
			body.put("visState", visState.toString());
			body.put("uiStateJSON", "{}");
			body.put("description", "");
			body.put("version", 1);
			body.put("kibanaSavedObjectMeta", new JSONObject());

			JSONObject searchSourceJSON = new JSONObject();
			searchSourceJSON.put("index", indexPattern);
			searchSourceJSON.put("query", new JSONObject());
			searchSourceJSON.getJSONObject("query").put("query_string", new JSONObject());
			searchSourceJSON.getJSONObject("query").getJSONObject("query_string").put("query", "*");
			searchSourceJSON.getJSONObject("query").getJSONObject("query_string").put("analyze_wildcard", true);
			searchSourceJSON.put("filter", new JSONArray());
			body.getJSONObject("kibanaSavedObjectMeta").put("searchSourceJSON", searchSourceJSON.toString());

			NetworkUtils.getUrlMetodText(
					requestMap, body.toString(), "POST", "http://localkibana.awswouri.com:" + kibanaPort
							+ "/elasticsearch/.kibana" + kibanaPort + "/visualization/" + id + "?op_type=create",
					USERNAME, PASSWORD);
		

		} catch (InvalidKeyException | NoSuchAlgorithmException | IllegalStateException | IOException
				| JSONException e1) {
			log.error("An error occur when creating visualization",e1.getMessage());

		}
	}
	

	public static void createVisualizationMetric(String id, String indexPattern, String kibanaPort, String field) {
		
		try {
			
			TreeMap<String, String> requestMap = new TreeMap();
			JSONObject body = new JSONObject();
			//body = new JSONObject();
			body.put("title", id);
//			body.put("id", "Memory");
			body.put("type", "visualization");
			
			JSONObject visState = new JSONObject();
			visState.put("title", id);
			visState.put("type", "metric");
			visState.put("params", new JSONObject());
			visState.getJSONObject("params").put("fontSize", "35");
			visState.put("aggs", new JSONArray());
			
			JSONObject agg = new JSONObject();
			agg = new JSONObject();
			agg.put("id", "1");
			agg.put("type", "max");
			agg.put("schema", "metric");
			agg.put("params", new JSONObject());
			visState.getJSONArray("aggs").put(agg);
			agg.getJSONObject("params").put("field", field);
			visState.put("listeners", "{}");
			body.put("visState", visState.toString());
			
			JSONObject uiStateJSON = new JSONObject();
			uiStateJSON.put("spy", new JSONObject());
			uiStateJSON.getJSONObject("spy").put("mode", new JSONObject());
			uiStateJSON.getJSONObject("spy").getJSONObject("mode").put("name", "null");
			uiStateJSON.getJSONObject("spy").getJSONObject("mode").put("fill", false);
			
			body.put("uiStateJSON", uiStateJSON.toString());
			body.put("description", "");
			body.put("version", 1);
			body.put("kibanaSavedObjectMeta", new JSONObject());

			JSONObject searchSourceJSON = new JSONObject();
			searchSourceJSON.put("index", indexPattern);
			searchSourceJSON.put("query", new JSONObject());
			searchSourceJSON.getJSONObject("query").put("query_string", new JSONObject());
			searchSourceJSON.getJSONObject("query").getJSONObject("query_string").put("query", "*");
			searchSourceJSON.getJSONObject("query").getJSONObject("query_string").put("analyze_wildcard", true);
			searchSourceJSON.put("filter", new JSONArray());
			body.getJSONObject("kibanaSavedObjectMeta").put("searchSourceJSON", searchSourceJSON.toString());
			
			
			NetworkUtils.getUrlMetodText(
					requestMap, body.toString(), "POST", "http://localkibana.awswouri.com:" + kibanaPort
							+ "/elasticsearch/.kibana" + kibanaPort + "/visualization/" + id + "?op_type=create",
					USERNAME, PASSWORD);

		} catch (InvalidKeyException | NoSuchAlgorithmException | IllegalStateException | IOException
				| JSONException e1) {
			log.error("An error occur when creating visualization",e1.getMessage());

		}
	}
	
	
	public static void createDashboard(String id, String kibanaPort, String id1, String id2, String id3, String id4, String id5, String id6, String id7, String id8) {
		
		try {
			TreeMap<String, String> requestMap = new TreeMap();
			JSONObject body = new JSONObject();
			body = new JSONObject();
			body.put("title", id);
			// body.put("id", "Memory");
			body.put("type", "dashboard");
			body.put("hits", 0);
			body.put("description", "");
			body.put("panelsJSON", new JSONArray());
			
			//JSONArray panelsJSON = new JSONArray();
			JSONArray panelsJSONS=new JSONArray();
			
			JSONObject panelsJson1 = new JSONObject();
			panelsJson1.put("col", 10);
			panelsJson1.put("id", id1);
			panelsJson1.put("panelIndex", 1);
			panelsJson1.put("row", 3);
			panelsJson1.put("size_x", 3);
			panelsJson1.put("size_y", 2);
			panelsJson1.put("type","visualization");
			panelsJSONS.put(panelsJson1);
			
			JSONObject panelsJson2 = new JSONObject();
			panelsJson2.put("col", 4);
			panelsJson2.put("id", id2);
			panelsJson2.put("panelIndex", 2);
			panelsJson2.put("row", 1);
			panelsJson2.put("size_x", 6);
			panelsJson2.put("size_y", 4);
			panelsJson2.put("type","visualization");
			panelsJSONS.put(panelsJson2);
			
			JSONObject panelsJson3 = new JSONObject();
			panelsJson3.put("col", 7);
			panelsJson3.put("id", id3);
			panelsJson3.put("panelIndex", 3);
			panelsJson3.put("row", 5);
			panelsJson3.put("size_x", 3);
			panelsJson3.put("size_y", 2);
			panelsJson3.put("type","visualization");
			panelsJSONS.put(panelsJson3);
			
			JSONObject panelsJson4 = new JSONObject();
			panelsJson4.put("col", 1);
			panelsJson4.put("id", id4);
			panelsJson4.put("panelIndex", 4);
			panelsJson4.put("row", 5);
			panelsJson4.put("size_x", 3);
			panelsJson4.put("size_y", 2);
			panelsJson4.put("type","visualization");
			panelsJSONS.put(panelsJson4);
			
			
			JSONObject panelsJson5 = new JSONObject();
			panelsJson5.put("col", 1);
			panelsJson5.put("id", id5);
			panelsJson5.put("panelIndex", 5);
			panelsJson5.put("row", 1);
			panelsJson5.put("size_x", 3);
			panelsJson5.put("size_y", 2);
			panelsJson5.put("type","visualization");
			panelsJSONS.put(panelsJson5);
			
			JSONObject panelsJson6 = new JSONObject();
			panelsJson6.put("col", 1);
			panelsJson6.put("id", id6);
			panelsJson6.put("panelIndex", 6);
			panelsJson6.put("row", 3);
			panelsJson6.put("size_x", 3);
			panelsJson6.put("size_y", 2);
			panelsJson6.put("type","visualization");
			panelsJSONS.put(panelsJson6);
			
			JSONObject panelsJson7 = new JSONObject();
			panelsJson7.put("col", 10);
			panelsJson7.put("id", id7);
			panelsJson7.put("panelIndex", 8);
			panelsJson7.put("row", 1);
			panelsJson7.put("size_x", 3);
			panelsJson7.put("size_y", 2);
			panelsJson7.put("type","visualization");
			panelsJSONS.put(panelsJson7);
			
			JSONObject panelsJson8 = new JSONObject();
			panelsJson8.put("id", id8);
			panelsJson8.put("panelIndex", 11);
			panelsJson8.put("row", 5);
			panelsJson8.put("size_x", 3);
			panelsJson8.put("size_y", 2);
			panelsJson8.put("type","visualization");
			panelsJSONS.put(panelsJson8);
			
			body.getJSONArray("panelsJSON").put(panelsJSONS.toString());
			
//			JSONObject optionsJSON = new JSONObject();
//			optionsJSON.put("darkTheme", false);
//			body.put("optionJSON", optionsJSON.toString());
			
			//body.put("uiStateJSON", "{}");
			body.put("version", 1);
			body.put("timeRestore", true);
			body.put("timeTo", "now");
			body.put("timeFrom", "now-15m");
		
			body.put("kibanaSavedObjectMeta", new JSONObject());

			JSONObject searchSourceJSON = new JSONObject();
			JSONArray params = new JSONArray();
			
			JSONObject paramss = new JSONObject();
			paramss.put("query", new JSONObject());
			paramss.getJSONObject("query").put("query_string", new JSONObject());
			paramss.getJSONObject("query").getJSONObject("query_string").put("analyze_wildcard", true);
			paramss.getJSONObject("query").getJSONObject("query_string").put("query", "*");
			paramss.put("filter", new JSONArray());
			
			params.put(paramss);
			searchSourceJSON.put("filter", params);
			
			body.getJSONObject("kibanaSavedObjectMeta").put("searchSourceJSON", searchSourceJSON.toString());

			NetworkUtils.getUrlMetodText(
					requestMap, body.toString(), "POST", "http://localkibana.awswouri.com:" + kibanaPort
							+ "/elasticsearch/.kibana" + kibanaPort + "/dashboard/" + id + "?op_type=create",
					USERNAME, PASSWORD);
			

		} catch (InvalidKeyException | NoSuchAlgorithmException | IllegalStateException | IOException
				| JSONException e1) {
			log.error("An error occur when creating visualization",e1.getMessage());

		}
	}
	

}