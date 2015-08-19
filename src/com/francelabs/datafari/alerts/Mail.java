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
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

/**Javadoc
 * 
 * 
 * This class sends mails, it reads it's configuration in a text file.
 * If it cannot find the file , it has a hardcoded default configuration. The configuration is made in the constructor.
 * 
 * @author Alexis Karassev
 */
public class Mail {
	private String smtpHost;
	private String from;
	private String username;
	private String password;
	private BufferedReader inputStream;
	private final static Logger LOGGER = Logger.getLogger(Mail.class
			.getName());
	public Mail() throws IOException{
		try{
			smtpHost = "smtp.gmail.com";												//Default address/smtp used
			from = "datafari.test@gmail.com";
			username = "datafari.test@gmail.com";
			password = "Datafari1";			
			String filePath = System.getenv("DATAFARI_HOME");							//Gets the directory of installation if in standard environment
			if(filePath==null){															//If in development environment	
				RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();		//Gets the D.solr.solr.home variable given in arguments to the VM
				List<String> arguments = runtimeMxBean.getInputArguments();
				for(String s : arguments){
					if(s.startsWith("-Dsolr.solr.home"))
						filePath = s.substring(s.indexOf("=")+1, s.indexOf("solr_home")-5);
				}
			}
			try{
				inputStream = new BufferedReader(new FileReader(filePath+"/bin/common/mail.txt")); //Get the configuration file
				String l;
				while ((l=inputStream.readLine()) != null) {
					l = ""+l.replaceAll("\\s","");
					if(l.startsWith("smtp")){									//Get the host
						smtpHost = l.substring(l.indexOf("=")+1,l.length()).trim();
					}
					else if(l.startsWith("from")){								//Get the address			
						from = l.substring(l.indexOf("=")+1,l.length()).trim();
					}
					else if(l.startsWith("user")){								//Get the user name
						username = l.substring(l.indexOf("=")+1,l.length()).trim();
					}
					else if(l.startsWith("pass")){								//Get the password
						password = l.substring(l.indexOf("=")+1,l.length()).trim();
					}
				}
				inputStream.close();
			}catch (IOException | IndexOutOfBoundsException e){
				LOGGER.error("Error while reading the mail configuration in the Mail constructor. Error 69045", e);
				return;
			}
		}catch(Exception e){
			LOGGER.error("Unindentified error while in the Mail constructor. Error 69522", e);
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
