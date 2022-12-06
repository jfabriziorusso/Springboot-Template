package it.frusso.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;


/**
 * Implementazione di una mappa estesa con funzinalita' di sostituzione placeholder
 * I placeholder devono essere racchiusi tra caratteri speciali ${ ... }
 * e saranno sostituiti con il contenuto della chiave che rappresentanto.
 * 
 * @author Fabrizio Russo
 *
 */
public class XProperties implements Serializable {

	private static final String START_CONST = "${";
	private static final String END_CONST   = "}";
	
	private Properties data = new Properties();
	
	public interface EntryProcessor {
		public void processEntry(String key, String val);
	}
	
	public XProperties() {}
	public XProperties (Map<String, String> map) {	importFrom(map); }
	public XProperties (XProperties map) {	addAll(map); }
	public XProperties (File file) throws IOException {	importFromFile(file);	}
	
	/**
	 * Carica i dati prelevandoli da un file di properties presente nel classpath
	 * @param resourceName - Path completo del file a partire dalla root del classpath (Es. /default.properties)
	 */
	public void loadFromResource(String resourceName) {
		if (resourceName != null || !resourceName.isEmpty()) {
			try {
				InputStream pInStream = getClass().getResourceAsStream(resourceName);
				if (pInStream != null) {
					data.load(pInStream);
					pInStream.close();
				}
			} catch (Exception e) {
				System.err.println("Error loading " +resourceName + " from classpath: " + e.getMessage());
			}
		}
	}
	
	/**
	 * Cancella tutti i dati
	 */
	public void clear() { data.clear(); }
	
	
	public void forEach(EntryProcessor processor) {
		for (Object x : data.keySet()) {
			String key = x.toString();
			String val = getAsString(key);
			processor.processEntry(key, val);
		}
	}
	
	/** 
	 * Verifica la presenza o meno di una chiave
	 * @param key - Chiave da verificare
	 * @return true se la chiave e' presente
	 */
	public boolean hasKey(String key) {
		if (key == null) return false;
		return data.containsKey(key);
	}
	
	
	/**
	 * Import the map into xProperties
	 */
	public void importFrom(Map<String, String> map) {
		for (Entry<String, String> e : map.entrySet()) {
			String key = e.getKey();
			String val = e.getValue();
			if (parseDirective(key + "=" + val)) return;
			data.put(e.getKey(), e.getValue());
		}
	}
	
	public void addAll(XProperties x) {
		for (Object e : x.keySet()) {
			String key = e.toString();
			String val = x.getAsOriginal(key);
			if (parseDirective(key + "=" + val)) return;
			data.put(key, val);
		}
	}
	
	
	public Properties exportToProperties(boolean asOriginal) {
		Properties p = new Properties();
		Enumeration<Object> e =  data.keys();
		while (e.hasMoreElements()) {
			String key = e.nextElement().toString();
			if (asOriginal)	p.put(key, data.getProperty(key)); 
			else p.put(key, getAsString(key)); 
		}
		return p;
	}
	
	
	public Map<String, String> exportToMap(boolean asOriginal) {
		Map<String, String> map = new TreeMap<String, String>();
		Enumeration<Object> e =  data.keys();
		while (e.hasMoreElements()) {
			String key = e.nextElement().toString();
			if (asOriginal)	map.put(key, data.getProperty(key)); 
			else map.put(key, getAsString(key)); 
		}
		return map;
	}
	
	public void put(String key, String val) {
		if (key == null || val == null) return;
		data.put(key, val);
	}

	public int size() { return data.size(); }
	public boolean contains(String key) { return data.containsKey(key); }
	
	public boolean remove(String key) {
		if (data == null) return false;
		Object obj =  data.remove(key);
		return (obj != null);
	}
	
	
	public void list(PrintStream ps) { data.list(ps); }
	
	public void list(PrintWriter ps) { data.list(ps); }
	
	public List<String> keys() {
		List<String> keys = new ArrayList<String>();
		for (Entry<Object, Object> e : data.entrySet()) {
			String k = e.getKey().toString();
			keys.add(k);
		}
		return keys;
	}
	
	/**
	 * Effettua tutte le sostituzioni del caso per valutare una espressione
	 * @param val - La stringa opportunamente elaborata
	 * @return
	 */
	public String eval(String val) {
		if (val == null || val.isEmpty()) return "";
		while (val.contains(START_CONST)) {
			int x1 = val.indexOf(START_CONST);
			int x2 = val.indexOf(END_CONST, x1);
			if (x1 < 0 || x2 < 0) return val;
			String key2 = val.substring(x1 + START_CONST.length(), x2);
			
			String val2 = data.getProperty(key2);
			if (val2 != null && val2.contains(START_CONST + key2 + END_CONST)) {
				String erroMessage = "WARNING in XProperties. (Avoiding stack overflow) - Trying to extract a value with same name of key: " + key2;
				throw new RuntimeException(erroMessage);
			}
			val2 = getAsString(key2);
			val2 = val.substring(0, x1) + eval(val2) + val.substring(x2 + END_CONST.length());
			val = val2;
		}
		return val;
	}
	
	/**
	 * Restituisce il valore di una chiave con l'elaborazoine effettuata
	 * @param key
	 * @return
	 */
	private String getExtended(String key) {
		String val = data.getProperty(key);
		if (val == null) return null;
		return eval(val);
	}
	
	
	/**
	 * Salva il contenuto della xProperties su in output stream
	 * @param stream - L'output stream su cui persistere (la chiusura � a carico del chiamante)
	 * @param title - Titolo da assegnare (come commento) nel file
	 * @throws IOException
	 */
	public void store(OutputStream stream, String title) throws IOException {
		String header = "#\n"
				+ "# " + title + "\n"
				+ "#\n";
		stream.write(header.getBytes());
		stream.write(dump().getBytes());
		stream.flush();
	}
	
	/**
	 * Salva il contenuto della xProperties su in output stream
	 * @param stream - L'output stream su cui persistere (la chiusura � a carico del chiamante)
	 * @throws IOException
	 */
	public void store(OutputStream stream) throws IOException {
		String title = "File saved on " + new Date();
		store(stream, title);
	}
	
	/**
	 * Slva il contenuto della xProperties su un file
	 * @param f - File su cui salvare
	 * @throws IOException
	 */
	public void store(File f) throws IOException {
		OutputStream s= new FileOutputStream(f);
		store(s);
		s.flush();
		s.close();
	}
	
	/**
	 * Effettua il merge della xProperties con il contenuto recuperato dallo stream di input
	 * @param in - InputStream da cui caricare i dati
	 */
	public void importFromInputStream(InputStream in) {
		if (in == null) return;
		String line = null;
		Scanner sc = new Scanner(in);
		while (sc.hasNextLine()) {
			line = sc.nextLine();
			
			if (line == null || line.isEmpty()) continue;
			if (line.startsWith("#")) continue;
			
			// Direttiva di include/import
			if (line.startsWith("@")) { 
				parseDirective(line); 
				continue; 
			}
			
			int pos = line.indexOf("=");
			if (pos < 0) continue;
			String sx = line.substring(0, pos).trim();
			String dx = line.substring(pos + 1).trim();
		
			put(sx, dx);
		}
		sc.close();
	}
	
	/**
	 * Importa i dati da un file
	 * @param file File da importare
	 * 
	 * @throws IOException
	 */
	public void importFromFile(File file) throws IOException {
		try {
			FileInputStream in = new FileInputStream(file);
			importFromInputStream(in);
			in.close();
		} catch (FileNotFoundException e) {}
	}
	

	/**
	 * Importa un file leggendolo dal classpath
	 * @param resName path del file (non deve iniziare con /)
	 * 
	 */
	public void importFromClasspath(String resName)  {
		if (resName == null) return;
		if (resName.startsWith("/")) resName = resName.substring(1);
		InputStream pInStream = getClass().getClassLoader().getResourceAsStream(resName);
		importFromInputStream(pInStream);
		try {
			if (pInStream != null) pInStream.close();
		} catch (Exception e) {}
	}
	
	
	
	// Analizza le varie direttive
	private boolean parseDirective(String line) {
		boolean result = false;  // La direttiva e' stata interpretata
		
		String dx = line.substring(1 + line.indexOf(":"));
		if (dx != null) dx = dx.trim();
		
		if (line.startsWith ("@importFile:")) {
			File f = new File(dx); 
			if (!f.exists()) {
				System.err.println("Error in @importFile directve: File don't exists: " + f.getAbsolutePath());
				return result;
			};
			
			try {
				importFromFile(f);
				result = true;
			} catch (IOException e) {
				System.err.println("Error in @importFile directive: " + e.getMessage()); 
				return result;
			}
		}
		
		
		if (line.startsWith("@importClasspath")) {
			try {
				importFromClasspath(dx);
				result = true;
			} catch (Exception e) {
				System.err.println("Error in @importClasspath directive : " + dx);
			}
		}
		
		return result;
	}
	
	
	/**
	 * Effettua il dump di una XProperties con i valori originali.
	 * Tutti i dati vengono scritti su una riga separata nel formato chiave = valore
	 * @return
	 */
	public String dump() {
		StringBuilder sb = new StringBuilder();
		for (Entry<Object, Object> e : data.entrySet()) {
			String key = e.getKey().toString();
			Object val = e.getValue().toString();
			String line = key + "=" + val.toString();
			sb.append(line + "\n");
		}
		return sb.toString();
	}
	
	
	public String toString() {
		StringBuilder sb = new StringBuilder("{ ");
		int i=0;
		for (Entry<Object, Object> e : data.entrySet()) {
			i++;
			String key = e.getKey().toString();
			Object val = getAsString(key);
			sb.append(key + "=" + val);
			if (i<data.size()) sb.append(", ");
		}
		sb.append(" }");
		return sb.toString();
	}
	
	public boolean isEmpty() { return data.isEmpty(); }
	
	
	
	@Override public int hashCode() {
		return data.hashCode();
	}
	
	@Override public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof XProperties)) return false;
		XProperties obj2 = (XProperties) obj;
		if (data.size() != obj2.size()) return false;
		if (data.hashCode() != obj2.data.hashCode()) return false;
		return true;
	}
	
	public Set<String> keySet() { 
		Set<String> set = new HashSet<String>();
		for (Object x : data.keySet()) set.add(x.toString());
		return set; 
	}
	
	
	public void put(Map<String, String> map) {
		for (Entry<String, String> e : map.entrySet()) {
			data.put(e.getKey(), e.getValue());
		}
	}
	
	
	// ----------------- SETTER -----------------------
	
	public void setProperty(String key, String value) {
		if (key != null && value != null) data.setProperty(key, value);
	}
	
	
	
	// ------------------- GETTERS -------------------------
	
	// Restituisce il valore originale non valutato
	public String getAsOriginal(String key) { return data.getProperty(key); }
	
	@Deprecated	public String getProperty(String key)  {return getExtended(key); }
	
	@Deprecated public String getProperty(String key, String defValue)  {
		String x = getExtended(key);
		if (x == null) return defValue;
		else return x;
	}
	
	public String getAsString(String key) { return getExtended(key); }
	
	public String getAsString(String key, String defValue)  {
		String x = getExtended(key);
		if (x == null) return defValue;
		else return x;
	}
	
	public boolean getAsBoolean(String key, boolean defaultValue) {
		String x = getExtended(key);
		if (x == null) return defaultValue;
		try {
			return Boolean.parseBoolean(x);
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	// Restituisce il valore della property come LONG
	public long getAsLong(String key, long defaultValue) {
		String x = getExtended(key);
		if (x == null) return defaultValue;
		try {
			return Long.parseLong(x);
		} catch (NumberFormatException e ) {
			return defaultValue;
		}
	}
		
		
	public double getAsDouble(String key, double defaultValue) {
		String x = getExtended(key);
		if (x == null) return defaultValue;
		try {
			return Double.parseDouble(x);
		} catch (NumberFormatException e ) {
			return defaultValue;
		}
	}
		
		
	public double getAsFloat(String key, float defaultValue) {
		String x = getExtended(key);
		if (x == null) return defaultValue;
		try {
			return Float.parseFloat(x);
		} catch (NumberFormatException e ) {
			return defaultValue;
		}
	}
		
	public File getAsFile(String path) {
		if (path == null) return null;
		File f = new File(path);
		return f;
	}
	
	
	// Restituisce il valore della property come INTERO
	public int getAsInteger(String key, int defaultValue) {
		String x = getExtended(key);
		if (x == null) return defaultValue;
		try {
			return Integer.parseInt(x);
		} catch (NumberFormatException e ) {
			return defaultValue;
		}
	}
		
	
	/**
	 * Effettua un ordinamento di una lista di XProperties basata sul valore di un tag presente in ognuna di esse
	 * @param list
	 * @param tagName
	 */
	public static void sortByTagName(final List<XProperties> list, final String tagName) {
		if (list == null || list.size() == 0) return;
		
		Comparator<XProperties> comparator = new Comparator<XProperties>() {
			public int compare(XProperties o1, XProperties o2) {
				String a = o1.getProperty(tagName);
				String b = o2.getProperty(tagName);
				if (a == null && b == null) return 0;
				if (a == null) return -1;
				if (b == null) return 1;
				return a.compareTo(b);	
			}
		};
		Collections.sort(list, comparator);
		
	}
	
	
	public void copyValueFrom(XProperties source, String key) {
		String val = source.getAsString(key);
		if (val != null) setProperty(key, val);
	}
		
	/**
	 * Effettua il parsing della command line e costruisce una mappa con 
	 * i parametri passati
	 * @param args
	 * @return
	 */
	public static XProperties parseCommandLine(String args[]) {
		XProperties data = new XProperties();
		if (args == null) return data;
		
		final String DEFAULT_VALUE = "true";
		
		int paramIndex = 0;
		for (int i=0; i< args.length; i++) {
			String paramName = args[i];
			String paramValue = DEFAULT_VALUE;

			if (paramName.startsWith("-")) {
				paramName = paramName.substring(1);
				
				try { 
					paramValue = args[i+1];
					if (!paramValue.startsWith("-")) {
						i++;
					} else {
						data.put(paramName, DEFAULT_VALUE);
						continue;
					}
					
				} catch (ArrayIndexOutOfBoundsException e) {}
			} else {
				paramValue = args[i];
				paramName = "param" + (paramIndex++);
			}
			
			data.put(paramName, paramValue);
		}
		
		return data;
	}
	
	// -----------------------------------------------  Test case
	public static void main(String[] args) throws Exception  {
		
		XProperties x = new XProperties();
		x.importFromClasspath("config.properties");
		System.out.println(x.dump());
		
	}
}
