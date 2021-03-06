package javaMeasure.control;

import javaMeasure.Batch;
import javaMeasure.User;
import javaMeasure.control.interfaces.IBatchMeasureController;
import javaMeasure.control.interfaces.ICConnector;
import javaMeasure.control.interfaces.IDasyFileReader;
import javaMeasure.control.interfaces.IDatabaseController;
import javaMeasure.control.interfaces.ILoginController;
import javaMeasure.control.interfaces.IMainController;
import javaMeasure.control.interfaces.INewBatchController;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class MainController implements IMainController{
	public static final boolean debug = true;
	ILoginController loginController;
	IDatabaseController databaseController;
	IBatchMeasureController batchMeasureController;
	IDasyFileReader dasyFileReader;
	ICConnector cConnector;
	INewBatchController newBatchController;
	User activeUser;

	public MainController(boolean testmode) {
		super();
		this.databaseController = new DataBaseController();
		this.dasyFileReader = new DasyFileReader();
		this.cConnector = new CConnector(testmode);
		
		setLookOfGuis();
	}

	@Override
	public void run() {
		loginController = new LoginController(this);
		//Responsibility delegated to LoginController
	}
	
	public void startNewBatchController(){
		batchMeasureController.showGui(false);
		newBatchController = new NewBatchController(this);
	}
	
	public void startNewBatchController(Batch activeBatch){
		batchMeasureController.showGui(false);
		newBatchController = new NewBatchController(this, activeBatch);
	}
	
	@Override
	public void userLoggedIn(User user) {
		System.out.println("User " + user.getUserName() + "Logged in");
		this.activeUser = user;
		loginController.showGui(false);
		batchMeasureController = new BatchMeasureController(this);
	}
	
//----- Getters and Setters ------
	
	public User getActiveUser() {
		return activeUser;
	}
	
	public IBatchMeasureController getBatchMeasureController() {
		return batchMeasureController;
	}

	public void setBatchMeasureController(
			IBatchMeasureController batchMeasureController) {
		this.batchMeasureController = batchMeasureController;
	}
	
	public ICConnector getcConnector() {
		return cConnector;
	}

	public IDasyFileReader getDasyController() {
		return dasyFileReader;
	}

	public IDatabaseController getDatabaseController() {
		return databaseController;
	}
	public ILoginController getLoginController() {
		return loginController;
	}
	
//----- Private Method to set the look of the Guis -------	


	private void setLookOfGuis() {
		//Sets Application to run with System Look instead of Swing look
		try {
			UIManager.setLookAndFeel(
			        UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void logOut() {
		batchMeasureController.showGui(false);
		loginController.showGui(true);
		activeUser= null;
		
	}

	

	
	
}
