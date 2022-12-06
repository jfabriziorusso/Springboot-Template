package it.frusso.springboot.services;

import java.util.Date;
import java.util.Properties;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.Mapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller per l'esposizione del servizio di test
 * @author Fabrizio Russo
 *
 */
@RestController
public class PingController {


	@RequestMapping(method = RequestMethod.GET, name = "ping", path = "/api/ping", produces = MediaType.APPLICATION_JSON_VALUE )
	public String pingInfo() {
		AboutBean res = new AboutBean();
		String info = new java.util.Date().toString();
		return info;
	}
	
	@RequestMapping(method = RequestMethod.GET, name = "info", path = "/api/about", produces = MediaType.APPLICATION_JSON_VALUE )
	public Properties aboutInfo() {
		Properties p = new Properties();
		p.put("timestamp", "" + System.currentTimeMillis());
		p.put("date", new Date().toString());
		return p;
	}

	
	public class AboutBean {
		private long timestamp = 0;
		private String printable = "";
		
		public AboutBean() {
			Date d = new Date();
			timestamp = d.getTime();
			printable = d.toString();
		}

		// Remember to implements getter and setter to avoid the error
		// "No converter found for return value of type: ....."
		public long getTimestamp() { return timestamp; }
		public void setTimestamp(long timestamp) { this.timestamp = timestamp;	}
		public String getPrintable() { 	return printable; }
		public void setPrintable(String printable) { this.printable = printable; }
	}
}
