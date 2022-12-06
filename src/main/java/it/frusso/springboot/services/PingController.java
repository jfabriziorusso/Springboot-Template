package it.frusso.springboot.services;

import java.io.InputStream;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Properties;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import it.frusso.springboot.StringResponse;

/**
 * Controller per l'esposizione del servizio di test
 * Utilizzare l'annotation "Tag" per ottenere un nome ed una descrizione all'interno dell'interfaccia swagger
 * 
 * @author Fabrizio Russo
 *
 */
@RestController
@Tag(name = "DefautController", description = "Servizi di monitoraggio e diagnostica API")
public class PingController {


	/**
	 * Restituisce la data attuale per verificare che il servizio e' attivo
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET, path = "/api/ping", produces = MediaType.APPLICATION_JSON_VALUE )
	public StringResponse pingInfo() {
		String info = new java.util.Date().toString();
		return new StringResponse(info);
	}
	
	/**
	 * Restituisce delle informazioni di 'about' prelevandole dal file di configurazione 'about.properties' presente nel classpath
	 * In ottica di CI/CD il file di properties può essere manipolato in fase di build per rispecchiare le ultime configurazione o l'ambiente 
	 * in cui gira il servizio.
	 */
	@RequestMapping(method = RequestMethod.GET, path = "/api/about", produces = MediaType.APPLICATION_JSON_VALUE )
	public Properties aboutInfo() {
		Properties p = new Properties();
		try {
			InputStream is = getClass().getResourceAsStream("/about.properties");
			if (is != null) p.load(is);
			is.close();
		} catch (Exception e) {
			System.err.println("Error loading file from classpath: " + e.getMessage());
		}
		
		Properties res = new Properties();
		for (Object x : p.keySet()) {
			String s = (String)x;
			if (s.startsWith("api.")) res.put(s, p.getProperty(s));
		}
		
		// Aggiunge il timestamp e la data attuale
		res.put("timestamp", "" + System.currentTimeMillis());
		res.put("date", new Date().toString());
		
		return res;
	}
	
}
