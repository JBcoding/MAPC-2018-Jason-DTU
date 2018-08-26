package logging;

import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class LoggingHandler extends StreamHandler 
{
	public LoggingHandler()
	{
		super();
		
		this.setFormatter(new LoggingFormatter());
	}
	
    @Override
    public void publish(LogRecord record) 
    {
    	System.err.println(this.getFormatter().format(record));
    	
        super.publish(record);
    }


    @Override
    public void flush() {
        super.flush();
    }


    @Override
    public void close() throws SecurityException {
        super.close();
    }	
}
