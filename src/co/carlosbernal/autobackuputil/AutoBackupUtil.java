package co.carlosbernal.autobackuputil;

import co.carlosbernal.autobackuputil.util.ConfProperties;
import co.carlosbernal.autobackuputil.util.Emailer;
import co.carlosbernal.autobackuputil.util.ShellCommand;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;
/**
 * Programa simple que saca backups de un sitio y envia un mail de notificaion.
 *
 * @author Carlos Bernal <bernalcarvajal@gmail.com>
 */
public class AutoBackupUtil {

    private static Date _currentDate;
    private static DateFormat _dateFormat;

    public static void main(String[] args) {

        //Correr este programa indefindadmente
        while (true) {

            //Notificar el inicio del proceso
            System.out.println("\n---\nIniciando Backup -- " + getCurrentTime() + "\n---\n");

            //Leer el archivo de propiedades
            System.out.println("Leyendo archivo de propiedades... -- " + getCurrentTime());
            ConfProperties.getInstance().SetFileOfProperties("config.conf");

            System.out.println("Nombre del proyecto: " + ConfProperties.getInstance().getPropertie("BKP_NAME") + "\n");

            //Verficar si se debe mostrar informacion de debug
            boolean showDebugInfo = false;
            if (ConfProperties.getInstance().getPropertie("SHOW_DEBUG_INFO").equalsIgnoreCase("true")) {
                showDebugInfo = true;
            }

            //Crear la carpeta donde se almacenara el respaldo
            System.out.println("Creando la carpeta donde se almacenara el respaldo... -- " + getCurrentTime());
            Date backupCurrentDate = new Date();
            String backupDate = getCurrentTime();

            String bkpFolderPath = ConfProperties.getInstance().getPropertie("PATH_BKPS_FOLDER");
            bkpFolderPath = bkpFolderPath + ConfProperties.getInstance().getPropertie("BKP_NAME")
                    + "_BKP_" + backupDate + "/";

            File bkpFolderDir = new File(bkpFolderPath);
            if (!bkpFolderDir.exists()) {
                bkpFolderDir.mkdirs();
            }

            //Sacar el respaldo de la base de datos
            System.out.println("Sacando copia de la base de datos... -- " + getCurrentTime());
            ShellCommand shell = new ShellCommand();
            String shellCmd = "";
            if (ConfProperties.getInstance().getPropertie("TAKE_DB_BKP").toLowerCase().equals("true")) {

                shellCmd = "mysqldump --user=" + ConfProperties.getInstance().getPropertie("DB_USERNAME")
                        + " --password=" + ConfProperties.getInstance().getPropertie("DB_PASSWORD")
                        + " " + ConfProperties.getInstance().getPropertie("DB_NAME")
                        + " > " + bkpFolderPath + ConfProperties.getInstance().getPropertie("BKP_NAME") + "_db_bkp.sql";

                if (showDebugInfo) {
                    System.out.println("------------------------------------------------------");
                    System.out.println("Comando a ejecutar: " + shellCmd);
                    System.out.println("------------------------------------------------------");
                    
                    shell.runCommand(shellCmd, null, false, true);
                }else{
                    shell.runCommand(shellCmd);
                }
            }

            //Copiar el sitio
            System.out.println("Sacando copia de la carpeta del sitio... -- " + getCurrentTime());
            shellCmd = "zip -r " + bkpFolderPath + ConfProperties.getInstance().getPropertie("BKP_NAME") + "_site_bkp.zip *";

            if (ConfProperties.getInstance().getPropertie("EXTRA_ZIP_PARAMETERS") != null
                    && !ConfProperties.getInstance().getPropertie("EXTRA_ZIP_PARAMETERS").equalsIgnoreCase("")) {
                shellCmd += " " + ConfProperties.getInstance().getPropertie("EXTRA_ZIP_PARAMETERS");
            }

            if (showDebugInfo) {
                System.out.println("------------------------------------------------------");
                System.out.println("Comando a ejecutar: " + shellCmd);
                System.out.println("------------------------------------------------------");
                
                shell.runCommand(shellCmd, ConfProperties.getInstance().getPropertie("PATH_FOLDER_TO_BKP"), false, true);
            }else{
                shell.runCommand(shellCmd, ConfProperties.getInstance().getPropertie("PATH_FOLDER_TO_BKP"));
            }

            //Eliminar backups viejos
            if (ConfProperties.getInstance().getPropertie("DELETE_OLD_BKPS").equalsIgnoreCase("true")) {

                System.out.println("Verificando si hay respaldos viejos por eliminar... -- " + getCurrentTime());
                String bkpsPath = ConfProperties.getInstance().getPropertie("PATH_BKPS_FOLDER");
                File bkpsDir = new File(bkpsPath);

                if (bkpsDir.listFiles() != null && bkpsDir.listFiles().length > 0) {

                    for (File fileEntry : bkpsDir.listFiles()) {
                        try {

                            Path pathToDir = FileSystems.getDefault().getPath(fileEntry.getAbsolutePath());
                            BasicFileAttributes dirAtributes = Files.readAttributes(pathToDir, BasicFileAttributes.class);

                            //Comparar el timpo
                            long expirationMills = Long.parseLong(ConfProperties.getInstance().getPropertie("DELETE_BKPS_OLDER_THAN"));
                            expirationMills = expirationMills * 86400 * 1000;//El valor viene en dias, por lo que se multiplica por el numero de segundo en un dia, y luego se pasa a milisegundos

                            Date expirationDate = new Date(backupCurrentDate.getTime() - expirationMills);
                            Date dirDate = new Date(dirAtributes.lastModifiedTime().toMillis());

                            if (expirationDate.after(dirDate)) {
                                System.out.println("Eliminando el respaldo: " + fileEntry.getName() + " -- " + getCurrentTime());
                                deleteFolder(fileEntry);
                            }

                        } catch (Exception ex) {
                            System.out.println("Error al consulatar las propiedades de una carpetda de respaldo.");
                            ex.printStackTrace(System.out);
                        }
                    }
                }
            }

            //Enviar email de notificacion
            System.out.println("Enviando email de notificaion... -- " + getCurrentTime());
            Emailer.sendEmailToAdmins(bkpFolderPath, backupDate);

            //Una vez terminado todo, calcular cuanto se tardo todo el proceso
            System.out.println("\n---\nTiempo de finalización: " + getCurrentTime() + "\n---\n");

            //Volver a ejecutar el programa en cierto tiempo
            try {
                long sleepTime = Long.parseLong(ConfProperties.getInstance().getPropertie("SLEEP_TIME"));
                TimeUnit.DAYS.sleep(sleepTime);
            } catch (InterruptedException ex) {
                System.out.println("Error Durante inactividad... -- " + getCurrentTime());
                ex.printStackTrace(System.out);
            }
        }
    }

    private static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) { //some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    public static String getCurrentTime() {
        if (_currentDate != null) {
            _currentDate.setTime(System.currentTimeMillis());
        } else {
            _currentDate = new Date();
        }

        if (_dateFormat == null) {
            _dateFormat = new SimpleDateFormat("MMM-dd-yyyy__hh-mm-ssa");
        }

        return _dateFormat.format(_currentDate);
    }
}