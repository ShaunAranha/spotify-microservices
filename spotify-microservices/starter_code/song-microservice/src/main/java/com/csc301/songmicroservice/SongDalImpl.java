package com.csc301.songmicroservice;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.mongodb.client.MongoCollection;

@Repository
public class SongDalImpl implements SongDal {

	private final MongoTemplate db;

	@Autowired
	public SongDalImpl(MongoTemplate mongoTemplate) {
		this.db = mongoTemplate;
	}

	@Override
	public DbQueryStatus addSong(Song songToAdd) {
		DbQueryStatus addSongQuery = new DbQueryStatus("",DbQueryExecResult.QUERY_ERROR_GENERIC);
		Query query = new Query(); 
		query.addCriteria(Criteria.where("songName").is(songToAdd.getSongName()));
		query.addCriteria(Criteria.where("songArtistFullName").is(songToAdd.getSongArtistFullName()));
		query.addCriteria(Criteria.where("songAlbum").is(songToAdd.getSongAlbum()));

		boolean dataExists = db.exists(query, "songs");
		
		if (dataExists) {
			addSongQuery.setMessage("song has already been added");
			addSongQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
			addSongQuery.setData(null);
		} else {
			
			db.insert(songToAdd, "songs");
			
			//songToAdd.setId(songToAdd._id);
			addSongQuery.setMessage("successfully added song");
			addSongQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
			addSongQuery.setData(songToAdd);
			
		}
		
		//do i need to update the id after adding the song
		return addSongQuery;
	}

	@Override
	public DbQueryStatus findSongById(String songId) {
		DbQueryStatus findSongIdQuery = new DbQueryStatus("",DbQueryExecResult.QUERY_ERROR_GENERIC);
		
		Query query = new Query();
		query.addCriteria(Criteria.where("_id").is(new ObjectId(songId.replace("\"", ""))));
		
		boolean dataExists = db.exists(query, "songs");
		
		if (dataExists) {
			Song foundSong = db.findOne(query, Song.class, "songs");
			findSongIdQuery.setMessage("successfully found song");
			findSongIdQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
			findSongIdQuery.setData(foundSong);
		} else {
			findSongIdQuery.setMessage("song can't be found");
			findSongIdQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			findSongIdQuery.setData(null);
		}

		return findSongIdQuery;
	}

	@Override
	public DbQueryStatus getSongTitleById(String songId) {
		DbQueryStatus findSongTitleIdQuery = new DbQueryStatus("",DbQueryExecResult.QUERY_ERROR_GENERIC);
		
		Query query = new Query();
		query.addCriteria(Criteria.where("_id").is(new ObjectId(songId.replace("\"", ""))));
		
		boolean dataExists = db.exists(query, "songs");
		
		if (dataExists) {
			Song foundSong = db.findOne(query, Song.class, "songs");
			findSongTitleIdQuery.setMessage("successfully found song");
			findSongTitleIdQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
			findSongTitleIdQuery.setData(foundSong);
		} else {
			findSongTitleIdQuery.setMessage("song can't be found");
			findSongTitleIdQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		}
		
		
		//trace request
		
		return findSongTitleIdQuery;
	}

	@Override
	public DbQueryStatus deleteSongById(String songId) {
		DbQueryStatus deleteSongQuery = new DbQueryStatus("",DbQueryExecResult.QUERY_ERROR_GENERIC);
		
		Query query = new Query();
		query.addCriteria(Criteria.where("_id").is(new ObjectId(songId)));
		
		boolean dataExists = db.exists(query, "songs");
		
		if (dataExists) {
			db.findAllAndRemove(query, "songs");
			deleteSongQuery.setMessage("successfully deleted song");
			deleteSongQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
			
		} else {
			deleteSongQuery.setMessage("song cannot be deleted because it doesn't exist");
			deleteSongQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		}
		
		
		//try catch for hexadecimal representation
		return deleteSongQuery;
	}

	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
	DbQueryStatus favCountQuery = new DbQueryStatus("",DbQueryExecResult.QUERY_ERROR_GENERIC);
	DbQueryStatus findSong = findSongById(songId);
	
		if(findSong.getdbQueryExecResult() == DbQueryExecResult.QUERY_OK) {
				Song songToChange = (Song) findSong.getData();
			if (shouldDecrement) {
				long songFavCount = songToChange.getSongAmountFavourites();
				
				if (songFavCount == 0) {
					favCountQuery.setMessage("Song favourite count already at 0");
					favCountQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
				} else {
					songToChange.setSongAmountFavourites(songFavCount - 1);
					db.save(songToChange, "songs");
					favCountQuery.setMessage("decreased song favourite count");
					favCountQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
				}
			} else {
				long songFavCount = songToChange.getSongAmountFavourites() + 1;
				songToChange.setSongAmountFavourites(songFavCount);
				db.save(songToChange, "songs");
				favCountQuery.setMessage("increased song favourite count");
				favCountQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
			}
	
		} else {
			favCountQuery.setMessage("can't update song count as song doesnt exist");
			favCountQuery.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		}
		return favCountQuery;
	}
}