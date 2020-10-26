package com.csc301.songmicroservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class SongController {

	@Autowired
	private final SongDalImpl songDal;

	private OkHttpClient client = new OkHttpClient();

	
	public SongController(SongDalImpl songDal) {
		this.songDal = songDal;
	}

	
	@RequestMapping(value = "/findSongById/{songId}", method = RequestMethod.GET)  //DONE
	public @ResponseBody Map<String, Object> findSongById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("GET %s", Utils.getUrl(request)));
		
		if (songId == null || !(songId instanceof String)) {
			 response.put("message", "The data provided has not been fully completed");
			 
			 response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null); 
			 
		} else {


			DbQueryStatus dbQueryStatus = songDal.findSongById(songId);
	
			response.put("message", dbQueryStatus.getMessage());
			response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		}
			
		return response;
	}

	
	@RequestMapping(value = "/getSongTitleById/{songId}", method = RequestMethod.GET)  //DONE
	public @ResponseBody Map<String, Object> getSongTitleById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("GET %s", Utils.getUrl(request)));

		if (songId == null || !(songId instanceof String)) {
			 response.put("message", "The data provided has not been fully completed");
			 
			 response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null); 
			 
		} else {


			DbQueryStatus songTitle = songDal.getSongTitleById(songId);
	
			
			Song songRetrieved = (Song) songTitle.getData();
			response = Utils.setResponseStatus(response, songTitle.getdbQueryExecResult(), songRetrieved.getSongName());

				
			
			
			
			response.put("message", songTitle.getMessage());
			
		}
			
		return response;
	}

	
	@RequestMapping(value = "/deleteSongById/{songId}", method = RequestMethod.DELETE) //DONE (look at trace request)
	public @ResponseBody Map<String, Object> deleteSongById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("DELETE %s", Utils.getUrl(request)));
		
		if (songId == null || !(songId instanceof String)) {
			 response.put("message", "The data provided has not been fully completed");
			 
			 response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null); 
		} else {
			DbQueryStatus deleteSong = songDal.deleteSongById(songId);
			response.put("message", deleteSong.getMessage());
			response = Utils.setResponseStatus(response, deleteSong.getdbQueryExecResult(), null);
			
			//call profile microservice as well
		}

		return response;
	}

	
	@RequestMapping(value = "/addSong", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addSong(@RequestParam Map<String, String> params, //DONE
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		
		response.put("path", String.format("POST %s", Utils.getUrl(request)));
	
		

		
		 if (params.get("songName")== null || (params.get("songArtistFullName")== null ) || 
				 (params.get("songAlbum") == null) || !(params.get("songName") instanceof String) || 
				 !(params.get("songArtistFullName") instanceof String) || !(params.get("songAlbum") instanceof String)) {
			 
			
			 response.put("message", "The data provided has not been fully completed");
			 
			 response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null); 
			 
		 } else {
			 Song songToAdd = new Song(params.get("songName"), params.get("songArtistFullName"), params.get("songAlbum"));
			 DbQueryStatus addSong = songDal.addSong(songToAdd);
			 response.put("message", addSong.getMessage());
			 response = Utils.setResponseStatus(response, addSong.getdbQueryExecResult(), addSong.getData());
			 
		 }
		 
		 

		return response;
	}

	
	@RequestMapping(value = "/updateSongFavouritesCount/{songId}", method = RequestMethod.PUT) //READY FOR TEST
	public @ResponseBody Map<String, Object> updateFavouritesCount(@PathVariable("songId") String songId,
			@RequestParam("shouldDecrement") String shouldDecrement, HttpServletRequest request) {

	
		
		Map<String, Object> response = new HashMap<String, Object>();
		//response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		
		
		
		 if (songId == null || shouldDecrement == null  || 
				!(songId instanceof String) || 
				 !(shouldDecrement instanceof String) ||  
				 (!shouldDecrement.equals("true") && !shouldDecrement.equals("false"))){
			
			 response.put("message", "The data provided has not been fully completed");
			 
			 response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null); 
		 } else {
			boolean shouldDecrease = Boolean.parseBoolean(shouldDecrement);
			DbQueryStatus updateCount = songDal.updateSongFavouritesCount(songId, shouldDecrease);
			response.put("message", updateCount.getMessage());
			response = Utils.setResponseStatus(response, updateCount.getdbQueryExecResult(), null);
		 }
		return response;
	}
}