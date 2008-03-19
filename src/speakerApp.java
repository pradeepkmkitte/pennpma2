import java.io.*;
import java.util.Scanner;

import marf.MARF;
import marf.util.MARFException;

public class speakerApp
{

	protected static SpeakersIdentDb soDB;
	static Scanner sc = new Scanner(System.in);
	
		  
	public static void main(String[ ] argv){
	
		//File with final ID
		File confirmed = new File("confirmed.txt");
		
		//Database text file
		File db = new File("newDB.txt");
		
		//List of all samples in the folder
		File[] aoFiles = new File("training-samples").listFiles();
			
		//Name of last sample to name new sample
		String lastSaved = "9998";
		
		//Name of new file
		String name = "";
		
		//Confirmed ID
		String identity = "";
	
		
		try
		{	
			db.createNewFile();
			newDB();			
			
			soDB = new SpeakersIdentDb("newDB.txt");
			
			//open text file and populate data structure
			soDB.connect();
			soDB.query();
			setConfig();
			
			
			if(argv.length>0){
				
				//Training cluster needs to be deleted
				File cluster = new File("marf.Storage.TrainingSet.100.301.512.gzbin");
								
				//delete old training cluster
				if(cluster.exists()){
					if(!cluster.delete())
						System.out.println("Cluster not deleted");
				}
				
				String strFileName = "";
				
				//TRAINING ON ENTIRE DIRECTORY, ONE FILE AT A TIME
				for(int i = 0; i < aoFiles.length; i++)
				{
					strFileName = aoFiles[i].getPath();
					if(aoFiles[i].isFile() && strFileName.toLowerCase().endsWith(".wav"))
					{
						train(strFileName);
					}
				}
			}
	
			/*start loop so that after a sample is collected, identified and 
			 * trained, a new sample may be collected
			 */
			while(true){
				//get number of last sample saved (numbers start at 1000)
				aoFiles = new File("training-samples").listFiles();
				lastSaved = aoFiles[aoFiles.length-1].getName();
				lastSaved=lastSaved.substring(0, 4);
				
				//start the record process, passes the number of the last sample saved
				record sample = new record(Integer.parseInt(lastSaved));
	
				//the name assigned to the sample by recorded
				name = sample.getName();
				
				//IDENTIFY
				ident(name);			

				
					//get actual ID input from console
					System.out.print("ID: ");
					identity = sc.nextLine();
					
//					//get actual ID input from File
//					while(!confirmed.exists()){}
//					BufferedReader conf =  new BufferedReader(new FileReader(confirmed));
//					identity=conf.readLine();
//					if(confirmed.delete())
//						System.out.println("..ID read..");					
				
				entryTrain(identity,name);
				
				newDB();
				soDB = new SpeakersIdentDb("newDB.txt");
				soDB.connect();
				soDB.query();
				setConfig();
				
				train("training-samples\\"+name);				
			}
		}

//		MARF specific errors thrown by ident, train, setConfig, connect, query
		catch(MARFException e)
		{
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
		}

//		IO error thrown by createNewFile
		catch(IOException e){
			System.out.println("error");
		}
		
//		close the db connection
		finally
		{
			try
			{
				soDB.close();
			}
			catch(Exception e)
			{
				e.printStackTrace(System.err);
				System.exit(-1);
			}
		}
	
	}


/**
 * Creates or edits the text file for a specific speaker. This text file
 * holds a list of the training samples for the speaker. If the speaker is new,
 * a new file is created. Otherwise, this file is added to the list.
 * 
 * @param identity (of the speaker)
 * @param filename (of the sample)
 */

	public static final void entryTrain(String identity, String filename)
	{
//		create File object to control this identity's database entry
		File eT = new File("databases\\" + identity + ".txt");
		Writer writing;
	    try{
//	    	if the entry already exists, add to the end of it
			if(eT.exists()){
				  BufferedReader input =  new BufferedReader(new FileReader(eT));
			      String reading=input.readLine();
			      writing = new BufferedWriter(new FileWriter(eT));
			      writing.write(reading+"|"+filename);
			      writing.close();
			}
//			otherwise, the entry is new, the id # and the filename has to be created in the new entry
			else{
				  System.out.println("create new entry...");
				  eT.createNewFile();
				  writing = new BufferedWriter(new FileWriter(eT));
			      writing.write(identity+", ,"+filename);
			      writing.close();
			}
		}
		catch(FileNotFoundException e){
			System.out.println("entryTransit -- file not found");
		}
		catch(IOException e){
			System.out.println("entryTransit -- io exception");
		}
	}
	
/**
 * Combines all of the entry files in the databases directory into 
 * the file newDB.txt
 */
	public static final void newDB()
	{
		
		File[] dbFiles = new File("databases").listFiles();
		
		
		
	    try {
	    	
//		  create new object for the database text file and writer to write to it
	      File newDB = new File("newDB.txt");
		  BufferedWriter output = new BufferedWriter(new FileWriter(newDB));
			
	      BufferedReader input;
	      String line = null;
	      
//	      use the reader to read each file in the directory
	      for(int i=0; i<dbFiles.length;i++)
	      {
	    	  input = new BufferedReader(new FileReader(dbFiles[i]));
	    	  
//	    	  add the first line in each file to the String array
	    	  try {
		        line = input.readLine();
		        output.append(line);
		        output.newLine();
		      }
		      finally {
		        input.close();
		        
		      }
	      }
	      
	      output.close();
	      
	    }
	    catch (IOException ex){
	      ex.printStackTrace();
	    }
		
	}
	
/**
* Indetifies a speaker using MARF given a filename. Returns filename, first guess
* and second best guess in a text file.
*/
	public static final void ident(String name) throws MARFException
	{
		String pstrFilename = "training-samples\\"+ name;
		MARF.setSampleFile(pstrFilename);
		MARF.recognize();

		// First guess
		int iIdentifiedID = MARF.queryResultID();

		// Second best
		int iSecondClosestID = MARF.getResultSet().getSecondClosestID();
		
		try{
			
//			create writer and file to communicate filename, first ID, and second best ID to PMA
			BufferedWriter writing;
			File toDATABASE = new File("svID.txt");
	        toDATABASE.createNewFile();
			
	        writing = new BufferedWriter(new FileWriter(toDATABASE));
			
	        writing.write(name);
	        writing.newLine();
			writing.write(Integer.toString(iIdentifiedID));
			writing.newLine();
			writing.write(Integer.toString(iSecondClosestID));
			      
			writing.close();
		}
		catch(IOException e){
			System.out.println("ident can't write to file!");
		}
	}


/**
* Updates training set with a new sample from a given file.
* 
*/
	public static final void train(String pstrFilename)throws MARFException
	{
		MARF.setSampleFile(pstrFilename);
		
//		uses speakerIdentDB to associate the training file to speaker
		int iID = soDB.getIDByFilename(pstrFilename, true);

		if(iID == -1)
		{
			System.out.println("No speaker found for \"" + pstrFilename + "\" for training.");
		}
		else
		{
			MARF.setCurrentSubject(iID);
			MARF.train();
		}
	}


	

	/**
	 * Sets appropriate configuration parameters as normalization
	 * for preprocessing, FFT for feature extraction, Euclidean
	 * distance for training and classification, and WAVE file format.
	 */
	public static final void setConfig() throws MARFException
	{
		MARF.setPreprocessingMethod(MARF.DUMMY);
		MARF.setFeatureExtractionMethod(MARF.FFT);
		MARF.setClassificationMethod(MARF.EUCLIDEAN_DISTANCE);
		MARF.setDumpSpectrogram(false);
		MARF.setSampleFormat(MARF.WAV);
	}
}