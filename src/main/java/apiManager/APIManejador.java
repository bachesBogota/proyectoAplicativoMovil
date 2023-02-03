package apiManager;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import procesamiento.Bache;
import udfjc.proyeto.proyectotesis.MainActivity;

public class APIManejador {

    private static String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJjbGllbnRfaWQiOiJEZXYiLCJjbGllbnRfc2VjcmV0IjoiRGV2UGFzcyJ9.z6N3qREztea2sj35gMMY0LPJBlH8t_k4RbfSDfs-wLo";
    private static String url = "https://darvaron14.pythonanywhere.com";
    //private static String url = "http://10.0.2.2:5000";



    public void enviarBache(AppCompatActivity app, Bache bache) {
        String urlSolicitud = url.concat("/baches");

        // Body
        JSONObject postData = new JSONObject();
        try {
            postData.put("latitud", Double.toString(bache.getLatitud()));
            postData.put("longitud", Double.toString(bache.getLongitud()));
            postData.put("direccion", bache.getDireccion());
            postData.put("metodo", bache.getMetodo());
            postData.put("sistema_coordenadas", bache.getSistema_coordenadas());
            postData.put("precision_gps", Double.toString(bache.getPrecision_gps()));
            postData.put("usuario", bache.getUsuario());

        } catch (JSONException e) {
            e.printStackTrace();
        }

        String postDataString = postData.toString();

        //Request cola
        RequestQueue queue = Volley.newRequestQueue(app);

        // Request
        StringRequest stringRequest = new StringRequest(Request.Method.POST, urlSolicitud,
                new Response.Listener<String>() { // Manejo de respuesta
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(app, "Bache creado satisfactoriamente", Toast.LENGTH_LONG).show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) { // Manejo del error
                Log.e("Bache", "El error es: " + error.toString());
                Toast.makeText(app, "Error: " + error.toString(), Toast.LENGTH_LONG).show();
            }
        }) {
            @Override
            public String getBodyContentType() { // Tipo de body
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() throws AuthFailureError { // Definido el body
                try {
                    return postDataString == null ? null : postDataString.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", postDataString, "utf-8");
                    return null;
                }
            }


            @Override
            public Map<String, String> getHeaders() throws AuthFailureError { // Headers del request
                Map<String, String> params = new HashMap<String, String>();
                params.put("token", token);
                params.put("Content-Type", "application/json");
                return params;
            }


        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(3000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    public void autorizacionUsuario(AppCompatActivity app, String usuario, String contrasena) {
        String urlSolicitud = url.concat("/usuarios/").concat(usuario);

        //Request cola
        RequestQueue queue = Volley.newRequestQueue(app);

        // Request
        StringRequest stringRequest = new StringRequest(Request.Method.POST, urlSolicitud,
                new Response.Listener<String>() { // Manejo de respuesta
                    @Override
                    public void onResponse(String response) {
                        if (response != null) {
                            Log.i("Autorizacion", "La respuesta es: " + response);
                            try {
                                JSONObject objRespuestaAPI = new JSONObject(response);
                                boolean autorizado = Boolean.valueOf(objRespuestaAPI.get("autorizado").toString());
                                if(autorizado){
                                    //Toast.makeText(app, "Usuario autorizado", Toast.LENGTH_LONG).show();
                                    MainActivity.autorizacionOb.setUsuario(usuario);
                                }else{
                                    //Toast.makeText(app, "Usuario no autorizado", Toast.LENGTH_LONG).show();
                                    MainActivity.autorizacionOb.setUsuario(null);
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Log.i("Autorizacion", "La respuesta es: null");
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) { // Manejo del error
                Log.e("Autorizacion", "El error es: " + error.toString());
                //Toast.makeText(app, "Error: " + error.toString(), Toast.LENGTH_LONG).show();
                Toast.makeText(app, "Usuario no autorizado", Toast.LENGTH_LONG).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError { // Headers del request
                Map<String, String> params = new HashMap<String, String>();
                params.put("token", token);
                params.put("Content-Type", "application/json");
                params.put("contrasena", contrasena);
                return params;
            }


        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(3000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(stringRequest);
    }

}
