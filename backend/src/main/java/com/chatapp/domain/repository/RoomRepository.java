package com.chatapp.domain.repository;

import com.chatapp.domain.model.Room;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface RoomRepository extends MongoRepository<Room, String> {

    @Query("{ 'members.userId': ?0 }")
    List<Room> findAllByMemberUserId(String userId);

    @Query("{ 'type': 'DIRECT', 'members.userId': { $all: [?0, ?1] }, $expr: { $eq: [{ $size: '$members' }, 2] } }")
    java.util.Optional<Room> findDirectRoom(String userId1, String userId2);
}
