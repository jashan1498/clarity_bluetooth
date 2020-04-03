package com.example.claritybluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static android.bluetooth.BluetoothClass.Device.Major.COMPUTER;
import static android.bluetooth.BluetoothClass.Device.Major.PHONE;

public class BluetoothRecyclerAdapter extends RecyclerView.Adapter<BluetoothRecyclerAdapter.BluetoothViewHolder> {
    private ArrayList<BluetoothDevice> bluetoothDeviceArrayList;
    private DeviceOnClickListener deviceCLickListener;
    private Context context;
    private Integer position = -1;
    private String state = "";

    public BluetoothRecyclerAdapter(ArrayList<BluetoothDevice> bluetoothDeviceArrayList) {
        this.bluetoothDeviceArrayList = bluetoothDeviceArrayList;
    }

    public void setData(@NotNull ArrayList<BluetoothDevice> nearbyDevices) {
        this.bluetoothDeviceArrayList = nearbyDevices;
        notifyDataSetChanged();
    }

    public void setState(int currentPosition, @NotNull String state) {
        this.position = currentPosition;
        this.state = state;
        notifyDataSetChanged();

    }

    public interface DeviceOnClickListener {
        void onDeviceClick(BluetoothDevice device, Integer position);
    }

    void setOnDeviceClickListener(DeviceOnClickListener deviceCLickListener) {
        this.deviceCLickListener = deviceCLickListener;
    }

    @NonNull
    @Override
    public BluetoothViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bluetooth_device_item, parent, false);
            context = parent.getContext();
        return new BluetoothViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BluetoothViewHolder holder, final int position) {

        holder.macAddress.setText(bluetoothDeviceArrayList.get(position).getAddress());
        holder.deviceName.setText(bluetoothDeviceArrayList.get(position).getName());

        if (bluetoothDeviceArrayList.get(position).getBluetoothClass().equals(PHONE)) {
            holder.deviceImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_phone, null));
        } else if (bluetoothDeviceArrayList.get(position).getBluetoothClass().equals(COMPUTER)) {
            holder.deviceImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_computer, null));
        } else {
            holder.deviceImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_device_hub, null));
        }
        if (bluetoothDeviceArrayList.get(position).getName().isEmpty()) {
            holder.deviceName.setText(bluetoothDeviceArrayList.get(position).getAddress());
            holder.macAddress.setText("");
        }
        if (position == this.position && !state.isEmpty()) {
            holder.connectButton.setText(state);
        }

        holder.connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deviceCLickListener != null) {
                    deviceCLickListener.onDeviceClick(bluetoothDeviceArrayList.get(position), position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return bluetoothDeviceArrayList.size();
    }

    class BluetoothViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName, macAddress, connectButton;
        ImageView deviceImage;

        BluetoothViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceImage = itemView.findViewById(R.id.device_image);
            deviceName = itemView.findViewById(R.id.device_name);
            macAddress = itemView.findViewById(R.id.mac_address);
            connectButton = itemView.findViewById(R.id.connect_btn);
        }
    }
}
