package it.frusso.util;



import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Logger;


public class ServiceHelper {

	public static final String CONTENT_TYPE_applicationJson = "application/json";
	public static final String CONTENT_TYPE_applicationXml= "application/xml";
	public static final String CONTENT_TYPE_playText = "play/text";
	public static final String CONTENT_TYPE_wwwFormUrlencoded = "application/x-www-form-urlencoded";

	private boolean traceMode = false;
	
	public void setTraceMode(boolean traceMode) {
		this.traceMode = traceMode;
	}
	

	
	public ServiceResponse doGet(ServiceRequest request) throws Exception {

		String url = request.endpoint;
		if (traceMode) System.out.println("Endpoint: " + url);;
		
		//Map<String, String> headerParams = request.headerParams; 
		//Map<String, String> httpParams = request.httpParams;
		
		URIBuilder builder = new URIBuilder(url);
		
		if (request.httpParams != null)	{
			for(String hKey : request.httpParams.keySet()) {
				builder.addParameter(hKey, request.httpParams.get(hKey));
				if (traceMode) System.out.println(" - httpParameter: " + hKey + " = " + request.httpParams.get(hKey));
			}
		}

		HttpClient httpClient = HttpClientBuilder.create().build();
//		final String finalUrl = builder.build().toString();
		HttpGet httpMethod = new HttpGet(url);
		
		if (request.headerParams != null)	{
			for(String hKey : request.headerParams.keySet()) {
				httpMethod.addHeader(hKey, request.headerParams.get(hKey));
				if (traceMode) System.out.println(" - Header: " + hKey + " = " + request.headerParams.get(hKey));
			}
		}
		
		
		HttpResponse httpResponse = httpClient.execute(httpMethod);
		ServiceResponse response = new ServiceResponse(httpResponse);

		if (traceMode) System.out.println("HttpStatusCode: " + response.getStatusCode());
		return response;
	}
	
//	public ServiceResponse doPostForm (ServiceRequest request, String body) throws Exception {
//		
//		HttpPost httpMethod = new HttpPost(request.endpoint);
//		if (request.httpParams != null)	{
//			List<NameValuePair> formparams = new ArrayList<NameValuePair>();
//			for(String hKey : request.httpParams.keySet()) {
//				formparams.add(new BasicNameValuePair(hKey, request.httpParams.get(hKey)));
//				if (traceMode) System.out.println(" - httpParameter: " + hKey + " = " + request.httpParams.get(hKey));
//			}
//			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, Consts.UTF_8);
//			httpMethod.setEntity(entity);
//		}
//		
//		if (request.headerParams != null)	{
//			for(String hKey : request.headerParams.keySet()) {
//				httpMethod.addHeader(hKey, request.headerParams.get(hKey));
//				if (traceMode) System.out.println(" - Header: " + hKey + " = " + request.headerParams.get(hKey));
//			}
//		}
//		
//		HttpClient httpClient = HttpClientBuilder.create().build();
//			
//			
//		HttpResponse httpResponse = httpClient.execute(httpMethod);
//		ServiceResponse response = new ServiceResponse(httpResponse);
//		
//		if (traceMode) System.out.println("HttpStatusCode: " + response.getStatusCode());
//		return response;
//		
//	}
	
	
	public ServiceResponse doPost(ServiceRequest request, String body) throws Exception {

		String url = request.endpoint;
		if (traceMode) System.out.println("Endpoint: " + url);;
		
		
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpPost httpMethod = new HttpPost(request.endpoint);
		
		if (traceMode) System.out.println(" - Uri: " + httpMethod.toString());
		
		// Aggiunge parametri in header
		if (request.headerParams != null)	{
			for(String hKey : request.headerParams.keySet()) {
				httpMethod.addHeader(hKey, request.headerParams.get(hKey));
				if (traceMode) System.out.println(" - Header: " + hKey + " = " + request.headerParams.get(hKey));
			}
		}
		
		// Aggiunge parametri alla richiesta
		if (request.httpParams != null)	{
			List<NameValuePair> formparams = new ArrayList<NameValuePair>();
			for(String hKey : request.httpParams.keySet()) {
				formparams.add(new BasicNameValuePair(hKey, request.httpParams.get(hKey)));
				if (traceMode) System.out.println(" - Parameter: " + hKey + " = " + request.httpParams.get(hKey));
			}
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, Consts.UTF_8);
			httpMethod.setEntity(entity);
		}

		if (body != null) {
			StringEntity entity = new StringEntity(body);
			httpMethod.setEntity(entity);
			if (traceMode) System.out.println("Payload: " + body);
		}
		
		
		HttpResponse httpResponse = httpClient.execute(httpMethod);
		ServiceResponse response = new ServiceResponse(httpResponse);

		if (traceMode) System.out.println("HttpStatusCode: " + response.getStatusCode());
		return response;
	}
	
	
	
	
	public static class ServiceRequest {
		private String endpoint = "";
		private Map<String, String> headerParams; 
		private Map<String, String> httpParams;
		
		public ServiceRequest() { init(null, null); }
		public ServiceRequest(Map<String, String> headerParams) { init(headerParams, null); }
		public ServiceRequest(Map<String, String> headerParams, Map<String, String> httpParams) { init(headerParams, httpParams); }
		
		
		public ServiceRequest setEndpoint(String endpoint) {
			this.endpoint = endpoint;
			return this;
		}
		
		public String getEndpoint() { return endpoint; }
		
		public ServiceRequest addAllHeaders(HttpServletRequest httpServletRequest) {
			Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				String hKey = headerNames.nextElement();
				headerParams.put(hKey, httpServletRequest.getHeader(hKey));
			}
			return this;
		}
		
		public ServiceRequest addHeader(String key, String value) {
			headerParams.put(key, value);
			return this;
		}
		
		public ServiceRequest addParam(String key, String value) {
			httpParams.put(key, value);
			return this;
		}
		
		private ServiceRequest init(Map<String, String> headerParams, Map<String, String> httpParams) {
			this.headerParams = headerParams;
			this.httpParams = httpParams;
			if (headerParams == null) this.headerParams = new HashMap<String, String>();
			if (httpParams == null) this.httpParams = new HashMap<String, String>();
			return this;
		}
		
		public ServiceRequest setContentType(String cType) {
			assert(headerParams != null);
			assert(cType != null && !cType.equals(""));
			headerParams.put("Content-Type", cType);
			return this;
		}
		
		public ServiceRequest setAuthorization(String auth) {
			assert(headerParams != null);
			assert(auth != null && !auth.equals(""));
			headerParams.put("Authorization", auth);
			return this;
		}
	}
	
	
	public static class ServiceResponse {
		int statusCode = -1;
		String responseBody = null;
		Map<String, String> headers = new HashMap<String, String>();
		
		public ServiceResponse() {}
		
		public ServiceResponse(HttpResponse httpResponse) throws IOException {
			statusCode = httpResponse.getStatusLine().getStatusCode();
			
			Header[] allHeaders = httpResponse.getAllHeaders();
			if (allHeaders != null) {
				for (Header h : allHeaders) {
					headers.put(h.getName(), h.getValue());
				}
			}
			
			HttpEntity httpEntity = httpResponse.getEntity();
			if (httpEntity != null) responseBody = EntityUtils.toString(httpEntity);
		}
		
		public int getStatusCode() { return statusCode; }
		public String getResponseBody() { return responseBody; }
		public Map<String, String> getHeaderReponse() { return headers; }
	}
}
