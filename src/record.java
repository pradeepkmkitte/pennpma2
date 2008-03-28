import java.io.*;
import javax.sound.sampled.*;
import java.util.*;

public class record{

  static AudioFormat format = new AudioFormat(8000.0F, 16, 1, true, true);
  static TargetDataLine line;
  static Scanner sc = new Scanner(System.in);
  int sampleNum;
  String fileName;
  String samplesFolder="speaker/training-samples";
   
  record(int sN){
	  	sampleNum= sN+1; //needs to be incremented to name new sample
	  	fileName = sampleNum + ".wav";
	    
//	    Recording starts by starting a 'capture thread', 
//	    once the thread is opened, it returns
	    findMic();

	    System.out.print("\tpress 'Enter' to stop recording");
	    String end = sc.nextLine();
	    
//	    When a line is read, stop and close the targetLine to stop writing
//	    to the file
	    line.stop();
	    line.close();
  }
  
  public String getName(){
	  return fileName;
  }

/*
 * This method finds a microphone (if none is found, the program ends), and then starts a 
 * thread to save into a destination file
 */
  private void findMic(){
    try{
  
      DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
      line = (TargetDataLine) AudioSystem.getLine(info);

      new SaveThread().start();

    }
//    LineUnavailableException thrown by AudioSystem.getLine
    catch (Exception e) {
      e.printStackTrace();
      System.exit(0);
    }
  }

/*
 * This is an inner Thread class that saves the input into a designated file
 */

	class SaveThread extends Thread{
	  public void run(){
	
//		set the file destination
	    File newSample = new File(samplesFolder+"/" + fileName);
	
	    try{
//	      open and start a data line to collect data with the designated format
	      line.open(format);
	      line.start();
	      
//	      write to the file using the line input with correct type and destination
	      AudioSystem.write( new AudioInputStream(line), AudioFileFormat.Type.WAVE, newSample);
	    }
//	    Exceptions thrown by AudioSystem.write and line.open
	    catch (Exception e){
	      e.printStackTrace();
	    }
	  }
	  
	}

}