package it.frusso.springboot;

/**
 * Wrapper per restituire una semlice stringa come response di un servizio
 * (In json anche una semplice stringa ha bisogn di un tag)
 * @author Fabrizio Russo
 *
 */
public final class StringResponse {

	private String response = null;
	
	public StringResponse(String x) { response = x; }
	
	public String getResponse() { return response; }
	public void setResponse(String response) { this.response = response; }
}
