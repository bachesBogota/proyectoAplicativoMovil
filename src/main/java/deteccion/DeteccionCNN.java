package deteccion;

import static udfjc.proyeto.proyectotesis.MainActivity.autorizacionOb;
import static udfjc.proyeto.proyectotesis.MainActivity.comenzarYolo;
import static udfjc.proyeto.proyectotesis.MainActivity.direccion;
import static udfjc.proyeto.proyectotesis.MainActivity.esperarADetectar;
import static udfjc.proyeto.proyectotesis.MainActivity.primeraVezYolo;
import static udfjc.proyeto.proyectotesis.MainActivity.tiempoTranscurrido;
import static udfjc.proyeto.proyectotesis.MainActivity.ubicacionActual;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect2d;
import org.opencv.core.Point;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import apiManager.APIManejador;
import procesamiento.Bache;
import udfjc.proyeto.proyectotesis.MainActivity;

public class DeteccionCNN {

    public static Net red;
    public static String cfg = "yolo4_t.cfg";
    public static String weights = "yolov4_t.weights";
    public static List<String> clasesNombres = Arrays.asList("Bache");
    public static Size size = new Size(416, 416);
    public static int intervalo = 15;
    public static float confianzaUmbral = 0.8f;


    private static String cargarArchivos(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            Log.i("ML", "Failed to upload a file");
        }
        return "";
    }

    public static Mat procesarFrame(Mat frame, AppCompatActivity app) {

        //Convierte a RGB
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);
        //Obtiene blob
        Mat blobImagen = Dnn.blobFromImage(frame, 1.0 / 255.0, size, new Scalar(0, 0, 0), true, false);

        //Define el input de la imagen
        red.setInput(blobImagen);

        //Resultados y capas de salida
        java.util.List<Mat> resultado = new java.util.ArrayList<Mat>(2);
        List<String> nombresCapas = red.getUnconnectedOutLayersNames();

        //Procesa la imagen
        red.forward(resultado, nombresCapas);

        List<Integer> clasesIds = new ArrayList<>();
        List<Float> confianzas = new ArrayList<>();
        List<Rect2d> rectangulos = new ArrayList<>();


        for (int i = 0; i < resultado.size(); i++) {
            //Iteramos por los resultados
            Mat nivel = resultado.get(i);

            for (int j = 0; j < nivel.rows(); j++) {
                Mat fila = nivel.row(j);
                //Obtenemos los puntajes
                Mat puntajes = fila.colRange(5, nivel.cols());

                //Hallamos la ubicación del de mayor puntaje
                Core.MinMaxLocResult mmUbicacion = Core.minMaxLoc(puntajes);

                float confianza = (float) mmUbicacion.maxVal;

                //Log.i("Modelo", "Confianza " + confianza);

                Point claseIdPoint = mmUbicacion.maxLoc;

                //Si pasa el umbral de confianza
                if (confianza > confianzaUmbral) {
                    //Calcula cuadrado y lo anade
                    int centroX = (int) (fila.get(0, 0)[0] * frame.cols());
                    int centroY = (int) (fila.get(0, 1)[0] * frame.rows());
                    int ancho = (int) (fila.get(0, 2)[0] * frame.cols());
                    int alto = (int) (fila.get(0, 3)[0] * frame.rows());


                    int izquierda = centroX - ancho / 2;
                    int arriba = centroY - alto / 2;

                    //Log.i("Modelo", "Izquierda arriba es " + String.valueOf(izquierda) + " " + String.valueOf(arriba));

                    clasesIds.add((int) claseIdPoint.x);
                    confianzas.add((float) confianza);
                    rectangulos.add(new Rect2d(izquierda, arriba, ancho, alto));
                }
            }
        }
        int bachesEncontradosCant = confianzas.size();

        // Si encontro algun bache
        if (bachesEncontradosCant >= 1) {
            //Generacion del reporte de nuevo bache -----------------------------------------------
            Bache nuevoBache = new Bache(ubicacionActual.getLatitude(), ubicacionActual.getLongitude(), ubicacionActual.getAccuracy(), direccion, "CNN", "WGS-84", autorizacionOb.getUsuario());
            new APIManejador().enviarBache(app, nuevoBache);
            app.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(app, "Bache Detectado CNN", Toast.LENGTH_LONG).show();
                }
            });


            //Espera X segundos y comienza a detectar baches de nuevo
            tiempoTranscurrido = 0;
            Log.i("Tiempo", "Tiempo de espera iniciado");
            esperarADetectar();
            //-------------------------------------------------------------------------------------

            //Continua con el procesamiento para la generacion de la imagen devuelta
            float umbralSupresion = 0.2f;

            MatOfFloat confianzasOb = new MatOfFloat(Converters.vector_float_to_Mat(confianzas));


            Rect2d[] arregloCajas = rectangulos.toArray(new Rect2d[0]);
            MatOfRect2d cajas = new MatOfRect2d();
            MatOfInt indices = new MatOfInt();
            cajas.fromList(rectangulos);

            //Realiza supresion de las cajas
            Dnn.NMSBoxes(cajas, confianzasOb, confianzaUmbral, umbralSupresion, indices);


            int[] ind = indices.toArray();

            //Anade cada una de las cajas resultantes a la imagen
            for (int i = 0; i < ind.length; ++i) {
                int indiceActual = ind[i];
                Rect2d caja = arregloCajas[indiceActual];
                int idClase = clasesIds.get(indiceActual);
                float conf = confianzas.get(indiceActual);
                int confianzaResultante = (int) (conf * 100);

                Imgproc.putText(frame, clasesNombres.get(idClase) + " " + confianzaResultante + "%", caja.tl(), Imgproc.FONT_HERSHEY_SIMPLEX, 2, new Scalar(255, 255, 0), 2);
                Imgproc.rectangle(frame, caja.tl(), caja.br(), new Scalar(255, 0, 0), 2);
            }
        }


        return frame;
    }

    public static void gestionarModelo(AppCompatActivity app) {
        if (!comenzarYolo) {
            if (!primeraVezYolo) {
                String modelConfiguration = cargarArchivos(cfg, app);
                String modelWeights = cargarArchivos(weights, app);
                red = Dnn.readNetFromDarknet(modelConfiguration, modelWeights);


                primeraVezYolo = true;
            }
        }
        comenzarYolo = !comenzarYolo;
    }
}
