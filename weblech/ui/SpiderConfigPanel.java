/**
 * SpiderConfigPanel.java: A graphcial panel for configuring a SpiderConfig object.
 * This panel provides a way to change the more practical options of the WebLech Spider.
 * It supports saving and opening of SpiderConfigurations from a file. It does not use
 * any of the "interesting" or "boring" url features, or the email link save file.
 */
package weblech.ui;

import weblech.spider.Spider;
import weblech.spider.SpiderConfig;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;

import java.awt.GridLayout;
import java.awt.FlowLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.util.Properties;
import java.util.ArrayList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SpiderConfigPanel extends JPanel implements ActionListener {

	/**
	 * A list of all of the spiders that the GUI will have downloading. It is assumed
	 * that the user knows how much bandwidth you have and really wants to try and get
	 * several different sites at the same time.
	 */
	private ArrayList spiders;
	/**
	 * Various text fields for the configuration options.
	 */
	private JTextField sitenamefield, dirfield, usernamefield, passwordfield, agentfield, depthfield, matchfield;
	/**
	 * A selection box for the number of threads a new Spider should use, I am limiting
	 * the number of threads to 4 for simplicity.
	 */
	private JComboBox threadbox;
	
	public SpiderConfigPanel()  {
		super ( new GridLayout ( 8, 1 ) );
		spiders = new ArrayList();
		
		/* Panel for the directory to save all files */
		JPanel sitepanel = new JPanel();
		((FlowLayout) sitepanel.getLayout()).setAlignment ( FlowLayout.RIGHT );
		JLabel sitelabel = new JLabel ( "Output Directory:" );
		dirfield = new JTextField ( System.getProperty ( "user.home" ), 20 );
		sitepanel.add ( sitelabel );
		sitepanel.add ( dirfield );
		
		/* Panel for the site to download */
		JPanel outputpanel = new JPanel();
		((FlowLayout) outputpanel.getLayout()).setAlignment ( FlowLayout.RIGHT );
		JLabel dirlabel = new JLabel ( "Download Site:" );
		sitenamefield = new JTextField ( "http://www.google.com/", 20 );
		outputpanel.add ( dirlabel );
		outputpanel.add ( sitenamefield );
		
		/* Panel for the HTTP username */
		JPanel usernamepanel = new JPanel();
		((FlowLayout) usernamepanel.getLayout()).setAlignment ( FlowLayout.RIGHT );
		JLabel usernamelabel = new JLabel ( "Username:" );
		usernamefield = new JTextField ( "", 20 );
		usernamepanel.add ( usernamelabel );
		usernamepanel.add ( usernamefield );
		
		/* Panel for the HTTP password */
		JPanel passpanel = new JPanel();
		((FlowLayout) passpanel.getLayout()).setAlignment ( FlowLayout.RIGHT );
		JLabel passwdlabel = new JLabel ( "Password:" );
		passwordfield = new JTextField ( "", 20 );
		passpanel.add ( passwdlabel );
		passpanel.add ( passwordfield );
		
		/* Panel for the HTTP user agent */
		JPanel agentpanel = new JPanel();
		((FlowLayout) agentpanel.getLayout()).setAlignment ( FlowLayout.RIGHT );
		JLabel agentlabel = new JLabel ( "User Agent:" );
		agentfield = new JTextField ( "WebLech [Version C]", 20 );
		agentpanel.add ( agentlabel );
		agentpanel.add ( agentfield );
		
		/* Panel for a simple string match downloading limiter (no match, no download) */
		JPanel matchpanel = new JPanel();
		((FlowLayout) matchpanel.getLayout()).setAlignment ( FlowLayout.RIGHT );
		JLabel matchlabel = new JLabel ( "Match String:" );
		matchfield = new JTextField ( "", 20 );
		matchpanel.add ( matchlabel );
		matchpanel.add ( matchfield );
		
		/* Provides a panel for placing both the maximum depth and threads for this spider */
		JPanel detailpanel = new JPanel ( new GridLayout ( 1, 2 ) );
		
		JPanel depthpanel = new JPanel();
		((FlowLayout) depthpanel.getLayout()).setAlignment ( FlowLayout.RIGHT );
		JLabel depthlabel = new JLabel ( "Max Depth:" );
		depthfield = new JTextField ( Integer.toString ( 0 ), 5 );
		depthpanel.add ( depthlabel );
		depthpanel.add ( depthfield );
		
		JPanel threadpanel = new JPanel();
		JLabel threadlabel = new JLabel ( "Spider Threads:" );
		Integer[] threaditems = { new Integer ( "1" ), new Integer ( "2" ), new Integer ( "3" ), new Integer ( "4" ) };
		threadbox = new JComboBox ( threaditems );
		threadpanel.add ( threadlabel );
		threadpanel.add ( threadbox );
		
		detailpanel.add ( depthpanel );
		detailpanel.add ( threadpanel );
		
		/* Panel of buttons for various operations */
		JPanel buttonpanel = new JPanel();
		JButton save = new JButton ( "Save" );
		JButton spiderbutton = new JButton ( "Spider It" );
		JButton open = new JButton ( "Open" );
		JButton qbutton = new JButton ( "Quit" );
		buttonpanel.add ( save );
		buttonpanel.add ( spiderbutton );
		buttonpanel.add ( open );
		buttonpanel.add ( qbutton );
		
		add ( sitepanel );
		add ( outputpanel );
		add ( usernamepanel );
		add ( passpanel );
		add ( agentpanel );
		add ( matchpanel );
		add ( detailpanel );
		add ( buttonpanel );
		
		/* Configure the button actions */
		save.setActionCommand ( "save" );
		open.setActionCommand ( "open" );
		spiderbutton.setActionCommand ( "spider" );
		qbutton.setActionCommand ( "quit" );
		save.addActionListener ( this );
		open.addActionListener ( this );
		spiderbutton.addActionListener ( this );
		qbutton.addActionListener ( this );
		LechLogger.debug ( "Actions" );
	}
	
	/**
	 * This method will create a Properties object good for instantiating a new SpiderConfig
	 * Object.
	 */
	private Properties createProperties() {
		Properties p = new Properties();
		p.setProperty ( "saveRootDirectory", dirfield.getText() );
		p.setProperty ( "startLocation", sitenamefield.getText() );
		p.setProperty ( "basicAuthUser", usernamefield.getText() );
		p.setProperty ( "basicAuthPassword", passwordfield.getText() );
		p.setProperty ( "urlMatch", matchfield.getText() );
		p.setProperty ( "spiderThreads", ((Integer) threadbox.getSelectedItem()).toString() );
		p.setProperty ( "maxDepth", depthfield.getText() );
		p.setProperty ( "userAgent", agentfield.getText() );
		p.setProperty ( "interestingURLs", "" );
		return p;
	}
	
	/**
	 * This method will extract all of the values from a SpiderConfig object that the GUI uses
	 * and updates the panel to show the values in the object.
	 */
	private void setSpiderConfig ( SpiderConfig sc ) {
		dirfield.setText ( sc.getSaveRootDirectory().toString() );
		sitenamefield.setText ( sc.getStartLocation().toString() );
		usernamefield.setText ( sc.getBasicAuthUser() );
		passwordfield.setText ( sc.getBasicAuthPassword() );
		matchfield.setText ( sc.getURLMatch() );
		int t = sc.getSpiderThreads();
		if ( t < 1 || t > 4 )   {
			t = 1;
			sc.setSpiderThreads ( t );
		}
		threadbox.setSelectedIndex ( t - 1 );
		depthfield.setText ( Integer.toString ( sc.getMaxDepth() ) );
		agentfield.setText ( sc.getUserAgent() );
	}
	
	/**
	 * This method will coordinate all of the actions for the various buttons used.
	 */
	public void actionPerformed ( ActionEvent event )   {
		String cmd = event.getActionCommand();
		/* Download a new site */
		if ( cmd.equals ( "spider" ) )  {
			SpiderConfig c = new SpiderConfig ( createProperties() );
			Spider spider = new Spider ( c );
			/* But only if we are not already downloading the site */
			if ( spiders.contains ( spider ) )  {
				LechLogger.warn ( "Already have an instance of a Spider at " + c.getStartLocation() );
				return;
			}
			spiders.add ( spider );
			spider.start();
		}
		/* Save the current configuration to a file */
		else if ( cmd.equals ( "save" ) )   {
			JFileChooser f = new JFileChooser ( System.getProperty ( "user.home" ) );
			int r = f.showSaveDialog ( this );
			if ( r != JFileChooser.APPROVE_OPTION )  return;
			File outfile = f.getSelectedFile();
			try {
				ObjectOutputStream os = new ObjectOutputStream ( new FileOutputStream ( outfile ) );
				os.writeObject ( new SpiderConfig ( createProperties() ) );
				os.close();
			}
			catch ( Exception exception )   {
				LechLogger.error ( exception.toString() );
			}
		}
		/* Open a saved configuration from a file */
		else if ( cmd.equals ( "open" ) )   {
			JFileChooser f = new JFileChooser ( System.getProperty ( "user.home" ) );
			int r = f.showOpenDialog ( this );
			if ( r != JFileChooser.APPROVE_OPTION )  return;
			File infile = f.getSelectedFile();
			if ( !infile.canRead() )  {
				LechLogger.error ( "file " + f.toString() + " is not readable" );
				return;
			}
			try {
				ObjectInputStream os = new ObjectInputStream ( new FileInputStream ( infile ) );
				SpiderConfig sc = (SpiderConfig) os.readObject();
				os.close();
				setSpiderConfig ( sc );
			}
			catch ( Exception exception ) {
				LechLogger.error ( exception.toString() );
			}
		}
		/* Just quit */
		else if ( cmd.equals ( "quit" ) )   {
			System.exit ( 0 );
		}
	}
}