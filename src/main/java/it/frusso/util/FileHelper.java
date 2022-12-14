package it.frusso.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FileHelper {

	public static String readTextFile(InputStream in) {
		
		StringBuffer sb = new StringBuffer();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line = "";
			while ((line = reader.readLine()) != null) {
				if (line == null) break;
				sb.append(line + "\n");
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		return sb.toString();
	}
}
