package co.carlosbernal.autobackuputil.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Calse estatica de ayuda para la lectura de archivos de propiedades.
 * 
 * Esta clase tambien contiene constantes de uso global de la aplicacion.
 * 
 * @author Carlos Bernal <bernalcarvajal@gmail.com>
 */
public class ConfProperties {

    /** Refrencia a la instancia de la clase 'Properties' que contine la informacion cargada **/
    private Properties prop = null;
    
    /** Unica intancia de esta clase **/
    private static ConfProperties instance = null;
    
    //Declarar el contructor como privado para que la clase no puede ser instanciada.
    private ConfProperties(){}

    /**
     * Asigna y carga el archivo de propiedades.
     * 
     * @param nameOfFile ruta al archivo de configuracion.
     */
    public void SetFileOfProperties(String nameOfFile) {
        this.prop = new Properties();
        try {
            FileInputStream is = new FileInputStream(nameOfFile);
            this.prop.load(is);
            is.close();
        } catch (IOException e) {
            String appPath = System.getProperties().getProperty("user.dir");
            System.out.print("ReadProperties - Error: " + e + ", path=" + appPath);
        }
    }

    /**
     * Obtiene una propiedad por su nombre
     * 
     * @param nameOfPropertie
     */
    public String getPropertie(String nameOfPropertie) {
        return this.prop.getProperty(nameOfPropertie);
    }

    /**
     * Imprime en consola las propiedades del archivo cargado.
     */
    public void showAllProperties() {
        Enumeration enumeration = this.prop.propertyNames();
        while (enumeration.hasMoreElements()) {
            String llave = (String) enumeration.nextElement();
            System.out.println(llave + "=" + this.prop.getProperty(llave));
        }
    }

    /**
     * @return Refrencia a la instancia de la clase 'Properties' que contine la informacion cargada
     */
    public Properties getPropetriesObject() {
        return this.prop;
    }
    
    /**
     * Obtiene la unica instancia de esta clase.
     */
    public static ConfProperties getInstance(){
        if(instance == null){
            instance = new ConfProperties();
        }
        return instance;
    }
}