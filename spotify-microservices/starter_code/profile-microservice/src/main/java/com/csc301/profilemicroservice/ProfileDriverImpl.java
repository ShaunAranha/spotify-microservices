package com.csc301.profilemicroservice;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;

@Repository
public class ProfileDriverImpl implements ProfileDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitProfileDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.userName)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.password)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT nProfile.userName IS UNIQUE";
				trans.run(queryStr);

				trans.success();
			}
			session.close();
		}
	}
	
	@Override
	public DbQueryStatus createUserProfile(String userName, String fullName, String password) {
		/*see if user exists and then make a playlist for them*/
		//call dbquery status methods
		DbQueryStatus profileQuery = new DbQueryStatus("", DbQueryExecResult.QUERY_OK);
		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			String query = "MATCH (p:profile) WHERE p.userName = $userName RETURN p.userName";
			StatementResult result = session.run(query, parameters( "userName", userName));
			
			if (result.hasNext()) {
				profileQuery.setMessage("Profile exists");
				profileQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
			} else {
				Map<String,Object> params = new HashMap<>();
				params.put("userName", userName);
				params.put("fullName", fullName);
				params.put("password", password);
				params.put("plName", userName+"-favourites");
				query = "CREATE (nProfile:profile {userName: $userName, fullName: $fullName, password: $password})" + 
				"-[:created]-> (nPlaylist:playlist {plName: $plName} )";
				session.run(query, params);
				profileQuery.setMessage("created user profile successfully");
				profileQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
				//might have to add profile.setData();
				
				//add the new profile with the new relationships 
			}
				
		}
			
		
		return profileQuery;
	}

	@Override
	public DbQueryStatus followFriend(String userName, String frndUserName) {
		DbQueryStatus followFriendQuery = new DbQueryStatus("", DbQueryExecResult.QUERY_OK);
		
		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			Map<String,Object> params = new HashMap<>();
			params.put("userName", userName);
			params.put("frndUserName", frndUserName);
			String query = "MATCH (uProfile:profile),(fProfile:profile) "
					+ "WHERE uProfile.userName = $userName AND fProfile.userName = $frndUserName"
					+ " RETURN uProfile.userName";
			StatementResult result = session.run(query, params);
			
			if (result.hasNext()) { //both nodes exist
				query = "MATCH (uProfile:profile),(fProfile:profile) "
						+ "WHERE uProfile.userName = $userName AND fProfile.userName = $frndUserName";
				String create = " MERGE (uProfile)-[:follows]->(fProfile)";
				String find = " RETURN EXISTS ( (uProfile)-[:follows]->(fProfile) )";
				result = session.run(query + find, params);
				if (result.hasNext()) { //if relationship already exists
					Record record = result.next();
					if (record.get(0).toString().equals("FALSE")){ 
						session.run(query + create, params);
						followFriendQuery.setMessage("Successfully followed the friend user specified");
						followFriendQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
					} else {
						followFriendQuery.setMessage("The user already follows the friend username provided.");
						followFriendQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
					}
				} 
				
			} else {
				followFriendQuery.setMessage("Can't create relationship as one user doesn't exist.");
				followFriendQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
			}
				
		}
		
		return followFriendQuery;
	}

	@Override
	public DbQueryStatus unfollowFriend(String userName, String frndUserName) {
		
		DbQueryStatus unfollowFriendQuery = new DbQueryStatus("", DbQueryExecResult.QUERY_OK);
		
		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			Map<String,Object> params = new HashMap<>();
			params.put("userName", userName);
			params.put("frndUserName", frndUserName);
			String query = "MATCH (uProfile:profile),(fProfile:profile) "
					+ "WHERE uProfile.userName = $userName AND fProfile.userName = $frndUserName"
					+ " RETURN uProfile.userName";
			StatementResult result = session.run(query, params);
			
			if (result.hasNext()) {  //if user and friend are already in the db
				query = "MATCH (uProfile:profile)-[f:follows]->(fProfile:profile) "
						+ "WHERE uProfile.userName = $userName AND fProfile.userName = $frndUserName";
				String delete = " DELETE f";
				String find = " RETURN EXISTS ( (uProfile)-[:follows]->(fProfile) )";
				result = session.run(query + find, params);
				if (result.hasNext()) { //if relationship already exists
					Record record = result.next();
					if (record.get(0).toString().equals("FALSE")){  //if user already unfollowed friend
						unfollowFriendQuery.setMessage("User already unfollowed friend.");
						unfollowFriendQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
						
					} else { //if they are following friend unfollow them
						session.run(query + delete, params);
						unfollowFriendQuery.setMessage("User has successfully unfollowed friend");
						unfollowFriendQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
					}
				} 
				
				
			} else {
				unfollowFriendQuery.setMessage("Can't unfollow friend as one user doesn't exist.");
				unfollowFriendQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
		
		
		}
		
		return unfollowFriendQuery;
	}

	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) { //TO FINISH
		
		DbQueryStatus songsQuery = new DbQueryStatus("", DbQueryExecResult.QUERY_OK);
		HashMap<String, ArrayList<String>> friendsSongs = new HashMap<String, ArrayList<String>>();
	
		try (Session session = ProfileMicroserviceApplication.driver.session()){
			String query =  "MATCH (p:profile) WHERE p.userName = $userName RETURN p.userName";
			StatementResult result = session.run(query, parameters( "userName", userName));
			if (result.hasNext()) { 
				
				query = "MATCH (p:profile)-[f:follows]->(friends:profile) WHERE p.userName = $userName RETURN friends.userName";
				result = session.run(query,parameters( "userName", userName));
				
				if (result.hasNext()) { //user follows friends
					while (result.hasNext()) {
						Record friend = result.next();
						
						String fUserName = friend.get(0).toString().replace("\"", "");
						
						
						//getting the friend's playlist
						 query = "MATCH (p:profile)-[:created]->(pl:playlist)-[:includes]->(s:song)"
								+ " WHERE p.userName = $userName RETURN s.songId";
						StatementResult playlistSongs = session.run(query, parameters( "userName", fUserName));
						
					;
						
						if (playlistSongs.hasNext()) {
						
							ArrayList<String> songs = new ArrayList<String>();
							while (playlistSongs.hasNext()) {
								Record songId = playlistSongs.next();
								songs.add(songId.get(0).toString());
								
							}
							friendsSongs.put(fUserName, songs);
							
							
						} else {
							friendsSongs.put(fUserName, null);
						}
						
					}
					songsQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
					songsQuery.setMessage("Got all user's friends' songs ");
					songsQuery.setData(friendsSongs);
					
				}
				
				else {
					//user doesn't follow anyfriends
					songsQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
					songsQuery.setMessage("User doesn't follow any friends");
					//songsQuery.setData(null);
				}
				
			}
			else { //user not found
				songsQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				songsQuery.setMessage("User doesn't exist");
				//songsQuery.setData(null);
			}
		}
	
			
		return songsQuery;
	}
}
