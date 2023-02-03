package deteccion;

import static udfjc.proyeto.proyectotesis.MainActivity.autorizacionOb;
import static udfjc.proyeto.proyectotesis.MainActivity.direccion;
import static udfjc.proyeto.proyectotesis.MainActivity.esperarADetectar;
import static udfjc.proyeto.proyectotesis.MainActivity.ordenada;
import static udfjc.proyeto.proyectotesis.MainActivity.pendiente;
import static udfjc.proyeto.proyectotesis.MainActivity.promedio5Amplitudes;
import static udfjc.proyeto.proyectotesis.MainActivity.tiempoEspera;
import static udfjc.proyeto.proyectotesis.MainActivity.tiempoTranscurrido;
import static udfjc.proyeto.proyectotesis.MainActivity.ubicacionActual;
import static udfjc.proyeto.proyectotesis.MainActivity.ubicacionActual;
import static udfjc.proyeto.proyectotesis.MainActivity.velocidadTemporal;

import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import apiManager.APIManejador;
import procesamiento.Bache;
import udfjc.proyeto.proyectotesis.MainActivity;

public class DeteccionAcelerometro {



    //Determina si se encuentra un bache por medio del acelerometro
    public static void validarAcelerometro(AppCompatActivity app) {
        if (ubicacionActual != null && ubicacionActual.hasSpeed() && ubicacionActual.getSpeed() != 0.0) {
            float velocidadAUsar = 0;
            int casoVelocidad = 0;
            if(velocidadTemporal != 0){
                casoVelocidad = 1;
                velocidadAUsar = velocidadTemporal;
            }else{
                casoVelocidad = 2;
                velocidadAUsar = ubicacionActual.getSpeed();
            }
            if ((promedio5Amplitudes >= ((velocidadAUsar * pendiente) + ordenada)) && tiempoTranscurrido == tiempoEspera) {
                Bache nuevoBache = new Bache(ubicacionActual.getLatitude(), ubicacionActual.getLongitude(), ubicacionActual.getAccuracy(), direccion, "Acelerómetro", "WGS-84", autorizacionOb.getUsuario());
                new APIManejador().enviarBache(app, nuevoBache);
                if(casoVelocidad == 1){
                    Toast.makeText(app, "Bache Detectado Acelerómetro velocidad temporal", Toast.LENGTH_LONG).show();
                }else if(casoVelocidad == 2){
                    Toast.makeText(app, "Bache Detectado Acelerómetro velocidad original", Toast.LENGTH_LONG).show();
                }


                //Espera X segundos y comienza a detectar baches de nuevo
                tiempoTranscurrido = 0;
                Log.i("Tiempo", "Tiempo de espera iniciado");
                esperarADetectar();
            }
        }
    }
}


