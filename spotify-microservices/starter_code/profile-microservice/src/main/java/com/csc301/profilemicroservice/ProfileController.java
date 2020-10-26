package com.csc301.profilemicroservice;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.csc301.profilemicroservice.Utils;
import com.csc301.profilemicroservice.DbQueryExecResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class ProfileController {
	public static final String KEY_USER_NAME = "userName";
	public static final String KEY_USER_FULLNAME = "fullName";
	public static final String KEY_USER_PASSWORD = "password";

	@Autowired
	private final ProfileDriverImpl profileDriver;

	@Autowired
	private final PlaylistDriverImpl playlistDriver;

	OkHttpClient client = new OkHttpClient();

	public ProfileController(ProfileDriverImpl profileDriver, PlaylistDriverImpl playlistDriver) {
		this.profileDriver = profileDriver;
		this.playlistDriver = playlistDriver;
	}

	@RequestMapping(value = "/profile", method = RequestMethod.POST)  //DONE
	public @ResponseBody Map<String, Object> createUserProfile(@RequestParam Map<String, String> params,
			HttpServletRequest request) {
		
		Map<String, Object> response = new HashMap<String, Object>();
		//check if params are good and type string
		 if (params.get("userName")== null || (params.get("fullName")== null ) || 
				 (params.get("password") == null) || !(params.get("userName") instanceof String) || 
				 !(params.get("password") instanceof String) || !(params.get("fullName") instanceof String)) {
			 //response.put("message", "The data provided has not been fully completed");
			

			 
			 response.put("message", "The data provided has not been fully completed");
			 
			 response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
			 
		 } else {
			 String userName = params.get("userName");
			 String fullname = params.get("fullName");
			 String password = params.get("password");
			 
			 DbQueryStatus userProfile = profileDriver.createUserProfile(userName, fullname, password); 
			 
			 
			 response.put("message",userProfile.getMessage());
			 
			 response = Utils.setResponseStatus(response, userProfile.getdbQueryExecResult(), null);
			 
			 //unsure about how to return this with the status 
		 }
		//MIGHT NEED TO FIX RETURN STATEMENTS

		return response;
	}

	@RequestMapping(value = "/followFriend/{userName}/{friendUserName}", method = RequestMethod.PUT) //READY TO TEST
	public @ResponseBody Map<String, Object> followFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();

		//might need to change checkers
		
		//add handler for user friending themselves
		 if (friendUserName == null || userName == null  || 
				  !(userName instanceof String) || 
				 !(friendUserName instanceof String) || (userName.equals(friendUserName))) {
			
			 response.put("message", "The data provided has not been fully completed");
			 response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
			 
		 } else {
			 
			 DbQueryStatus follow = this.profileDriver.followFriend(userName, friendUserName); 
			 
			 response.put("message", follow.getMessage());
			 response = Utils.setResponseStatus(response, follow.getdbQueryExecResult(), null);
		 }

		return response;
	}

	@RequestMapping(value = "/getAllFriendFavouriteSongTitles/{userName}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getAllFriendFavouriteSongTitles(@PathVariable("userName") String userName,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		ObjectMapper mapper = new ObjectMapper();
		response.put("path", String.format("GET %s", Utils.getUrl(request)));
		
		//might need to change checkers
		 if (userName == null  || !(userName instanceof String)) {

			
			 response.put("message", "The data provided has not been fully completed");
			 response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
		 } else {
			 
			 DbQueryStatus songsList = profileDriver.getAllSongFriendsLike(userName); 
			 
			 if (songsList.getData() != null) {
				 HashMap<String, ArrayList<String>> friends = (HashMap<String, ArrayList<String>>) songsList.getData();
				 System.out.println(friends);
				 HashMap<String, ArrayList<String>> friendsSongTitles = new HashMap<String, ArrayList<String>>();
				 
				 for (Map.Entry<String, ArrayList<String>> friend: friends.entrySet()) {
					    ArrayList<String> songs = friend.getValue();
					    ArrayList<String> songTitles = new ArrayList<String>();
					    
					    for(String friendSongs: songs){
							HttpUrl.Builder urlBuilder = HttpUrl.parse("http://localhost:3001" + "/getSongTitleById").newBuilder();
							urlBuilder.addPathSegment(friendSongs);
							String url = urlBuilder.build().toString();
							 Request songCheck = new Request.Builder().url(url).method("GET", null).build();	
							 
							 Call call = client.newCall(songCheck);
							 
							 Response responseFromSongMs = null;
							 String friendSongBody;
							
							 try { //getting the result from the call and adding it to the list
									responseFromSongMs = call.execute();
									friendSongBody = responseFromSongMs.body().string();
									System.out.println("responseFromSongMs: "+ friendSongBody);
									Map<String, Object> map =  mapper.readValue(friendSongBody, Map.class);
									
									if (map.containsKey("data")) { //see if song was deleted of not in mongo
										songTitles.add(map.get("data").toString());
									} else {
										songTitles.add("song deleted");
									}
								
							 } catch (IOException e) {
								e.printStackTrace();
								 
							 }
								    	
					    }
					    friendsSongTitles.put(friend.getKey(), songTitles); 
					    
					 
				 }
				 songsList.setData(friendsSongTitles);
				 response = Utils.setResponseStatus(response, songsList.getdbQueryExecResult(), songsList.getData());
			 } else {
				 response.put("message", songsList.getMessage());
				 response = Utils.setResponseStatus(response, songsList.getdbQueryExecResult(), songsList.getData());
			 }
		 }
	
		
		return response;

	}


	@RequestMapping(value = "/unfollowFriend/{userName}/{friendUserName}", method = RequestMethod.PUT) //DONE
	public @ResponseBody Map<String, Object> unfollowFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		 if (friendUserName == null || userName == null  || 
				  !(userName instanceof String) || 
				 !(friendUserName instanceof String) || (userName.equals(friendUserName))) {

			 response.put("message", "The data provided has not been fully completed or invalid");
			 response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
		 } else {
			 
			 DbQueryStatus unfollow = profileDriver.unfollowFriend(userName, friendUserName); 
			 
			 response.put("message", unfollow.getMessage());
			 response = Utils.setResponseStatus(response, unfollow.getdbQueryExecResult(), null);
		 }
		
		//MIGHT NEED TO FIX RETURN STATEMENTS
		 //add handler for user unfriending themselves
		return response;

	}

	@RequestMapping(value = "/likeSong/{userName}/{songId}", method = RequestMethod.PUT)  //READY FOR TEST
	public @ResponseBody Map<String, Object> likeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> response = new HashMap<String, Object>();
		 if (songId == null || userName == null  || 
				  !(userName instanceof String) || 
				 !(songId instanceof String)) {

			 response.put("message", "The data provided has not been fully completed");
			 

			 response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
		 } else {
			 
			 //check mongo if song exists first 
			HttpUrl.Builder urlBuilder = HttpUrl.parse("http://localhost:3001" + "/findSongById").newBuilder();
			urlBuilder.addPathSegment(songId);
			String url = urlBuilder.build().toString();
			 RequestBody body = RequestBody.create(null, new byte[0]);
			 Request songCheck = new Request.Builder().url(url).method("GET", null).build();	
			 
			
			 Call call = client.newCall(songCheck);
			 Response responseFromSongMs = null;
			 String songServiceBody;
			
			 
				try {
					responseFromSongMs = call.execute();
					
					songServiceBody = responseFromSongMs.body().string();
					Map<String, Object> map =  mapper.readValue(songServiceBody, Map.class);
					System.out.println(map);
					
					//if song doesn't exist return not found 
					
					
					if (map.get("data") == null) { //if data gives null
						response.put("message", "Song doesn't exist in mongo");
						response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_NOT_FOUND,null);
					
					} else {
					
						//else go into neo4j
						 DbQueryStatus like = playlistDriver.likeSong(userName, songId);
						 
						 if (like.getMessage().equals("You've successfully liked this song")) { //able to like song in neo4j
							 
							 urlBuilder = HttpUrl.parse("http://localhost:3001" + "/updateSongFavouritesCount").newBuilder();
							 urlBuilder.addPathSegment(songId);
							 urlBuilder.addQueryParameter("shouldDecrement", "false");
							 url = urlBuilder.build().toString();
							 
							 Request likeSong = new Request.Builder().url(url).method("PUT", body).build();
							 call = client.newCall(likeSong);
							 responseFromSongMs = call.execute();
							 
							 response.put("message", "Successfully increased song favourite count in mongo.");
							 response = Utils.setResponseStatus(response, like.getdbQueryExecResult(), null);
							 
						 } else {
							 response.put("message", like.getMessage());
							 response = Utils.setResponseStatus(response, like.getdbQueryExecResult(), null);
							 
						 }	 
						 
					}
					//depending on what body gives call song service in neo4j
					//make call again to song microcontroller to increase like count
					
					//response.put("data", mapper.readValue(addServiceBody, Map.class));
				} catch (IOException e) {
					e.printStackTrace();
				}

			 
			
		 }
		


		return response;
	}

	@RequestMapping(value = "/unlikeSong/{userName}/{songId}", method = RequestMethod.PUT)      //READY FOR TEST
	public @ResponseBody Map<String, Object> unlikeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) {

		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> response = new HashMap<String, Object>();
		 if (songId == null || userName == null  || 
				  !(userName instanceof String) || 
				 !(songId instanceof String)) {

			 response.put("message", "The data provided has not been fully completed");
			 
			 
			 response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
		 } else {
			 
			 //check mongo if song exists first 
			HttpUrl.Builder urlBuilder = HttpUrl.parse("http://localhost:3001" + "/findSongById").newBuilder();
			urlBuilder.addPathSegment(songId);
			String url = urlBuilder.build().toString();
			 Request songCheck = new Request.Builder().url(url).method("GET", null).build();
			 
			 
			 Call call = client.newCall(songCheck);
			 Response responseFromSongMs = null;
			 String songServiceBody;
			 RequestBody body = RequestBody.create(null, new byte[0]);
			 
				try {
					responseFromSongMs = call.execute();
					songServiceBody = responseFromSongMs.body().string();
					Map<String, Object> map =  mapper.readValue(songServiceBody, Map.class);
					System.out.println(map);
					
					//if song doesn't exist return not found 
					
					if (map.get("data") == null) { //if data gives null
						response.put("message", "Song doesn't exist in mongo");
						response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_NOT_FOUND,null);
					
					} else {
					
						//else go into neo4j
						 DbQueryStatus unlike = playlistDriver.unlikeSong(userName, songId);
						 
						 if (unlike.getMessage().equals("You've successfully unliked this song")) { //able to like song in neo4j
							 urlBuilder = HttpUrl.parse("http://localhost:3001" + "/updateSongFavouritesCount").newBuilder();
							 urlBuilder.addPathSegment(songId);
							 urlBuilder.addQueryParameter("shouldDecrement", "true");
							 url = urlBuilder.build().toString();
							 
							 Request unlikeSong = new Request.Builder().url(url).method("PUT", body).build();
							 call = client.newCall(unlikeSong);
							 responseFromSongMs = call.execute();
							 
							 response.put("message", "Successfully decreased song favourite count in mongo.");
							 response = Utils.setResponseStatus(response, unlike.getdbQueryExecResult(), null);
							 
						 } else {
							 response.put("message", unlike.getMessage());
							 response = Utils.setResponseStatus(response, unlike.getdbQueryExecResult(), null);
							 
						 }	 
						 
					}

				} catch (IOException e) {
					e.printStackTrace();
				}

			 
			
		 }
		


		return response;
	}

	@RequestMapping(value = "/deleteAllSongsFromDb/{songId}", method = RequestMethod.PUT)  //READY FOR TEST
	public @ResponseBody Map<String, Object> deleteAllSongsFromDb(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		


		 if (songId == null || !(songId instanceof String)) {

			 	
			 response.put("message", "The data provided has not been fully completed");
			 
			 
			 response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC,null);
		 } else {
			 
					//if song doesn't exist return not found 

						 DbQueryStatus delete = playlistDriver.deleteSongFromDb(songId);
						 response.put("message", delete.getMessage());
						 response = Utils.setResponseStatus(response, delete.getdbQueryExecResult(), null);
						
			
		 }
		

		return response;

	}
}