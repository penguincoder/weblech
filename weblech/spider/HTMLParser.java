/*
 * This is the MIT license, see also http://www.opensource.org/licenses/mit-license.html
 *
 * Copyright (c) 2001 Brian Pitcher
 * Copyright (c) 2004 Andrew Coleman
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package weblech.spider;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.FileWriter;
import java.io.PrintWriter;

import weblech.ui.LechLogger;

public class HTMLParser
{
	private SpiderConfig config;

	public HTMLParser(SpiderConfig config)
    {
        this.config = config;
    }

    public List parseLinksInDocument(URL sourceURL, String textContent)
    {
        return parseAsHTML(sourceURL, textContent);
    }

    private List parseAsHTML(URL sourceURL, String textContent)
    {
        LechLogger.debug("parseAsHTML()");
        ArrayList newURLs = new ArrayList();
        HashSet newURLSet = new HashSet();

		/* note from coleman:
		 * I had to add a few tags into this, namely the link and embeds. weblech should download flash
		 * movies, mpegs, avis, and anything else that it finds on the page. even stylesheets :)
		 */
        extractAttributesFromTags("img", "src", sourceURL, newURLs, newURLSet, textContent);
        extractAttributesFromTags("a", "href", sourceURL, newURLs, newURLSet, textContent);
        extractAttributesFromTags("body", "background", sourceURL, newURLs, newURLSet, textContent);
        extractAttributesFromTags("frame", "src", sourceURL, newURLs, newURLSet, textContent);
		extractAttributesFromTags("link", "href", sourceURL, newURLs, newURLSet, textContent);
		extractAttributesFromTags("embed", "src", sourceURL, newURLs, newURLSet, textContent);
        extractAttributesFromTags("IMG", "SRC", sourceURL, newURLs, newURLSet, textContent);
        extractAttributesFromTags("A", "HREF", sourceURL, newURLs, newURLSet, textContent);
        extractAttributesFromTags("BODY", "BACKGROUND", sourceURL, newURLs, newURLSet, textContent);
        extractAttributesFromTags("FRAME", "SRC", sourceURL, newURLs, newURLSet, textContent);
		extractAttributesFromTags("LINK", "HREF", sourceURL, newURLs, newURLSet, textContent);
		extractAttributesFromTags("EMBED", "SRC", sourceURL, newURLs, newURLSet, textContent);

        if(newURLs.size() == 0)
        {
            LechLogger.debug("Got 0 new URLs from HTML parse, check HTML\n" + textContent);
        }
        LechLogger.debug("Returning " + newURLs.size() + " urls extracted from page");
        return newURLs;
    }

    private void extractAttributesFromTags(String tag, String attr, URL sourceURL, List newURLs, Set newURLSet, String input)
    {
        LechLogger.debug("extractAttributesFromTags(" + tag + ", " + attr + ", ...)");

        int startPos = 0;
        String startTag = "<" + tag + " ";
        String attrStr = attr + "=\"";
        while(true)
        {
            int tagPos = input.indexOf(startTag, startPos);
            if(tagPos < 0)
            {
                return;
            }
            int attrPos = input.indexOf(attrStr, tagPos + 1);
            if(attrPos < 0)
            {
                startPos = tagPos + 1;
                continue;
            }
            int nextClosePos = input.indexOf(">", tagPos + 1);
            if(attrPos < nextClosePos)
            {
                // Ooh, found one
                int closeQuotePos = input.indexOf("\"", attrPos + attrStr.length() + 1);
                if(closeQuotePos > 0)
                {
                    String urlStr = input.substring(attrPos + attrStr.length(), closeQuotePos);
                    if(urlStr.indexOf('#') != -1)
                    {
                        urlStr = urlStr.substring(0, urlStr.indexOf('#'));
                    }
                    //LechLogger.debug("Found possible URL string: " + URL);

                    if(isMailTo(urlStr))
                    {
                        logMailURL(urlStr);
                    }
                    else
                    {
                        try
                        {

                            URL u = new URL(sourceURL, urlStr);
                            if(newURLSet.contains(u))
                            {
                                //LechLogger.debug("Already found URL on page: " + u);
                            }
                            else
                            {
                                newURLs.add(u);
                                newURLSet.add(u);
                                //LechLogger.debug("Found new URL on page: " + u);
                            }
                        }
                        catch(MalformedURLException murle)
                        {
                        }
                    }
                }
                startPos = tagPos + 1;
                continue;
            }
            else
            {
                startPos = tagPos + 1;
                continue;
            }
        }
    }

    private void logMailURL(String url)
    {
        LechLogger.debug("logMailURL()");

        try
        {
            FileWriter appendedFile = new FileWriter(config.getMailtoLogFile().toString(), true);
            PrintWriter pW = new PrintWriter(appendedFile);
            pW.println(url);
            pW.flush();
            pW.close();
        }
        catch(IOException ioe)
        {
            LechLogger.warn("Caught IO exception writing mailto URL:" + ioe.getMessage(), ioe);
        }
    }

    /**
     * Check if a particular URL looks like it's a mailto: style link.
     */
    private boolean isMailTo(String url)
    {
        if(url == null)
        {
            return false;
        }

        url = url.toUpperCase();
        return (url.indexOf("MAILTO:") != -1);
    }
}
