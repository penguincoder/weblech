/**
 * LechLogger.java: A Graphical Logger
 * The original weblech had a simple text interface and used an apache.org library for logging.
 * When i wanted to make this thing into a graphical appk, I realized the text logging
 * was going to have to go. It assumes you only want to log to one location (not a problem for
 * this application).
 */
package weblech.ui;

import javax.swing.JTextArea;
import java.io.IOException;

public class LechLogger {
	
	/**
	 * The actual text area that will perform all output.
	 */
	private static JTextArea _loggerWindow;
	/**
	 * These are flags for enabling different types of logging mechanisms.
	 */
	private static boolean error_enable, warn_enable, info_enable, debug_enable;
	
	/**
	 * Only need one initialization since this will be shared between many different
	 * objects.
	 */
	static  {
		_loggerWindow = null;
		error_enable = true;
		warn_enable = true;
		info_enable = true;
		debug_enable = true;
	}
	
	/** Everybody wants to log, but you only need one logger! */
	public LechLogger()	{
	}
	
	/**
	 * Sets the textual component to perform the logging.
	 */
	public static void setTextArea ( JTextArea textarea )	{
		_loggerWindow = textarea;
	}
	
	/**
	 * A private method for actually writing the messages.
	 * It is synchronized because the weblech spider is multi
	 * threaded.
	 */
	private static synchronized void log ( String msg )  {
		if ( _loggerWindow == null ) {
			//System.out.println ( msg );
			return;
		}
		_loggerWindow.setEditable ( true );
		_loggerWindow.append ( msg );
		_loggerWindow.append ( "\n" );
		_loggerWindow.setEditable ( false );
	}
	
	/**
	 * Toggle error logging.
	 */
	public static void setErrorLogging()	{
		error_enable = !error_enable;
	}
	
	/**
	 * Log an error message.
	 */
	public static void error ( String msg ) {
		if ( !error_enable ) return;
		log ( "*error>" + msg );
	}
	
	/**
	 * Log an error message and an exception.
	 */
	public static void error ( String msg, Exception exception ) {
		if ( !error_enable ) return;
		log ( "*error>" + msg + "\n" + exception.getMessage() );
	}
	
	/**
	 * Toggle informational messages.
	 */
	public static void setInformationalLogging()	{
		info_enable = !info_enable;
	}
	
	/**
	 * Log an informational message.
	 */
	public static void info ( String msg ) {
		if ( !info_enable ) return;
		log ( "^info>" + msg );
	}
	
	/**
	 * Toggle warning messages.
	 */
	public static void setWarningLogging()	{
		warn_enable = !warn_enable;
	}
	
	/**
	 * Log a warning message.
	 */
	public static void warn ( String msg ) {
		if ( !warn_enable ) return;
		log ( "-warn>" + msg );
	}
	
	/**
	 * Log a warning message with an exception.
	 */
	public static void warn ( String msg, IOException exception ) {
		if ( !warn_enable ) return;
		log ( "-warn>" + msg + "\n" + exception.getMessage() );
	}
	
	/**
	 * Toggle debug messages to be printed.
	 */
	public static void setDebugLogging()	{
		debug_enable = !debug_enable;
	}
	
	/**
	 * Log a deubgging statement to the logging text area.
	 */
	public static void debug ( String msg ) {
		if ( !debug_enable ) return;
		log ( "@debug>" + msg );
	}
}