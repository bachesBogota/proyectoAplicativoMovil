package udfjc.proyeto.proyectotesis;

import static procesamiento.Permiso.solicitarPermisosTodo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;


import java.util.Observable;
import java.util.Observer;

import apiManager.APIManejador;
import procesamiento.Permiso;

public class LogInA extends AppCompatActivity {

    Button btn_Ingresar;
    EditText usuarioET, contrasenaET;
    static ProgressBar cargaW;

    public static boolean emailValido(String target) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    public void cargarMainActivity(){
        Intent intent =  new Intent(this, MainActivity.class);
        startActivity(intent);
        this.finish();
    }

    public Observer observadorAutorizacion = new Observer() {
        @Override
        public void update(Observable o, Object valor) {
            if(valor!= null){
                Toast.makeText(LogInA.this, "Autorización válida", Toast.LENGTH_LONG).show();
                cargarMainActivity();

            }else{
                Toast.makeText(LogInA.this, "Autorización inválida", Toast.LENGTH_LONG).show();
                btn_Ingresar.setClickable(true);
            }
            LogInA.cargaW.setVisibility(View.INVISIBLE);

        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        btn_Ingresar = findViewById(R.id.boton_ingresar);
        usuarioET = findViewById(R.id.correo);
        contrasenaET = findViewById(R.id.clave);
        cargaW = findViewById(R.id.cargaWidget);

        solicitarPermisosTodo(LogInA.this);

        btn_Ingresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                if(!usuarioET.getText().toString().isEmpty() && !contrasenaET.getText().toString().isEmpty()) {
                    if (emailValido(usuarioET.getText().toString())) {
                        new APIManejador().autorizacionUsuario(LogInA.this, usuarioET.getText().toString(), contrasenaET.getText().toString());
                        cargaW.setVisibility(View.VISIBLE);
                        btn_Ingresar.setClickable(false);
                    } else {
                        Toast.makeText(LogInA.this, "Formato de correo invalido", Toast.LENGTH_LONG).show();
                    }
                }else{
                    Toast.makeText(LogInA.this, "Ambos campos deben ser ingresados", Toast.LENGTH_LONG).show();
                }
            }
        });

        MainActivity.autorizacionOb.addObserver(observadorAutorizacion);
    }
}