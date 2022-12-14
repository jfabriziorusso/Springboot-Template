package it.frusso.springboot.services;

import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

	private static Logger logger = LoggerFactory.getLogger(NexusWSController.class);
	
	private static String nexusSoapRequest = "";
	private static XProperties nexusConfig = new XProperties();
	
	@Autowired HttpServletRequest httpServletRequest;
	
	
	// Insieme dei servizi esposti da nexus (ognuno richiaa il metodo callNexus() 
	
	@PostMapping(path = "/reteAgenzia", produces = MediaType.APPLICATION_XML_VALUE)
	public ResponseEntity<String> reteAgenzia(@RequestBody(required = false) String body) {
		return callNexus("reteAgenzia", body);
	}
	
	
	
	// --------------------------------------------------------------------
	
	static {
		try {
			InputStream in = NexusWSController.class.getResourceAsStream("/nexusSoapRequest.xml");
			nexusSoapRequest = FileHelper.readTextFile(in);
			in.close();
			logger.info("Loaded file 'nexusSoapRequest.xml' as soap templare request for NEUXS");
			
			nexusConfig.loadFromResource("/nexusConfig.properties");
			logger.info("Loaded nexus config properties");
			logger.info(nexusConfig.toString());
			
		} catch (Exception e) {
			logger.error("Error loading xml template: " + e.getMessage());
		}
	}
	
	
	private ResponseEntity<String> callNexus(final String serviceName, final String body) {
		
		HttpStatus httpStatusResult = HttpStatus.OK;
		
		// Recupera la username da un parametro dell' header
		String userHeaderKey = nexusConfig.getAsString("nexusws.userHeaderName");
		String userName = httpServletRequest.getHeader(userHeaderKey);
		
		String xml = null;
		if (body == null || body.isEmpty()) {
			httpStatusResult = HttpStatus.BAD_REQUEST;
			xml = makeErrorResponse(404, "Bad Request - payload empty");
			return new ResponseEntity<String>(xml, httpStatusResult);
		}
		
		if (userName == null || userName.isEmpty()) {
			httpStatusResult = HttpStatus.PROXY_AUTHENTICATION_REQUIRED;
			xml = makeErrorResponse(407, "ProxyAuthenticationRequired - Empty or not valid userName");
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
		serviceRequest.addHeader("SAOPAction", "uri:call");
		serviceRequest.addHeader("Content-Type", ServiceHelper.CONTENT_TYPE_playText);
		serviceRequest.setEndpoint(nexusConfig.getAsString("nexusws.backendURL"));

		// Effettua la chiamata a Nexus
		long timeA = System.currentTimeMillis();
		ServiceResponse serviceResponse = null;
		try {
			serviceResponse = serviceHelper.doPost(serviceRequest, xml); 
			if (serviceResponse == null) throw new RuntimeException("Invalid response from nexus");
		} catch (Exception e) {
			httpStatusResult = HttpStatus.INTERNAL_SERVER_ERROR;
			xml = makeErrorResponse(httpStatusResult.ordinal(), httpStatusResult.value() + " - Error calling backend : " + e.getMessage());
			logger.error("Error calling Nexus: " + httpStatusResult.ordinal() + " - " + e.getMessage());
			return new ResponseEntity<String>(xml, httpStatusResult);
		}
		long timeB = System.currentTimeMillis();
		long nexusTimeMillis = (timeB - timeA);
		
		logger.debug("Nexus called service " + serviceName + " in " + nexusTimeMillis + "ms");
		int statusCode = serviceResponse.getStatusCode();
		String nexusResponse = serviceResponse.getResponseBody();
		
		if (statusCode != HttpStatus.OK.ordinal()) {
			xml = makeErrorResponse(statusCode, "Nexus Error - Nexus response statusCode = " + statusCode);
			return new ResponseEntity<String>(xml, httpStatusResult);
		}
		
		xml = nexusResponse;
		return new ResponseEntity<String>(xml, httpStatusResult);
	}
	
	/*
	 * Costruisce il messaggio di errore di risposta al client
	 */
	private String makeErrorResponse(int errorCode, String errorDescription) {
		StringBuilder sb = new StringBuilder();
		sb.append("<xml version='1.0'>");
		sb.append("\n<proxyResult errorCode='" + errorCode + "'>");
		sb.append("\t<errorCode>" + errorDescription + "</errorCode>");
		sb.append("\n</proxyResult>");
		sb.append("\n</xml>");
		return sb.toString();
	}
}