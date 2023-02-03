package procesamiento;

import android.Manifest;
import android.os.Build;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import udfjc.proyeto.proyectotesis.MainActivity;

public class Permiso {

    public static void permisosNoGarantizados(AppCompatActivity app){
        Toast.makeText(app, "Esta aplicación requiere de los permisos mencionados para poder ejecutarse correctamente", Toast.LENGTH_LONG).show();
        app.finish();
    }

    public static void permisosGarantizados(AppCompatActivity app){
        Toast.makeText(app, "Permisos concedidos", Toast.LENGTH_LONG).show();
    }

    public static void solicitarPermisosTodo(AppCompatActivity app) {
        app.requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }
}
