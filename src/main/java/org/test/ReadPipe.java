/**
 * 
 */
package org.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * @author Mike
 * 
 */
public class ReadPipe {

	/**
	 * 
	 */
	public ReadPipe() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));

		ReadableByteChannel in = Channels.newChannel(System.in);

		PrintWriter pw = new PrintWriter(new File(args[0]));

		while (true) {

			String currentLine = reader.readLine();

			if (currentLine != null) {
				pw.println(currentLine);
				pw.flush();
			}

		}

	}

}
