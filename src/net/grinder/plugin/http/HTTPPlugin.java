// The Grinder
// Copyright (C) 2001  Paco Gomez
// Copyright (C) 2001  Philip Aston

// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

package net.grinder.plugin.http;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import net.grinder.plugininterface.PluginThreadContext;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.SimplePluginBase;
import net.grinder.plugininterface.Test;
import net.grinder.plugininterface.ThreadCallbacks;
import net.grinder.util.FilenameFactory;
import net.grinder.util.GrinderException;
import net.grinder.util.GrinderProperties;


/**
 * Simple HTTP client benchmark.
 * 
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
public class HttpPlugin extends SimplePluginBase
{
    private PluginThreadContext m_pluginThreadContext = null;
    private FilenameFactory m_filenameFactory = null;
    private HashMap m_callData = new HashMap();
    private boolean m_logHTML = true;
    private HttpMsg m_httpMsg = null;
    private int m_currentIteration = 0; // How many times we've done all the URL's
    private final DateFormat m_dateFormat =
	new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss zzz");

    /**
     * Inner class that holds the data for a call.
     */
    protected class CallData implements HttpRequestData
    {
	private String m_urlString;
	private  String m_okString;
	private long m_ifModifiedSince = -1;
	private String m_postString = null;
    
	public CallData(Test test) throws PluginException
	{
	    final GrinderProperties testParameters = test.getParameters();

	    try {
		m_urlString = testParameters.getMandatoryProperty("url");
	    }
	    catch (GrinderException e) {
		throw new PluginException(
		    "URL for Test " + test.getTestNumber() + " not specified",
		    e);
	    }

	    m_okString = testParameters.getProperty("ok", null);

	    final String ifModifiedSinceString =
		testParameters.getProperty("ifModifiedSince", null);

	    if (ifModifiedSinceString != null) {
		try {
		    final Date date =
			m_dateFormat.parse(ifModifiedSinceString);
	
		    m_ifModifiedSince = date.getTime();
		}
		catch (ParseException e) {
		    m_pluginThreadContext.logError(
			"Couldn't parse ifModifiedSince date '" +
			ifModifiedSinceString + "'");
		}
	    }

	    final String postFilename =
		testParameters.getProperty("post", null);

	    if (postFilename != null) {
		try {
		    final FileReader in = new FileReader(postFilename);
		    final StringWriter writer = new StringWriter(512);
		    
		    char[] buffer = new char[4096];
		    int charsRead = 0;

		    while ((charsRead = in.read(buffer, 0, buffer.length)) > 0)
		    {
			writer.write(buffer, 0, charsRead);
		    }
		
		    in.close();
		    writer.close();
		    m_postString = writer.toString();
		}
		catch (IOException e) {
		    m_pluginThreadContext.logError(
			"Could not read post data from " + postFilename);

		    e.printStackTrace(System.err);
		}
	    }	    
	}

	public String getURLString() { return m_urlString; }
	public String getContextURLString() { return null; }
	public String getPostString() { return m_postString; }
	public long getIfModifiedSince() { return m_ifModifiedSince; }
	public String getOKString() { return m_okString; }

	protected void setURLString(String s) { m_urlString = s; }
	protected void setPostString(String s) { m_postString = s; }
	protected void setIfModifiedSince(long l) { m_ifModifiedSince = l; }
	protected void setOKString(String s) { m_okString = s; }
    }

    /**
     * This method initializes the plug-in.
     */    
    public void initialize(PluginThreadContext pluginThreadContext)
	throws PluginException
    {
	m_pluginThreadContext = pluginThreadContext;

	final GrinderProperties parameters =
	    pluginThreadContext.getPluginParameters();

	m_filenameFactory = pluginThreadContext.getFilenameFactory();

	boolean useCookies = parameters.getBoolean("keepSession", false);

	if (useCookies) {
	    m_pluginThreadContext.logError(
		"'keepSession' has been renamed to 'useCookies'." +
		"Please update your grinder.properties.");
	}
	else {
	    useCookies = parameters.getBoolean("useCookies", true);
	}

	m_httpMsg = new HttpMsg(pluginThreadContext,
				useCookies,
				parameters.getBoolean("followRedirects",
						      false));

	m_logHTML = parameters.getBoolean("logHTML", false);
    }

    public void beginCycle() throws PluginException
    {
	// Reset cookie if necessary.
	m_httpMsg.reset();      
    }

    /**
     * This method processes the URLs.
     */    
    public boolean doTest(Test test) throws PluginException
    {
	final Integer testNumber = test.getTestNumber();
	
	CallData callData = (CallData)m_callData.get(testNumber);
	
	if (callData == null) {
	    callData = createCallData(m_pluginThreadContext, test);
	    m_callData.put(testNumber, callData);
	}
	
	// Do the call.
	final String page;

	try {
	    page = m_httpMsg.sendRequest(callData);
	}
	catch (IOException e) {
	    throw new PluginException("HTTP IOException: " + e, e);
	}

	final boolean error;
	final String okString = callData.getOKString();

	if (page == null) {
	    error = okString != null;
	}
	else {
	    error = okString != null && page.indexOf(okString) == -1;
	
	    if (m_logHTML || error) {
		final String filename =
		    m_filenameFactory.createFilename("page",
						     "_" + m_currentIteration +
						     "_" + testNumber +
						     ".html");
		try {
		    final BufferedWriter htmlFile =
			new BufferedWriter(new FileWriter(filename, false));

		    htmlFile.write(page);
		    htmlFile.close();
		}
		catch (IOException e) {
		    throw new PluginException("Error writing to " + filename +
					      ": " + e, e);
		}

		if (error) {
		    m_pluginThreadContext.logError(
			"The 'ok' string ('" + okString +
			"') was not found in the page received. " +
			"The output has been written to '" + filename + "'");
		}
	    }
	}

	return !error;
    }

    /**
      * Give derived classes a chance to be interesting.
      */
    protected CallData createCallData(PluginThreadContext pluginThreadContext,
 				      Test test)
	throws PluginException
    {
 	return new CallData(test);
    }

    public void endCycle() throws PluginException
    {
	m_currentIteration++;
    }
}
