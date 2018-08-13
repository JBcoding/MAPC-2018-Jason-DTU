package mapc2017.logging;

import java.io.File;

public class ErrorLogger extends Logger {
	
	private static ErrorLogger instance;
	
	public ErrorLogger()
	{
		super(getDirectory() + getFileName());
	}
	
	public static void reset() 
	{
		if (instance != null) 
			instance.close();
		instance = new ErrorLogger();
	}
	
	public static Logger get() 
	{
		if (instance == null) reset();
		return instance;
	}
	
	public static String getDirectory()
	{
		File path = new File("output/errors");
		
		if (!path.isDirectory())
			path.mkdir();

		return path.getPath() + "/";
	}

	public static String getFileName()
	{
		return String.format("errors_%d.txt", System.currentTimeMillis());
	}

}
