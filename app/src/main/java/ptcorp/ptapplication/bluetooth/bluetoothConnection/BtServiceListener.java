package ptcorp.ptapplication.bluetooth.bluetoothConnection;

/**
 * Created by oskarg on 2018-02-27.
 */

public interface BtServiceListener {
    void onBluetoothConnected();
    void onBluetoothDisconnected();
}