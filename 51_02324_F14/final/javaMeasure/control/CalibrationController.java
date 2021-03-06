package javaMeasure.control;

import javaMeasure.Measurement.MeasurementType;
import javaMeasure.PropertyHelper;
import javaMeasure.control.interfaces.ICConnector;
import javaMeasure.control.interfaces.ICalibrationController;
import javaMeasure.view.CalibrationGui;
import javaMeasure.view.interfaces.ICalibrationGui;

public class CalibrationController implements ICalibrationController{
	private ICalibrationGui gui;
	private double zeroVal = 0;
	private double calibrateVal = 0;
	private double calibrationConstant = 0;
	private ICConnector conn;
	public CalibrationController(ICConnector conn) {
		super();
		this.conn = conn;
		this.gui = new CalibrationGui(this);
	}

	@Override
	public void zeroBtnPressed() {
		try {
			zeroVal = conn.readMeasurements(MeasurementType.STROKE, 1, 0)[0].getMeasureValue();
			gui.setZeroVolt(zeroVal);
			calculateconstant();
		} catch (ConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public void calibrateBtnPressed() {
		try {
			calibrateVal = conn.readMeasurements(MeasurementType.STROKE, 1, 0)[0].getMeasureValue();
			gui.setCalibrationVolt(calibrateVal);
			calculateconstant();
		} catch (ConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	private void calculateconstant() {
		if (calibrateVal != 0 && zeroVal != 0){
			calibrationConstant = (calibrateVal - zeroVal)/gui.getCalibrationLength();
			System.out.println(calibrationConstant);
			gui.setCalibrationConstant(calibrationConstant);
			PropertyHelper.writeToProperty("calibrationConstant", Double.toString(calibrationConstant));
			gui.enableOk(true);
		}
		
	}

	@Override
	public void okBtnPressed() {
		gui.showGui(false);
		
	}

	@Override
	public void cancelBtnPressed() {
		gui.showGui(false);
		
	}

	@Override
	public double getCalibrationConstant() {
		return calibrationConstant;
	}
	
	public static void main(String[] args){
		CalibrationController c = new CalibrationController(new CConnector(true));
		c.gui.run();
		
	}

}
