/**
 * Troll.java: A user interface to the weblech spider download utility.
 */
package weblech.ui;

/* I like to explicitly import all of my packages to remind me to KISS */
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;

import java.awt.Dimension;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class Troll extends JFrame implements ActionListener	{

	/** 
	 * This SpiderConfigPanel is a custom panel that provides many of the more
	 * practical features of the weblech spider. It also controls the spiders
	 * created by the user.
	 */
	private SpiderConfigPanel configpanel;
	/**
	 * This is the area that all of the logging facilities will use. This makes debugging
	 * in a system like Max OS X much simpler (:^)
	 */
	private static JTextArea logarea;
	
	/* This just initializes the logging text box and readies it for recording events before
	 * the rest of the object is even loaded.
	 */
	static  {
		logarea = new JTextArea();
		LechLogger.setTextArea ( logarea );
		LechLogger.setDebugLogging();
	}
	
	Troll()	{
		super ( "Troll" );
		Dimension initialsize = new Dimension ( 400, 375 );
		setSize ( initialsize);
		
		/* Create a menubar for controlling which aspects of the log you wish to see */
		JMenuBar menubar = new JMenuBar();
		JMenu logmenu = new JMenu ( "Log Options" );
		JCheckBoxMenuItem showdebug = new JCheckBoxMenuItem ( "Show Debug Messages", false );
		showdebug.setActionCommand ( "debug" );
		showdebug.addActionListener ( this );
		JCheckBoxMenuItem showinfo = new JCheckBoxMenuItem ( "Show Informational Messages", true );
		showinfo.setActionCommand ( "info" );
		showinfo.addActionListener ( this );
		JCheckBoxMenuItem showwarn = new JCheckBoxMenuItem ( "Show Warnings", true );
		showwarn.setActionCommand ( "warn" );
		showwarn.addActionListener ( this );
		JCheckBoxMenuItem showerror = new JCheckBoxMenuItem ( "Show Errors", true );
		showerror.setActionCommand ( "error" );
		showerror.addActionListener ( this );
		logmenu.add ( showdebug );
		logmenu.add ( showinfo );
		logmenu.add ( showwarn );
		logmenu.add ( showerror );
		menubar.add ( logmenu );
		
		/* A simple tab interface between configuration and error checking */
		configpanel = new SpiderConfigPanel();
		JPanel logpanel = new JPanel();
		logpanel.add ( logarea );
		JScrollPane logscroller = new JScrollPane ( logpanel );
		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab ( "Spider", configpanel );
		tabs.addTab ( "Log", logscroller );
		
		/* Configure the JFrame to a usable state */
		setJMenuBar ( menubar );
		getContentPane().add ( tabs );
		setLocationRelativeTo ( null );
		setVisible ( true );
		setDefaultCloseOperation ( JFrame.EXIT_ON_CLOSE );
	}
	
	/**
	 * This method basically toggles all of the logging options.
	 */
	public void actionPerformed ( ActionEvent event )   {
		String cmd = event.getActionCommand();
		if ( cmd.equals ( "debug" ) )   {
			LechLogger.setDebugLogging();
		}
		else if ( cmd.equals ( "info" ) )   {
			LechLogger.setInformationalLogging();
		}
		else if ( cmd.equals ( "warn" ) )   {
			LechLogger.setDebugLogging();
		}
		else if ( cmd.equals ( "error" ) )  {
			LechLogger.setErrorLogging();
		}
	}
	
	/**
	 * Create a new troll and go
	 */
	public static void main ( String[] args )	{
		Troll t = new Troll();
	}
}
