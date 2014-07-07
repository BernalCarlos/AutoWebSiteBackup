package co.carlosbernal.autobackuputil.util;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Clase encargada de enviar los emails necesarios.
 *
 * @author Carlos Bernal <bernalcarvajal@gmail.com>
 */
public class Emailer {

    public static void sendEmailToAdmins(String bkpFolderPath, String bkpDate) {

        try {

            //Cargar las propiedades del servidor de correo
            Properties mailProp = ConfProperties.getInstance().getPropetriesObject();

            //Obtener los destinatarios
            InternetAddress[] mailTo = InternetAddress.parse(mailProp.getProperty("SEND_EMAIL_TO"));

            //Conectar con el servidor de correo
            final String user = mailProp.getProperty("mail.smtp.user");
            final String pass = mailProp.getProperty("mail.smtp.password");

            Session session = Session.getInstance(mailProp,
                    new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });

            //Crear el mensaje
            String mailSubject = "Se ha generado el backup de "
                    + ConfProperties.getInstance().getPropertie("BKP_NAME")
                    + " - " + bkpDate;

            String mailBody = "<br>"+mailSubject+"<br><br>"+
                    "Ruta absoluta al backup: "+bkpFolderPath;

            Message message = new MimeMessage(session);
            message.setRecipients(Message.RecipientType.TO, mailTo);

            message.setSubject(mailSubject);

            message.setContent(mailBody, "text/html");

            //enviar el mensaje
            Transport.send(message);

        } catch (Exception ex) {
            System.out.println("Error al enviar el correo de notificacion de una orden...");
            ex.printStackTrace(System.out);
        }

    }
}
