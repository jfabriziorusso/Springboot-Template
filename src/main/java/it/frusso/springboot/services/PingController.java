package it.frusso.springboot.services;

import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import it.frusso.springboot.StringResponse;

/**
 * Controller per l'esposizione del servizio di test
 * @author Fabrizio Russo
 *
 */
@RestController
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
		
		// Aggiunge il timestamp e la data attuale
		p.put("timestamp", "" + System.currentTimeMillis());
		p.put("date", new Date().toString());
		
		return p;
	}
	
}
