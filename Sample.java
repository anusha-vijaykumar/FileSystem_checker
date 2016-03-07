package Lesson1;  
  
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;  
import java.io.FileOutputStream;
import java.io.FileReader; 
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;  
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Iterator;
//import java.util.Iterator;  // for parsing array in json file
import java.util.Scanner;
import java.util.Vector;

import org.json.simple.JSONArray;
//import org.json.simple.JSONArray;  // for parsing array in json file
import org.json.simple.JSONObject;  
import org.json.simple.parser.JSONParser;  
import org.json.simple.parser.ParseException;  

public class Sample {  
	// change to path of file system to be checked
	
	static String path = "C:\\Users\\anusha\\Documents\\ASSIGNMENTS\\OS\\project 1\\FS\\FS";
	static long directorylist[]= new long[100];
	static final long BLOCKSIZE = 4096;
	static int o=0;
	static long root = 0;
	long start,end;
	static int p = 0;
	static Vector<Integer> freeblocklist = new Vector<Integer>();
	public static void main(String[] args) throws java.text.ParseException, IOException, ParseException {  
		
		System.out.println("*********************FILE SYSTEM CHECKER***********************\n");
		//try(Scanner scan = new Scanner(System.in)){ // used to get file system location from the user
			//System.out.println("enter the location of the filesystem");
			//String path = scan.nextLine();
			verifydevid(path); // used to verify if devid is correct
			verifysuperblock();
			verifyfreeblocks(path);
			verifydirectory(root,root);
			writefiles();
		//}
	}
	

	
// ***************function to verify device id************************************
	public static void verifydevid(String location){
		JSONParser parser = new JSONParser();  
		try {  
			String filelocation = path + "\\fusedata.0";// get location of superblock
			//System.out.println("File location is " + " " + filelocation);
			Object obj = parser.parse(new FileReader(filelocation));  
  			JSONObject jsonObject = (JSONObject) obj;  
  			System.out.println("Details from SUPER BLOCK");
  			long creationTime = (Long) jsonObject.get("creationTime"); 
			System.out.println("CreationTime: "+creationTime);  
  
			long mounted = (Long) jsonObject.get("mounted");  
			System.out.println("Mounted: "+mounted);  
   
			long devId = (Long) jsonObject.get("devId");  
			System.out.println("DevId: "+devId); 
   
			long freeStart = (Long) jsonObject.get("freeStart");  
			System.out.println("FreeStart: "+freeStart); 
   
			long freeEnd = (Long) jsonObject.get("freeEnd");  
			System.out.println("FreeEnd: "+freeEnd); 
			
   			long root = (Long) jsonObject.get("root");  
			System.out.println("root: "+root); 
   
			long maxBlocks = (Long) jsonObject.get("maxBlocks");  
			System.out.println("maxBlocks: "+maxBlocks+"\n\n"); 
			
			if (devId != 20){ // to print appropriate statements to the user
				System.out.println("The devId of the file system is wrong\n\n");
				System.exit(0);
				}
		} catch (FileNotFoundException e) {  
			e.printStackTrace();  
		} catch (IOException e) {  
			e.printStackTrace();  
		} catch (ParseException e) {  
			e.printStackTrace();  
		}
} 
	

//***************Function to verify files*****************************

	private static void verifyfile(Long location, String name) throws java.text.ParseException, FileNotFoundException, IOException, ParseException {
		verifytime(location);// call function to verify files
		JSONParser parser = new JSONParser();  
				String filelocation = path + "\\fusedata."+location;
				System.out.println("Verifying file fusedata."+location+". . . .\n\n");
		File f = new File(filelocation);
		if(!f.exists()){//check if file exists
			System.out.println("file at location "+ filelocation + "does not exist\n\n");
			System.exit(0);
		}
		 // if file exists remove it from freeblocklist[]
		int free = freeblocklist.indexOf((int)(long)(location));
		freeblocklist.remove(free);
		// read the file and extract values of indirect and size
		Object obj = parser.parse(new FileReader(filelocation));  
		JSONObject jsonObject = (JSONObject) obj;
		long indirect = (Long) jsonObject.get("indirect");
		long size = (Long) jsonObject.get("size");
		if(indirect == 0){
			if(size > BLOCKSIZE){
				System.out.println("Error: Size is greater than BLOCKSIZE and Indirect = 0 in file fusedata."+ location+" \n\n");
			}
		}else {
				if(size <= BLOCKSIZE){
					System.out.println("Error: Size is lesser than BLOCKSIZE and Indirect = 1 in file fusedata."+ location+" \n\n");
					long address_pt = (Long) jsonObject.get("location");
					freeblocklist.remove(freeblocklist.indexOf((int)(long)address_pt));
					jsonObject.remove("indirect");
					jsonObject.put("indirect",(Object)(long) 0);
					int[] loc = getloc(address_pt,size);
					if(loc.length >= 0){
						jsonObject.remove("location");
						jsonObject.put("location",(Object)(long)loc[0]);
						freeblocklist.addElement((int)(long)(address_pt));
						try {
							String jsonstring = jsonObject.toString();
							// writing correct data into the file
							File file = new File(filelocation);
							BufferedWriter writer = new BufferedWriter(new FileWriter(file));
							writer.write(jsonstring);
							writer.flush();
							writer.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					}else{
						long address = (Long) jsonObject.get("location");
						verifyaddress(address,size);
					}
				}
}
		

//***************** function to get location as indicated in the file where indirect=1********************	
	
	private static int[] getloc(long address_pt,long size) throws IOException {
		String filelocation = path + "\\fusedata."+address_pt;
		File f = new File(filelocation);
		BufferedReader read1 = new BufferedReader(new FileReader(filelocation));
		String line=read1.readLine();
		// split the data to extract the location from the array
		String[] splitdata = line.split("[,\\[\\]]");
		int[] ret;
		if(size/ BLOCKSIZE == 0){
			// means only one file enough to store data
			ret = new int[1];
			ret[0]=Integer.parseInt(splitdata[1]);
			freeblocklist.remove(freeblocklist.indexOf(ret[0]));
			return ret;
		}
		else{
			int inneed = (int) (size/BLOCKSIZE);
			ret = new int[splitdata.length-inneed+1];
			for(int t=inneed;t<splitdata.length;t++)
				ret[t]=Integer.parseInt(splitdata[t]);
			return ret;	
		}
	}


//*************function to verify address in case of indirect=1*************
	
	private static void verifyaddress(long address, long size) throws IOException {
			String filelocation = path + "\\fusedata."+address;
			File f = new File(filelocation);
			if(!f.exists()){
				System.out.println("file does not exist"+ filelocation+" \n\n");
				System.exit(0);
			}
			// remove file that exists from frrblocklist[]
			int free = freeblocklist.indexOf((int)(long)(address));
			freeblocklist.remove(free);
			//System.out.println("File location is " + " " + filelocation);
			BufferedReader read1 = new BufferedReader(new FileReader(filelocation));
			String line=read1.readLine();
			// to check if the file contains an array
			if(!(line.indexOf("[")>=0 && line.indexOf("]") >=0)){
				System.out.println("  'indirect' location not an array\n\n");
			}
			else{
				read1.close();
				int ifiles = 0;
				BufferedReader read11 = new BufferedReader(new FileReader(filelocation));
				while((line = read11.readLine())!=null){
					String[] splitdata = line.split("[,\\[\\]]");
					for (int i=1;i<splitdata.length;i++){
						if(!(splitdata[i] == null) || !(splitdata[i].length() == 0)){
							free = freeblocklist.indexOf(Integer.parseInt(splitdata[i]));
							freeblocklist.remove(free);
							ifiles++;
						}
					}
				}
				int inneed = (int) (size/BLOCKSIZE);
				if(inneed != ifiles)
					System.out.println(" required number of indirect files is "+(inneed+1)+" but current indirect files count is "+ifiles+ "/n/n");
			}
		}
		

//**************** function to verify directory********************

	private static void verifydirectory(Long location, long parent) throws java.text.ParseException, FileNotFoundException, IOException, ParseException {
		JSONParser parser = new JSONParser();  
		String filelocation = path + "\\fusedata."+location;
		File f = new File(filelocation);
		if(!f.exists()){
			System.out.println("file does not exist"+ filelocation+ "\n\n");
			System.exit(0);
		}
		// if file exists remove from freeblocklist[]
		int free = freeblocklist.indexOf((int)(long)(location));
		freeblocklist.remove(free);
		
		Object obj = parser.parse(new FileReader(filelocation));  
		JSONObject jsonObject = (JSONObject) obj;  
		// verify time for all directories
		verifytime(location);
		int dot=0,dotdot=0;
		JSONArray listOfStates = (JSONArray) jsonObject.get("filename_to_inode_dict"); 
		 Iterator<?>  iterator = listOfStates.iterator(); 
		 while (iterator.hasNext()) {  
			 JSONObject innerobj = (JSONObject) iterator.next();
			 String type = (String) innerobj.get("type");
			// System.out.println((String)innerobj.get("name"));
			  if(type.compareTo("d")==0){
				  String name = (String) innerobj.get("name");
				  long locate = (Long) innerobj.get("location");
				  if(name.compareTo(".")==0){
					  dot=1;
					  // verifying location of . and ..
					  if(locate != location){
						  System.out.println(" location of '.' in fusedata."+location+" is incorrect\n\n");
						// if wrong then write correct data into location of current directory
						  System.out.println("Correcting '.' entry in the file. . . . ");
						  innerobj.remove("location");
						  innerobj.put("location",(Object)(long) location);
							String jsonstring = jsonObject.toString();
									// writing correct data into the file
							File file = new File(filelocation);
							BufferedWriter writer = new BufferedWriter(new FileWriter(file));
							writer.write(jsonstring);
							writer.flush();
							System.out.println("Entry for '.' is successfully corrected in fusedata."+location+" \n\n");
							writer.close();
					  }
					}
				  else if (name.compareTo("..")==0){
					  dotdot=1;
					  if(locate != parent){
						  System.out.println(" location of '..' in fusedata."+location+" is incorrect");
						// if wrong then write correct data into location of current directory
						  System.out.println("Correcting '..' entry in the file. . . . ");
						  innerobj.remove("location");
						  innerobj.put("location",(Object)(long) parent);
							String jsonstring = jsonObject.toString();
									// writing correct data into the file
							File file = new File(filelocation);
							BufferedWriter writer = new BufferedWriter(new FileWriter(file));
							writer.write(jsonstring);
							writer.flush();
							System.out.println("Entry for '..' is successfully corrected in fusedata."+location+ " \n\n");
							writer.close();
					  }
					}
				  else
					  // recursively call for other directories
					 verifydirectory(locate, location );  
			  }
			  else if(type.compareTo("f")==0)
				  verifyfile((Long) innerobj.get("location"),(String) innerobj.get("name"));
		 }
		 if(dot != 1 || dotdot != 1)
				System.out.println(" '.' and '..' entry not found in file fusedata."+location+ "\n\n");
					
}

	
	
// ****************function to verify time***********************
	
	public static void verifytime(long loc) throws java.text.ParseException{
		JSONParser parser = new JSONParser();  
		try {  
			long currenttime = System.currentTimeMillis()/1000;
			String filelocation1 = path + "\\fusedata."+loc;
			Object obj1 = parser.parse(new FileReader(filelocation1));  
			JSONObject jsonObject1 = (JSONObject) obj1;  
		// to capture atime from json file
			long atime = (Long) jsonObject1.get("atime");  
			
		// to capture ctime from json file
			long ctime = (Long) jsonObject1.get("ctime");  
			
		// to capture mtime from json file
			long mtime = (Long) jsonObject1.get("mtime");  
			System.out.println("verifying time at "+"fusedata."+loc+"  . . . .\n");
			if( currenttime < atime )
				System.out.println("atime not in past in file"+ "fusedata." + loc +" \n");
			if ( currenttime < ctime )
				System.out.println("ctime not in past in file"+ "fusedata." + loc + "\n");
			if ( currenttime < mtime )
				System.out.println("mtime not in past in file"+ "fusedata." + loc + "\n");
			
				
		}catch (FileNotFoundException e1) {  
		e1.printStackTrace();  
	} catch (IOException e1) {  
		e1.printStackTrace();  
	} catch (ParseException e1) {  
		e1.printStackTrace();  
	}  
}

	
//*****************function to verify superblock************
	
	private static void verifysuperblock()
			throws IOException, ParseException, FileNotFoundException {
		System.out.println("Verifying Super Block. . . . \n");
		JSONParser parser = new JSONParser();
		String filelocation = path+"\\fusedata.0";
		Object obj = parser.parse(new FileReader(filelocation));  
		JSONObject jsonObject = (JSONObject) obj;  
  
		long creationTime = (Long) jsonObject.get("creationTime");  
		root = (Long) jsonObject.get("root");  
		// to get number of seconds since epoch right now
		long currenttime = System.currentTimeMillis()/1000;
		// verify creation time
		if(currenttime < creationTime)
			System.out.println("creation time in fusedata0 is wrong\n\n");
	}


	
//*************function to verify free blocks in the filesystem**************

public static void verifyfreeblocks(String location) throws IOException, ParseException{
	
		String infilefreelistblock[] = new String[10000];
		String filelocation1 = path+"\\fusedata.0";
		JSONParser parser = new JSONParser();
		Object obj1 = parser.parse(new FileReader(filelocation1));  
		JSONObject jsonObject1 = (JSONObject) obj1;  
  
		long freeStart = (Long) jsonObject1.get("freeStart");
		long freeEnd = (Long) jsonObject1.get("freeEnd");
		//System.out.println(freeStart+ " " + freeEnd);
		for ( long k=freeStart;k<=freeEnd;k++){
			String filelocation = location + "\\fusedata."+k;
			//System.out.println("File location is " + " " + filelocation);
			// to capture the blocks mentioned as free in the files 1-25
			BufferedReader read1 = new BufferedReader(new FileReader(filelocation));
			String line;
			while((line = read1.readLine())!=null){
				// split function to split the contents of the array
				String[] splitdata = line.split("[,\\[\\]]");
				for (int i=1;i<splitdata.length;i++){
					if(!(splitdata[i] == null) || !(splitdata[i].length() == 0)){
						//System.out.println(splitdata[i]);
						infilefreelistblock[p]=splitdata[i];
						p++;
					}	
				}		
			}
		}

		long maxblocks = (Long) jsonObject1.get("maxBlocks");
		int mx = (int)(long)maxblocks;
		int fe = (int)(long)freeEnd;
		int j=0;
		// find all the block numbers that are not present in the files 1- 25 and add them to freeblocklist vector
      for(int i=fe+1;i<=mx-1;i++){
    	  try{
    		  int t = Integer.parseInt(infilefreelistblock[j].trim());
    	 
    		  if((j<infilefreelistblock.length) && (i==t))
                   j++;
             else{
            	 	freeblocklist.addElement(i);
             }
    		 
             } catch(NumberFormatException e){
            	 	System.out.println("error!\n\n");
             	}
      }
    
}

	  

// *************function to write correct data into files--- correct free block list**************

private static void writefiles() throws IOException {
	System.out.println(" Reconfiguring File System. . . . \n");
	int value=0;
	for(int g=0;g<freeblocklist.size();g++){
		value = (int)freeblocklist.elementAt(g)/ 400;
		String filelocation = path + "\\fusedata."+(value+1);
		//System.out.println("File location is " + " " + filelocation);
		BufferedReader read1 = new BufferedReader(new FileReader(filelocation));
		String line;
		int x=0;
		Vector <Integer> filefreelist = new Vector<Integer>();
		while((line = read1.readLine())!=null){
			line.replaceAll(" ","");
			String[] splitdata = line.split("[,\\[\\]]");
			for (int i=1;i<splitdata.length;i++){
				if((splitdata[i] != null) && (splitdata[i].length() != 0)){
					String test = splitdata[i].replaceAll(" ","");
					filefreelist.addElement(Integer.parseInt(test));
					x++;
				}	
			}		
		}
		read1.close();
		filefreelist.addElement(freeblocklist.elementAt(g));
		String towrite = filefreelist.toString();
		File file = new File(filelocation);
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write(towrite);
		writer.flush();
		writer.close();
	}	
	
	System.out.println("\nDone! Successfully Reconfigured\n");
	}

}
	
