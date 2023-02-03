package procesamiento;

public class Bache {

    private double latitud, longitud, precision_gps;
    private String direccion, metodo, sistema_coordenadas, usuario;

    public Bache(double latitud, double longitud, double precision_gps, String direccion, String metodo, String sistema_coordenadas, String usuario) {
        this.latitud = latitud;
        this.longitud = longitud;
        this.precision_gps = precision_gps;
        this.direccion = direccion;
        this.metodo = metodo;
        this.sistema_coordenadas = sistema_coordenadas;
        this.usuario = usuario;
    }

    public double getLatitud() {
        return latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public double getPrecision_gps() {
        return precision_gps;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getMetodo() {
        return metodo;
    }

    public String getSistema_coordenadas() {
        return sistema_coordenadas;
    }

    public String getUsuario() { return usuario; }

}
