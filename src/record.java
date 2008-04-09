import java.io.*;
import javax.sound.sampled.*;
import java.util.*;

public class record{

	static AudioFormat format = new AudioFormat(8000.0F, 16, 1, true, true);
	static TargetDataLine line;
	static Scanner sc = new Scanner(System.in);


	String fileName;
	String samplesFolder="speaker/training-samples";

	record(){

		fileName = "temp.wav";

		try{
			//    Recording starts by starting a 'capture thread', 
			//    once the thread is opened, it returns
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
			line = (TargetDataLine) AudioSystem.getLine(info);
		}
		catch (Exception e){
			System.out.println("error creating record");
		}

		new SaveThread().start();

		System.out.print("\tpress 'Enter' to stop recording");
		String end = sc.nextLine();

		stopRecord();
	}

	public void stopRecord(){
		line.stop();
		line.close();
	}

	public String getName(){
		return fileName;
	}


	/*
	 * This is an inner Thread class that saves the input into a designated file
	 */

	class SaveThread extends Thread{
		public void run(){

//			set the file destination
			File newSample = new File(samplesFolder+"/" + fileName);

			try{
//				open and start a data line to collect data with the designated format
				line.open(format);
				line.start();

//				write to the file using the line input with correct type and destination
				AudioSystem.write( new AudioInputStream(line), AudioFileFormat.Type.WAVE, newSample);
			}
//			Exceptions thrown by AudioSystem.write and line.open
			catch (Exception e){
				e.printStackTrace();
			}
		}

	}

}
