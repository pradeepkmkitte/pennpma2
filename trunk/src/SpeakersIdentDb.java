import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import marf.Storage.Database;
import marf.Storage.StorageException;
import marf.util.Arrays;
import marf.util.Debug;


/**
 * <p>Class SpeakersIdentDb manages database of speakers on the application level.</p>
 * <p>XXX: Move stats collection over to MARF.</p>
 *
 * <p>$Id: SpeakersIdentDb.java,v 1.24 2006/01/08 14:47:43 mokhov Exp $</p>
 *
 * @author Serguei Mokhov
 * @version $Revision: 1.24 $
 * @since 0.0.1
 */
public class SpeakersIdentDb
extends Database
{
	private static String dbFolder = "speaker/databases";

	private Hashtable fCount = null;

	/**
	 * A vector of vectors of speakers info pre-loded on <code>connect()</code>.
	 * @see #connect()
	 */
	private Hashtable oDB = null;

	/**
	 * "Database connection".
	 */
	private BufferedReader oConnection = null;

	public SpeakersIdentDb(final String pstrFileName)
	{
		this.strFilename = pstrFileName;
		this.oDB = new Hashtable();
		this.fCount = new Hashtable();

	}

	/**
	 * Retrieves Speaker's ID by a sample filename.
	 * @param pstrFileName Name of a .wav file for which ID must be returned
	 * @param pbTraining indicates whether the filename is a training (<code>true</code>) sample or testing (<code>false</code>)
	 * @return int ID
	 * @throws StorageException in case of an error in any I/O operation
	 */
	public final int getIDByFilename(final String pstrFileName)
	throws StorageException
	{
		String strFilenameToLookup;



		// Extract actual file name without preceeding path (if any)
		if(pstrFileName.lastIndexOf('/') >= 0)
		{
			strFilenameToLookup = pstrFileName.substring(pstrFileName.lastIndexOf('/') + 1, pstrFileName.length());
		}
		else if(pstrFileName.lastIndexOf('\\') >= 0)
		{
			strFilenameToLookup = pstrFileName.substring(pstrFileName.lastIndexOf('\\') + 1, pstrFileName.length());
		}
		else
		{
			strFilenameToLookup = pstrFileName;
		}

		Enumeration oIDs = this.oDB.keys();

		// Traverse all the info vectors looking for sample filename
		while(oIDs.hasMoreElements())
		{
			Integer oID = (Integer)oIDs.nextElement();

			Debug.debug("File: " + pstrFileName + ", id = " + oID.intValue());

			Vector oSpeakerInfo = (Vector)this.oDB.get(oID);
			Vector oFilenames;

			oFilenames = (Vector)oSpeakerInfo.elementAt(0);

			// Start from 1 because 0 is speaker's name
			for(int i = 0; i < oFilenames.size(); i++)
			{
				String strCurrentFilename = (String)oFilenames.elementAt(i);

				if(strCurrentFilename.equals(strFilenameToLookup))
				{
					int ID = oID.intValue();

//					fCount[ID]++;

					return ID;
				}
			}
		}

		return -1;
	}

	/**
	 * Retrieves Speaker's ID by a sample filename.
	 * @param pstrFileName Name of a .wav file for which ID must be returned
	 * @param pbTraining indicates whether the filename is a training (<code>true</code>) sample or testing (<code>false</code>)
	 * @return int ID
	 * @throws StorageException in case of an error in any I/O operation
	 */
	public final Hashtable getNumberPerID()
	throws StorageException
	{
		return fCount;
	}

	/**
	 * Connects to the "database" of speakers (opens the text file :-)).
	 * @throws StorageException in case of any I/O error
	 */
	public void connect()
	throws StorageException
	{
		// That's where we should establish file linkage and keep it until closed
		try
		{
			this.oConnection = new BufferedReader(new FileReader(this.strFilename));
			this.bConnected = true;
		}
		catch(IOException e)
		{
			throw new StorageException
			(
					"Error opening speaker DB: \"" + this.strFilename + "\": " +
					e.getMessage() + "."
			);
		}

	}

	/**
	 * Retrieves speaker's data from the text file and populates
	 * internal data structures. Uses StringTokenizer to parse
	 * data read from the file.
	 * @throws StorageException in case of any I/O error
	 */
	public void query()
	throws StorageException
	{
		// That's where we should load db results into internal data structure

		String strLine;
		int iID = -1;

		try
		{
			strLine = this.oConnection.readLine();


			while(strLine != null)
			{
				StringTokenizer oTokenizer = new StringTokenizer(strLine, ",");
				Vector oSpeakerInfo = new Vector();

				// get ID
				if(oTokenizer.hasMoreTokens())
				{
					iID = Integer.parseInt(oTokenizer.nextToken());
				}
 
				// training file names
				Vector oTrainingFilenames = new Vector();

				if(oTokenizer.hasMoreTokens())
				{
					StringTokenizer oSTK = new StringTokenizer(oTokenizer.nextToken(), "|");

					while(oSTK.hasMoreTokens())
					{
						strLine = oSTK.nextToken();
						oTrainingFilenames.add(strLine);
					}
				}

				oSpeakerInfo.add(oTrainingFilenames);
				fCount.put(iID, oTrainingFilenames.size());

				Debug.debug("Putting ID=" + iID + " along with info vector of size " + oSpeakerInfo.size());

				this.oDB.put(new Integer(iID), oSpeakerInfo);

				strLine = this.oConnection.readLine();
			}
		}
		catch(IOException e)
		{
			throw new StorageException
			(
					"Error reading from speaker DB: \"" + this.strFilename +
					"\": " + e.getMessage() + "."
			);
		}

	}

	/**
	 * Closes (file) database connection.
	 * @throws StorageException if not connected or fails to close inner reader
	 */
	public void close()
	throws StorageException
	{
		// Close file
		if(this.bConnected == false)
		{
			throw new StorageException("SpeakersIdentDb.close() - not connected");
		}

		try
		{
			this.oConnection.close();
			this.bConnected = false;
		}
		catch(IOException e)
		{
			throw new StorageException(e.getMessage());
		}
	}

	private static int getLast(String folder, String fS){
		int ID = -1;
		String temp1,temp2;
		int temp;
		final String fString = fS;

		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(fString);
			}
		};

		File[] aoFiles = new File(folder).listFiles(filter);
		for(int i = 0;i < aoFiles.length;i++){
			temp1 = aoFiles[i].getName();
			temp2 = temp1.substring(0,temp1.lastIndexOf(fString));
			temp=Integer.parseInt(temp2);
			if(temp>ID)
				ID=temp;
		}

		return ID;

	}
}

//EOF