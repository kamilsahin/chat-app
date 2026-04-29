package com.chatapp.domain.repository;

import com.chatapp.domain.model.Room;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface RoomRepository extends MongoRepository<Room, String> {

    @Query("{ 'members.userId': ?0 }")
    List<Room> findAllByMemberUserId(String userId);

    @Query("{ 'members.userId': ?0, 'type': ?1 }")
    List<Room> findAllByMemberUserIdAndType(String userId, Room.RoomType type);

    @Query("{ 'members.userId': ?0 }")
    Slice<Room> findByMemberUserId(String userId, Pageable pageable);

    @Query("{ 'members.userId': ?0, 'type': ?1 }")
    Slice<Room> findByMemberUserIdAndType(String userId, Room.RoomType type, Pageable pageable);

    @Query("{ 'type': 'DIRECT', 'members.userId': { $all: [?0, ?1] }, $expr: { $eq: [{ $size: '$members' }, 2] } }")
    java.util.Optional<Room> findDirectRoom(String userId1, String userId2);
}
