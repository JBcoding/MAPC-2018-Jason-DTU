package logging;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggerFactory {

	public static Logger createFileLogger(String team)
	{
		Logger logger = Logger.getLogger("DataLogger");

		for (Handler handler : logger.getHandlers())
			handler.setLevel(Level.OFF);
		
		logger.setUseParentHandlers(false);
		
		Handler handler;
		try {
			LocalDateTime date = LocalDateTime.now();
			
			String dateString = date.getYear() + "-" + date.getMonthValue() + "-" 
					+ date.getDayOfMonth() + " " 
					+ date.getHour() + "-" + date.getMinute();
			
			handler = new FileHandler("logs\\client\\results-team-" + team + "-" + dateString + ".log");
			
			handler.setLevel(Level.ALL);
			
			handler.setFormatter(new LoggingFormatter());
			
			logger.addHandler(handler);
		} 
		catch (SecurityException | IOException e) 
		{
			e.printStackTrace();
		}
		
		return logger;
	}
}
