package udfjc.proyeto.proyectotesis;

import static deteccion.DeteccionAcelerometro.validarAcelerometro;
import static deteccion.DeteccionCNN.confianzaUmbral;
import static deteccion.DeteccionCNN.intervalo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.List;

import deteccion.DeteccionCNN;
import observer.ObservableAutorizacion;
import apiManager.APIManejador;
import procesamiento.Bache;
import procesamiento.Permiso;

public class MainActivity extends AppCompatActivity implements SensorEventListener, CameraBridgeViewBase.CvCameraViewListener2 {

    //GPS
    public static SensorManager sensorManager;
    public static final int INTERVALO_GPS = 1000;
    private static FusedLocationProviderClient fusedLocationProviderClient;
    public static Location ubicacionActual;
    private static LocationRequest locationRequest;
    private static LocationCallback locationCallBack;
    static List<Address> direcciones = null;
    public static String direccion = "No disponible";


    //Permisos
    public static final int PERMISSIONS_FINE_LOCATION = 99;
    public static ObservableAutorizacion autorizacionOb = new ObservableAutorizacion();

    //Interfaz
    Button btn_anadirHueco;
    TextView tv_lat, tv_lon, tv_altitud, tv_precision, tv_velocidad, tv_gpsModo, tv_direccion,
            tv_accX, tv_accY, tv_accZ;
    Switch sw_gps, sw_acelerometro, sw_camara;

    TextView ti_ponderacionAX, ti_ponderacionAY, ti_ponderacionAZ, ti_pendiente, ti_ordenada, ti_velocidadTemp,
            ti_confianza, ti_intervalo;



    //Acelerometro
    public static Integer contadorAceleraciones = 0;
    public static Float[] ultimas5Amplitudes;
    public static Float promedio5Amplitudes = Float.parseFloat(String.valueOf(0.0));
    public static Float pendiente, ordenada, velocidadTemporal;
    public static Float ponderacionAX, ponderacionAY, ponderacionAZ;
    //Variable para esperar X segundos entre baches por acelerometro
    public static int tiempoTranscurrido = 3;
    public static int tiempoEspera = 3;
    public static boolean detectarConAcelerometro = false;

    //CNN y Camara
    public static CameraBridgeViewBase cameraBridgeViewBase;
    public static BaseLoaderCallback baseLoaderCallback;
    public static int contadorFrame = 0;
    public static boolean comenzarYolo = false;
    public static boolean primeraVezYolo = false;
    //Para determinar cuando se esta procesando un frame actualmente;
    public static boolean disponible = true;


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.i("Permisos", String.valueOf(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) + " " +
                    String.valueOf(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) + " " +
                    String.valueOf(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)) + " " +
                    String.valueOf(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) + " " +
                    String.valueOf(PackageManager.PERMISSION_GRANTED));
            Permiso.permisosNoGarantizados(MainActivity.this);
        }

        //Cargando referencias
        btn_anadirHueco = findViewById(R.id.botonNuevoHueco);
        tv_lat = findViewById(R.id.tv_latitud);
        tv_lon = findViewById(R.id.tv_longitud);
        tv_altitud = findViewById(R.id.tv_altitud);
        tv_precision = findViewById(R.id.tv_precision);
        tv_velocidad = findViewById(R.id.tv_velocidad);
        tv_gpsModo = findViewById(R.id.tv_gpsModo);
        tv_direccion = findViewById(R.id.tv_direccion);
        tv_accX = findViewById(R.id.tv_aceleracionX);
        tv_accY = findViewById(R.id.tv_aceleracionY);
        tv_accZ = findViewById(R.id.tv_aceleracionZ);
        sw_acelerometro = findViewById(R.id.sw_acelerometro);
        sw_camara = findViewById(R.id.sw_camara);
        sw_gps = findViewById(R.id.sw_gps);
        ti_ponderacionAX = findViewById(R.id.tinput_ax);
        ti_ponderacionAY = findViewById(R.id.tinput_ay);
        ti_ponderacionAZ = findViewById(R.id.tinput_az);
        ti_pendiente = findViewById(R.id.tinput_pendiente);
        ti_ordenada = findViewById(R.id.tinput_ordenada);
        ti_velocidadTemp = findViewById(R.id.tinput_velocidadT);
        ti_confianza = findViewById(R.id.tinput_confianza);
        ti_intervalo = findViewById(R.id.tinput_intervalo);

        //Propiedades del gps
        locationRequest = new LocationRequest();
        locationRequest.setInterval(INTERVALO_GPS); // Modo sin max precision
        locationRequest.setFastestInterval(INTERVALO_GPS); // Modo max precision
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //Propiedades acelerometro
        pendiente = Float.parseFloat(String.valueOf(0.1));
        ordenada = Float.parseFloat(String.valueOf(11));
        velocidadTemporal = Float.parseFloat(String.valueOf(0));
        ultimas5Amplitudes = new Float[]{(float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0};
        ponderacionAX = (float) 1.0;
        ponderacionAY = (float) 1.0;
        ponderacionAZ = (float) 1.0;

        //Valores por defecto
        ti_ponderacionAX.setText(String.valueOf(ponderacionAX));
        ti_ponderacionAY.setText(String.valueOf(ponderacionAY));
        ti_ponderacionAZ.setText(String.valueOf(ponderacionAZ));
        ti_pendiente.setText(String.valueOf(pendiente));
        ti_ordenada.setText(String.valueOf(ordenada));
        ti_velocidadTemp.setText(String.valueOf(velocidadTemporal));
        ti_confianza.setText(String.valueOf(confianzaUmbral));
        ti_intervalo.setText(String.valueOf(intervalo));

        //Campos de entrada texto ------------------------------------------------------
        ti_ponderacionAX.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    ponderacionAX = Float.parseFloat(ti_ponderacionAX.getText().toString());
                    Toast.makeText(MainActivity.this, "ponderacion aceleración eje X actualizada a: " + String.valueOf(ponderacionAX), Toast.LENGTH_LONG).show();
                }
            }
        });

        ti_ponderacionAY.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    ponderacionAY = Float.parseFloat(ti_ponderacionAY.getText().toString());
                    Toast.makeText(MainActivity.this, "ponderacion aceleración eje Y actualizada a: " + String.valueOf(ponderacionAY), Toast.LENGTH_LONG).show();
                }
            }
        });

        ti_ponderacionAZ.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    ponderacionAZ = Float.parseFloat(ti_ponderacionAZ.getText().toString());
                    Toast.makeText(MainActivity.this, "ponderacion aceleración eje Z actualizada a: " + String.valueOf(ponderacionAZ), Toast.LENGTH_LONG).show();
                }
            }
        });

        ti_pendiente.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    pendiente = Float.parseFloat(ti_pendiente.getText().toString());
                    Toast.makeText(MainActivity.this, "Pendiente actualizada a: " + String.valueOf(pendiente), Toast.LENGTH_LONG).show();
                }
            }
        });

        ti_ordenada.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    ordenada = Float.parseFloat(ti_ordenada.getText().toString());
                    Toast.makeText(MainActivity.this, "Ordenada actualizada a: " + String.valueOf(ordenada), Toast.LENGTH_LONG).show();
                }
            }
        });

        ti_velocidadTemp.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    velocidadTemporal = Float.parseFloat(ti_velocidadTemp.getText().toString());
                    Toast.makeText(MainActivity.this, "Velocidad temporal actualizada a: " + String.valueOf(velocidadTemporal), Toast.LENGTH_LONG).show();
                }
            }
        });

        ti_confianza.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    confianzaUmbral = Float.parseFloat(ti_confianza.getText().toString());
                    Toast.makeText(MainActivity.this, "Confianza actualizada a: " + String.valueOf(confianzaUmbral), Toast.LENGTH_LONG).show();
                }
            }
        });

        ti_intervalo.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    intervalo = Integer.parseInt(ti_intervalo.getText().toString());
                    Toast.makeText(MainActivity.this, "Intervalo actualizado a: " + String.valueOf(intervalo), Toast.LENGTH_LONG).show();
                }
            }
        });

        // -----------------------------------------------


        //Cuando el intervalo dispare un evento
        locationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);


                ubicacionActual = locationResult.getLastLocation();
                //Toast.makeText(MainActivity.this,"Se actualizo", Toast.LENGTH_LONG).show();
                actualizarUI(ubicacionActual);
            }
        };

        //Configuracion switches
        sw_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw_gps.isChecked()) {
                    // Modo de alta precision
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    tv_gpsModo.setText("Usando GPS");
                } else {
                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    tv_gpsModo.setText("Usando torres cercanas + red");
                }
            }
        });

        //Acelerometro
        sw_acelerometro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw_acelerometro.isChecked()) {
                    detectarConAcelerometro = true;
                } else {
                    detectarConAcelerometro = false;
                }
            }
        });

        sw_camara.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeteccionCNN.gestionarModelo(MainActivity.this);
            }
        });

        //Configuracion botones
        btn_anadirHueco.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bache nuevoBache = new Bache(ubicacionActual.getLatitude(), ubicacionActual.getLongitude(), ubicacionActual.getAccuracy(), direccion, "Manual", "WGS-84", autorizacionOb.getUsuario());
                new APIManejador().enviarBache(MainActivity.this, nuevoBache);

            }
        });


        //Configura el manager del sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 200000, null);//SensorManager.SENSOR_DELAY_NORMAL);


        //Inicia el gps y solicita permisos
        actualizarGPS();

        cameraBridgeViewBase = (JavaCameraView) findViewById(R.id.camara);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);
        cameraBridgeViewBase.setCameraPermissionGranted();

        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);
                switch (status) {
                    case BaseLoaderCallback.SUCCESS:
                        cameraBridgeViewBase.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };


    }

    protected void obtenerDireccion(Geocoder geo, Location ub) {
        //De manera asincronca obtiene la direccion aproximada
        new AsyncTask<String, String, String>() {

            @Override
            protected void onPreExecute() {
            }

            @Override
            protected String doInBackground(String... params) {
                try {
                    direcciones = geo.getFromLocation(ub.getLatitude(), ub.getLongitude(), 1);
                    direccion = direcciones.get(0).getAddressLine(0);
                } catch (IOException e) {
                }

                return null;
            }

            /**
             * Update list ui after process finished.
             */
            protected void onPostExecute(String result) {
                if (direccion != null) {
                    tv_direccion.setText(direccion);
                } else {
                    tv_direccion.setText("No disponible");
                }

            }

        }.execute();

    }

    private void actualizarUI(Location ultimaUbicacion) {
        // Actualiza toda la interfaz grafica del gps

        tv_lat.setText(String.valueOf(ultimaUbicacion.getLatitude()));
        tv_lon.setText(String.valueOf(ultimaUbicacion.getLongitude()));
        tv_precision.setText(String.valueOf(ultimaUbicacion.getAccuracy()));

        if (ultimaUbicacion.hasAltitude()) {
            tv_altitud.setText(String.valueOf(ultimaUbicacion.getAltitude()));
        } else {
            tv_altitud.setText("No disponible");
        }

        if (ultimaUbicacion.hasSpeed()) {
            tv_velocidad.setText(String.valueOf(ultimaUbicacion.getSpeed()));
        } else {
            tv_velocidad.setText("No disponible");
        }

        Geocoder geocoder = new Geocoder(this);
        obtenerDireccion(geocoder, ultimaUbicacion);
    }

    public void actualizarGPS() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

        //Comprueba si se tiene permisos de ubicación
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //Permisos concedidos
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        actualizarUI(location);
                        ubicacionActual = location;
                    }
                }
            });
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //Cuando ocurre un cambio en los datos del acelerometro
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            tv_accX.setText(String.valueOf(event.values[0]));
            tv_accY.setText(String.valueOf(event.values[1]));
            tv_accZ.setText(String.valueOf(event.values[2]));


            Double promedio = Math.sqrt(Math.pow(event.values[0] * ponderacionAX, 2.0) + Math.pow(event.values[1] * ponderacionAY, 2.0) + Math.pow(event.values[2] * ponderacionAZ, 2.0));
            Float avg = Float.parseFloat(String.valueOf(promedio));
            ultimas5Amplitudes[contadorAceleraciones] = avg;
            contadorAceleraciones += 1;
            contadorAceleraciones = contadorAceleraciones % 5;
            promedio5Amplitudes = (ultimas5Amplitudes[0] + ultimas5Amplitudes[1] + ultimas5Amplitudes[2] + ultimas5Amplitudes[3] + ultimas5Amplitudes[4]) / 5;
            if (detectarConAcelerometro) {
                validarAcelerometro(MainActivity.this);
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //Espera para que no se dupliquen baches
    public static void esperarADetectar() {
        new AsyncTask<String, String, String>() {

            @Override
            protected void onPreExecute() {
            }

            @Override
            protected String doInBackground(String... params) {
                try {
                    Thread.sleep(tiempoEspera * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            /**
             * Update list ui after process finished.
             */
            protected void onPostExecute(String result) {
                Log.i("Tiempo", "Tiempo de espera finalizado");
                tiempoTranscurrido = tiempoEspera;
            }

        }.execute();
    }

    //Metodos camara
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //Cada vez que encuentra un frame
        Mat frame = inputFrame.rgba();

        if (comenzarYolo) {
            if (contadorFrame % intervalo != 0 || tiempoTranscurrido != tiempoEspera) {
                contadorFrame += 1;
                frame = null;
            } else if (contadorFrame % intervalo == 0) {
                disponible = false;
                contadorFrame += 1;
                frame = DeteccionCNN.procesarFrame(frame, MainActivity.this);
                disponible = true;
            }
        }

        return frame;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        if (comenzarYolo) {
            DeteccionCNN.gestionarModelo(MainActivity.this);
        }
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(getApplicationContext(), "Problema en resumen", Toast.LENGTH_LONG).show();
        } else {
            baseLoaderCallback.onManagerConnected(baseLoaderCallback.SUCCESS);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }
    }

}