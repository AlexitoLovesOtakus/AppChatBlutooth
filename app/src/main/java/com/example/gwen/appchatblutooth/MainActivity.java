package com.example.gwen.appchatblutooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {


    Button listen , send ,listDevices;
    ListView listView;
    TextView messages, status;
    EditText mensaje;

    static final int STATE_LISTENING=1;
    static final int STATE_CONNECTING=2;
    static final int STATE_CONNECTED=3;
    static final int STATE_CONECTION_FAILED=4;
    static final int STATE_MESSAGE_RECEIVED=5;

    int REQUEST_ENABLE_BLUTOOTH=1;

    private static final String APP_NAME="BTChat";
    private static final UUID MYUUID=UUID.fromString("dadcf2d7-a025-4997-9fe0-3b734c2c861a");



    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice[] btArray;
    SendReceive sendReceive;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listen =(Button)findViewById(R.id.buttonListen);
        send=(Button)findViewById(R.id.buttonSendMessage);
        listDevices=(Button)findViewById(R.id.buttonListDevices);
        listView=(ListView)findViewById(R.id.listView);
        messages=(TextView)findViewById((R.id.textViewMessages));
        status=(TextView)findViewById(R.id.textViewStatus);
        mensaje=(EditText)findViewById(R.id.editTextMensaje);

        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if(!bluetoothAdapter.isEnabled()){

            Intent enableIntent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BLUTOOTH);


        }
        implementListeners();

    }

    private void implementListeners() {

        listDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Set<BluetoothDevice> bt=bluetoothAdapter.getBondedDevices();
                String[] strings=new String[bt.size()];
                btArray=new BluetoothDevice[bt.size()];
                int index=0;

                if (bt.size()>0){

                    for (BluetoothDevice device:bt){

                        btArray[index]=device;
                        strings[index]=device.getName();
                        index++;
                    }

                    ArrayAdapter<String> arrayAdapter=new ArrayAdapter<>(getApplicationContext(),android.R.layout.simple_expandable_list_item_1,strings);
                    listView.setAdapter(arrayAdapter);

                }

            }
        });

        listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ServerClass serverClass=new ServerClass();
                serverClass.start();
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ClientClass clientClass=new ClientClass(btArray[i]);
                clientClass.start();

                status.setText("Conectando");


            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String srring=String.valueOf(mensaje.getText());
                sendReceive.write(srring.getBytes());
            }
        });

    }

    private class ServerClass extends Thread{


        private BluetoothServerSocket serverSocket;
        public ServerClass(){

            try {
                serverSocket=bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,MYUUID);
            }catch (IOException e){

                e.printStackTrace();
            }
        }

        public  void run(){


            BluetoothSocket socket=null;
            while (socket==null){

                try {
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTING;
                    handler.sendMessage(message);

                    socket=serverSocket.accept();

                }catch (IOException e){
                    e.printStackTrace();
                    Message message=Message.obtain();
                    message.what=STATE_CONECTION_FAILED;
                    handler.sendMessage(message);
                }

                if (socket!=null){

                    Message message=Message.obtain();
                    message.what=STATE_CONNECTED;
                    handler.sendMessage(message);

                    sendReceive=new SendReceive(socket);
                    sendReceive.start();
                    break;



                }
            }
        }
    }

    private class ClientClass extends Thread{

        private BluetoothDevice device;
        private BluetoothSocket socket;

        public  ClientClass(BluetoothDevice device1){

            device =device1;

            try {
                socket=device.createRfcommSocketToServiceRecord(MYUUID);
            } catch (IOException e) {
                e.printStackTrace();
            }


        }

        public  void  run(){

            try {
                socket.connect();
                Message message=Message.obtain();
                message.what=STATE_CONNECTED;
                handler.sendMessage(message);

                sendReceive=new SendReceive(socket);
                sendReceive.start();

            } catch (IOException e) {
                e.printStackTrace();
                Message message=Message.obtain();
                message.what=STATE_CONECTION_FAILED;
                handler.sendMessage(message);
            }



        }






    }


    private class SendReceive extends Thread{

        private final BluetoothSocket bluetoothSocket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public  SendReceive(BluetoothSocket socket){

            bluetoothSocket =socket;
            InputStream tempIn=null;
            OutputStream tempOut=null;

            try {
                tempIn=bluetoothSocket.getInputStream();
                tempOut=bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();

            }

            inputStream=tempIn;
            outputStream=tempOut;
            }

            public void run(){

            byte[]buffer=new byte[1024];
            int bytes;

            while (true){

                try {
                    bytes=inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
            }

            public void write(byte[]bytes){

                try {
                    outputStream.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }




    }

    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){

                case STATE_LISTENING:
                    status.setText("Listening");
                    break;

                case STATE_CONNECTING:
                        status.setText("Conectando");
                        break;
                case STATE_CONNECTED:
                    status.setText("Conectado");
                    break;
                case STATE_CONECTION_FAILED:
                    status.setText("Conexion Fallida");
                case STATE_MESSAGE_RECEIVED:
                    byte[]readBuff= (byte[]) msg.obj;
                    String tempMsg=new String(readBuff,0,msg.arg1);
                    messages.setText(tempMsg);

                    break;




            }
            return true;
        }
    });

}
