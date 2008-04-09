import java.io.*;
import java.util.Hashtable;

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

	static record sample;

	static String database = "speaker/newDB.txt";
	static String samplesFolder = "speaker/training-samples";
	static String dbFolder = "speaker/databases";
	static String cName = "marf.Storage.TrainingSet.100.301.512.gzbin";
	static String sent = "speaker/speakerids.txt";
	static String temp = samplesFolder+"/temp.wav";


	public static void main(String[ ] argv){

		try{
			setConfig();
		}
		catch(MARFException e){
			System.err.println("Cannot set configuration");
		}

		if(argv[0].compareToIgnoreCase("identify")==0){
			System.out.print("\nbegin...");
			begin();
		}
		else if(argv[0].compareToIgnoreCase("save")==0){
			IDFound(argv[1]);
		}
		else if(argv[0].compareToIgnoreCase("erase")==0){
			delete(argv[1]);
		}
		else if(argv[0].compareToIgnoreCase("train")==0){
			System.out.println("\ntraining voices...");
			totTrain();
		}
		else if(argv[0].compareToIgnoreCase("reset")==0){
			reset();
		}
//		else if(argv[0].compareToIgnoreCase("stop")==0){
//		stopRec();
//		}
		else{
			System.err.println("No function selected.");
		}

	}

	/**
	 * 
	 * Starts record process and IDs sample. Returns top 2 IDs to the text file 'sent'.
	 * Saves sample as 'temp.wav'. User must press 'Enter' in console to end this function.
	 * 
	 */       

	public static void begin(){

		//Database text file
		db = new File(database);


		try
		{      
			File tempsample = new File(temp);    
			if(tempsample.exists()){
				if((new File(temp)).delete()){
					System.out.print("(Old temp sample cleared.)");
				}

			}

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

//	public static void stopRec(){
//	sample=new record(2);
//	try{                       
//	// IDENTIFY
//	ident(temp);

//	}
//	catch (MARFException e){
//	System.err.println(e.getMessage());
//	e.printStackTrace(System.err);
//	}

//	}


	public static void IDFound(String id){
		File tempsample = new File(temp);
		if(tempsample.exists()){
			try{

				int identity = Integer.parseInt(id);
				int number = entrySize(identity)+1;


				File old = new File(temp);
//				System.out.println(old.getPath());
File change = new File(samplesFolder+"/"+identity+"_"+number+".wav");
//System.out.println(change.getPath());

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

entryTrain(id,name);

newDB();
soDB = new SpeakersIdentDb(database);
soDB.connect();
soDB.query();

train(samplesFolder+"/"+name);


File guess = new File(sent);
guess.delete();

System.out.println("\n"+name+" saved.");
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
	 * Trains all WAVE files. Initializes system.
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


	private static Hashtable entrySize(){
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
					soDB.getIDByFilename(strFileName, true);
				}
			}


			counts = soDB.getNumberPerID();

		}catch(MARFException e){
			System.err.println("change DB error");
		}finally{
			return counts;
		}
	}

	private static int entrySize(int input){
		Hashtable output = entrySize();
		if(output.size()>0)
			return (Integer)output.get(input);
		else
			return 0;

	}


	/**
	 * Creates or edits the text file for a specific speaker. This text file
	 * holds a list of the training samples for the speaker. If the speaker is new,
	 * a new file is created. Otherwise, this file is added to the list.
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
	 * Indetifies a speaker using MARF given a filename. Returns filename, first guess
	 * and second best guess in a text file.
	 */
	private static final void ident(String name) throws MARFException
	{
		String pstrFilename = samplesFolder+"/"+name;
		File tFile = new File(pstrFilename);

		MARF.setSampleFile(pstrFilename);
		MARF.recognize();

		int iIdentifiedID = 0, iSecondClosestID = 0;
		int numResults = MARF.getResultSet().size();

		if ( numResults == 0 )
			System.out.println("No results");

		else {

			// First guess
			iIdentifiedID = MARF.queryResultID();
		}

		if ( numResults > 1 ) {

			// Second best
			iSecondClosestID = MARF.getResultSet().getSecondClosestID();
		}

		try{

			// create writer and file to communicate filename, first ID, and second best ID to PMA
			BufferedWriter writing, nS;
			File toDATABASE = new File(sent);
			toDATABASE.createNewFile();

			writing = new BufferedWriter(new FileWriter(toDATABASE));

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
	private static final void train(String pstrFilename)throws MARFException
	{
		MARF.setSampleFile(pstrFilename);
		File sample = new File(pstrFilename);

//		uses speakerIdentDB to associate the training file to speaker
		int iID = soDB.getIDByFilename(pstrFilename, true);

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

	///**
	//* Updates training set with a new sample from a given file.
	//*
	//*/
//	public static final boolean train(String pstrFilename, int[] index)throws MARFException
//	{
//	MARF.setSampleFile(pstrFilename);
//	File sample = new File(pstrFilename);

////	uses speakerIdentDB to associate the training file to speaker
//	int iID = soDB.getIDByFilename(pstrFilename, true);
	//
//	if(iID == -1)
//	{
//	System.out.println("No speaker found for \"" + pstrFilename + "\" for training.\nEnter new ID:");
////	sample.delete();
//	String nID = sc.nextLine();
//	System.out.println("ID "+nID+" sample number: ");
//	String num = sc.nextLine();

//	if(num.equals("1")){
//	File temp = new File(dbFolder+"/"+nID+".txt");
//	temp.delete();
//	}
//	entryTrain(nID, pstrFilename);


//	}
//	else
//	{
//	System.out.println(pstrFilename + "\tID:"+iID);
//	String nFilename =samplesFolder+"/"+change(pstrFilename,iID,index[iID]+1);
//	boolean unchanged = nFilename.equalsIgnoreCase(pstrFilename);
//	if(!unchanged)
//	System.out.println(nFilename+" created.");
////	MARF.setSampleFile(nFilename);
////	MARF.setCurrentSubject(iID);
////	MARF.train();
//	return unchanged;
//	}

//	return true;
//	}       


}

