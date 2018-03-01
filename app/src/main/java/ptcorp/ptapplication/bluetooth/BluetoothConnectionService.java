package ptcorp.ptapplication.bluetooth;

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
    private UUID mAppUUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private BtHostThread mHostThread;
    private BtClientThread mClientThread;
    private BtConnectedThread mConnectedThread;
    private BluetoothAdapter mBtAdapter;
    private BluetoothDevice mBtDevice;
    private BluetoothGatt mBtGatt;
    private BtServiceListener mListener;
    private boolean mConnected = false;

    private int[] mRssiAverage = new int[5];
    private int mRssiTotal = 0;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mConnectedThread != null) mConnectedThread.disconnect();
        if (mHostThread != null) mHostThread.stopBtHost();
        if (mClientThread != null) mClientThread.stopBtClient();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new BtBinder();
    }

    /**
     * BtServiceConnection binder class. It contains an method called getService used to get an instance of the started service.
     */
    public class BtBinder extends Binder {
        public BluetoothConnectionService getService() {
            return BluetoothConnectionService.this;
        }
    }

    /**
     * Stops any attempt to connect to a Host as a client and start listening for incoming Bluetooth connections as a Host.
     */
    public void startBtServer() {
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
     * Connect to a found Bluetooth Host device as a client.
     * @param device BluetoothDevice
     */
    public void connectToDevice(BluetoothDevice device) {
        if (!mConnected) {
            mBtDevice = device;

            mBtGatt = mBtDevice.connectGatt(getApplicationContext(), true, new RssiListener());

            mClientThread = new BtClientThread(mBtDevice);
            mClientThread.start();
        }
    }

    /**
     * Disconnect from the Bluetooth connection.
     */
    public void disconnectFromDevice() {
        mConnectedThread.disconnect();
    }

    /**
     * Set a listener for listening to Bluetooth events.
     * @param listener BtServiceListener
     */
    public void setListener(BtServiceListener listener) {
        mListener = listener;
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
                    Log.e(TAG, "Error occurred when trying to accept client.", e);
                    break;
                }
            }
        }

        private void stopBtHost() {
            try {
                mmRunning = false;
                if (mmBtServerSocket != null) mmBtServerSocket.close();
                mConnected = false;
            } catch (IOException e) {
                Log.e(TAG, "Could'nt close bluetooth socket", e);
            }
        }
    }

    /**
     * BluetoothClientThread used to connect to a bluetooth host device.
     */
    private class BtClientThread extends Thread {
        private final BluetoothDevice mmDevice;
        private final BluetoothSocket mmSocket;

        BtClientThread(BluetoothDevice bTDevice) {
            mmDevice = bTDevice;
            BluetoothSocket tmpSocket = null;
            try {
                tmpSocket = mmDevice.createRfcommSocketToServiceRecord(mAppUUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmSocket = tmpSocket;
        }

        @Override
        public void run() {
            mBtAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when trying to connect to bluetooth host", e);
                try {
                    mmSocket.close();
                } catch (IOException e1) {
                    Log.e(TAG, "Error occurred when trying to close socket in client thread", e);
                }
                return;
            }
            handleConnectedDevice(mmSocket);
        }

        void stopBtClient() {
            try {
                mmSocket.close();
                mConnected = false;
            } catch (IOException e) {
                Log.e(TAG, "stopBtClient: Error occurred trying to close bluetooth socket", e);
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
                tmpIn = new ObjectInputStream(mmBtSocket.getInputStream());
                tmpOut = new ObjectOutputStream(mmBtSocket.getOutputStream());
            } catch (IOException e) {
                Log.e(TAG, "BtConnectedThread: Error occurred trying to create output/input streams", e);
            }
            mBtOIS = tmpIn;
            mBtOOS = tmpOut;

            mListener.onConnected();
        }

        @Override
        public void run() {
            // Can only be used on client phone
            if (mBtGatt != null) {
                new Thread(new RssiReader()).start();
            }

            while(running) {
                try {
                    Log.d(TAG, "Listening for incoming bl");
                    Object obj = mBtOIS.readObject();


                } catch (ClassNotFoundException | IOException e) {
                    Log.e(TAG, "Error when listening for incoming object.", e);
                    break;
                }
            }
            disconnect();
        }

        public void write(Object obj) {
            try {
                mBtOOS.writeObject(obj);

            } catch (IOException e) {
                Log.e(TAG, "write: Error when sending object", e);
            }
        }

        void disconnect() {
            Log.i(TAG, "Closing bluetooth connected socket");
            running = false;
            try {
                mmBtSocket.close();
                mListener.onDisconnected();
                mConnected = false;
            } catch (IOException e) {
                Log.e(TAG, "disconnect: Error when closing connected socket", e);
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

    private class RssiListener extends BluetoothGattCallback {
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            mRssiTotal -= mRssiAverage[mRssiAverage.length - 1];
            for (int i = 0; i < mRssiAverage.length - 1; i++) {
                mRssiAverage[i+1] = mRssiAverage[i];
            }
            mRssiAverage[0] = rssi;
            mRssiTotal += rssi;

            // TODO: 2018-03-01 Calculate distance
        }
    }
}