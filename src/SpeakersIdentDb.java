/*
 * SpeakersIdentDb.java
 * 
 * Scott Kyle, Erika Sanchez, and Meredith Skolnick
 *
 * Purpose: Organizes wave files by id.
 */ 

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

public class SpeakersIdentDb
extends Database
{
	private static String dbFolder = "speaker/databases";

	private Hashtable fCount = null;

	/**
	 * A vector of vectors of speakers info pre-loded on connect()
	 * 
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
	 * Retrieves hashtable with number of wave samples for every ID in the database
	 */
	public final Hashtable getNumberPerID()
	{
		return fCount;
	}

	/**
	 * Connects to the "database" of speakers (opens the text file).
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
		String strLine;
		int iID = -1;

		try
		{
			strLine = this.oConnection.readLine();
			Vector oSpeakerInfo, oTrainingFilenames;
			StringTokenizer oTokenizer, oSTK;

			while(strLine != null)
			{
				oTokenizer = new StringTokenizer(strLine, ",");
				oSpeakerInfo = new Vector();

				// get ID
				if(oTokenizer.hasMoreTokens())
				{
					iID = Integer.parseInt(oTokenizer.nextToken());
				}
 
				// training file names
				oTrainingFilenames = new Vector();

				if(oTokenizer.hasMoreTokens())
				{
					oSTK = new StringTokenizer(oTokenizer.nextToken(), "|");

					while(oSTK.hasMoreTokens())
					{
						strLine = oSTK.nextToken();
						oTrainingFilenames.add(strLine);
					}
				}

				oSpeakerInfo.add(oTrainingFilenames);
				
				//populates hashtable with size of each entry in wave files
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
}