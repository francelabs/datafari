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

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import com.francelabs.datafari.utils.ScriptConfiguration;

/**Javadoc
 * 
 * 
 * This class sends mails, it reads it's configuration in a text file.
 * If it cannot find the file , it has a hardcoded default configuration. The configuration is made in the constructor.
 * 
 * @author Alexis Karassev
 */
public class Mail {
	
	
	private String smtpHost = "smtp.gmail.com";												//Default address/smtp used
	private String from = "datafari.test@gmail.com";
	private String username = "datafari.test@gmail.com";
	private String password = "Datafari1";			
	
	
	private final static Logger LOGGER = Logger.getLogger(Mail.class
			.getName());
	public Mail() throws IOException{
		try{
			smtpHost = ScriptConfiguration.getProperty("smtp");
			from = ScriptConfiguration.getProperty("from");
			username = ScriptConfiguration.getProperty("user");
			password = ScriptConfiguration.getProperty("pass");
		}catch(IOException e){
			LOGGER.error("Error while reading the mail configuration in the Mail constructor. Error 69045", e);
			return;
		}

	}
	/** Javadoc
	 * 
	 * sends a mail
	 * @param subject : the subject of the mail
	 * @param text : the text of the mail
	 * @param dest : the destination address
	 * @param copyDest : (optionnal set to "" if not wanted) an other destination
	 * @throws IOException,  
	 * @throws AddressException
	 * @throws MessagingException
	 * 
	 */
	public void sendMessage(String subject, String text, String dest, String copyDest)  { 
		try{
			Properties props = new Properties();
			props.put("mail.smtp.host", smtpHost);
			props.put("mail.smtp.auth", "true");				

			Session session = Session.getDefaultInstance(props);			//Set the smtp 
			session.setDebug(true);

			MimeMessage message = new MimeMessage(session);   
			try{
				message.setFrom(from);											//Set the destination and copy Destination if there are some
				if(copyDest!=""){
					message.addRecipients(Message.RecipientType.TO, new InternetAddress[] { new InternetAddress(dest), 
							new InternetAddress(copyDest) });
				}
				else{
					message.addRecipient(Message.RecipientType.TO ,new InternetAddress(dest));
				}
				message.setSubject(subject);
				message.setText(text);											//Set the content of the mail

				Transport tr = session.getTransport("smtps");
				tr.connect(smtpHost, username, password);						//Connect to the address
				message.saveChanges();

				tr.sendMessage(message,message.getAllRecipients());				//Send the message
				tr.close();
			}catch(MessagingException e){	
				LOGGER.error("Error while sending the mail. Error 69046", e);
				return;
			}

		}catch(Exception e){
			LOGGER.error("Unindentified error while in Mail sendMessage(). Error 69523", e);
			return;
		}
	}

}
