import net.freeutils.httpserver.HTTPServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import static net.freeutils.httpserver.HTTPServer.*;

public class Main {
	private static Database db;
	
	
	public static void main(String[] args) {
		Log.status("starting vokTrainer Server".toUpperCase());
		
		db  = new Database();
		if (db.isValid()) {
			Log.success("Database connection established");
			
			HTTPServer server = new HTTPServer(1337);
			VirtualHost host = server.getVirtualHost(null);
			//All responses have statuses in header to check for errors
			
			//TODO: DELETE
			host.addContext("/get/lists", new getLists());
			
			//param: name, email, ...
			host.addContext("/create/account", new createUserHandler());
			
			//params: list limit				| returns all users up to limit
			host.addContext("/get/users", new getUsersHandler());
			
			//param: user:id
			//host.addContext("/get/user", new getUserHandler());
			
			//param: food:id || food:name
			//host.addContext("/get/food", new getFoodHandler());
			
			//param: food:category
			//host.addContext("/get/foods", new getFoodsHandler());
			
			
			try {
				server.start();
			} catch (IOException e) {
				Log.critical("httpserver start failed");
				Log.critical("Aborting Server");
				System.exit(-1);
			}
			Log.success("httpserver start succesful");
			
		} else {
			Log.critical("Database connection failed.");
			Log.critical("Aborting Server");
			System.exit(-1);
		}
		
	}
	
	private static void sendResponse(HTTPServer.Response response, int status, JSONObject responseObject) {
		response.getHeaders().add("Content-Type", "application/json");
		response.getHeaders().add("Access-Control-Allow-Origin", "*");
		try {
			response.send(status, responseObject.toString());
		} catch (IOException e) {
			Log.error("Response cannot be sent");
		}
	}
	
	/**
	 * @param 	response	httpserver response
	 * @return	json with status 400, http status 400
	 * @see			HTTPServer.Response
	 */
	private static Integer sendBadApiReq(HTTPServer.Response response) {
		Log.error("[API] bad request");
		
		JSONObject object = new JSONObject();
		object.put("header", new JSONObject().put("status", 400));
		
		sendResponse(response, 400, object);
		return 400;
	}
	
	private static class createUserHandler implements ContextHandler {
		
		@Override
		public int serve(HTTPServer.Request request, HTTPServer.Response response) throws IOException {
			Map<String, String> params = request.getParams();
			
			JSONObject responseObject = new JSONObject();
			JSONObject header = new JSONObject();
			JSONArray wordsResp = new JSONArray();
			
			try {
				db.execute("INSERT INTO user_data VALUES (DEFAULT, ?, ?, ?, ?, ?)",
						params.get("email"),
						params.get("age"),
						params.get("gender"),
						params.get("name"),
						params.get("password"));
				
				
			} catch (Exception e) {
				sendBadApiReq(response);
			}
			
			header.put("status", 200);
			responseObject.put("header", header.put("added", wordsResp));
			
			sendResponse(response, 200, responseObject);
			return 0;
		}
	}
	
	private static class getUsersHandler implements ContextHandler {
		@Override
		public int serve(HTTPServer.Request request, HTTPServer.Response response) throws IOException {
			Log.warning("getUsers request");
			JSONObject responseObject = new JSONObject();
			JSONObject header = new JSONObject();
			JSONArray results = new JSONArray();
			
			try {
				ResultSet resultSet = db.execute("SELECT * FROM user_data");
				
				while (resultSet.next()) {
					JSONObject result = new JSONObject();
					
					result.put("id", resultSet.getInt("id_User"));
					result.put("name", resultSet.getInt("name"));
					result.put("age", resultSet.getInt("age"));
					result.put("gender", resultSet.getInt("gender"));
					
					results.put(result);
				}
				
				header.put("status", 200);
				responseObject.put("results", results);
				responseObject.put("header", header);
				
				sendResponse(response, 200, responseObject);
				Log.status("");
			} catch (SQLException e) {
				e.printStackTrace();
			}
				
			return 0;
			}
		}
	
	
	
	private static class getLists implements ContextHandler {
		@Override
		public int serve(HTTPServer.Request request, HTTPServer.Response response) throws IOException {
			Log.warning("NEW getLISTS REQUEST");
			JSONObject responseObject = new JSONObject();
			JSONObject header = new JSONObject();
			JSONArray results = new JSONArray();
			
			try {
				ResultSet resultSet = db.execute("SELECT * FROM list_Index");
				
				while (resultSet.next()) {
					JSONObject result = new JSONObject();
					
					result.put("id", resultSet.getInt("id_list"));
					result.put("name", resultSet.getString("name"));
					result.put("lang_1", resultSet.getString("lang_1"));
					result.put("lang_2", resultSet.getString("lang_2"));
					
					results.put(result);
				}
				
				header.put("status", 200);
				responseObject.put("results", results);
				responseObject.put("header", header);
				
				sendResponse(response, 200, responseObject);
				Log.status("everything worked.");
				return 0;
			} catch (Exception e) {
				//e.printStackTrace();
				//super helpful error
				Log.error("Something went wrong.");
				return 400;
			}
		}
	
	}
	private static class addContent implements ContextHandler {
		@Override
		public int serve(HTTPServer.Request request, HTTPServer.Response response) throws IOException {
			Map<String, String> params = request.getParams();
			
			JSONObject responseObject = new JSONObject();
			JSONObject header = new JSONObject();
			JSONArray wordsResp = new JSONArray();
			
			int list_id = Integer.parseInt(params.get("list"));
			String words = params.get("words");
			
			
			String[] wordsArray = words.split(",");
			for (String aWordsArray : wordsArray) {
				try {
					db.execute("INSERT INTO list_items VALUES (" + list_id + ", " + aWordsArray + ")");
					wordsResp.put(aWordsArray);
				} catch (Exception e) {
					sendBadApiReq(response);
				}
			}
			
			header.put("status", 200);
			responseObject.put("header", header.put("added", wordsResp));
			
			sendResponse(response, 200, responseObject);
			return 0;
		}
	
	}
	
}
