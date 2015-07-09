package com.me3tweaks.modmanager.modupdater;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.apache.commons.io.FileUtils;

import com.me3tweaks.modmanager.Mod;
import com.me3tweaks.modmanager.ModManager;
import com.me3tweaks.modmanager.ModManagerWindow;
import com.me3tweaks.modmanager.ResourceUtils;
import com.me3tweaks.modmanager.modupdater.ModUpdateWindow.HTTPDownloadUtil;

@SuppressWarnings("serial")
public class AllModsUpdateWindow extends JDialog {
	boolean error = false;
	JButton cancelButton;
	JLabel statusLabel;
	private JFrame callingWindow;
	private ArrayList<Mod> updateableMods;
	private ArrayList<UpdatePackage> upackages;
	private AllModsDownloadTask amdt;

	public AllModsUpdateWindow(JFrame callingWindow, ArrayList<Mod> updateableMods) {
		this.updateableMods = updateableMods;
		this.callingWindow = callingWindow;

		setupWindow();
		amdt = new AllModsDownloadTask();
		amdt.execute();
		setVisible(true);
	}
	
	public AllModsDownloadTask getAmdt() {
		return amdt;
	}

	public void setAmdt(AllModsDownloadTask amdt) {
		this.amdt = amdt;
	}

	private void setupWindow() {
		this.setTitle("Mod Updater");
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.setPreferredSize(new Dimension(300, 90));
		this.setResizable(false);
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/resource/icon32.png")));
		this.pack();
		this.setLocationRelativeTo(callingWindow);
		
		JPanel panel = new JPanel(new BorderLayout());

		statusLabel = new JLabel("0/0 files downloaded");

		panel.add(new JLabel("Obtaining latest mod information from ME3Tweaks..."), BorderLayout.NORTH);
		panel.add(statusLabel, BorderLayout.SOUTH);
		// updatePanel.add(actionPanel);
		// updatePanel.add(sizeLabel);

		// aboutPanel.add(loggingMode, BorderLayout.SOUTH);
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		this.getContentPane().add(panel);
	}

	void setStatusText(String text) {
		statusLabel.setText(text);
	}
	
	
	/**
	 * Execute file download in a background thread and update the progress.
	 * 
	 * @author www.codejava.net
	 * 
	 */
	class AllModsDownloadTask extends SwingWorker<Void, Object> {
		private int numModstoUpdate;
		private int numProcessed;		
		public void pause() {
		    try {
				this.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public synchronized void resume() {
		    this.notify();
		    ModManager.debugLogger.writeMessage("Sent resume wakeup notification");
		}
		
		/**
		 * Executed in background thread
		 */
		@Override
		protected Void doInBackground() throws Exception {
			// Iterate through files to download and put them in the update
			// folder
			upackages = new ArrayList<UpdatePackage>();
			for (Mod mod : updateableMods){
				UpdatePackage upackage = ModXMLTools.validateLatestAgainstServer(mod);
				if (upackage != null) {
					//update available
					upackages.add(upackage);
				} else {
					ModManager.debugLogger.writeMessage(mod.getModName()+" is up to date/not eligible");
				}
			}
			
			if (upackages.size() <= 0) {
				return null;
			}
			
			for (UpdatePackage upackage : upackages){
				ModManager.debugLogger.writeMessage("Processing: " + upackage.getMod().getModName());
				ModUpdateWindow muw = new ModUpdateWindow(upackage);
				muw.startAllModsUpdate(AllModsUpdateWindow.this);
				while (muw.isShowing()) {
					Thread.sleep(500);
				}
			}
			
			return null;
		}

		@Override
		public void process(List<Object> chunks) {
			setStatusText(numProcessed + "/" + numModstoUpdate + " mods updated");
		}

		/**
		 * Executed in Swing's event dispatching thread
		 */
		@Override
		protected void done() {
			dispose();

			if (upackages.size() <= 0) {
				JOptionPane.showMessageDialog(callingWindow, "All updatable mods are up to date.");
				return;
			}
			JOptionPane.showMessageDialog(callingWindow, upackages.size()+" mod(s) have been successfully updated.\nMod Manager will now reload mods.");
			callingWindow.dispose();
			new ModManagerWindow(false);
		}
	}


	public void continueUpdating() {
		ModManager.debugLogger.writeMessage("Resuming all-mods update thread");
		amdt.resume();
		
	}
}
