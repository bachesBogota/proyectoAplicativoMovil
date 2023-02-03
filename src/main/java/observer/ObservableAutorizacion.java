package observer;

import java.io.Serializable;
import java.util.Observable;

public class ObservableAutorizacion extends Observable implements Serializable {

    private String usuario = null;

    public String getUsuario(){
        return usuario;
    }

    public void setUsuario(String usuario){
        this.usuario = usuario;
        this.setChanged();
        this.notifyObservers(usuario);
    }

}
