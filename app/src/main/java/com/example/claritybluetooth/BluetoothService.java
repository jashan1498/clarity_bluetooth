package com.example.claritybluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static com.example.claritybluetooth.Constants.STATE_CONNECTED;
import static com.example.claritybluetooth.Constants.STATE_CONNECTING;
import static com.example.claritybluetooth.Constants.STATE_CONNECTION_FAILED;
import static com.example.claritybluetooth.Constants.STATE_LISTEN;


public class BluetoothService {

    private static final String SERVER_NAME = "SERVER_CLARITY";
    private BluetoothServerSocket bluetoothServerSocket;
    private static final String TAG = "BLUETOOTH_THREAD";
    private BluetoothSocket bluetoothSocket;
    private final BluetoothAdapter bluetoothAdapter;
    public static final UUID MY_UUID = java.util.UUID.fromString("12b20df8-7335-11ea-bc55-0242ac130003");
    private BluetoothAcceptThread bluetoothAcceptThread;
    private BluetoothConnectThread connectThread;
    private BluetoothConnectedThread connectedThread;

    private final Handler mHandler;

    public BluetoothService(Handler handler) {
        // get bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
        start();
    }

    // this thread listens for the connection
    private class BluetoothAcceptThread extends Thread {

        BluetoothAcceptThread() {
            if (MY_UUID != null) {
                try {
                    // listen using listen Using Insecure Rfcomm With Service Record
                    bluetoothServerSocket = BluetoothAdapter.getDefaultAdapter().listenUsingInsecureRfcommWithServiceRecord(SERVER_NAME, MY_UUID);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void run() {
            super.run();

            BluetoothSocket socket = null;
            // obtain handler's message queue
            Message message = Message.obtain(mHandler);

            try {
                // set message argument to pass to handler callback
                message.arg1 = STATE_LISTEN;
                // start listening for connection
                socket = bluetoothServerSocket.accept();
                // connection success -- pass state to callback
                message.arg1 = STATE_CONNECTED;
            } catch (Exception e) {
                // connection failed
                message.arg1 = STATE_CONNECTION_FAILED;
            }
            if (socket != null) {
                connectedDevice(socket);
            }
            // send data to callback
            message.sendToTarget();
        }

        // used to cancel the connection
        void cancel() {
            Log.d(TAG, "cancel thread");
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                Log.d(TAG, "error while closing thread");
            }
        }
    }

    // this thread connects to the device
    private class BluetoothConnectThread extends Thread {

        BluetoothConnectThread(BluetoothDevice device, UUID uuid) {
            try {
                // create Rfcomm Socket To Service Record with another device
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            super.run();

            // cancel bluetooth discovery
            bluetoothAdapter.cancelDiscovery();
            // obtain handlers messaging queue
            Message message = Message.obtain(mHandler);
            try {
                // set state in message arguments
                message.arg1 = STATE_CONNECTING;
                // connect to other device
                bluetoothSocket.connect();
                // connection success --set state in message arguments
                message.arg1 = STATE_CONNECTED;
            } catch (IOException e) {
                try {
                    message.arg1 = STATE_CONNECTION_FAILED;
                    // close socket in case of error
                    bluetoothSocket.close();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
            // send message to handler callback
            message.sendToTarget();
            connectedDevice(bluetoothSocket);
        }
    }

    // this thread responsible for sending messages back and fourth
    private class BluetoothConnectedThread extends Thread {

        private BluetoothSocket connectedSocket;
        private InputStream inputStream;
        private OutputStream outputStream;

        BluetoothConnectedThread(BluetoothSocket socket) {
            this.connectedSocket = socket;
            inputStream = null;
            outputStream = null;

            try {
                // get input stream
                inputStream = connectedSocket.getInputStream();
                // get output stream
                outputStream = connectedSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            super.run();

            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    Message message = Message.obtain(mHandler);
                    // read input stream
                    bytes = inputStream.read(buffer);
                    // set the read information as message argument which will be  passed to handler callback to handle
                    message.arg1 = bytes;
                    // verify information received and pass to callback
                    message.arg2 = VerificationHelper.Companion.verifyInfo(buffer, bytes);
                    // send data to handler
                    message.sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        void write(byte[] bytes) {
            try {
                // write to output stream
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // used to cancel the connection
        public void cancel() {
            Log.d(TAG, "cancel thread");
            try {
                // close socket
                connectedSocket.close();
            } catch (IOException e) {
                Log.d(TAG, "error while closing thread");
            }
        }
    }

    // handle connection to the device after first connection is established
    private void connectedDevice(BluetoothSocket bluetoothSocket) {
        // initialise bluetooth connect thread
        connectedThread = new BluetoothConnectedThread(bluetoothSocket);
        connectedThread.start();
    }

    public synchronized void start() {
        if (connectThread != null) {
            // if thread already exists -- cancel it
            connectThread.interrupt();
            connectThread = null;
        }
        if (bluetoothAcceptThread == null) {
            // initialise bluetooth accept thread
            bluetoothAcceptThread = new BluetoothAcceptThread();
            bluetoothAcceptThread.start();
        }
    }

    public void write(byte[] bytes) {
        if (connectedThread != null) {
            connectedThread.write(bytes);
        }
    }

    public void startClient(BluetoothDevice device, UUID uuid) {
        connectThread = new BluetoothConnectThread(device, uuid);
        connectThread.start();
    }
}


