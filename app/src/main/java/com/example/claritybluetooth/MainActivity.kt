package com.example.claritybluetooth

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.claritybluetooth.Constants.Companion.STATE_CONNECTED
import com.example.claritybluetooth.Constants.Companion.STATE_CONNECTED_TEXT
import com.example.claritybluetooth.Constants.Companion.STATE_CONNECTING
import com.example.claritybluetooth.Constants.Companion.STATE_CONNECTING_TEXT
import com.example.claritybluetooth.Constants.Companion.STATE_DATA_COMPLETE
import com.example.claritybluetooth.Constants.Companion.STATE_DATA_LOST
import com.example.claritybluetooth.Constants.Companion.STATE_LISTEN
import com.example.claritybluetooth.Constants.Companion.STATE_LISTEN_TEXT
import com.example.claritybluetooth.Constants.Companion.STATE_NONE
import com.example.claritybluetooth.Constants.Companion.STATE_NONE_TEXT
import com.example.claritybluetooth.VerificationHelper.Companion.getByteArray
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private var nearbyDevices: ArrayList<BluetoothDevice> = ArrayList()
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var currentPosition: Int = 0
    private lateinit var adapter: BluetoothRecyclerAdapter
    private lateinit var broadcastReceiver: BroadcastReceiver
    private lateinit var handler: Handler
    private lateinit var serviceHandler: Handler
    private lateinit var bluetoothService: BluetoothService
    private var isBluetoothEnabled = false

    companion object {
        const val REQUEST_CODE_BLUETOOTH = 1234
        const val PERMISSION_REQUEST_CODE = 1521
        const val TAG = "BLUETOOTH_DATA_TRANSFER"

    }

    // callback to handle messages
    private val handlerCallback: Handler.Callback = Handler.Callback {
        when (it.arg1) {
            STATE_NONE -> {
                status_text.text = STATE_NONE_TEXT
            }
            STATE_LISTEN -> {
                status_text.text = STATE_LISTEN_TEXT
                adapter.setState(currentPosition, STATE_LISTEN_TEXT)
            }
            STATE_CONNECTING -> {
                status_text.text = STATE_CONNECTING_TEXT
                adapter.setState(currentPosition, STATE_CONNECTING_TEXT)
            }
            STATE_CONNECTED -> {
                status_text.text = STATE_CONNECTED_TEXT
                adapter.setState(currentPosition, STATE_CONNECTED_TEXT)
                redirect()
            }

        }
        when (it.arg2) {
            STATE_DATA_COMPLETE -> {
                Log.d(TAG, "received")
//                Toast.makeText(this, "data successfully received", Toast.LENGTH_SHORT).show()
            }
            STATE_DATA_LOST -> {
//                Toast.makeText(this, "data lost", Toast.LENGTH_SHORT).show()
            }
        }
        false
    }

    private fun redirect() {
        Toast.makeText(this, "connected", Toast.LENGTH_LONG).show()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        checkBluetoothStatus()
        handler = Handler(handlerCallback)
        bluetoothService = BluetoothService(handler)

        serviceHandler = Handler()
        val runnable: Runnable = object : Runnable {
            override fun run() {
                send("hello")
                serviceHandler.postDelayed(this, 30)
            }
        }

        start_sending.setOnClickListener {
            serviceHandler.postDelayed(runnable, 30)
            start_sending.isEnabled = false
        }

        stop_sending.setOnClickListener {
            serviceHandler.removeCallbacks(runnable)
            start_sending.isEnabled = true
        }

        blue_switch.setOnCheckedChangeListener { _, isChecked -> toggle(isChecked) }
        discoverable_btn.setOnClickListener { setDiscoverable() }
        listen_btn.setOnClickListener { listen() }
        initialSettings()
    }

    // disable bluetooth
    private fun disableBluetooth() {
        bluetoothAdapter.disable()
        disableBluetoothView()
        nearbyDevices.clear()
        adapter.setData(nearbyDevices)
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE
            )
        }
    }

    // enable bluetooth
    private fun enableBluetooth() {
        bluetoothAdapter.let {
            it.isEnabled.run {
                startActivityForResult(
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    REQUEST_CODE_BLUETOOTH
                )
            }
        }
    }

    private fun checkBluetoothStatus() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        isBluetoothEnabled = bluetoothAdapter.isEnabled

        if (isBluetoothEnabled)
            enableBluetooth()
        else
            disableBluetoothView()
    }

    // handle intent result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_BLUETOOTH && resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Bluetooth Started", Toast.LENGTH_LONG).show()
            enableBluetoothView()
            listNearbyDevices()
        } else {
            Toast.makeText(this, "Bluetooth is Important for application to work", Toast.LENGTH_LONG).show()
            enableBluetooth()
        }
        if (PERMISSION_REQUEST_CODE == requestCode) {
            if (PackageManager.PERMISSION_GRANTED != resultCode)
                requestLocationPermission()
        }
    }

    private fun listNearbyDevices() {
        bluetoothAdapter.startDiscovery()
        nearbyDevices.clear()
    }

    // setup recycler view with adapter and broadcast receiver
    private fun initialSettings() {
        var bluetoothDevice: BluetoothDevice? = null
        adapter = BluetoothRecyclerAdapter(nearbyDevices)
        devices_recycler_view.adapter = adapter
        devices_recycler_view.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        adapter.setOnDeviceClickListener { device, position ->
            currentPosition = position
            connectDevice(device!!)
        }

        // initialise broadcast receiver and register it
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (BluetoothDevice.ACTION_FOUND == intent?.action)
                    bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                bluetoothDevice?.let {
                    if (!nearbyDevices.contains(it))
                        nearbyDevices.add(it)
                }
                adapter.setData(nearbyDevices)
                if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == intent?.action) {
                    val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                    Log.d("bonding", " $state")
                }
            }
        }

        val intentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        registerReceiver(broadcastReceiver, intentFilter)
    }

    private fun connectDevice(device: BluetoothDevice) {
        bluetoothAdapter.cancelDiscovery()
        if (device.bondState == BluetoothDevice.BOND_BONDED) {
            bluetoothService.startClient(device, BluetoothService.MY_UUID)
        } else if (device.bondState == BluetoothDevice.BOND_NONE) {
            if (device.createBond()) {
                bluetoothService.startClient(device, BluetoothService.MY_UUID)
            }
        }
    }

    fun send(string: String) {
        val bytes = getByteArray(string)
        bluetoothService.write(bytes)
    }

    private fun listen() {
        bluetoothService.start()
        listen_btn.isEnabled = false
    }

    // toggle bluetooth connection
    private fun toggle(checked: Boolean) {

        if (checked) {
            enableBluetooth()
            requestLocationPermission()
        } else {
            disableBluetooth()
        }
    }

    // enable bluetooth views
    private fun enableBluetoothView() {
        // enabled
        discoverable_btn.isEnabled = true
        toolbar_parent2.setBackgroundColor(resources.getColor(R.color.colorAccent, null))
        blue_tooth_status_text.text = resources.getString(R.string.bluetooth_enabled)
        blue_switch.isChecked = true
    }

    // disable bluetooth views
    private fun disableBluetoothView() {
        discoverable_btn.isEnabled = false
        toolbar_parent2.setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
        blue_tooth_status_text.text = resources.getString(R.string.enable_bluetooth)
        blue_switch.isChecked = false
    }

    // set bluetooth discoverable
    private fun setDiscoverable() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 400)
        startActivity(intent)
    }

    // unregister receiver
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }
}
