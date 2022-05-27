package com.example.ota;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ota.service.BLEService;
import com.example.ota.service.OtapController;
import com.example.ota.utils.BLEAttributes;
import com.example.ota.utils.BLEStateEvent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends BaseServiceActivity implements OtapController.SendChunkCallback {

    private static final String TAG = "Main Activity";
    private TextView text;
    private Button button;



    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt=null;
    private BluetoothLeScanner mBluetoothLeScanner;


    private Timer m_waitNewRequesTimer;
    private TimerTask m_waitNewRequestTimerTask;

    Context context;

    private String mac_addr="11:22:33:44:55:14";
    private String file_location="/storage/emulated/0/Documents/data.srec";

    int complete;
    int error;
    int percent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setmDeviceAddress(mac_addr);
        super.toggleState(true);
        setContentView(R.layout.activity_main);
        check_permissions();
        text=findViewById(R.id.text);
        button=findViewById(R.id.button);

        Timer timer = new Timer();

        button.setOnClickListener(new View.OnClickListener() {
           @Override
          public void onClick(View v) {
            send_file(mac_addr, file_location);
            timer.schedule(new TimerTask() {
                   @Override
                   public void run() {
                       runOnUiThread(new Runnable() {
                           @Override
                           public void run() {
                               if(get_status() == -1)
                                   text.setText("Disconnected");
                               if(get_status() == -2)
                                   text.setText("Connecting");
                               if(get_status() == 100)
                                   text.setText("Done");
                               if(get_status() >=0&&get_status()<100)
                                   text.setText("In progress: "+get_status()+"%");
                               if(get_status() <= -3)
                                   text.setText("Error");
                           }
                       });
                       if(get_status()<=-3)
                           BLEService.INSTANCE.disconnect();
                   }
               }, 0, 200);
         }
        });

        context=getApplicationContext();
        final BluetoothManager bluetoothManager =
        (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();



    }

    int get_status(){
        if(BLEService.INSTANCE.getConnectionState()==BLEService.State.STATE_DISCONNECTED)
            return -1;
        if(BLEService.INSTANCE.getConnectionState()==BLEService.State.STATE_CONNECTING)
            return -2;
        if(BLEService.INSTANCE.getConnectionState()==BLEService.State.STATE_CONNECTED) {
            if(error!=0)
                return -3;
            if(complete==1)
                return 100;
            else
                return percent;
        }
        return -4;
    }

    void send_file(String mac, String file){
        BLEService.INSTANCE.connect(mac, false);
        //wait for connection

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
               if(BLEService.INSTANCE.getConnectionState()==BLEService.State.STATE_DISCONNECTED){
                    timer.cancel();
                }else if(BLEService.INSTANCE.getConnectionState() == BLEService.State.STATE_CONNECTED){


                    File f = new File(file);
                    Uri fileUri = Uri.fromFile(f);
                    //File newImgFile = f;
                    try {
                        File newImgFile = OtapController.getInstance().readSrecToCreateImg(context, fileUri);
                        Uri imgUri = Uri.fromFile(newImgFile);
                        OtapController.ImageInfo imageInfo = OtapController.getInstance().getNewImgImageInfo(context, imgUri);
                        //
                        OtapController.getInstance().setNewImageInfo(imageInfo);
                        OtapController.getInstance().setSendChunkCallback(MainActivity.this);
                        OtapController.getInstance().sendNewImageInfoResponse();
                        Log.e("Main", "Done in main");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                   timer.cancel();
                }

            }
        }, 0, 100);





    }




    void check_permissions(){
        if (
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int a = 0;
                requestPermissions( new String[] {
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                                Manifest.permission.BLUETOOTH,
                                Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                        },
                        a);
            }
        }
    }



    @Override
    public void onEventMainThread(BLEStateEvent.DataAvailable e) {
        super.onEventMainThread(e);
        if (e == null) return;
        BluetoothGattCharacteristic gattCharacteristic = e.characteristic;
        final String charaterUuid = gattCharacteristic.getUuid().toString();
//        Log.d(TAG, "uuid = " + gattCharacteristic.getUuid().toString());
        if (BLEAttributes.OTAP_CONTROL_POINT_CHARACTERISTIC.equals(charaterUuid)) {
            final byte[] data = gattCharacteristic.getValue();
            if (null != data && 0 < data.length) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                Log.d(TAG, "hang ve " + stringBuilder.toString());
                switch (data[0]) {
                    case OtapController.CMD_ID_NEW_IMAGE_INFO_REQUEST:
                        error=0;
                        complete=0;
//                        Toast.makeText(this, "hang ve" + stringBuilder.toString(), Toast.LENGTH_SHORT).show();
                        OtapController.getInstance().handleNewImageInfoRequest(data);
                        break;
                    case OtapController.CMD_ID_IMAGE_BLOCK_REQUEST:
//                        Toast.makeText(this, "hang nu" + stringBuilder.toString(), Toast.LENGTH_SHORT).show();
                        boolean request = OtapController.getInstance().handleImageBlockRequest(data);
                        // send chunk data
                        if (request) {
                            // cancel waiting
                            if (null != m_waitNewRequestTimerTask) m_waitNewRequestTimerTask.cancel();
                            if (null != m_waitNewRequesTimer) m_waitNewRequesTimer.cancel();
                            // send image
                            new SendImageTask().execute();
                        }
                        break;
                    case OtapController.CMD_ID_IMAGE_TRANSFER_COMPLETE:
                        complete=1;
                        OtapController.getInstance().handleImageTransferComplete(data);
                        break;
                    case OtapController.CMD_ID_STOP_IMAGE_TRANSFER:
                        OtapController.getInstance().handleStopImageTransfer(data);
                        break;
                    case OtapController.CMD_ID_ERROR:
                        error=1;
                        OtapController.getInstance().handleErrorNotification(data);
                        break;
                    case OtapController.CMD_ID_IMAGE_CHUNK:
                        Log.d(TAG, "received chunk data " + stringBuilder.toString());
                        break;
                    case OtapController.CMD_ID_NEW_IMAGE_INFO_RESPONSE:
                        Log.d(TAG, "received NEW_IMAGE_INFO_RESPONSE " + stringBuilder.toString());
                        OtapController.getInstance().sendDummyImageChunk();
                        break;
                    default:
//                        Toast.makeText(this, "hang ve" + stringBuilder.toString(), Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    }

    @Override
    public void onEventMainThread(BLEStateEvent.Disconnected e) {
        super.onEventMainThread(e);
        OtapController.getInstance().interruptSending();
//        makeText_otapBtOpen.setEnabled(false);
//        bTouchAblele = false;
//        m_otapBtUpload.setEnabled(false);
    }

    @Override
    public void onEvent(BLEStateEvent.ServiceDiscovered e) {
        super.onEvent(e);
        OtapController.getInstance().resetInterrupt();
        BLEService.INSTANCE.request(BLEAttributes.OTAP, BLEAttributes.OTAP_CONTROL,
                BLEService.Request.INDICATE);
    }

    public void onEventMainThread(BLEStateEvent.ServiceDiscovered e) {
//        m_otapBtOpen.setEnabled(!OtapController.getInstance().isSendingInProgress());
//        bTouchAble = m_otapBtOpen.isEnabled();
        if (null != OtapController.getInstance().getNewImageInfo()) {
//            m_otapBtUpload.setEnabled(true);
//            m_otapBtUpload.setText(R.string.upload);
        }
    }

    @Override
    public void onStopSending(boolean sentDone) {
        // if sent done (received cmdId 06) -> back to Device scan screen
        if (sentDone) {
//            AlertDialog.Builder builder = new AlertDialog.Builder(OtapActivity.this);
//            builder.setMessage(R.string.megSendBinaryDone);
//            builder.setCancelable(false);
//            builder.setPositiveButton(R.string.btOk, new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    finish();
//                }
//            });
//            builder.create().show();
            Log.e("Main", "Done");
            complete=1;
        } else {
            waitingNewRequestAfterError();
        }

    }

    /**
     * waiting a new request after has an error
     */
    private void waitingNewRequestAfterError() {
        if (null != m_waitNewRequestTimerTask) m_waitNewRequestTimerTask.cancel();
        if (null != m_waitNewRequesTimer) m_waitNewRequesTimer.cancel();
        m_waitNewRequesTimer = new Timer("WaitNewRequestTimerTask", false);
        m_waitNewRequestTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (MainActivity.this.isFinishing()) return;
                if (!OtapController.getInstance().hasAnError()) return;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        error=1;
                        Toast.makeText(MainActivity.this, "Has an ERROR without a new request", Toast.LENGTH_SHORT).show();
                        toggleState(false);
                    }
                });
            }
        };
        m_waitNewRequesTimer.schedule(m_waitNewRequestTimerTask, 3000);
    }

    @Override
    public void onSentTrunk(long startPosition, long endPosition, int frameNoInBlock, int blockSize) {
        OtapController.ImageInfo imageInfo = OtapController.getInstance().getNewImageInfo();
        if (null != imageInfo) {
            double fileSize = imageInfo.getFileSizeInKb();
            double aux_percent = (endPosition * 100) / (fileSize * 1024);
            Log.d(TAG, "percent = " + aux_percent);
             percent = (int) Math.round(aux_percent);
//            m_otapPbUpload.setProgress(nPercent);
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    tvProgressNumber.setText(nPercent + "%");
//                }
//            });
        }else
            Log.e("aaa", "img is null");
    }

    private class SendImageTask extends AsyncTask<Objects, Objects, Objects> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Objects doInBackground(Objects... params) {
            if (isFinishing()) {
                return null;
            }
            try {
                OtapController.getInstance().sendImgImageChunk(MainActivity.this, 50);
            } catch (IOException ex) {
                error=1;
                Log.e(TAG, "ERROR ", ex);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Objects objects) {
            super.onPostExecute(objects);
        }
    }

}