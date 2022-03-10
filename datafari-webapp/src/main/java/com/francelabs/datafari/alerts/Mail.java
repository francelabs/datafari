/*******************************************************************************
 * Copyright 2015 France Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.alerts;

import java.io.IOException;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.utils.AlertsConfiguration;
import com.sun.mail.util.MailSSLSocketFactory;

/**
 * Javadoc
 *
 *
 * This class sends mails, it reads it's configuration in a text file. If it cannot find the file , it has a hardcoded default configuration. The configuration is made in the constructor.
 *
 * @author Alexis Karassev
 */
public class Mail {

  private String smtpHost = "smtp.gmail.com"; // Default address/smtp used
  private String smtpPort = "25";
  private String smtpSecurity = "tls";
  private String from = "datafari.test@gmail.com";
  private String username = "datafari.test@gmail.com";
  private String password = "Datafari1";

  private final static Logger LOGGER = LogManager.getLogger(Mail.class.getName());

  public Mail() throws IOException {
    smtpHost = AlertsConfiguration.getInstance().getProperty(AlertsConfiguration.SMTP_ADDRESS);
    smtpPort = AlertsConfiguration.getInstance().getProperty(AlertsConfiguration.SMTP_PORT);
    smtpSecurity = AlertsConfiguration.getInstance().getProperty(AlertsConfiguration.SMTP_SECURITY);
    from = AlertsConfiguration.getInstance().getProperty(AlertsConfiguration.SMTP_FROM);
    username = AlertsConfiguration.getInstance().getProperty(AlertsConfiguration.SMTP_USER);
    password = AlertsConfiguration.getInstance().getProperty(AlertsConfiguration.SMTP_PASSWORD);

  }

  /**
   * Javadoc
   *
   * sends a mail
   *
   * @param subject
   *          : the subject of the mail
   * @param text
   *          : the text of the mail
   * @param dest
   *          : the destination address
   * @param copyDest
   *          : (optionnal set to "" if not wanted) an other destination
   * @throws Exception
   *
   */
  public void sendMessage(final String subject, final String text, final String dest, final String copyDest) throws Exception {

    try {
      final Properties props = new Properties();
      props.put("mail.smtp.host", smtpHost);
      props.put("mail.smtp.port", smtpPort);
      props.put("mail.smtp.ssl.trust", smtpHost);
      props.put("mail.smtp.timeout", 30000);

      if (smtpSecurity.toLowerCase().contentEquals("tls")) {
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.enable", "false");
      } else if (smtpSecurity.toLowerCase().contentEquals("ssl")) {
        final MailSSLSocketFactory sf = new MailSSLSocketFactory();
        sf.setTrustedHosts(new String[] { smtpHost });
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.checkserveridentity", "false");
        props.put("mail.smtp.ssl.socketFactory", sf);
      } else if (smtpSecurity.toLowerCase().contentEquals("tls+ssl")) {
        props.put("mail.smtp.starttls.enable", "true");
        final MailSSLSocketFactory sf = new MailSSLSocketFactory();
        sf.setTrustedHosts(new String[] { smtpHost });
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.checkserveridentity", "false");
        props.put("mail.smtp.ssl.socketFactory", sf);
      } else {
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.smtp.ssl.enable", "false");
      }

      Session session;
      if (!username.isEmpty()) {
        props.put("mail.smtp.auth", "true");
        session = Session.getInstance(props, new Authenticator() {
          @Override
          protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password);
          }
        });
      } else {
        props.put("mail.smtp.auth", "false");
        session = Session.getInstance(props);
      }

      session.setDebug(true);

      final MimeMessage message = new MimeMessage(session);

      message.setFrom(new InternetAddress(from)); // Set the
      // destination and
      // copy
      // Destination if there are some
      if (copyDest != "") {
        message.addRecipients(Message.RecipientType.TO, new InternetAddress[] { new InternetAddress(dest), new InternetAddress(copyDest) });
      } else {
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(dest));
      }
      message.setSubject(subject);
      message.setText(text); // Set the content of the mail

      Transport.send(message);
    } catch (final Exception e) {
      LOGGER.error("Unable to send mail", e);
      throw e;
    }

  }

}
