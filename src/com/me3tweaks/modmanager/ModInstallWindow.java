package com.me3tweaks.modmanager;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.me3tweaks.modmanager.basegamedb.BasegameHashDB;
import com.me3tweaks.modmanager.basegamedb.RepairFileInfo;
import com.me3tweaks.modmanager.objects.Mod;
import com.me3tweaks.modmanager.objects.ModJob;
import com.me3tweaks.modmanager.objects.ModType;

@SuppressWarnings("serial")
/**
 * Window that injects the files into the game/dlc.
 * 
 * @author Mgamerz
 *
 */
public class ModInstallWindow extends JDialog {
	JLabel infoLabel;
	String BioGameDir;
	final int levelCount = 7;
	JTextArea consoleArea;
	String consoleQueue[];
	String currentText;
	JProgressBar progressBar;

	ModManagerWindow callingWindow;

	public ModInstallWindow(ModManagerWindow callingWindow, ModJob[] jobs, String BioGameDir, Mod mod) {
		// callingWindow.setEnabled(false);
		this.callingWindow = callingWindow;
		this.BioGameDir = BioGameDir;
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		this.setTitle("Applying Mod");
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.setPreferredSize(new Dimension(320, 220));
		consoleQueue = new String[levelCount];

		setupWindow();

		this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/resource/icon32.png")));
		this.pack();
		this.setLocationRelativeTo(callingWindow);
		new InjectionCommander(jobs, mod).execute();
		this.setVisible(true);
	}

	private void setupWindow() {
		JPanel rootPanel = new JPanel(new BorderLayout());
		JPanel northPanel = new JPanel(new BorderLayout());
		// TODO Auto-generated method stub
		infoLabel = new JLabel("<html>Applying mod to game...<br>This may take a few minutes.</html>");
		northPanel.add(infoLabel, BorderLayout.NORTH);
		progressBar = new JProgressBar(0, 100);
		progressBar.setStringPainted(true);
		progressBar.setIndeterminate(false);

		northPanel.add(progressBar, BorderLayout.SOUTH);
		northPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		rootPanel.add(northPanel, BorderLayout.NORTH);

		consoleArea = new JTextArea();
		consoleArea.setLineWrap(true);
		consoleArea.setWrapStyleWord(true);

		consoleArea.setEditable(false);

		rootPanel.add(consoleArea, BorderLayout.CENTER);
		getContentPane().add(rootPanel);
	}

	class InjectionCommander extends SwingWorker<Boolean, String> {
		int completed = 0;
		int numjobs = 0;
		Mod mod;
		ModJob[] jobs;
		ArrayList<String> failedJobs;

		protected InjectionCommander(ModJob[] jobs, Mod mod) {
			this.mod = mod;
			numjobs = jobs.length;
			failedJobs = new ArrayList<String>();
			ModManager.debugLogger.writeMessage("Starting the InjectionCommander utility. Number of jobs to do: " + numjobs);
			this.jobs = jobs;
			ModManager.debugLogger.writeMessage("Using ME3Explorer from: " + ModManager.getME3ExplorerEXEDirectory(false));
		}

		@Override
		public Boolean doInBackground() {
			ModManager.debugLogger.writeMessage("Starting the background thread for PatchWindow");

			for (ModJob job : jobs) {
				ModManager.debugLogger.writeMessage("Starting mod job");
				boolean result = false;
				switch (job.getModType()) {
				case ModJob.DLC:
					result = processDLCJob(job);
					break;
				case ModJob.BASEGAME:
					result = processBasegameJob(job);
					break;
				case ModJob.CUSTOMDLC:
					result = processCustomDLCJob(job);
					break;
				}
				if (result) {
					completed++;
					ModManager.debugLogger.writeMessage("Successfully finished mod job");

				} else {
					ModManager.debugLogger.writeMessage("Mod job failed: " + job.getDLCFilePath());
					failedJobs.add(job.getDLCFilePath());
				}
				publish(Integer.toString(completed));
			}
			return true;
		}

		private boolean processBasegameJob(ModJob job) {
			publish("Processing basegame files...");
			File bgdir = new File(ModManager.appendSlash(BioGameDir));
			String me3dir = ModManager.appendSlash(bgdir.getParent());
			// Make backup folder if it doesn't exist
			String backupfolderpath = me3dir.toString() + "cmmbackup\\";
			File cmmbackupfolder = new File(backupfolderpath);
			cmmbackupfolder.mkdirs();
			ModManager.debugLogger.writeMessage("Basegame backup directory should have been created if it does not exist already.");
			// Prep replacement job
			BasegameHashDB bghDB = null; // don't load if not necessary.
			String[] filesToReplace = job.getFilesToReplace();
			String[] newFiles = job.getNewFiles();
			int numFilesToReplace = filesToReplace.length;
			ModManager.debugLogger.writeMessage("Number of files to replace in the basegame: " + numFilesToReplace);
			for (int i = 0; i < numFilesToReplace; i++) {
				String fileToReplace = filesToReplace[i];
				String newFile = newFiles[i];

				// Check for backup
				File basegamefile = new File(me3dir + fileToReplace);
				File backupfile = new File(backupfolderpath + fileToReplace);
				Path originalpath = Paths.get(basegamefile.toString());

				ModManager.debugLogger.writeMessage("Checking for backup file at " + backupfile);
				if (!backupfile.exists()) {
					// backup the file
					if (bghDB == null) {
						publish(ModType.BASEGAME + ": Loading repair database");
						bghDB = new BasegameHashDB(null, new File(BioGameDir).getParent(), false);
					}
					Path backuppath = Paths.get(backupfile.toString());
					backupfile.getParentFile().mkdirs();

					String relative = ResourceUtils.getRelativePath(basegamefile.getAbsolutePath(), me3dir, File.separator);
					RepairFileInfo rfi = bghDB.getFileInfo(relative);
					// validate file to backup.
					boolean justInstall = false;
					boolean installAndUpdate = false;
					if (rfi == null) {
						int reply = JOptionPane.showOptionDialog(null,
								"<html>The file:<br>" + relative + "<br>is not in the repair database. "
										+ "Installing this file may overwrite your default setup if you restore and have custom mods like texture swaps installed.<br></html>",
								"Backing Up Unverified File", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
								new String[] { "Add to DB and install", "Install file", "Cancel mod installation" }, "default");
						switch (reply) {
						case JOptionPane.CANCEL_OPTION:
							return false;
						case JOptionPane.NO_OPTION:
							justInstall = true;
							break;
						case JOptionPane.YES_OPTION:
							installAndUpdate = true;
							break;
						}
					}

					// Check filesize
					if (!justInstall && !installAndUpdate) {
						if (basegamefile.length() != rfi.filesize) {
							// MISMATCH!
							int reply = JOptionPane.showOptionDialog(null,
									"<html>The filesize of the file:<br>" + relative + "<br>does not match the one stored in the repair game database.<br>" + basegamefile.length()
											+ " bytes (installed) vs " + rfi.filesize + " bytes (database)<br><br>"
											+ "This file could be corrupted or modified since the database was created.<br>"
											+ "Backing up this file may overwrite your default setup if you use custom mods like texture swaps when you restore.<br></html>",
									"Backing Up Unverified File", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
									new String[] { "Backup and update DB", "Backup this file", "Cancel mod installation" }, "default");
							switch (reply) {
							case JOptionPane.CANCEL_OPTION:
								return false;
							case JOptionPane.NO_OPTION:
								justInstall = true;
								break;
							case JOptionPane.YES_OPTION:
								installAndUpdate = true;
								break;
							}
						}
					}

					// Check hash
					if (!justInstall && !installAndUpdate) {
						// this is outside of the previous if statement as the
						// previous one could set the restoreAnyways variable
						// again.
						try {
							if (!MD5Checksum.getMD5Checksum(basegamefile.getAbsolutePath()).equals(rfi.md5)) {
								int reply = JOptionPane.showOptionDialog(null,
										"<html>The hash of the file:<br>" + relative + "<br>does not match the one stored in the repair game database.<br>"
												+ "This file has changed since the database was created.<br>"
												+ "Backing up this file may overwrite your default setup if you use custom mods like texture swaps when restoring.<br></html>",
										"Backing Up Unverified File", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
										new String[] { "Backup and update DB", "Backup this file", "Cancel mod installation" }, "default");
								switch (reply) {
								case JOptionPane.CANCEL_OPTION:
									return false;
								case JOptionPane.NO_OPTION:
									justInstall = true;
									break;
								case JOptionPane.YES_OPTION:
									installAndUpdate = true;
									break;
								}
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							ModManager.debugLogger.writeException(e);
						}
					}

					try {
						// backup and then copy file
						Files.copy(originalpath, backuppath);
						ModManager.debugLogger.writeMessage("Backed up " + fileToReplace);
						if (installAndUpdate) {
							ArrayList<File> updateFile = new ArrayList<File>();
							updateFile.add(basegamefile);
							bghDB.updateDB(updateFile);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				// install file.
				try {
					ModManager.debugLogger.writeMessage("Installing mod file: " + newFile);
					publish(ModType.BASEGAME + ": Installing " + FilenameUtils.getName(newFile));
					Path newfilepath = Paths.get(newFile);
					Files.copy(newfilepath, originalpath, StandardCopyOption.REPLACE_EXISTING);
					ModManager.debugLogger.writeMessage("Installed mod file: " + newFile);
				} catch (IOException e) {
					ModManager.debugLogger.writeException(e);
				}
			}
			return true;
		}

		private boolean processDLCJob(ModJob job) {
			// TODO Auto-generated method stub
			// System.out.println("Processing DLCJOB");
			ArrayList<String> commandBuilder = new ArrayList<String>();
			commandBuilder.add(ModManager.appendSlash(ModManager.getME3ExplorerEXEDirectory(true)) + "ME3Explorer.exe");
			commandBuilder.add("-dlcinject");
			String sfarName = "Default.sfar";
			if (job.TESTPATCH) {
				sfarName = "Patch_001.sfar";
			}
			commandBuilder.add(ModManager.appendSlash(BioGameDir) + ModManager.appendSlash(job.getDLCFilePath()) + sfarName);
			String[] filesToReplace = job.getFilesToReplace();
			String[] newFiles = job.getNewFiles();
			ModManager.debugLogger.writeMessage("Number of files to replace: " + filesToReplace.length);

			publish("Injecting " + filesToReplace.length + " files into " + job.getDLCFilePath() + "\\" + sfarName);
			for (int i = 0; i < filesToReplace.length; i++) {
				commandBuilder.add(filesToReplace[i]);
				commandBuilder.add(newFiles[i]);
				// System.out.println("adding file to command");
			}

			// System.out.println("Building command");
			String[] command = commandBuilder.toArray(new String[commandBuilder.size()]);
			// Debug stuff
			StringBuilder sb = new StringBuilder();
			for (String arg : command) {
				sb.append(arg + " ");
			}
			ModManager.debugLogger.writeMessage("Executing injection command: " + sb.toString());
			Process p = null;
			int returncode = 1;
			try {
				ProcessBuilder pb = new ProcessBuilder(command);
				ModManager.debugLogger.writeMessage("Executing process for DLC Injection Job.");
				// p = Runtime.getRuntime().exec(command);
				p = pb.start();
				ModManager.debugLogger.writeMessage("Executed command, waiting...");
				returncode = p.waitFor();
			} catch (IOException | InterruptedException e) {
				ModManager.debugLogger.writeMessage(ExceptionUtils.getStackTrace(e));
				e.printStackTrace();
				return false;
			}

			ModManager.debugLogger.writeMessage("processDLCJob RETURN VAL: " + (p != null && returncode == 0));
			return (p != null && returncode == 0);
		}

		private boolean processCustomDLCJob(ModJob job) {
			File dlcdir = new File(ModManager.appendSlash(BioGameDir) + "DLC" + File.separator);
			
			for (int i = 0; i < job.getFilesToReplace().length; i++) {
				String fileDestination = dlcdir + job.getFilesToReplace()[i];
				String fileSource = job.getNewFiles()[i];
				// install file.
				try {
					ModManager.debugLogger.writeMessage("Processing CustomDLC Job.");
					publish(ModType.CUSTOMDLC + ": Installing " + FilenameUtils.getName(fileSource));
					Path sourcePath = Paths.get(fileSource);
					Path destPath = Paths.get(fileDestination);
					File dest = new File(fileDestination);
					dest.mkdirs();
					Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
					ModManager.debugLogger.writeMessage("Installed mod file: " + dest.getAbsolutePath());
				} catch (IOException e) {
					ModManager.debugLogger.writeException(e);
				}
			}
			return true;
		}

		@Override
		protected void process(List<String> updates) {
			// System.out.println("Restoring next DLC");
			for (String update : updates) {
				try {
					ModManager.debugLogger.writeMessage(update);
					Integer.parseInt(update); // see if we got a number. if we
												// did that means we should
												// update the bar
					if (numjobs != 0) {
						progressBar.setValue((int) (((float) completed / numjobs) * 100));
					}
				} catch (NumberFormatException e) {
					// this is not a progress update, it's a string update
					addToQueue(update);
				}
			}

		}

		@Override
		protected void done() {
			if (numjobs != completed) {
				// failed something
				StringBuilder sb = new StringBuilder();
				sb.append("Failed to process mod installation.\nSome parts of the install may have succeeded.\nTurn on debugging via Help>About and check the log file.");
				callingWindow.labelStatus.setText(" Failed to install at least 1 part of mod");
				JOptionPane.showMessageDialog(null, sb.toString(), "Error", JOptionPane.ERROR_MESSAGE);
				ModManager.debugLogger.writeMessage(mod.getModName() + " failed to fully install.");
			} else {
				// we're good
				callingWindow.labelStatus.setText(" " + mod.getModName() + " installed");
			}
			finishPatch();
			return;
		}
	}

	protected void finishPatch() {
		ModManager.debugLogger.writeMessage("Finished installing mod.");
		dispose();
	}

	public void addToQueue(String newLine) {
		for (int i = consoleQueue.length - 1; i >= 1; i--) {
			consoleQueue[i] = consoleQueue[i - 1];
		}
		consoleQueue[0] = newLine;
		updateInfo();
	}

	public String getConsoleString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < consoleQueue.length; i++) {
			sb.append((consoleQueue[i] != null) ? consoleQueue[i] : "");
			if (i < consoleQueue.length - 1) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	private void updateInfo() {
		consoleArea.setText(getConsoleString());
	}
}