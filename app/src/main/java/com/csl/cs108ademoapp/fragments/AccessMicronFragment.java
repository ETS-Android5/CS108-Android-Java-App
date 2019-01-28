package com.csl.cs108ademoapp.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.csl.cs108ademoapp.AccessTask;
import com.csl.cs108ademoapp.GenericTextWatcher;
import com.csl.cs108ademoapp.MainActivity;
import com.csl.cs108ademoapp.R;
import com.csl.cs108library4a.Cs108Connector;
import com.csl.cs108library4a.ReaderDevice;

public class AccessMicronFragment extends CommonFragment {
    final boolean DEBUG = true;
	EditText editTextRWTagID, editTextAccessRWAccPassword, editTextaccessRWAntennaPower;
    TextView textViewConfigOk, textViewCalibrationOk, textViewSensorCodeOk, textViewRssiCodeOk, textViewTemperatureCodeOk;
    CheckBox checkBoxConfig, checkBoxCalibration, checkBoxSensorCode, checkBoxRssiCode, checkBoxTemperatureCode;

    TextView textViewModelCode, textViewCalibrationVersion, textViewSensorCode, textViewRssiCode, textViewTemperatureCode;
	private Button buttonRead;

    enum ReadWriteTypes {
        NULL, MODELCODE, CALIBRATION, SENSORCODE, RSSICODE, TEMPERATURECODE
    }
    ReadWriteTypes readWriteTypes;

    private AccessTask accessTask;
    private int modelCode = -1;
    private int calCode1, calTemp1, calCode2, calTemp2, calVer = -1;
    private boolean changedSelectIndex = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState, false);
        return inflater.inflate(R.layout.fragment_access_micron, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        editTextRWTagID = (EditText) getActivity().findViewById(R.id.accessMNTagID);
        editTextAccessRWAccPassword = (EditText) getActivity().findViewById(R.id.accessMNAccPasswordValue);
        editTextAccessRWAccPassword.addTextChangedListener(new GenericTextWatcher(editTextAccessRWAccPassword, 8));
        editTextAccessRWAccPassword.setText("00000000");

        textViewConfigOk = (TextView) getActivity().findViewById(R.id.accessMNModelCodeOK);
        textViewCalibrationOk = (TextView) getActivity().findViewById(R.id.accessMNCalibrationOK);
        textViewSensorCodeOk = (TextView) getActivity().findViewById(R.id.accessMNSensorCodeOK);
        textViewRssiCodeOk = (TextView) getActivity().findViewById(R.id.accessMNRssiCodeOK);
        textViewTemperatureCodeOk = (TextView) getActivity().findViewById(R.id.accessMNTemperatureCodeOK);
        checkBoxConfig = (CheckBox) getActivity().findViewById(R.id.accesMNModelCodeTitle);
        checkBoxCalibration = (CheckBox) getActivity().findViewById(R.id.accesMNCalibrationTitle);
        checkBoxSensorCode = (CheckBox) getActivity().findViewById(R.id.accesMNSensorCodeTitle);
        checkBoxRssiCode = (CheckBox) getActivity().findViewById(R.id.accesMNRssiCodeTitle);
        checkBoxTemperatureCode = (CheckBox) getActivity().findViewById(R.id.accesMNTemperatureCodeTitle);

        textViewModelCode = (TextView) getActivity().findViewById(R.id.accessMNModelCode);
        textViewCalibrationVersion = (TextView) getActivity().findViewById(R.id.accessMNCalibrationVersion);
        textViewSensorCode = (TextView) getActivity().findViewById(R.id.accessMNSensorCode);
        textViewRssiCode = (TextView) getActivity().findViewById(R.id.accessMNRssiCode);
        textViewTemperatureCode = (TextView) getActivity().findViewById(R.id.accessMNTemperatureCode);

        ArrayAdapter<CharSequence> arrayAdapterUnit = ArrayAdapter.createFromResource(getActivity(), R.array.coldChain_unit_options, R.layout.custom_spinner_layout);
        arrayAdapterUnit.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<CharSequence> arrayAdapterUnit1 = ArrayAdapter.createFromResource(getActivity(), R.array.coldChain_IntervalUnit_options, R.layout.custom_spinner_layout);
        arrayAdapterUnit1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<CharSequence> arrayAdapterEnable = ArrayAdapter.createFromResource(getActivity(), R.array.coldChain_enable_options, R.layout.custom_spinner_layout);
        arrayAdapterEnable.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        editTextaccessRWAntennaPower = (EditText) getActivity().findViewById(R.id.accessMNAntennaPower);
        editTextaccessRWAntennaPower.setText(String.valueOf(300));

        buttonRead = (Button) getActivity().findViewById(R.id.accessMNReadButton);
        buttonRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MainActivity.mCs108Library4a.isBleConnected() == false) {
                    Toast.makeText(MainActivity.mContext, R.string.toast_ble_not_connected, Toast.LENGTH_SHORT).show();
                    return;
                } else if (MainActivity.mCs108Library4a.isRfidFailure()) {
                    Toast.makeText(MainActivity.mContext, "Rfid is disabled", Toast.LENGTH_SHORT).show();
                    return;
                }
                startAccessTask();
            }
        });

        setupTagID();
        MainActivity.mCs108Library4a.setSameCheck(false);
    }

    @Override
    public void onDestroy() {
        if (accessTask != null) accessTask.cancel(true);
        MainActivity.mCs108Library4a.setSameCheck(true);
        if (changedSelectIndex) {
            MainActivity.mCs108Library4a.setInvSelectIndex(1);
            MainActivity.mCs108Library4a.setSelectCriteria(false, 0, 0, 0, 0, "");
            MainActivity.mCs108Library4a.setInvSelectIndex(0);
        }
        MainActivity.mCs108Library4a.restoreAfterTagSelect();
        super.onDestroy();
    }

    boolean userVisibleHint = false;
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(getUserVisibleHint()) {
            userVisibleHint = true;
            MainActivity.mCs108Library4a.appendToLog("AccessMicronFragment is now VISIBLE");
            setupTagID();
            //            setNotificationListener();
        } else {
            userVisibleHint = false;
            MainActivity.mCs108Library4a.appendToLog("AccessMicronFragment is now INVISIBLE");
//            MainActivity.mCs108Library4a.setNotificationListener(null);
        }
    }

    public AccessMicronFragment() {
        super("AccessMicronFragment");
    }

    void setupTagID() {
        ReaderDevice tagSelected = MainActivity.tagSelected;
        boolean bSelected = false;
        if (tagSelected != null) {
            if (tagSelected.getSelected() == true) {
                String stringDetail = tagSelected.getDetails();
                int indexUser = stringDetail.indexOf("TID=");
                if (indexUser != -1) {
                    //
                }
                bSelected = true;
                if (editTextRWTagID != null) editTextRWTagID.setText(tagSelected.getAddress());

                stringDetail = tagSelected.getDetails();
                indexUser = stringDetail.indexOf("USER=");
                if (indexUser != -1) {
                    String stringUser = stringDetail.substring(indexUser + 5);
                    MainActivity.mCs108Library4a.appendToLog("stringUser = " + stringUser);

                    boolean bEnableBAPMode = false;
                    int number = Integer.valueOf(stringUser.substring(3, 4), 16);
                    if ((number % 2) == 1) bEnableBAPMode = true;
                }
            }
        }
    }

    void startAccessTask() {
        if (DEBUG) MainActivity.mCs108Library4a.appendToLog("startAccessTask()");
        if (updating == false) {
            updating = true; bankProcessing = 0;
            checkBoxConfig.setChecked(true); MainActivity.mCs108Library4a.setInvSelectIndex(0);
            mHandler.removeCallbacks(updateRunnable);
            mHandler.post(updateRunnable);
        }
    }
    boolean updating = false; int bankProcessing = 0;
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            boolean rerunRequest = false; boolean taskRequest = false;
            if (accessTask == null) {
                if (DEBUG) MainActivity.mCs108Library4a.appendToLog("AccessMicronChainFragment().updateRunnable(): NULL accessReadWriteTask");
                taskRequest = true;
            } else if (accessTask.getStatus() != AsyncTask.Status.FINISHED) {
                rerunRequest = true;
                if (DEBUG) MainActivity.mCs108Library4a.appendToLog("AccessMicronChainFragment().updateRunnable(): accessReadWriteTask.getStatus() =  " + accessTask.getStatus().toString());
            } else {
                taskRequest = true;
                if (DEBUG) MainActivity.mCs108Library4a.appendToLog("AccessMicronChainFragment().updateRunnable(): FINISHED accessReadWriteTask");
            }
            if (processResult()) { rerunRequest = true; MainActivity.mCs108Library4a.appendToLog("processResult is TRUE");}
            else if (taskRequest) {
                boolean invalid = processTickItems();
                MainActivity.mCs108Library4a.appendToLog("processTickItems, invalid = " + invalid);
                if (bankProcessing++ != 0 && invalid == true)   rerunRequest = false;
                else {
                    int selectBank = 1;
                    int selectOffset = 32;
                    String selectMask = editTextRWTagID.getText().toString();
                    accessTask = new AccessTask(
                            buttonRead, null,
                            invalid,
                            selectMask, selectBank, selectOffset,
                            editTextAccessRWAccPassword.getText().toString(),
                            Integer.valueOf(editTextaccessRWAntennaPower.getText().toString()),
                            Cs108Connector.HostCommands.CMD_18K6CREAD,
                            0, false, false,true,
                            null, null, null, null, null);
                    accessTask.execute();
                    rerunRequest = true;
                    MainActivity.mCs108Library4a.appendToLog("accessTask is created with selectBank = " + selectBank);
                }
            }
            if (rerunRequest) {
                mHandler.postDelayed(updateRunnable, 500);
                if (DEBUG) MainActivity.mCs108Library4a.appendToLog("AccessMicronChainFragment().updateRunnable(): Restart");
            }
            else    updating = false;
            MainActivity.mCs108Library4a.appendToLog("AccessMicronChainFragment().updateRunnable(): Ending with updating = " + updating);
        }
    };

    String getTemperatue(String stringInput) {
        byte bValue = Byte.parseByte(stringInput.substring(0,1), 16);
        byte bValue2 = Byte.parseByte(stringInput.substring(1, 2), 16); bValue2 <<= 4;
        byte bValue3 = Byte.parseByte(stringInput.substring(2, 3), 16); bValue2 |= bValue3;
        String stringValue = "";
        if ((bValue & 0x01) != 0) { stringValue = "-"; bValue2 ^= 0xFF; if (bValue2 != 0xFF) bValue2++; }
        stringValue += String.valueOf((bValue2 & 0xFF) >> 2);
        switch (bValue2 & 0x03) {
            case 1:
                stringValue += ".25";
                break;
            case 2:
                stringValue += ".50";
                break;
            case 3:
                stringValue += ".75";
                break;
        }
        return  stringValue;
    }

    //    int accessBank, accSize, accOffset;
//    int restartCounter = 0; int restartAccessBank = -1;
    boolean processResult() {
        String accessResult = null;
        if (accessTask == null) return false;
        else if (accessTask.getStatus() != AsyncTask.Status.FINISHED) return false;
        else {
            if (DEBUG) MainActivity.mCs108Library4a.appendToLog("AccessTask FINISHED");
            accessResult = accessTask.accessResult;
            if (accessResult == null) {
                if (readWriteTypes == ReadWriteTypes.MODELCODE) {
                    textViewConfigOk.setText("E");
                    checkBoxConfig.setChecked(false);
                } else if (readWriteTypes == ReadWriteTypes.CALIBRATION) {
                    textViewCalibrationOk.setText("E");
                    checkBoxCalibration.setChecked(false);
                } else if (readWriteTypes == ReadWriteTypes.SENSORCODE) {
                    textViewSensorCodeOk.setText("E"); checkBoxSensorCode.setChecked(false);
                } else if (readWriteTypes == ReadWriteTypes.RSSICODE) {
                    textViewRssiCodeOk.setText("E"); checkBoxRssiCode.setChecked(false);
                } else if (readWriteTypes == ReadWriteTypes.TEMPERATURECODE) {
                    textViewTemperatureCodeOk.setText("E"); checkBoxTemperatureCode.setChecked(false);
                }
            } else {
                if (DEBUG) MainActivity.mCs108Library4a.appendToLog("accessResult = " + accessResult);
                if (readWriteTypes == ReadWriteTypes.MODELCODE) {
                    textViewConfigOk.setText("O");
                    checkBoxConfig.setChecked(false);
                    readWriteTypes = ReadWriteTypes.NULL;
                    boolean valid = false;
                    if (accessResult.length() > 7) {
                        if (accessResult.substring(0, 7).matches("E282401")) { modelCode = 1; valid = true; }
                        else if (accessResult.substring(0, 7).matches("E282402")) { modelCode = 2; valid = true; }
                        else if (accessResult.substring(0, 7).matches("E282403")) { modelCode = 3; valid = true; }
                    }
                    if (valid) { textViewModelCode.setText(accessResult.substring(4)); checkBoxCalibration.setChecked(true); }
                    else Toast.makeText(MainActivity.mContext, "This is not Micron 40X tag !!!", Toast.LENGTH_SHORT).show();
                } else if (readWriteTypes == ReadWriteTypes.CALIBRATION) {
                    textViewCalibrationOk.setText("O"); checkBoxCalibration.setChecked(false); readWriteTypes = ReadWriteTypes.NULL;
                    if (accessResult.length() >= 16) {
                        int crc = Integer.parseInt(accessResult.substring(0, 4), 16);
                        calCode1 = Integer.parseInt(accessResult.substring(4, 7), 16);
                        calTemp1 = Integer.parseInt(accessResult.substring(7, 10), 16); calTemp1 >>= 1;
                        calCode2 = Integer.parseInt(accessResult.substring(9, 13), 16); calCode2 >>= 1; calCode2 &= 0xFFF;
                        calTemp2 = Integer.parseInt(accessResult.substring(12, 16), 16); calTemp2 >>= 2; calTemp2 &= 0x7FF;
                        calVer = Integer.parseInt(accessResult.substring(15, 16),16); calVer &= 0x3;
                        MainActivity.mCs108Library4a.appendToLog("crc = " + crc + ", code1 = " + calCode1 + ", temp1 = " + calTemp1 + ", code2 = " + calCode2 + ", temp2 = " + calTemp2 + ", ver = " + calVer);
                        textViewCalibrationVersion.setText(String.valueOf(calVer));
                    }
                } else if (readWriteTypes == ReadWriteTypes.SENSORCODE) {
                    textViewSensorCodeOk.setText("O"); checkBoxSensorCode.setChecked(false); readWriteTypes = ReadWriteTypes.NULL;
                    int iTemp = 0;
                    if (accessResult.length() >= 4) {
                        if (modelCode == 3) {
                            iTemp = Integer.parseInt(accessResult.substring(1,4), 16); iTemp &= 0x1FF;
                            textViewSensorCode.setText(String.format("%03X", iTemp));
                        } else {
                            iTemp = Integer.parseInt(accessResult.substring(2,4), 16); iTemp &= 0x1F;
                            textViewSensorCode.setText(String.format("%02X", iTemp));
                        }
                    }
                } else if (readWriteTypes == ReadWriteTypes.RSSICODE) {
                    textViewRssiCodeOk.setText("O"); checkBoxRssiCode.setChecked(false); readWriteTypes = ReadWriteTypes.NULL;
                    if (accessResult.length() >= 4) {
                        int iTemp = Integer.parseInt(accessResult.substring(2,4), 16); iTemp &= 0x1F;
                        textViewRssiCode.setText(String.format("%02X", iTemp));
                    }
                } else if (readWriteTypes == ReadWriteTypes.TEMPERATURECODE) {
                    textViewTemperatureCodeOk.setText("O"); checkBoxTemperatureCode.setChecked(false); readWriteTypes = ReadWriteTypes.NULL;
                    if (accessResult.length() >= 4) {
                        float fTemperature = Integer.parseInt(accessResult.substring(0,4), 16);
                        fTemperature = ((float)calTemp2 - (float)calTemp1) * (fTemperature - (float) calCode1);
                        fTemperature /= ((float) (calCode2) - (float)calCode1);
                        fTemperature += (float) calTemp1;
                        fTemperature -= 800;
                        fTemperature /= 10;
                        textViewTemperatureCode.setText(accessResult.substring(0,4) + (calVer != -1 ? ("(" + String.format("%.1f", fTemperature) + (char) 0x00B0 + "C" + ")") : ""));
                    }
                }
            }
            accessTask = null;
            return true;
        }
    }
    boolean processTickItems() {
        boolean invalidRequest1 = false;
        int accBank = 0, accSize = 0, accOffset = 0;
        String writeData = "";

        if (editTextRWTagID.getText().toString().length() == 0) invalidRequest1 = true;
        else if (checkBoxConfig.isChecked() == true) {
            accBank = 2; accOffset = 0; accSize = 2; readWriteTypes = ReadWriteTypes.MODELCODE;
            textViewConfigOk.setText(""); textViewModelCode.setText(""); modelCode = -1;
        } else if (checkBoxCalibration.isChecked() == true) {
            accBank = 3; accOffset = 8; accSize = 4; readWriteTypes = ReadWriteTypes.CALIBRATION;
            textViewCalibrationOk.setText(""); textViewCalibrationVersion.setText(""); calVer = -1;
        } else if (checkBoxSensorCode.isChecked() == true && modelCode != -1) {
            accBank = 0; if (modelCode == 1) accBank = 3;
            accOffset = 11; if (modelCode == 3) accOffset = 12;
            accSize = 1; readWriteTypes = ReadWriteTypes.SENSORCODE;
            textViewSensorCodeOk.setText(""); textViewSensorCode.setText("");
        } else if (checkBoxRssiCode.isChecked() == true && modelCode != -1) {
            MainActivity.mCs108Library4a.setInvSelectIndex(0);
            MainActivity.mCs108Library4a.setSelectCriteria(true, 4, 0, 3, (modelCode == 3 ? 0xD0 : 0xA0), "1F");
            MainActivity.mCs108Library4a.setInvSelectIndex(1); changedSelectIndex = true;
            accBank = 0; accOffset = 13; if (modelCode == 1) { accBank = 3; accOffset = 9; }
            accSize = 1; readWriteTypes = ReadWriteTypes.RSSICODE;
            textViewRssiCodeOk.setText(""); textViewRssiCode.setText("");
        } else if (checkBoxTemperatureCode.isChecked() == true && modelCode == 3 && calVer != -1) {
            MainActivity.mCs108Library4a.setInvSelectIndex(0);
            MainActivity.mCs108Library4a.setSelectCriteria(true, 4, 0, 3, 0xE0, "");
            MainActivity.mCs108Library4a.setInvSelectIndex(1); changedSelectIndex = true;
            accBank = 0; accOffset = 14; accSize = 1; readWriteTypes = ReadWriteTypes.TEMPERATURECODE;
            textViewTemperatureCodeOk.setText(""); textViewTemperatureCode.setText("");
        } else {
            invalidRequest1 = true;
        }

        if (invalidRequest1 == false) {
            if (MainActivity.mCs108Library4a.setAccessBank(accBank) == false) {
                invalidRequest1 = true;
            }
        }
        if (invalidRequest1 == false) {
            if (MainActivity.mCs108Library4a.setAccessOffset(accOffset) == false) {
                invalidRequest1 = true;
            }
        }
        if (invalidRequest1 == false) {
            if (accSize == 0) {
                invalidRequest1 = true;
            } else if (MainActivity.mCs108Library4a.setAccessCount(accSize) == false) {
                invalidRequest1 = true;
            }
        }
        return invalidRequest1;
    }
}