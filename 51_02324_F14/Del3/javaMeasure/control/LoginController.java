package javaMeasure.control;

import java.awt.EventQueue;

import javaMeasure.User;
import javaMeasure.control.interfaces.IDatabaseController;
import javaMeasure.control.interfaces.ILoginController;
import javaMeasure.control.interfaces.IMainController;
import javaMeasure.control.interfaces.IDatabaseController.DataBaseException;
import javaMeasure.control.interfaces.IDatabaseController.UserNotFoundException;
import javaMeasure.view.LoginGui;
import javaMeasure.view.interfaces.ILoginGui;

import java.util.*;

public class LoginController implements ILoginController{
	private ILoginGui Gui;
	private IMainController mainCtrl;

	//Constructor
	public LoginController(IMainController mainCtrl) {
		super();
		this.mainCtrl = mainCtrl;
		this.Gui = new LoginGui(this);
		EventQueue.invokeLater(this.Gui);
		ArrayList<User> users = new ArrayList<User>();
//		User[] users = null;
		try {
			users = mainCtrl.getDatabaseController().getUserList();
		} catch (DataBaseException e) {
			System.err.println("could not load user list");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] UserStrings = new String[users.size()];
		for (int i = 0; i<users.size();i++){
			UserStrings[i] = users.get(i).getUserName();
		}
		Gui.setUserList(UserStrings);
	}

	@Override
	public void setGui(ILoginGui Gui) {
		this.Gui = Gui;
	}

	@Override
	public void showGui(boolean b) {
		Gui.showGui(b);	
	}

	@Override
	public void bthLoginPressed() {
		String loginString = Gui.getSelectedUser();
		User user=null;
		try {
			user = mainCtrl.getDatabaseController().getUserFromString(loginString);
			mainCtrl.userLoggedIn(user);
		} catch (DataBaseException e) {
			System.err.println("Database Connection failed - LoginController");
			e.printStackTrace();
		} catch (UserNotFoundException e) {
			userNotFound(loginString);
		}
		
		
	}

	private void userNotFound(String loginString) {
		System.out.println("No such user in db");
		this.Gui.showNoSuchUser();
	}

	@Override
	public void btnNewUserPressed() {
		IDatabaseController dbCtrl = mainCtrl.getDatabaseController();
		//Check if user already is in db;
		boolean userExists = false;
		try {
			userExists =  dbCtrl.isUserNameInDB(this.Gui.getSelectedUser());
		} catch (DataBaseException e) {
			System.err.println("Database Error - LoginController");
			e.printStackTrace();
		}
		if (userExists){
			this.Gui.showUserAlreadyExists();
		} else {
			//Insert new User in to db
			String userName = null;
			try {
				userName = this.Gui.getSelectedUser();
				dbCtrl.addToDB(new User(userName, 0));
			} catch (DataBaseException e) {
				System.err.println("Db Error - LoginController");
				e.printStackTrace();
			}
			try {
				mainCtrl.userLoggedIn(dbCtrl.getUserFromString(userName));
			} catch (DataBaseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UserNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
	}



}