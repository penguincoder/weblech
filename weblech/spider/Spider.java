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

import weblech.ui.LechLogger;

import java.util.*;
import java.io.*;
import java.net.URL;

public class Spider implements Runnable, Constants
{
    /** Config for the spider */
    private SpiderConfig config;
    /**
     * Download queue.
     * Thread safety: To access the queue, first synchronize on it.
     */
    private DownloadQueue queue;
    /**
     * Set of URLs downloaded or scheduled, so we don't download a
     * URL more than once.
     * Thread safety: To access the set, first synchronize on it.
     */
    private Set urlsDownloadedOrScheduled;
    /**
     * Set of URLs currently being downloaded by Spider threads.
     * Thread safety: To access the set, first synchronize on it.
     */
    private Set urlsDownloading;
    /**
     * Number of downloads currently taking place.
     * Thread safety: To modify this value, first synchronize on
     *                the download queue.
     */
    private int downloadsInProgress;
    /** Whether the spider should quit */
    private boolean quit;
    /** Count of running Spider threads. */
    private int running;
    /** Time we last checkpointed. */
    private long lastCheckpoint;

    public Spider(SpiderConfig config)
    {
        this.config = config;
        queue = new DownloadQueue(config);
        queue.queueURL(new URLToDownload(config.getStartLocation(), 0));
        urlsDownloadedOrScheduled = new HashSet();
        urlsDownloading = new HashSet();
        downloadsInProgress = 0;
        lastCheckpoint = 0;
    }

    public void start()
    {
        quit = false;
        running = 0;

        for(int i = 0; i < config.getSpiderThreads(); i++)
        {
            LechLogger.info("Starting Spider thread");
            Thread t = new Thread(this, "Spider-Thread-" + (i + 1));
            t.start();
            running++;
        }
    }

    public void stop()
    {
        quit = true;
    }

    public boolean isRunning()
    {
        return running == 0;
    }

    private void checkpointIfNeeded()
    {
        if(config.getCheckpointInterval() == 0)
        {
            return;
        }

        if(System.currentTimeMillis() - lastCheckpoint > config.getCheckpointInterval())
        {
            synchronized(queue)
            {
                if(System.currentTimeMillis() - lastCheckpoint > config.getCheckpointInterval())
                {
                    writeCheckpoint();
                    lastCheckpoint = System.currentTimeMillis();
                }
            }
        }
    }

    private void writeCheckpoint()
    {
        LechLogger.debug("writeCheckpoint()");
        try
        {
            FileOutputStream fos = new FileOutputStream("spider.checkpoint", false);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(queue);
            oos.writeObject(urlsDownloading);
            oos.close();
        }
        catch(IOException ioe)
        {
            LechLogger.warn("IO Exception attempting checkpoint: " + ioe.getMessage(), ioe);
        }
    }

    public void readCheckpoint()
    {
        try
        {
            FileInputStream fis = new FileInputStream("spider.checkpoint");
            ObjectInputStream ois = new ObjectInputStream(fis);
            queue = (DownloadQueue) ois.readObject();
            urlsDownloading = (Set) ois.readObject();
            queue.queueURLs(urlsDownloading);
            urlsDownloading.clear();
        }
        catch(Exception e)
        {
            LechLogger.error("Caught exception reading checkpoint: " + e.getMessage(), e);
        }
    }

    public void run()
    {
        HTMLParser htmlParser = new HTMLParser(config);
        URLGetter urlGetter = new URLGetter(config);

        while((queueSize() > 0 || downloadsInProgress > 0) && quit == false)
        {
            checkpointIfNeeded();
            if(queueSize() == 0 && downloadsInProgress > 0)
            {
                // Wait for a download to finish before seeing if this thread should stop
                try
                {
                    Thread.sleep(QUEUE_CHECK_INTERVAL);
                }
                catch(InterruptedException ignored)
                {
                }
                // Have another go at the loop
                continue;
            }
            else if(queueSize() == 0)
            {
                break;
            }
            URLToDownload nextURL;
            synchronized(queue)
            {
                nextURL = queue.getNextInQueue();
                downloadsInProgress++;
            }
            synchronized(urlsDownloading)
            {
                urlsDownloading.add(nextURL);
            }
            int newDepth = nextURL.getDepth() + 1;
            int maxDepth = config.getMaxDepth();
            synchronized(urlsDownloading)
            {
                urlsDownloading.remove(nextURL);
            }
            List newURLs = downloadURL(nextURL, urlGetter, htmlParser);

            newURLs = filterURLs(newURLs);

            ArrayList u2dsToQueue = new ArrayList();
            for(Iterator i = newURLs.iterator(); i.hasNext(); )
            {
                URL u = (URL) i.next();
                // Download if not yet downloaded, and the new depth is less than the maximum
                synchronized(urlsDownloadedOrScheduled)
                {
                    if(!urlsDownloadedOrScheduled.contains(u)
                    && (maxDepth == 0 || newDepth <= maxDepth))
                    {
                        u2dsToQueue.add(new URLToDownload(u, nextURL.getURL(), newDepth));
                        urlsDownloadedOrScheduled.add(u);
                    }
                }
            }
            synchronized(queue)
            {
                queue.queueURLs(u2dsToQueue);
                downloadsInProgress--;
            }
        }
        LechLogger.info("Spider thread stopping [" + config.getStartLocation() + "]" );
        running--;
    }

    /**
     * Get the size of the download queue in a thread-safe manner.
     */
    private int queueSize()
    {
        synchronized(queue)
        {
            return queue.size();
        }
    }

    /**
     * Get a URL, and return new URLs that are referenced from it.
     *
     * @return A List of URL objects.
     */
    private List downloadURL(URLToDownload url, URLGetter urlGetter, HTMLParser htmlParser)
    {
        LechLogger.debug("downloadURL(" + url + ")");

        // Bail out early if image and already on disk
        URLObject obj = new URLObject(url.getURL(), config);
        if(obj.existsOnDisk())
        {
            if(config.refreshHTMLs() && (obj.isHTML() || obj.isXML()))
            {
                LechLogger.info("Q: [" + queue + "] " + url);
                obj = urlGetter.getURL(url);
            }
            else if(config.refreshImages() && obj.isImage())
            {
                LechLogger.info("Q: [" + queue + "] " + url);
                obj = urlGetter.getURL(url);
            }
        }
        else
        {
            LechLogger.info("Q: [" + queue + "] " + url);
            obj = urlGetter.getURL(url);
        }

        if(obj == null)
        {
            return new ArrayList();
        }

        if(!obj.existsOnDisk())
        {
            obj.writeToFile();
        }

        if(obj.isHTML() || obj.isXML())
        {
            return htmlParser.parseLinksInDocument(url.getURL(), obj.getStringContent());
        }
        else if(obj.isImage())
        {
            return new ArrayList();
        }
        else
        {
            LechLogger.warn("Unknown content type received: " + obj.getContentType());
            LechLogger.info("URL was " + url);
            return new ArrayList();
        }
    }

    private List filterURLs(List URLs)
    {
        String match = config.getURLMatch();
        ArrayList retVal = new ArrayList();

        synchronized(urlsDownloadedOrScheduled)
        {
            for(Iterator i = URLs.iterator(); i.hasNext(); )
            {
                URL u = (URL) i.next();
                if(urlsDownloadedOrScheduled.contains(u))
                {
                    continue;
                }

                String s = u.toString();
                if(s.indexOf(match) != -1)
                {
                    retVal.add(u);
                }
            }
        }
        return retVal;
    }
	
	/* Method By Coleman
	 * A basic check to see if there is another spider downloading the same thing
	 */
	protected boolean compareSpiderConfig ( SpiderConfig sc )   {
		return config.getStartLocation().equals ( sc.getStartLocation() );
	}
	
	/* Method By Coleman
	 * A method to determine if one spider is downloading the same file as another spider
	 */
	public boolean equals ( Object o )  {
		if ( !o.getClass().isInstance ( this ) ) return false;
		return ((Spider) o).compareSpiderConfig ( config );
	}

}
