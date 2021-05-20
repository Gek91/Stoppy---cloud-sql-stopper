package functions;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.DatabaseInstance;
import com.google.api.services.sqladmin.model.Settings;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;

import functions.PubSubHandlerFunction.PubSubMessage;


public class PubSubHandlerFunction implements BackgroundFunction<PubSubMessage> {

	private static final Logger logger = Logger.getLogger(PubSubHandlerFunction.class.getName());

	private static final HttpTransport httpTransport = getHttpTransport();
	private static final JacksonFactory jacksonFactory = new JacksonFactory();
	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final SQLAdmin SQLAdminService = getSQLAdminService();

	private static final String PROJECT_ID = System.getenv("GCP_PROJECT");
//	private static final String PROJECT_ID = process.env.GCLOUD_PROJECT;
	
	public void accept(PubSubMessage message, Context context) {
		
		MessageInfo info = getinfoFromPubSubMessage(message);
		
		if(info.action != null && info.instanceId != null) {
			
			executeOperation(info);
			
		} else {
			logger.warning("Invalid input values: " + info.action.getCommand() + " - " + info.instanceId);
		}
	}
	
	private void executeOperation(MessageInfo info) {
		
		try {
			DatabaseInstance patchIstanceRequest = new DatabaseInstance();
			
			Settings settings = new Settings();
			if(info.action.equals(Action.START)) {
				settings.setActivationPolicy("ALWAYS");
			} else if(info.action.equals(Action.STOP)) {
				settings.setActivationPolicy("NEVER");
			}
			
			patchIstanceRequest.setSettings(settings);
		
			logger.info("project id: " + PROJECT_ID);
			
			SQLAdminService.instances().patch(PROJECT_ID, info.instanceId, patchIstanceRequest).execute();
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static SQLAdmin getSQLAdminService() {
		
		try {
			
			GoogleCredentials credential = GoogleCredentials.getApplicationDefault();
		
			HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credential);        
		    
			return new SQLAdmin.Builder(httpTransport, jacksonFactory, requestInitializer).build();
		 
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static HttpTransport getHttpTransport() {
		try {
			return GoogleNetHttpTransport.newTrustedTransport();
		} catch (GeneralSecurityException | IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private MessageInfo getinfoFromPubSubMessage(PubSubMessage message) {
		
		try {
			MessageInfo info = new MessageInfo();
			
			Map<String, String> valueMap = objectMapper.readValue(new String(Base64.getDecoder().decode(message.data)), Map.class);
		
			info.action = Action.getActionFromCommand(valueMap.get("action"));
			info.instanceId = valueMap.get("instanceId");
			
			return info;
			
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	public static class MessageInfo {
		String instanceId;
		Action action;
	}
	
	enum Action {
		
		START("start"),
		STOP("stop");
		
		private String command;
		
		private Action(String command) {
			this.command = command;
		}
		
		public String getCommand() {
			return this.command;
		}
		
		public static Action getActionFromCommand(String command) {
			 
			Action result = null;
			
			if(command != null) {
				
				for (Action value : Action.values()) {
					if(value.getCommand().equals(command)) {
						result = value;
						break;
					}
				}

			}
			
			return result;
		}
	}
	
	public static class PubSubMessage {
		String data;
		Map<String, String> attributes;
		String messageId;
		String publishTime;
	}
	
	
//	public static void main(String[] args) {
//		
//		PubSubHandlerFunction functions = new PubSubHandlerFunction();
//		
//		MessageInfo info = new MessageInfo();
//		info.action = Action.STOP;
//		info.instanceId = "istance-test";
//		
//		functions.executeOperation(info);
//	}
}
