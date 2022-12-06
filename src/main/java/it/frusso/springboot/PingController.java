package it.frusso.springboot;

import java.util.Date;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {


	@GetMapping(value = "/ping")
	public AboutBean getAboutInfo() {

		AboutBean res = new AboutBean();
		return res;
		
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
