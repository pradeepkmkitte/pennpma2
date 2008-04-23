import java.io.*;
import java.util.*;

import marf.MARF;
import marf.util.MARFException;
import marf.Storage.*;

public class speakerApp
{
	//Database of IDs and their wave files
	protected static SpeakersIdentDb soDB;

	//Current file name
	private static String name="";

	//List of all samples in the folder
	private static File[] aoFiles;

	//Database text file
	private static File db;

	//Will be initialized to record
	private static record sample;

	//folders and files
	private static String database = "speaker/newDB.txt";
	private static String samplesFolder = "speaker/training-samples";
	private static String dbFolder = "speaker/databases";
	private static String cName = "marf.Storage.TrainingSet.100.301.512.gzbin";
	private static String sent = "speaker/speakerids.txt";
	private static String temp = samplesFolder+"/temp.wav";


	public static void main(String[ ] argv)throws MARFException{

		try{
			//MARF needs settings before anything else can be done
			setConfig();
		}
		catch(MARFException e){
			System.err.println("Cannot set configuration");
		}
		if(argv[0].compareToIgnoreCase("identify")==0){
			begin();
		}
		else if(argv[0].compareToIgnoreCase("save")==0){
			IDFound(argv[1],temp);
		}
		else if(argv[0].compareToIgnoreCase("erase")==0){
			delete(argv[1]);
		}
		else if(argv[0].compareToIgnoreCase("train")==0){
			totTrain();
		}
		else if(argv[0].compareToIgnoreCase("retest")==0){
			retest();
		}
//		else if(argv[0].compareToIgnoreCase("test")==0){
//			totTrain();
//
//			for(int i=2;i<52;i++){
//				ident("test.wav");
//				File sen = new File(sent);
//				File save = new File("speaker/ids/"+entrySize(9972)+".txt");
//				System.out.println("Rename "+(i-1)+" "+sen.renameTo(save));
//				IDFound("9972",samplesFolder+"/9972_"+i+".wav");
//			}
//		}
		else if(argv[0].compareToIgnoreCase("reset")==0){
			reset();
		}
		else{
			System.err.println("No function selected.");
		}

	}

	/**
	 * 
	 * Starts record process and IDs sample. Returns top 2 IDs to the text file 'sent'.
	 * Saves sample as 'temp.wav'. User must press 'Enter' in console to end this function.
	 * If a temp sample already exists, it is overwritten.
	 * 
	 */       

	public static void begin(){

		try
		{      
			File tempsample = new File(temp);    
			if(tempsample.exists()){
				if((new File(temp)).delete()){
//					System.out.print("(Old temp sample cleared.)");
				}

			}

			newDB();
			soDB = new SpeakersIdentDb(database);

			// open text file and populate data structure
			soDB.connect();
			soDB.query();

			// start the record process, passes the number of the last sample saved
			sample = new record();


			// the name assigned to the sample by recorded
			name = sample.getName();

			for(float f=0;f<1e6;f++){}

			// IDENTIFY
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

	/**
	 * 
	 * Renames the temp.wav and trains it into the system given the correct ID 
	 * After the training, the 'sent' file created by begin() is deleted
	 * 
	 */       
	public static void found(String id, String fName){
		File tempsample = new File(fName);
//		System.out.println("\tSaving: "+tempsample.getPath());
		if(tempsample.exists()){
			try{

				int identity = Integer.parseInt(id);
				File old = new File(fName);
				name = old.getName();

				entryTrain(id,name);

				newDB();
				soDB = new SpeakersIdentDb(database);
				soDB.connect();
				soDB.query();
				
				train(samplesFolder+"/"+name);
				
				
				File guess = new File(sent);
				guess.delete();
				
			}
//              MARF specific errors thrown by ident, train, setConfig, connect, query
			catch(MARFException e)
			{
				System.err.println(e.getMessage());
				e.printStackTrace(System.err);
			}       
			//              close the db connection
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
			System.out.println("\nno WAVE file to save");
	}

	public static void IDFound(String id, String fName){
		File tempsample = new File(fName);

		if(tempsample.exists()){
			try{

				int identity = Integer.parseInt(id);
				int number = entrySize(identity)+1;


				File old = new File(fName);

				File change = new File(samplesFolder+"/"+identity+"_"+number+".wav");
				
				if(change.exists()){
					for(int i=1;i<number;i++){
						change = new File(samplesFolder+"/"+identity+"_"+i+".wav");
						if(change.exists())
							break;
					}
				}


				if(old.renameTo(change)){
					for(float f=0;f<1e6;f++){};
					name = change.getName();
				}
				else
					name = old.getName();
				
				if(name.equalsIgnoreCase("temp.wav")){
					System.out.println("Didn't change file name. No saving done.");
					System.exit(0);
				}
				
				System.out.println("\n"+name+" saved.");

				entryTrain(id,name);

				newDB();
				soDB = new SpeakersIdentDb(database);
				soDB.connect();
				soDB.query();
				
				train(samplesFolder+"/"+name);
				
				
				File guess = new File(sent);
				guess.delete();
				
			}
//              MARF specific errors thrown by ident, train, setConfig, connect, query
			catch(MARFException e)
			{
				System.err.println(e.getMessage());
				e.printStackTrace(System.err);
			}       
			//              close the db connection
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
			System.out.println("\nno WAVE file to save");
	}

	
	/**
	 * 
	 * Trains all WAVE files.
	 * 
	 */       

	public static void totTrain(){

		aoFiles = new File(samplesFolder).listFiles();


		try{

			//Training cluster needs to be deleted
			File cluster = new File(cName);

			//delete old training cluster
			if(cluster.exists()){
				if(!cluster.delete())
					System.err.println("Cluster not deleted");
			}

			db = new File(database);
			db.createNewFile();
			newDB();                        

			soDB = new SpeakersIdentDb(database);

			//open text file and populate data structure
			soDB.connect();
			soDB.query();

			String strFileName = "";
			String pstrFileName = "";

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
			System.err.println("Error creating file");
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


	/**
	 * 
	 * Reset the entire database. Deletes all wav files, all txt files, cluster, 
	 * and the file with sent IDs.
	 * 
	 */       



	public static void reset(){
		File[] aFiles = new File(samplesFolder).listFiles();
		File[] bFiles = new File(dbFolder).listFiles();
		for(int i=0;i<aFiles.length;i++){
			if(!aFiles[i].delete())
				System.out.println(aFiles[i].getName()+" not deleted.");
		}
		for(int i=0;i<bFiles.length;i++){
			if(!bFiles[i].delete())
				System.out.println(bFiles[i].getName()+" not deleted.");
		}
		File clear = new File(database);
		if(clear.exists()){
			if(!clear.delete())
				System.out.println(database+" not deleted.");
		}
		clear= new File(cName);
		if(clear.exists()){
			if(!clear.delete())
				System.out.println("Cluster not deleted.");
		}
		clear= new File(sent);
		if(clear.exists()){
			if(!clear.delete())
				System.out.println(sent+" not deleted.");
		}
	}


	/**
	 * 
	 * Delete one ID from the database. Deletes txt entry file, and all WAVE files
	 * 
	 */              


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
				idCheck = soDB.getIDByFilename(aoFiles[i].getName());
				String path=aoFiles[i].getPath();
				if(Integer.parseInt(id)==idCheck){
					if(path.toLowerCase().endsWith(".wav")){
						if(aoFiles[i].delete()){
							System.out.println("ID matches: "+aoFiles[i].getName()+" <- deleted");
						}
					}
				}
			}

			File delEntry = new File(dbFolder+"/"+id+".txt");
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
	
	private static void retest(){
		try{
			
			StringTokenizer token;
			String folder = "speaker/results";
			File[] boFiles = new File(dbFolder).listFiles();
			
			File result, sen;
			BufferedReader read;
			Writer write;
			String temp, id;
			
			for(int i=0;i<boFiles.length;i++){
				id = boFiles[i].getName();
				
				if (id.indexOf(".txt") < 0)
					continue;
				
				id = id.substring(0, id.indexOf(".txt"));
				System.out.println(id+"\t");
				read = new BufferedReader(new FileReader(boFiles[i]));
				temp = read.readLine();
				token = new StringTokenizer(temp,"|");
				temp = token.nextToken();
				write = new BufferedWriter(new FileWriter(boFiles[i]));
				write.append(temp);		
				write.close();
				
				totTrain();

				for(int j=1;j<4;j++){
					ident(id+"_"+(j+1)+".wav");
					sen = new File(sent);
					result = new File(folder+"/"+id+"_"+j+".txt");
					System.out.println("\tRename results "+j+": "+sen.renameTo(result));
					found(id,samplesFolder+"/"+id+"_"+(j+1)+".wav");					
				}
			}
		}
		catch(IOException e){
			e.printStackTrace();
		}
		
	}
	
	private static int entrySize(int input){
		Hashtable counts = new Hashtable();
		try{

			String strFileName;

			soDB = new SpeakersIdentDb(database);

			//open text file and populate data structure
			soDB.connect();
			soDB.query();

			aoFiles = new File(samplesFolder).listFiles();
			for(int i = 0; i < aoFiles.length; i++)
			{
				strFileName = aoFiles[i].getPath();
				if(aoFiles[i].isFile() && strFileName.toLowerCase().endsWith(".wav"))
				{
					soDB.getIDByFilename(strFileName);
				}
			}


			counts = soDB.getNumberPerID();

		}catch(MARFException e){
			System.err.println("change DB error");
		}finally{
			if(counts.containsKey(input))
				return (Integer)counts.get(input);
			else
				return 0;
		}
	}

	
	
	/**
	 * Creates or edits the text file for a specific speaker. This text file
	 * holds a list of the training samples for the speaker. If the speaker is new,
	 * a new text file is created. Otherwise, this wave file is added to the list.
	 *
	 * @param identity (of the speaker)
	 * @param filename (of the sample)
	 */

	private static final void entryTrain(String identity, String filename)
	{
//		create File object to control this identity's database entry
		File eT = new File(dbFolder+"/" + identity + ".txt");
		Writer writing;
		try{
//			if the entry already exists, add to the end of it
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
//				System.out.println("create new entry...");
				eT.createNewFile();
				writing = new BufferedWriter(new FileWriter(eT));
				writing.write(identity+","+filename);
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
	private static final void newDB()
	{

		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".txt");
			}
		};

		File[] dbFiles = new File(dbFolder).listFiles(filter);

		try {

//			create new object for the database text file and writer to write to it
			File newDB = new File(database);
			BufferedWriter output = new BufferedWriter(new FileWriter(newDB));

			BufferedReader input;
			String line = null;

//			use the reader to read each file in the directory
			for(int i=0; i<dbFiles.length;i++)
			{
				input = new BufferedReader(new FileReader(dbFiles[i]));

//				add the first line in each file to the String array
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
	 * Indetifies a speaker using MARF given a filename. Returns list of all IDs in order
	 * in a text file.
	 */
	private static final void ident(String name)
	{
		try{

			String pstrFilename = samplesFolder+"/"+name;
			File tFile = new File(pstrFilename);
//			System.out.println("\tIdenting: "+tFile.getPath());
			MARF.setSampleFile(pstrFilename);
			MARF.recognize();
	
			int iIdentifiedID = MARF.queryResultID();
			
			Result[] results =  MARF.getResultSet().getResultSetSorted();

			int[] ids = new int[results.length];
			double[] outcomes = new double[results.length]; 
			double first = results[0].getOutcome();
			
			// create writer and file to communicate filename, first ID, and second best ID to PMA
			BufferedWriter writing, nS;
			File toDATABASE = new File(sent);
			toDATABASE.createNewFile();
			writing = new BufferedWriter(new FileWriter(toDATABASE));
			
			for(int i=0;i<results.length;i++){
				ids[i] = results[i].getID();
				outcomes[i] = results[i].getOutcome();
				writing.write(ids[i]+"\t"+first/outcomes[i]+"\t"+entrySize(ids[i]));
				writing.newLine();
			}			
			writing.close();
		}
		catch(IOException e){
			System.out.println("ident can't write to file!");
		}
		catch(MARFException e){
//			System.err.println(e.getMessage());
//			e.printStackTrace(System.err);
			System.out.println("Sorry. I hiccupped. Please record again.");
			System.exit(0);
		}
	}

	
	/**
	 * Updates training set with a new sample from a given file.
	 * 
	 */
	private static final void train(String pstrFilename)throws MARFException
	{
		MARF.setSampleFile(pstrFilename);
		File sample = new File(pstrFilename);

//		uses speakerIdentDB to associate the training file to speaker
		int iID = soDB.getIDByFilename(pstrFilename);

		if(iID == -1)
		{
			System.out.println("No speaker found for \"" + pstrFilename + "\" for training.");
			sample.delete();
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
	private static final void setConfig() throws MARFException
	{
		MARF.setPreprocessingMethod(MARF.DUMMY);
		MARF.setFeatureExtractionMethod(MARF.FFT);
		MARF.setClassificationMethod(MARF.EUCLIDEAN_DISTANCE);
		MARF.setDumpSpectrogram(false);
		MARF.setSampleFormat(MARF.WAV);
	}
	
	private static final void test(){
		try{
			File dest = new File("speaker/ids/"+entrySize(9972)+".txt");
			dest.createNewFile();
			File orig = new File(sent);
			System.out.println(orig.renameTo(dest));
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}

}

