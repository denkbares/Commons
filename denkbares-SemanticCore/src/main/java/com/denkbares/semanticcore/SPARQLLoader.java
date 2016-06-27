package com.denkbares.semanticcore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import de.d3web.utils.Log;

/**
 * @author Sebastian Furth (denkbares GmbH)
 */
public class SPARQLLoader {

	public static String load(String file, Class<?> c) {

		StringBuilder query = new StringBuilder();

		try {
			InputStream in = getInputStream(file, c);
			Reader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));

			int ch;
			while ((ch = reader.read()) > -1) {
				query.append((char) ch);
			}
			reader.close();
		}
		catch (IOException e) {
			Log.severe("Exception while reading file " + file, e);
		}

		return query.toString();
	}

	private static InputStream getInputStream(String file, Class<?> c) throws FileNotFoundException {
		File f = new File(file);
		if (f.exists()) {
			return new FileInputStream(f);
		}
		return c.getResourceAsStream(file);
	}

}
