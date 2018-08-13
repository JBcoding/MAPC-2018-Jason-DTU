package mapc2017.logging;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public abstract class Logger extends PrintStream {

	public Logger(String fileName) {
		super(getOutputStream(fileName), true);
	}
	
	public static BufferedOutputStream getOutputStream(String fileName) {
		try { return new BufferedOutputStream(new FileOutputStream(fileName)); } 
		catch (FileNotFoundException e) { e.printStackTrace(); return null; }
	}

	public void printSeparator() {
		this.println("--------------------------");
	}	

}
