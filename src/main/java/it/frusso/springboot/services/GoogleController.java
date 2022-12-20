package it.frusso.springboot.services;

import java.io.InputStream;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/google")
@Tag(name = "GoogleController", description = "Wrapper per servizi GOOGLE")
public class GoogleController {

	private Logger logger = LoggerFactory.getLogger(GoogleController.class);
	
	private String googleApiKey = null;
	private String googleServiceEndpoint = null;
	
	private XProperties googleConfig = new XProperties();
	
	public GoogleController() {
		String fileName = "/" + getClass().getSimpleName() + ".properties";
		try {
			googleConfig.loadFromResource(fileName);
			logger.info("Loaded google config properties: " + fileName + " -> " + googleConfig);
			googleApiKey = googleConfig.getAsString("googleApiKey");
			googleServiceEndpoint =  googleConfig.getAsString("googleServiceEndpoint");
			
			if (googleApiKey == null || googleServiceEndpoint == null) 
				throw new IllegalArgumentException("Unable to configure GoogleController. Mandatory keys not present");
		} catch (Exception e) {
			logger.error("Error loading config file " + fileName + ":" + e.getMessage());
		}
	}
	
	
	
	@RequestMapping(method = RequestMethod.POST, path = "/geocode", 
			produces = MediaType.APPLICATION_JSON_VALUE )
	public String geocode(@RequestBody(required = false) GeoCodeRequest body) {
		
		// Recupera l'indirizzo da geolocalizzare
		String address = body.getAddress();
		
		// Costruisce la chiamata verso GOOGLE
		ServiceHelper sHelper = new ServiceHelper();
		//sHelper.setTraceMode(true);
		ServiceRequest sRequest = new ServiceRequest();
		sRequest.setEndpoint(googleServiceEndpoint);
		sRequest.addParam("key", googleApiKey);	// Imposta l'apiKey
		sRequest.addParam("address", address);
		
		ServiceResponse sResponse = null;
		HttpStatus status = null;
		try {
			sResponse = sHelper.doGet(sRequest);
			status = HttpStatus.resolve(sResponse.getStatusCode());
			if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
			if (!status.is2xxSuccessful()) 
				throw new RuntimeException("Invalid response code: " + status);
		} catch (Exception e) {
			e.printStackTrace();
			String error = e.getMessage();
			if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
			
			JSONObject errorResponse = new JSONObject();
			errorResponse.put("Status", "Error");
			errorResponse.put("Description", error);
			errorResponse.put("httpStatus", status.toString());
			return errorResponse.toString();
		}
		
		// Recupera la risposta di google
		String jsonResponse = sResponse.getResponseBody();
		
		// Restituisce la risposta al chiamante
		return jsonResponse;
	}
	
	// Oggetto REQUEST per il servizio geocode
	public static class GeoCodeRequest {
		private String address;
		
		public GeoCodeRequest() {}
		public GeoCodeRequest(String address) { this.address = address; }
		
		public String getAddress() { return address; }
		public void setAddress(String address) { this.address = address; }
	}
	
	
	
	
	public static void main(String[] args) {
		
		GoogleController gc = new GoogleController();
	
		GeoCodeRequest s = new GeoCodeRequest("Viale Pico della Mirandola, 129 Roma");
		String str = gc.geocode(s);
		System.out.println(str);
	}
}
