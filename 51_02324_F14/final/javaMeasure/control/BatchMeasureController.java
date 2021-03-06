package javaMeasure.control;

import java.awt.EventQueue;
import java.sql.Timestamp;
import java.util.Date;
import java.util.ArrayList;

import javaMeasure.*;
import javaMeasure.Measurement.MeasurementType;
import javaMeasure.control.interfaces.*;
import javaMeasure.control.interfaces.IDatabaseController.DataBaseException;
import javaMeasure.view.*;
import javaMeasure.view.interfaces.*;


/**
 * @author Voermadal
 * Controller for the main GUI (BatchMeasureGui)
 * 
 */
public class BatchMeasureController implements IBatchMeasureController {
	private IBatchMeasureGui batchGUI;
	private IMainController mainController;
	private Batch activeBatch;
	private DirectoryListener directoryListener;

	/**
	 * Constructor for BatchMeasureController.
	 * creates the GUI needed to trigger events
	 * also has an active batch that is instantiated as null
	 * mainController is used to get other controllers and get the active user
	 * @param mainController
	 */
	public BatchMeasureController(IMainController mainController){
		super();
		this.activeBatch = null;
		this.mainController = mainController;
		this.batchGUI = new BatchMeasureGui(this);
		EventQueue.invokeLater(batchGUI);
		System.out.println("Active user is: " + this.mainController.getActiveUser().getUserName());
	}

	/**
	 * Shows gui.
	 * Used when other controllers wants this gui shown
	 */
	public void showGui(boolean visible){
		batchGUI.setVisibility(visible);
	}

	/**
	 * delegates responsibilty to newBatchController where a new batch can be created. (edit mode is false)
	 * 
	 */
	public void btnNewBatchPressed() {
		mainController.startNewBatchController();
		batchGUI.updateLog("New Batch window opened...");
	}

	// opens a modified (newBatchGui) interface, where the specifications for activeBatch can be edited
	/**
	 * Called from BatchMeasureGui
	 * Updates log and call mainController to open newBatchController in editMode
	 */
	public void btnEditBatchSettingsPressed(){
		if(getActiveBatch() != null){
			mainController.startNewBatchController(getActiveBatch());
			batchGUI.updateLog("Edit batch settings window opened...");
		} else {
			batchGUI.updateLog("Could not open edit batch window - No batch loaded.");
		}
	}

	// load batch method. asks for a batchname and gets that batch from database
	// TODO change JOptionPane for batchname, the dropdown list is not optimal should probably be new user Interface with fx autocomplete combobox 
	/**
	 * opens popup window in BatchMeasureGui where String is returned.
	 * cheks if String matches a batchname in database.
	 * if so the batch is 
	 */
	public void btnGetBatchPressed() {
		String batchnumber = batchGUI.getLoadBatchName(); // uses JOptionPane in GUI to get batchname
		if(batchnumber != null){			
			try {
				Batch batch = mainController.getDatabaseController().getBatch(batchnumber);
				if(batch != null){
				setActiveBatch(batch);
				} else{
					batchGUI.showPopupMessage("There is no batch with this name.", "No batch found");
				}
			} catch (DataBaseException e) {
				System.err.println(e.getMessage());
				batchGUI.updateLog("Error in receiving batches from database!");
			}
		}
	}

	// gets stroke measurement from USB DAQ by calling CConnector
	/**
	 * 
	 */
	public void btnStrokePressed() {

		Measurement[] measurement = null;
		if(activeBatch != null){
			try {
				batchGUI.updateLog("measuring stroke value...");
				//method returns an array of measurements, therefore the argument 1, and 1000 is not of importance when only 1 measurement
				// last argument is for time between measurements
				measurement = mainController.getcConnector().readMeasurements(MeasurementType.STROKE, 1, 1000);

				// sets the batch specific values for measurement
				measurement[0].setBatchID(activeBatch.getBatchID());
				measurement[0].setMeasurementType(MeasurementType.STROKE);
				measurement[0].setElementNo(activeBatch.getCurrentStrokeElement());
				measurement[0].setVerified(true); // true is default when getting measurements
				measurement[0].setMeasureValue((float) ((measurement[0].getMeasureValue()/Float.parseFloat(PropertyHelper.readFromProperty("calibrationConstant")))-0.019531));
				if(measurement != null)
					try {
						boolean measurementAdded = activeBatch.addMeasurement(measurement[0]);
						if(!measurementAdded)
						{
							batchGUI.showPopupMessage("measurements should be taken in sync", "measurements not synced");
						} else
						{
							// after measurement is added to batch, it is added to database
							mainController.getDatabaseController().addToDB(measurement[0]);
							batchGUI.updateLog("measurement added to database");
						}

					} catch (DataBaseException e) {
						System.err.println(e.getMessage());
						batchGUI.updateLog("Could not add stroke measurement to database");
					}
			} catch (ConnectionException e) {
				System.err.println(e.getMessage());
				batchGUI.updateLog("Was not able to connect to DAQ");
				batchGUI.updateLog("Check your connection to the DAQ");
			}
			// gui is updated as the last thing, but only if there is an active batch
			//			batchGUI.updateTable(activeBatch);
			updateTable();
		} else batchGUI.showPopupMessage("You have to create or load batch before making measurements", "No active batch");
	}

	public void btnLeakCurrent() {
		if(activeBatch == null){
			batchGUI.showPopupMessage("You have to create batch before reading measurements", "No batch settings loaded");
		} else{
			// popup window where the folder with dasyLab files. If cancel, path is null
			String path = batchGUI.getDasyPath();
			if(path != null){
				// creates single directoryListener and after that only changes the path in the directoryListener
				if(this.directoryListener != null){
					this.directoryListener.setPath(path);
				}
				else{
					this.directoryListener = new DirectoryListener(path, this);
					directoryListener.start();
				}
			}
		}
	}

	@Override
	public void btnApproveBatchPressed() {
		if(activeBatch != null){
			if(activeBatch.getApproved_by() == null){
				activeBatch.setApproved_by(mainController.getActiveUser().getUserName());
				activeBatch.setApproved_date(new Timestamp(new Date().getTime()));
				batchApproved(false);
				batchGUI.updateLog("batch is approved");
			} else{
				activeBatch.setApproved_by(null);
				activeBatch.setApproved_date(null);
				batchApproved(true);
				batchGUI.updateLog("batch is disapproved");
			}

			try {
				mainController.getDatabaseController().updateBatch(activeBatch);

			} catch (DataBaseException e) {
				batchGUI.updateLog("Failed to update batch in database");
				System.out.println(e.getMessage());
			}
		}

	}

	@Override
	public void btnDeleteStroke() {
		// trying to delete stroke measurement in activebatch. false is returned if failed and therefore code in if is not run
		if(activeBatch != null && activeBatch.deleteLastStrokeMeasurement()){
			try {
				// measurement is deleted from batch and now trying to delete from database
				mainController.getDatabaseController().deleteMeasurement(activeBatch.getBatchID(), activeBatch.getCurrentStrokeElement(), MeasurementType.STROKE);
				//				batchGUI.updateTable(activeBatch);
				updateTable();
				batchGUI.updateLog("last stroke measurement deleted");
			} catch (DataBaseException e) {
				System.err.println(e.getMessage());
				batchGUI.updateLog("unable to delete measurement");
			}		
		}
		else{
			// this could happen if measurements in table get out of sync if measurement is deleted or if there is no measurement to delete
			batchGUI.updateLog("Not able to delete measurement!");
		}
	}

	@Override
	public void btnDeleteLeak() {
		if(activeBatch != null && activeBatch.deleteLastLeakMeasurement()){
			try {
				mainController.getDatabaseController().deleteMeasurement(activeBatch.getBatchID(), activeBatch.getCurrentLeakElement(), MeasurementType.LEAK);
				//				batchGUI.updateTable(activeBatch);
				updateTable();
				batchGUI.updateLog("last leak measurement deleted");
			} catch (DataBaseException e) {
				System.err.println(e.getMessage());
				batchGUI.updateLog("unable to delete measurement");
			}
		}
		else{
			batchGUI.updateLog("Not able to delete measurement!");
		}
	}

	@SuppressWarnings("static-access")
	public void btnLogOutPressed() {
		if(this.directoryListener != null){
			this.directoryListener.interrupt();
			System.out.println(this.directoryListener.interrupted()); // check if directoryListener stops
		}
		mainController.logOut();
	}

	public void addLeakMeasurement(String path, String filename){

		// gets the timestamp for what value should be read in dasyLab file from config.properties
		long timestamp = Long.parseLong(PropertyHelper.readFromProperty("leakvalue"));

		System.out.println("timestamp: " + timestamp);
		System.out.println(path + "/" + filename);

		// creating leak measurement to be added to batch and saved in database		
		Measurement measurement = mainController.getDasyController().getCurrentValue(timestamp, path + "/" + filename);
		measurement.setBatchID(activeBatch.getBatchID());
		measurement.setElementNo(activeBatch.getCurrentLeakElement());

		// adds the measurement to guis JTable before saved in database	
		batchGUI.updateLog("new leak measurement: " + measurement.getMeasureValue());
		batchGUI.updateLog("read from: " + filename);

		try {
			if(!activeBatch.addMeasurement(measurement)) // if failed to add measurement to batch this is not run
			{
				batchGUI.showPopupMessage("measurements should be taken in sync", "measurements not in sync");
			} else
			{
				mainController.getDatabaseController().addToDB(measurement);
				updateTable();
				//				batchGUI.updateTable(activeBatch);
			}
		} catch (DataBaseException e) {
			System.err.println(e.getMessage());
			batchGUI.updateLog("not able to add measurement to database");
		}
	}

	public IMainController getMainController() {
		return mainController;
	}

	// sets activeBatch and updates specs and table with measurements
	public void setActiveBatch(Batch activeBatch){
		this.activeBatch = activeBatch;
		int profileId = this.activeBatch.getProfileID();
		BatchProfile bp = null;
		batchGUI.updateLog("active batch: " + this.activeBatch.getBatchString());
		try {
			batchGUI.updateLog("loading batch settings...");
			bp = mainController.getDatabaseController().getBatchProfile(profileId);
			this.batchGUI.updateSettings(bp.getProfileSettings());
		} catch (DataBaseException e) {
			batchGUI.updateLog("failed to load batch profile from database");
			System.err.println(e.getMessage());
		}
		if(activeBatch.getApproved_by() != null)
			batchApproved(false);
		else batchApproved(true);

		//		batchGUI.updateTable(this.activeBatch);


		updateTable();
		System.out.println("testing new list for JTable");
	}

	private void batchApproved(boolean approved) {

		batchGUI.btnBatchApproved(approved);
		batchGUI.enableDeleteLeak(approved);
		batchGUI.enableDeleteStroke(approved);
		batchGUI.enableEditBatchSettings(approved);
		batchGUI.enableLeakMeasurement(approved);
		batchGUI.enableStrokeMeasurement(approved);

	}

	public Batch getActiveBatch(){
		return activeBatch;
	}

	public IBatchMeasureGui getBatchMeasureGui(){
		return batchGUI;
	}

	@Override
	public void updateMeasurements(int elementNumber, boolean verified) {
		// updates verified value for both stroke and leak measurement for the chosen element
		activeBatch.updateMeasurements(elementNumber, verified);
		try{
			mainController.getDatabaseController().updateMeasurement(activeBatch.getMeasurement(elementNumber, MeasurementType.LEAK));
			mainController.getDatabaseController().updateMeasurement(activeBatch.getMeasurement(elementNumber, MeasurementType.STROKE));
		} catch(DataBaseException e){
			System.err.println(e.getMessage());
			batchGUI.updateLog("failed to update measurements");
		}	
	}

	public void updateTable(){
		if(activeBatch == null) return;

		ArrayList<Measurement[]> measurementList = activeBatch.getMeasurementsList();
		System.out.println("measurementList: " + measurementList);
		//		ArrayList<String[]> newList = new ArrayList<>();
		String[][] newList = new String[measurementList.size()][4];
		System.out.println("newList: " + newList);

		for(int i = 0; i < measurementList.size(); i++){

			try{
				System.out.println(measurementList.get(i)[0].getVerified());
				newList[i][0] = String.valueOf(measurementList.get(i)[0].getVerified());
				System.out.println(measurementList.get(i)[0].getElementNo());
				newList[i][1] = String.valueOf(measurementList.get(i)[0].getElementNo());
				System.out.println(measurementList.get(i)[0].getMeasureValue());
				newList[i][2] = String.valueOf(measurementList.get(i)[0].getMeasureValue());
//				System.out.println(measurementList.get(i)[1].getMeasureValue());
				if(measurementList.get(i)[1] != null){
				newList[i][3] = String.valueOf(measurementList.get(i)[1].getMeasureValue());
				}
			} catch (NullPointerException e){
				System.out.println(measurementList.get(i)[1].getVerified());
				newList[i][0] = String.valueOf(measurementList.get(i)[1].getVerified());
				System.out.println(measurementList.get(i)[1].getElementNo());
				newList[i][1] = String.valueOf(measurementList.get(i)[1].getElementNo());
				System.out.println(measurementList.get(i)[1].getMeasureValue());
				newList[i][3] = String.valueOf(measurementList.get(i)[1].getMeasureValue());
			}

		}



		batchGUI.updateTable(newList);





	}


}
