package javaMeasure.control;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;

import javaMeasure.Batch;
import javaMeasure.BatchProfile;
import javaMeasure.BatchSetting;
import javaMeasure.PropertyHelper;
import javaMeasure.Validator;
import javaMeasure.control.interfaces.*;
import javaMeasure.control.interfaces.IDatabaseController.DataBaseException;
import javaMeasure.view.interfaces.*;
import javaMeasure.view.*;

/**
 * @author Runi
 *
 */
public class NewBatchController implements INewBatchController {
	private IMainController mainController;
	private INewBatchGui newBatchGui;
	private BatchProfile batchProfile;
	private Batch activeBatch;

	public NewBatchController(IMainController mainController){

		this.mainController = mainController;
		this.newBatchGui = new NewBatchGui(this, false);
		try {
			this.newBatchGui.setSettings(getDefaultBatchProfile());
		} catch (DataBaseException e) {
			System.err.println("Database error when trying to retrieve default batch profile");
			System.err.println(e.getMessage());
		}
		this.newBatchGui.setVisibility(true);
	}
	//	En overload af construkteren, der bliver brugt, når vi åbner NewBatchGui i editmode
	public NewBatchController(IMainController mainController, Batch activeBatch){
		this.mainController = mainController;
		this.newBatchGui = new NewBatchGui(this, true);
		try {
			this.batchProfile = mainController.getDatabaseController().getBatchProfile(activeBatch.getProfileID());
			this.newBatchGui.setSettings(batchProfile);
		} catch (DataBaseException e) {
			System.err.println("Database error when trying to retrieve active batch profile");
			System.err.println(e);
		}
		this.activeBatch = activeBatch;
		this.newBatchGui.setbatchName(this.activeBatch.getBatchString());
		this.newBatchGui.setVisibility(true);
	}


	public IMainController getMainController() {
		return mainController;
	}

	/**
	 * call to this method should handle null pointer exceptions in case. key, value, or profile with specific key does not exist
	 * should probably be a private method. Not sure if it is needed elsewhere - Rúni
	 */
	@Override
	public BatchProfile getDefaultBatchProfile() throws DataBaseException {
		return getBatchProfile(PropertyHelper.readFromProperty("defaultProfile"));
	}

	/**
	 * is currently not being used, but should be in the future - Rúni
	 */
	public void setDefaultBatchProfile(String batchProfilename) {
		PropertyHelper.writeToProperty("defaultProfile", batchProfilename);
	}

	@Override
	public BatchProfile getBatchProfile(String profileName) throws DataBaseException {
		return mainController.getDatabaseController().getBatchProfile(profileName);
	}
	@Override
	public ArrayList<String> getSavedBatchProfiles(){
		try{
			return mainController.getDatabaseController().getSavedBatchProfilesNames();
		} catch (DataBaseException dbe){
			System.out.println("failed to get batch profilenames in newBatchController: " + dbe.getMessage());
			return null;
		}
	}

	/**
	 * saves batch settings with chosen name.
	 * notice that the name can not be under 2 characters. 
	 * the null check appears when the user presses cancel instead, then we just have to make sure that code is not run.
	 */
	@Override
	public void saveBatchSettingsPressed(String profileName, ArrayList<String> profileSettings) {
		
		int indexOfError = validateProfileFloats(profileSettings);
		// -1 is returned if no errors are found
		if(indexOfError != -1){
			newBatchGui.showPopupMessage("An error was found in " + PropertyHelper.readFromProperty("TextBoxNames", "FloatIndexName" + indexOfError) + ".\n This should only contain the values: 0-9 and decimals should be seperated by '.'", "Input validation failed");
			return; // is returned so code below is not run
		}
		
		if(profileName == null){System.out.println("canceled profile saving");}
		else if(profileName.length() < 2){
			this.newBatchGui.showPopupMessage("Name should be at least 2 characters", "Invalid name"); //TODO remove hardcoded 2, should be regular expression input verification - Rúni 
		}
		else if(profileName != null){
			ArrayList<BatchSetting> profile = new ArrayList<>();
			BatchProfile bp = null;
			try {
				bp = mainController.getDatabaseController().getBatchProfile(profileName);
			} catch (DataBaseException e1) {
			}
			if(bp == null){
				for(int i = 0; i < profileSettings.size(); i++){
					profile.add(new BatchSetting(i, null, null, profileSettings.get(i)));
				}
				try {
					mainController.getDatabaseController().saveBatchProfile(new BatchProfile(profileName, profile));
				} catch (DataBaseException e) {
					// TODO should be more specific. profileAlreadyExistsException
					System.err.println("failed to save batchProfile in database - newBatchController");
				}
			}
		}
	}

	//	Metode til at gemme de opdaterede indstillinger til Batchet. 

	@Override
	public void saveEditedBatchSettingsPressed(ArrayList<String> profileSettings) throws DataBaseException {
		
		int indexOfError = validateProfileFloats(profileSettings);
		// -1 is returned if no errors are found
		if(indexOfError != -1){
			newBatchGui.showPopupMessage("An error was found in " + PropertyHelper.readFromProperty("TextBoxNames", "FloatIndexName" + indexOfError) + ".\n This should only contain the values: 0-9 and decimals should be seperated by '.'", "Input validation failed");
			return; // is returned so code below is not run
		}
		
		ArrayList<BatchSetting> settings = new ArrayList<>();

		//Reading settings from GUI textboxes
		for(int i = 0; i < profileSettings.size(); i++){
			settings.add(new BatchSetting(batchProfile.getProfileSettings().get(i).getId(), null, null, profileSettings.get(i)));
		}

		try {
			//Updates batch and sets the active batch in batchMeasureController;

			mainController.getDatabaseController().updateBatchSettings(settings, mainController.getBatchMeasureController().getActiveBatch().getProfileID());
			mainController.getBatchMeasureController().setActiveBatch(activeBatch);
		} catch (DataBaseException e) {
			e.printStackTrace();
		}
		//Shift view to BatchMeasure view
		newBatchGui.setVisibility(false);
		mainController.getBatchMeasureController().showGui(true);


	}

	/* (non-Javadoc)
	 * @see javaMeasure.control.interfaces.INewBatchController#createBatchpressed(java.lang.String, java.util.ArrayList)
	 */
	@Override
	public void createBatchpressed(String batchString, ArrayList<String> profileSettings) {
		
		boolean verification = false;

		try {
			verification = isBatchInDB(batchString);
		} catch (DataBaseException e1) {
			e1.printStackTrace();
		}
		if(batchString.equals("")){
			newBatchGui.showPopupMessage("Batch name has to be chosen", "No batch name");
		}
		else if(verification){
			newBatchGui.showPopupMessage("A batch with this ID already exists!", "Could not create batch");;
		} else{
			int indexOfError = validateProfileFloats(profileSettings);
			// -1 is returned if no errors are found
			if(indexOfError != -1){
				newBatchGui.showPopupMessage("An error was found in " + PropertyHelper.readFromProperty("TextBoxNames", "FloatIndexName" + indexOfError) + ".\n This should only contain the values: 0-9 and decimals should be seperated by '.'", "Input validation failed");
				return; // is returned so code below is not run
			}
			ArrayList<BatchSetting> settings = new ArrayList<>();
			//parsing profileSettings to ArrayList<BatchSetting>
			for(int i = 0; i < profileSettings.size(); i++){
				settings.add(new BatchSetting(i, null, null, profileSettings.get(i)));
			}
			// Creating batchProfile with settings from above
			BatchProfile bp = new BatchProfile(null, settings);
			int profileID = -1;
			try {
				//Saves batchProfile in DB and creates a batch with the batchProfile ID
				profileID = mainController.getDatabaseController().saveBatchProfile(bp);
				Batch b = new Batch(-1, batchString, profileID, mainController.getActiveUser().getUserName(), new Timestamp(new Date().getTime()), null, null);
				System.out.println(b.getCreated_date().getTime());
				//Saves batch and sets the active batch in batchMeasureController
				mainController.getDatabaseController().addToDB(b);
				mainController.getBatchMeasureController().setActiveBatch(b);
			} catch (DataBaseException e) {
				e.printStackTrace();
			}
			//Shift view to BatchMeasure view
			newBatchGui.setVisibility(false);
			mainController.getBatchMeasureController().showGui(true);
		}
	}

	@Override
	public void loadBatchSettingsPressed(String profilename) {
		BatchProfile bp = null;
		try {
			//Loads a batchProfile from DB
			bp =  getBatchProfile(profilename);
		} catch (DataBaseException e) {
			e.printStackTrace();
		}
		//Updates settings on GUI.
		this.newBatchGui.setSettings(bp);
	}

	@Override
	public void cancelPressed() {
		newBatchGui.setVisibility(false);
		mainController.getBatchMeasureController().showGui(true);

	}

	@Override
	public boolean isBatchInDB(String batchNumber) throws DataBaseException {
		return mainController.getDatabaseController().isBatchInDB(batchNumber);

	}

	@Override
	public void deleteBatchProfilePressed(String profileName) throws DataBaseException {
		BatchProfile bp = null;
		try {
			//Loads a batchProfile from DB
			bp =  getBatchProfile(profileName);
		} catch (DataBaseException e) {
			e.printStackTrace();
		}
		mainController.getDatabaseController().deleteBatchProfile(bp);
	}


	/**
	 * @param ArrayList<String> checks this array of settings if valid. Is only checking normal values and tolerance array is of String beceause it is checked before parsed to BathSetting array
	 * 
	 * @return the first index of which profilesetting that is invalid. if everything is allright -1 is returned
	 * because it is possible to have empty strings, these are not checked
	 */
	private int validateProfileFloats(ArrayList<String> settings){
		int firstIndex = Integer.parseInt(PropertyHelper.readFromProperty("firstFloatIndexInProfile"));
		int lastIndex = Integer.parseInt(PropertyHelper.readFromProperty("lastFloatIndexInProfile"));
		for(int i = firstIndex; i <= lastIndex; i++){
			if(!settings.get(i).equals("")){
				if(!Validator.validateFloat(settings.get(i))){
					return i;
				}
			}
		}
		return -1;
	}


}
