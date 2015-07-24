package com.me3tweaks.modmanager.objects;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.FilenameUtils;

import com.me3tweaks.modmanager.ModManager;

/** Contains data that the DLC Injector can understand.
 * It is typically passed as a property container object. (object that contains properties)
 * @author FemShep
 *
 */
public class ModJob {
	//job types
	public static final int BASEGAME = 1;
	public static final int DLC = 0;
	public static final int CUSTOMDLC = 2;
	public boolean TESTPATCH = false; //testpatch flag for patch window
	private int modType;
	String DLCFilePath;
	private String jobName;
	ArrayList<String> sourceFolders; //CUSTOMDLC (used only for writing desc file)
	private ArrayList<String> destFolders; //CUSTOMDLC (used only for writing desc file)
	
	public ArrayList<String> newFiles;
	ArrayList<String> filesToReplace;
	
	/** Holds many parameters that are required to inject files into a DLC Sfar file.
	 * @param DLCFilePath Path to the DLC Sfar file.
	 */
	public ModJob(String DLCFilePath, String jobName){
		setModType(DLC);
		this.setJobName(jobName);
		this.DLCFilePath = DLCFilePath;
		newFiles = new ArrayList<String>();
		filesToReplace = new ArrayList<String>();
	}
	
	/** Creates a basegame modjob. It doesn't need a path since it can be derived without the need for one.
	 * @param DLCFilePath Path to the DLC Sfar file.
	 */
	public ModJob(){
		setModType(BASEGAME);
		setJobName(ModType.BASEGAME);
		newFiles = new ArrayList<String>();
		filesToReplace = new ArrayList<String>();
	}

	public String getDLCFilePath() {
		return (getModType() == BASEGAME) ? "Basegame" : DLCFilePath;
	}

	public String[] getNewFiles() {
		return newFiles.toArray(new String[newFiles.size()]);
	}

	/**
	 * Adds a matching set of files to add
	 * @param newFile Source file that will be injected
	 * @param fileToReplace File path in DLC or basegame that will be updated
	 * @return
	 */
	public boolean addFileReplace(String newFile, String fileToReplace) {
		File file = new File(newFile);
		if (!file.exists()){
			ModManager.debugLogger.writeMessage("Source file doesn't exist: "+newFile);
			return false;
		}
		if (getModType() == BASEGAME) {
			//check first char is \
			if (fileToReplace.charAt(0) != '\\'){
				fileToReplace = "\\"+fileToReplace;
			}
		} else {
			//its dlc
			if (fileToReplace.charAt(0) != '/'){
				fileToReplace = "/"+fileToReplace;
			}
		}
		
		newFiles.add(newFile);
		filesToReplace.add(fileToReplace);
		return true;
	}

	/**
	 * Gets the array of files that will be replaced
	 * @return
	 */
	public String[] getFilesToReplace() {
		return filesToReplace.toArray(new String[filesToReplace.size()]);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((DLCFilePath == null) ? 0 : DLCFilePath.hashCode());
		result = prime * result + getModType();
		return result;
	}	
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ModJob other = (ModJob) obj;
		if (DLCFilePath == null) {
			if (other.DLCFilePath != null)
				return false;
		} else if (!DLCFilePath.equals(other.DLCFilePath))
			return false;
		if (getModType() != other.getModType())
			return false;
		return true;
	}

	/**
	 * Returns if this job has a PCConsoleTOC in it already
	 * @return
	 */
	public boolean hasTOC() {
		for (String newFile : newFiles) {
			if (FilenameUtils.getName(newFile).equals("PCConsoleTOC.bin")) {
				return true;
			}
		}
		return false;
	}

	public int getModType() {
		return modType;
	}

	public void setModType(int modType) {
		this.modType = modType;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public ArrayList<String> getDestFolders() {
		return destFolders;
	}

	public void setDestFolders(ArrayList<String> destFolders) {
		this.destFolders = destFolders;
	}
}