package ptcorp.ptapplication.bluetooth.bluetoothConnection;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

/**
 * Created by oskarg on 2018-02-26.
 *
 */

public class BluetoothConnectionService extends Service {
    public static final String BT_APP_NAME = "PhoneTennisBT";

    private static final String TAG = "BtServiceConnection";
    private static final UUID mAppUUID = UUID.fromString("030d51ce-ca04-4256-bce7-7b09dbc7d1ad");

    private BtHostThread mHostThread;
    private BtClientThread mClientThread;
    private BtConnectedThread mConnectedThread;
    private BluetoothAdapter mBtAdapter;
    private BluetoothDevice mBtDevice;
    private BluetoothGatt mBtGatt;
    private BtServiceListener mListener;
    private boolean mConnected = false;
    private GattListener mRssiListener;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mConnectedThread != null) mConnectedThread.disconnect();
        if (mHostThread != null) mHostThread.stopBtHost();
        if (mClientThread != null) mClientThread.stopBtClient();
        if (mBtGatt != null) mBtGatt.close();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        return new BtBinder();
    }

    /**
     * BtServiceConnection binder class. It contains an method called getService used to get an instance of the started service.
     */
    class BtBinder extends Binder {
        BluetoothConnectionService getService() {
            return BluetoothConnectionService.this;
        }
    }

    public BluetoothDevice getSelectedDevice() {
        return mBtDevice;
    }

    /**
     * Writes an object to the connected device.
     * @param obj Any object
     */
    public void writeObject(Object obj) {
        if (mConnected && mConnectedThread != null)
            mConnectedThread.write(obj);
    }

    /**
     * Stops any attempt to connect to a Host as a client and start listening for incoming Bluetooth connections as a Host.
     */
    public void startBtHost() {
        if (mClientThread != null) {
            mClientThread.stopBtClient();
            mClientThread = null;
        }

        if (mHostThread == null) {
            mHostThread = new BtHostThread();
            mHostThread.start();
        }
    }

    /**
     * Stop hosting a Bluetooth server.
     */
    public void stopBtHost() {
        if (mHostThread != null) mHostThread.stopBtHost();
    }

    /**
     * Pair to a found Bluetooth device.
     * @param device BluetoothDevice
     */
    public void pairDevice(BluetoothDevice device) {
        if (!mConnected) {
            mBtDevice = device;
            device.createBond();
        }
    }

    /**
     * Connect to a paired Bluetooth host device. Call method {@link #pairDevice(BluetoothDevice), pairDevice} before trying to connect to the device.
     * @param device BluetoothDevice
     */
    public void connectToDevice(BluetoothDevice device) {
        mBtDevice = device;
        mRssiListener = new GattListener();
        mBtGatt = mBtDevice.connectGatt(getApplicationContext(), true, mRssiListener);

        mClientThread = new BtClientThread(mBtDevice);
        mClientThread.start();
    }

    /**
     * Set a listener for listening to Bluetooth events.
     * @param listener BtServiceListener
     */
    public void setListener(BtServiceListener listener) {
        mListener = listener;
    }

    /**
     * Get the average rssi value. An estimate of the signal strength between two devices.
     * @return int average rssi.
     */
    public int getAverageRssi() {
        return mRssiListener != null ? mRssiListener.getAverageRssi() : 0;
    }

    /**
     * Starts a connected thread which listens for incoming messages and can send outgoing messages.
     * @param mmSocket BluetoothSocket
     */
    private void handleConnectedDevice(BluetoothSocket mmSocket) {
        mConnected = true;
        mConnectedThread = new BtConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    /**
     * BluetoothHost listens for incoming connections.
     */
    private class BtHostThread extends Thread {
        private BluetoothServerSocket mmBtServerSocket;
        private boolean mmRunning = true;

        BtHostThread() {
            BluetoothServerSocket tmpSocket = null;
            try {
                tmpSocket = mBtAdapter.listenUsingRfcommWithServiceRecord(BT_APP_NAME, mAppUUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmBtServerSocket = tmpSocket;
        }

        @Override
        public void run() {
            BluetoothSocket socket;
            while(mmRunning) {
                try {
                    socket = mmBtServerSocket.accept();
                    if (socket != null) {
                        mBtAdapter.cancelDiscovery();
                        stopBtHost();
                        handleConnectedDevice(socket);
                    }
                } catch (IOException e) {
                    Log.w(TAG, "Error occurred when trying to accept client.", e);
                    break;
                }
            }
        }

        private void stopBtHost() {
            try {
                mmRunning = false;
                mHostThread = null;
                if (mmBtServerSocket != null) mmBtServerSocket.close();
            } catch (IOException e) {
                Log.w(TAG, "Could'nt close bluetooth socket", e);
            }
        }
    }

    /**
     * BluetoothClientThread used to connect to a bluetooth host device.
     */
    private class BtClientThread extends Thread {
        private final BluetoothDevice mmDevice;
        private final BluetoothSocket mmSocket;
        private boolean closing;

        BtClientThread(BluetoothDevice bTDevice) {
            mmDevice = bTDevice;
            BluetoothSocket tmpSocket = null;
            try {
                tmpSocket = mmDevice.createRfcommSocketToServiceRecord(mAppUUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmSocket = tmpSocket;
            closing = false;
        }

        @Override
        public void run() {
            if (!mConnected) {
                mBtAdapter.cancelDiscovery();

                try {
                    mmSocket.connect();
                    handleConnectedDevice(mmSocket);
                } catch (IOException e) {
                    Log.w(TAG, "Error occurred when trying to connect to bluetooth host", e);
                    if (!closing) {
                        mListener.onHostError();
                    }
                    try {
                        mmSocket.close();
                    } catch (IOException e1) {
                        Log.w(TAG, "Error occurred when trying to close socket in client thread", e);
                    }
                }
            }
        }

        void stopBtClient() {
            try {
                closing = true;
                mClientThread = null;
                mmSocket.close();
            } catch (IOException e) {
                Log.w(TAG, "Error occurred trying to close bluetooth socket in client thread", e);
            }
        }
    }

    private class BtConnectedThread extends Thread {
        private final BluetoothSocket mmBtSocket;
        private final ObjectInputStream mBtOIS;
        private final ObjectOutputStream mBtOOS;

        private boolean running = true;

        BtConnectedThread(BluetoothSocket mmBtSocket) {
            this.mmBtSocket = mmBtSocket;
            ObjectInputStream tmpIn = null;
            ObjectOutputStream tmpOut = null;
            try {
                tmpOut = new ObjectOutputStream(mmBtSocket.getOutputStream());
                tmpIn = new ObjectInputStream(mmBtSocket.getInputStream());
            } catch (IOException e) {
                Log.w(TAG, "BtConnectedThread: Error occurred trying to create output/input streams", e);
            }
            mBtOIS = tmpIn;
            mBtOOS = tmpOut;

            // Dont notify if not connected to device.
            if (mBtOOS != null && mBtOIS != null ) {
                mListener.onBluetoothConnected();
                running = true;
            } else {
                running = false;
                disconnect();
            }
        }

        @Override
        public void run() {
            // Can only be used on client phone
            if (mBtGatt != null) {
                new Thread(new RssiReader()).start();
            }

            while(running) {
                try {
                    mListener.onMessageReceived(mBtOIS.readObject());
                } catch (IOException e) {
                    if (running) {
                        mListener.onBluetoothError();
                        Log.w(TAG, "Error when listening for incoming object.", e);
                    }
                    break;
                } catch (ClassNotFoundException e) {
                    Log.w(TAG, "Error when receiving object, wrong class", e);
                }
            }
            disconnect();
        }

        void write(Object obj) {
            try {
                mBtOOS.writeObject(obj);
                mBtOOS.reset();
            } catch (IOException e) {
                Log.w(TAG, "Error when sending object", e);
            }
        }

        void disconnect() {
            Log.i(TAG, "Closing bluetooth connected socket");
            if (running) {
                running = false;
                try {
                    mBtOOS.close();
                    mBtOIS.close();
                    mmBtSocket.close();
                    if (mBtGatt != null) mBtGatt.disconnect();
                    mListener.onBluetoothDisconnected(null);
                } catch (IOException e) {
                    mListener.onBluetoothDisconnected(e);
                    Log.w(TAG, "Error when closing connected socket", e);
                }
                mConnected = false;
            }
        }
    }

    private class RssiReader implements Runnable {
        @Override
        public void run() {
            while(mConnected) {
                try {
                    mBtGatt.readRemoteRssi();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class GattListener extends BluetoothGattCallback {
        private final int AMOUNT_OF_AVERAGE = 5;

        private int mRssiPos = 0;
        private int[] mRssiAverage = new int[AMOUNT_OF_AVERAGE];
        private int mAverageRssi = 0;

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                gatt.disconnect();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from device Gatt");
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            mRssiAverage[mRssiPos % AMOUNT_OF_AVERAGE] = Math.abs(rssi);
            int rssiTotal = 0;

            for (int i = 0; i < AMOUNT_OF_AVERAGE; i++) {
                rssiTotal += mRssiAverage[mRssiPos];
            }
            mAverageRssi = rssiTotal / AMOUNT_OF_AVERAGE;
        }

        int getAverageRssi() {
            if (mRssiPos > AMOUNT_OF_AVERAGE) {
                return mAverageRssi;
            }
            return 0;
        }
    }
}
