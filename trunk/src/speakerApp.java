import java.io.*;

import marf.MARF;
import marf.util.MARFException;

public class speakerApp
{

	protected static SpeakersIdentDb soDB;
	
	//Current file name
	static String name="";
	
	//List of all samples in the folder
	static File[] aoFiles;
	
	//Database text file
	static File db;
	
	static String database = "speaker/newDB.txt";
	static String samplesFolder = "speaker/training-samples";
	static String dbFolder = "speaker/databases";
	static String cName = "marf.Storage.TrainingSet.100.301.512.gzbin";
	static String nSave = "speaker/nameSave.txt";
	static String sent = "speaker/speakerids.txt";
	
	
	public static void main(String[ ] argv){
		
//		System.out.println(argv[0]);
		
		try{
			setConfig();
		}
		catch(MARFException e){
			System.out.println("can't set config");
		}
		
		if(argv[0].compareToIgnoreCase("begin")==0){
			System.out.print("begin...");
			begin();
		}
		else if(argv[0].compareToIgnoreCase("IDfound")==0){
			IDfound(argv[1]);
		}
		else if(argv[0].compareToIgnoreCase("delete")==0){
			delete(argv[1]);
		}
		else if(argv[0].compareToIgnoreCase("totTrain")==0){
			totTrain();
		}
		else{
			System.out.println("enter a function, dumbass!");
		}
		
	}
	
	public static void begin(){
		
		//Database text file
		db = new File(database);
					
		String lastSaved = "9998";
			
				
		try
		{	
			soDB = new SpeakersIdentDb(database);
			
			//open text file and populate data structure
			soDB.connect();
			soDB.query();
			
				
			//get number of last sample saved (numbers start at 1000)
			aoFiles = new File(samplesFolder).listFiles();
			lastSaved = aoFiles[aoFiles.length-1].getName();
			lastSaved=lastSaved.substring(0, 4);
				
			//start the record process, passes the number of the last sample saved
			record sample = new record(Integer.parseInt(lastSaved));
	
			//the name assigned to the sample by recorded
			name = sample.getName();
				
			//IDENTIFY
			ident(name);			
				
		}

//		MARF specific errors thrown by ident, train, setConfig, connect, query
		catch(MARFException e)
		{
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
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

	public static void totTrain(){
		
		System.out.println("Hold up. I'm training. Patience is a virtue.");
		
		aoFiles = new File(samplesFolder).listFiles();

		try{
			
			//Training cluster needs to be deleted
			File cluster = new File(cName);
							
			//delete old training cluster
			if(cluster.exists()){
				if(!cluster.delete())
					System.out.println("Cluster not deleted");
			}
			
			db = new File(database);
			db.createNewFile();
			newDB();			
			
			soDB = new SpeakersIdentDb(database);
			
			//open text file and populate data structure
			soDB.connect();
			soDB.query();
						
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
		catch(MARFException e)
		{
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
		}

//		IO error thrown by createNewFile
		catch(IOException e){
			System.out.println("error");
		}
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

	public static void IDfound(String id){
		File nameSave = new File(nSave);
		if(nameSave.exists()){
			try{
				
				BufferedReader nS = new BufferedReader(new FileReader(nameSave));
				name = nS.readLine();
				nS.close();
				entryTrain(id,name);
				
				newDB();
				soDB = new SpeakersIdentDb(database);
				soDB.connect();
				soDB.query();
					
				train(samplesFolder+"/"+name);
				
				nameSave.delete();
				
				File guess = new File(sent);
				guess.delete();
			}
	//		MARF specific errors thrown by ident, train, setConfig, connect, query
			catch(MARFException e)
			{
				System.err.println(e.getMessage());
				e.printStackTrace(System.err);
			}
			catch(IOException e){
				System.out.println("IDfound name read broke");
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
		else
			System.out.println("The ID has already been set for the last sample recorded.");
	}

	public static void delete(String id){
		try{
			
			soDB = new SpeakersIdentDb(database);
			
			//open text file and populate data structure
			soDB.connect();
			soDB.query();
			
			aoFiles = new File(samplesFolder).listFiles();
			int idCheck;
			for(int i=0;i<aoFiles.length;i++){
				MARF.setSampleFile(aoFiles[i].getName());
				idCheck = soDB.getIDByFilename(aoFiles[i].getName(), true);
				String path=aoFiles[i].getPath();
				if(Integer.parseInt(id)==idCheck){
					if(path.toLowerCase().endsWith(".wav")){
						if(aoFiles[i].delete()){
							System.out.println("ID matches: "+aoFiles[i].getName()+" <- deleted");
						}
					}
				}
			}
			
			File delEntry = new File(database+"/"+id+".txt");
			if(!delEntry.exists())
				System.out.println("can't find text file!");
			else
				delEntry.delete();
			
			totTrain();
		}
		catch(MARFException e){
			System.out.println("aoFiles[i].getName error");
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
		File eT = new File(database+"/" + identity + ".txt");
		Writer writing;
	    try{
//	    	if the entry already exists, add to the end of it
			if(eT.exists()){
				  BufferedReader input =  new BufferedReader(new FileReader(eT));
			      String reading=input.readLine();
			      input.close();
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
			System.out.println("entryTrain -- file not found");
		}
		catch(IOException e){
			System.out.println("entryTrain -- io exception");
		}
	}
	
/**
 * Combines all of the entry files in the databases directory into 
 * the file newDB.txt
 */
	public static final void newDB()
	{
		
		File[] dbFiles = new File(dbFolder).listFiles();
		
	    try {
	    	
//		  create new object for the database text file and writer to write to it
	      File newDB = new File(database);
		  BufferedWriter output = new BufferedWriter(new FileWriter(newDB));
			
	      BufferedReader input;
	      String line = null;
	      
//	      use the reader to read each file in the directory
	      for(int i=1; i<dbFiles.length;i++)
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
		String pstrFilename = samplesFolder+"/"+ name;
		MARF.setSampleFile(pstrFilename);
		MARF.recognize();
		
		if ( MARF.getResultSet().size() == 0 ) {
			System.out.println("No results");
			System.exit(0);
		}

		// First guess
		int iIdentifiedID = MARF.queryResultID();

		// Second best
		int iSecondClosestID = MARF.getResultSet().getSecondClosestID();
		
		try{
			
//			create writer and file to communicate filename, first ID, and second best ID to PMA
			BufferedWriter writing, nS;
			File toDATABASE = new File(sent);
	        File nameSave = new File(nSave);
			toDATABASE.createNewFile();
			nameSave.createNewFile();
			
	        writing = new BufferedWriter(new FileWriter(toDATABASE));
	        nS = new BufferedWriter(new FileWriter(nameSave));
	        
			
	        nS.write(name);
	        nS.close();
	        
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