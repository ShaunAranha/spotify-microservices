package com.csc301.profilemicroservice;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;

@Repository
public class PlaylistDriverImpl implements PlaylistDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitPlaylistDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nPlaylist:playlist) ASSERT exists(nPlaylist.plName)";
				trans.run(queryStr);
				trans.success();
			}
			session.close();
		}
	}

	@Override
	public DbQueryStatus likeSong(String userName, String songId) { //need to add song if it doesn't exist in mongo
		DbQueryStatus likeSongQuery = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_GENERIC);
		try (Session session = ProfileMicroserviceApplication.driver.session()){
			String query = "MATCH (u:profile)-[:created]->(p:playlist)"
					+ " WHERE u.userName = $userName AND p.plName = $playlistName";
			String giveName =  " RETURN u.userName";
			Map<String,Object> params = new HashMap<>();
			params.put("userName", userName);
			params.put("playlistName", userName+"-favourites");
			
			
			StatementResult result = session.run(query + giveName, params);
			
			if (result.hasNext()) { //everything exists
				// for the relationship
				String songCheck = "MERGE (s:song {songId: $songId})";
				
				result = session.run(songCheck,parameters( "songId", songId));
				
				String query1 = "MATCH (p:playlist),(s:song)"
						+ " WHERE p.plName = $playlistName AND s.songId = $songId";
				String relations =  " RETURN EXISTS ( (p)-[:includes]->(s))";
				params.put("songId", songId);
				result = session.run(query1 + relations, params);
				
				
				if (result.hasNext()) {
					
					Record exists = result.next();
					System.out.println(exists.get(0).toString());
					
					if (exists.get(0).toString().equals("FALSE")) { //if user hasn't liked song previously
						String likeSong = " MERGE (p)-[:includes]->(s)";
						session.run(query1 + likeSong, params);
						likeSongQuery.setMessage("You've successfully liked this song");
						likeSongQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
					} else { // if user already liked song
						likeSongQuery.setMessage("You've already liked this song"); 
						likeSongQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
					}
					

				}
			} else { // one item atleast didn't exist
				likeSongQuery.setMessage("The playlist or the user don't exist in the database");
				likeSongQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
 
				//how to check mongodb if the song exists	
			
		}
		return likeSongQuery;
	}

	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {  //DONE
		
		
		DbQueryStatus unlikeSongQuery = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_GENERIC);
		try (Session session = ProfileMicroserviceApplication.driver.session()){
			String query = "MATCH (u:profile)-[:created]->(p:playlist)"
					+ " WHERE u.userName = $userName AND p.plName = $playlistName";
			String giveName =  " RETURN u.userName";
			Map<String,Object> params = new HashMap<>();
			params.put("userName", userName);
			params.put("playlistName",userName+"-favourites");
			
			
			
			
			StatementResult result = session.run(query + giveName, params);
			
			if (result.hasNext()) { //everything exists
				// for the relationship
				
				String query1 = "MATCH (p:playlist),(s:song)"
						+ " WHERE p.plName = $playlistName AND s.songId = $songId";
				String relations =  " RETURN EXISTS ( (p)-[:includes]->(s))";
				params.put("songId", songId);
				result = session.run(query1 + relations, params);
				
				if (result.hasNext()) {
					Record exists = result.next();
					
					if (exists.get(0).toString().equals("FALSE")) { //if user already unliked the song
						unlikeSongQuery.setMessage("You've already unliked this song");
						unlikeSongQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
						
					} else { // if user hasn't unliked the song
						String query2 = "MATCH (p:playlist)-[i:includes]->(s:song) "
								+ "WHERE p.plName = $playlistName AND s.songId = $songId";
						String unlikeSong = " DELETE i";
						session.run(query2 + unlikeSong, params);
						unlikeSongQuery.setMessage("You've successfully unliked this song");
						unlikeSongQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
					}
					

				}
			} else { // one item atleast didn't exist in the database
				unlikeSongQuery.setMessage("The playlist or the user don't exist in the database");
				unlikeSongQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
 
			
			
		}
		return unlikeSongQuery;
	}

	@Override
	public DbQueryStatus deleteSongFromDb(String songId) {
		
		
		DbQueryStatus deleteSongQuery = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_GENERIC);
		try (Session session = ProfileMicroserviceApplication.driver.session()){
			String query = "MATCH (s:song) WHERE s.songId = $songId RETURN s.songId"; // might need to change 
			
			StatementResult result = session.run(query,parameters( "songId", songId));
			if (result.hasNext()) { //song exists
				query = "MATCH (s:song) WHERE s.songId = $songId";
				String relExists = " RETURN EXISTS ( (:playlist)-[:includes]->(s) )";
				
				result = session.run(query + relExists, parameters( "songId", songId));
		
				
				if (result.hasNext()) {
					Record canDelete = result.next();
					System.out.println(canDelete.get(0).toString());
					if (canDelete.get(0).toString().equals("FALSE")) { //song not in any favourites playlist
						deleteSongQuery.setMessage("Can't delete song because it has never been liked by any profile.");
						deleteSongQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
					} else { //deletes the song from all favourites playlist
						String deleteSong = " MATCH (s:song) WHERE s.songId = $songId DETACH DELETE s";
						session.run(deleteSong, parameters( "songId", songId));
						deleteSongQuery.setMessage("Song successfully removed from the db");
						deleteSongQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
					}
				}	
				
			} else {
				deleteSongQuery.setMessage("Song doesn't exist in neo4j");
				deleteSongQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
		
		}	
			return deleteSongQuery;
	}
}
