package it.frusso.springboot.services;

import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.frusso.util.FileHelper;
import it.frusso.util.ServiceHelper;
import it.frusso.util.ServiceHelper.ServiceRequest;
import it.frusso.util.ServiceHelper.ServiceResponse;
import it.frusso.util.XProperties;

/**
 * Controller per l'esposizione del servizi di Nexus
 * L'annotation 'RequestMapping' sulla classe definisce un prefisso comune a tutti i servizi
 * Utilizzare l'annotation "Tag" per ottenere un nome ed una descrizione all'interno dell'interfaccia swagger
 * 
 * @author Fabrizio Russo
 *
 */
@RestController
@RequestMapping("/nexusws")
@Tag(name = "NexusWSController", description = "Servizi Nexus - Implementazione XML")
public class NexusWSController {

	private Logger logger = LoggerFactory.getLogger(NexusWSController.class);
	
	private String nexusSoapRequest = "";
	private XProperties nexusConfig = new XProperties();
	
	// Insieme dei servizi esposti da nexus (ognuno richiaa il metodo callNexus() 
	
	@PostMapping(path = "/reteAgenzia", produces = MediaType.APPLICATION_XML_VALUE)
	public ResponseEntity<String> reteAgenzia(HttpServletRequest httpServletRequest, @RequestBody(required = false) String body) {
		return callNexus(httpServletRequest, body);
	}
	
	@PostMapping(path = "/infoAgenzia", produces = MediaType.APPLICATION_XML_VALUE)
	public ResponseEntity<String> infoAgenzia(HttpServletRequest httpServletRequest, @RequestBody(required = false) String body) {
		return callNexus(httpServletRequest, body);
	}
	
	
	// --------------------------------------------------------------------
	
	public NexusWSController() {
		try {
			InputStream in = NexusWSController.class.getResourceAsStream("/nexusSoapRequest.xml");
			nexusSoapRequest = FileHelper.readTextFile(in);
			in.close();
			logger.info("Loaded file 'nexusSoapRequest.xml' as soap template request for NEUXS");
			
			nexusConfig.loadFromResource("/nexusConfig.properties");
			logger.info("Loaded nexus config properties: " + nexusConfig.toString());
		} catch (Exception e) {
			logger.error("Error loading xml template: " + e.getMessage());
		}
	}
	
	
	private ResponseEntity<String> callNexus(
			final HttpServletRequest httpServletRequest, final String body) {
		
		String transactionId = getTransactionId(httpServletRequest);
		
		
		// Recupera il nome del servizio come ultima parte del path
		String requestUri = httpServletRequest.getRequestURI();
		String serviceName = requestUri.substring(1+ requestUri.lastIndexOf("/"));
		
		logger.debug(transactionId + " - Starting request for service " + requestUri);
		
		HttpStatus httpStatusResult = HttpStatus.OK;
		
		// Recupera la username da un parametro dell' header
		String userHeaderKey = nexusConfig.getAsString("nexusws.userHeaderName");
		String userName = httpServletRequest.getHeader(userHeaderKey);
		
		String xml = null;
		if (body == null || body.isEmpty()) {
			httpStatusResult = HttpStatus.BAD_REQUEST;
			String errorMessage = "Bad Request - payload empty";
			xml = makeErrorResponse(httpServletRequest, 404, errorMessage);
			logger.debug(transactionId + " - Error: " + errorMessage);
			return new ResponseEntity<String>(xml, httpStatusResult);
		}
		
		if (userName == null || userName.isEmpty()) {
			httpStatusResult = HttpStatus.PROXY_AUTHENTICATION_REQUIRED;
			String errorMessage = "ProxyAuthenticationRequired - Empty or not valid userName";
			xml = makeErrorResponse(httpServletRequest, 407, errorMessage);
			logger.debug(transactionId + " - Error: " + errorMessage);
			return new ResponseEntity<String>(xml, httpStatusResult);
		}
		
		// Costruisce il payload da girare a nexus
		String escapedBody = StringEscapeUtils.escapeXml10(body);
		XProperties prop = new XProperties();
		prop.put("userName", userName);
		prop.put("serviceName", serviceName);
		prop.put("xmlMessage", escapedBody);
		xml = new String(nexusSoapRequest);
		xml = prop.eval(xml);
		
		
		// Effettua la chiamata a Nexus
		ServiceHelper serviceHelper = new ServiceHelper();
		ServiceRequest serviceRequest = new ServiceRequest();
		serviceRequest.addAllHeaders(httpServletRequest); // Riporta tutti gli header al backend
		serviceRequest.addHeader("SAOPAction", "uri:call");
		serviceRequest.setContentType(ServiceHelper.CONTENT_TYPE_playText);
		serviceRequest.setEndpoint(nexusConfig.getAsString("nexusws.backendURL"));

		// Effettua la chiamata a Nexus
		long timeA = System.currentTimeMillis();
		ServiceResponse serviceResponse = null;
		try {
			serviceResponse = serviceHelper.doPost(serviceRequest, xml); 
			if (serviceResponse == null) throw new RuntimeException("Empty response from nexus");
		} catch (Exception e) {
			httpStatusResult = HttpStatus.INTERNAL_SERVER_ERROR;
			
			String errorMessage = "Internal Proxy Error - Error calling backend : " + e.getMessage();
			xml = makeErrorResponse(httpServletRequest, 407, errorMessage);
			logger.debug(transactionId + " - Error: " + errorMessage);
			xml = makeErrorResponse(httpServletRequest, 500, errorMessage);
			return new ResponseEntity<String>(xml, httpStatusResult);
		}
		
		long timeB = System.currentTimeMillis();
		long nexusTimeMillis = (timeB - timeA);
		logger.debug(transactionId + " - Nexus called service " + serviceName + " in " + nexusTimeMillis + "ms");

		int statusCode = serviceResponse.getStatusCode();
		String nexusResponse = serviceResponse.getResponseBody();
		
		if (statusCode != 200) {
			String errorMessage = "Nexus Error - Nexus response statusCode = " + statusCode;
			xml = makeErrorResponse(httpServletRequest, statusCode, errorMessage);
			logger.debug(transactionId + " - Error: " + errorMessage);
			return new ResponseEntity<String>(xml, httpStatusResult);
		}
		
		xml = nexusResponse;
		logger.debug(transactionId + " - Responding to client with statusCode : " + httpStatusResult.toString());
		
		return new ResponseEntity<String>(xml, httpStatusResult);
	}
	
	private String getTransactionId(HttpServletRequest httpServletRequest) {
		String transactioIdHeaderName = nexusConfig.getAsString("transactioIdHeaderName");
		String res = httpServletRequest.getHeader(transactioIdHeaderName);
		if (res == null) res = httpServletRequest.getHeader("activityId");
		if (res == null) res = httpServletRequest.getHeader("x-transaction");
		if (res == null) res = UUID.randomUUID().toString();
		return res;
	}

	/*
	 * Costruisce il messaggio di errore di risposta al client
	 */
	private String makeErrorResponse(HttpServletRequest httpServletRequest, 
			int errorCode, String errorDescription) {
		StringBuilder sb = new StringBuilder();
		sb.append("<xml version='1.0'>");
		sb.append("\n<proxyResult errorCode='" + errorCode + "'>");
		sb.append("\t<requestedUri>" + httpServletRequest.getRequestURI() + "</requestedUri>");
		sb.append("\t<errorCode>" + errorDescription + "</errorCode>");
		sb.append("\n</proxyResult>");
		sb.append("\n</xml>");
		
		return sb.toString();
	}
	
	
	/*
	 * Servizi di amministrazione e controllo
	 * Questi servizi non devono essere esposti al pubblico ma servono solo
	 * per verica dello stato di configurazione del sistema
	 */
	
	// Restituisce il file di configurazione di nexus
	@RequestMapping(method = RequestMethod.GET, path="/admin/getRuntimeConfig", produces = MediaType.APPLICATION_JSON_VALUE)
	public Properties adminGetRuntimeConfig(HttpServletRequest httpServletRequest) {
		return nexusConfig.exportToProperties(false);
	}
	
	// Restituisce il template usato per effettuare le chiamate a nexus
	@RequestMapping(method = RequestMethod.GET, path="/admin/getCallTemplate", produces = MediaType.APPLICATION_XML_VALUE)
	public ResponseEntity<String> adminGetCallTemplate(HttpServletRequest httpServletRequest) {
		return new ResponseEntity<String>(nexusSoapRequest, HttpStatus.OK);
	}
		
}
