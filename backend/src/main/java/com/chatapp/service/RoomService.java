package com.chatapp.service;

import com.chatapp.domain.model.Room;
import com.chatapp.domain.model.Room.Member;
import com.chatapp.domain.model.Room.MemberRole;
import com.chatapp.domain.model.Room.RoomType;
import com.chatapp.domain.model.User;
import com.chatapp.domain.repository.MessageRepository;
import com.chatapp.domain.repository.RoomRepository;
import com.chatapp.domain.repository.UserRepository;
import com.chatapp.api.dto.MuteRequest.MuteDuration;
import com.chatapp.internal.dto.CreateRoomRequest;
import com.chatapp.internal.dto.RoomSummaryDto;
import com.chatapp.internal.dto.UpdateRoomRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    public Room createRoom(CreateRoomRequest request) {
        List<Member> members = new ArrayList<>();
        Instant now = Instant.now();

        for (String memberId : request.memberIds()) {
            MemberRole role = memberId.equals(request.adminId()) ? MemberRole.ADMIN : MemberRole.MEMBER;
            members.add(Member.builder()
                    .userId(memberId)
                    .role(role)
                    .joinedAt(now)
                    .build());
        }

        Room room = Room.builder()
                .type(request.type())
                .name(request.name())
                .avatarUrl(request.avatarUrl())
                .members(members)
                .active(Boolean.TRUE)
                .build();

        return roomRepository.save(room);
    }

    public void deleteRoom(String roomId) {
        roomRepository.deleteById(roomId);
    }

    public void deactivateRoom(String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        room.setActive(Boolean.FALSE);
        roomRepository.save(room);
    }

    public Room addMembers(String roomId, List<String> memberIds) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        Instant now = Instant.now();
        List<Member> members = new ArrayList<>(room.getMembers());

        for (String memberId : memberIds) {
            boolean alreadyMember = members.stream()
                    .anyMatch(m -> m.getUserId().equals(memberId));
            if (!alreadyMember) {
                members.add(Member.builder()
                        .userId(memberId)
                        .role(MemberRole.MEMBER)
                        .joinedAt(now)
                        .build());
            }
        }

        room.setMembers(members);
        return roomRepository.save(room);
    }

    public Room removeMember(String roomId, String userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        List<Member> members = room.getMembers().stream()
                .filter(m -> !m.getUserId().equals(userId))
                .toList();

        room.setMembers(members);
        return roomRepository.save(room);
    }

    public List<Room> getRoomsForUser(String userId, Room.RoomType type) {
        List<Room> rooms = type != null
                ? roomRepository.findAllByMemberUserIdAndType(userId, type)
                : roomRepository.findAllByMemberUserId(userId);
        enrichDirectRooms(rooms, userId);
        return rooms;
    }

    private void enrichDirectRooms(List<Room> rooms, String currentUserId) {
        Set<String> otherUserIds = rooms.stream()
                .filter(r -> r.getType() == RoomType.DIRECT)
                .flatMap(r -> r.getMembers().stream())
                .map(Member::getUserId)
                .filter(id -> !id.equals(currentUserId))
                .collect(Collectors.toSet());

        log.info("[enrich] currentUserId={} otherUserIds={}", currentUserId, otherUserIds);

        if (otherUserIds.isEmpty()) return;

        Map<String, User> userMap = userRepository.findAllByExternalIdIn(otherUserIds)
                .stream()
                .collect(Collectors.toMap(User::getExternalId, u -> u));

        log.info("[enrich] found {} users in DB out of {} needed", userMap.size(), otherUserIds.size());

        for (Room room : rooms) {
            if (room.getType() != RoomType.DIRECT) continue;

            room.getMembers().stream()
                    .map(Member::getUserId)
                    .filter(id -> !id.equals(currentUserId))
                    .findFirst()
                    .map(userMap::get)
                    .ifPresent(user -> {
                        room.setName(user.getDisplayName());
                        room.setAvatarUrl(user.getAvatarUrl());
                    });
        }
    }

    public Slice<Room> getRoomsForUser(String userId, RoomType type, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "lastActivityAt")
                .and(Sort.by(Sort.Direction.DESC, "createdAt"));
        PageRequest pageable = PageRequest.of(page, size, sort);
        Slice<Room> slice = type != null
                ? roomRepository.findByMemberUserIdAndType(userId, type, pageable)
                : roomRepository.findByMemberUserId(userId, pageable);

        List<Room> rooms = slice.getContent();
        enrichDirectRooms(rooms, userId);
        attachUnreadCounts(rooms, userId);
        return slice;
    }

    /** Single aggregation to get unread counts for all rooms in one query. */
    private void attachUnreadCounts(List<Room> rooms, String userId) {
        if (rooms.isEmpty()) return;
        List<String> roomIds = rooms.stream().map(Room::getId).toList();

        // One $match + $group instead of N count queries
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(
                        Criteria.where("roomId").in(roomIds)
                                .and("senderId").ne(userId)
                                .and("isDeleted").ne(true)
                                .and("readBy.userId").ne(userId)
                ),
                Aggregation.group("roomId").count().as("count")
        );

        record UnreadResult(String id, long count) {}

        Map<String, Long> counts = mongoTemplate
                .aggregate(agg, "messages", UnreadResult.class)
                .getMappedResults()
                .stream()
                .collect(Collectors.toMap(UnreadResult::id, UnreadResult::count));

        rooms.forEach(r -> r.setUnreadCount(counts.getOrDefault(r.getId(), 0L)));
    }

    public Slice<RoomSummaryDto> getRoomSummariesForUser(String userId, RoomType type, int page, int size) {
        Slice<Room> rooms = getRoomsForUser(userId, type, page, size);
        return rooms.map(room -> new RoomSummaryDto(
                room,
                messageRepository.findFirstByRoomIdOrderByCreatedAtDesc(room.getId()).orElse(null),
                messageRepository.countByRoomIdAndSenderIdNotAndReadByUserIdNot(room.getId(), userId)
        ));
    }

    public Room updateRoom(String roomId, UpdateRoomRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        if (request.name() != null && !request.name().isBlank()) room.setName(request.name());
        if (request.avatarUrl() != null) room.setAvatarUrl(request.avatarUrl());

        return roomRepository.save(room);
    }

    public Room getRoom(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
    }

    public Room findOrCreateDirectRoom(String userId1, String userId2) {
        return roomRepository.findDirectRoom(userId1, userId2)
                .orElseGet(() -> createRoom(new CreateRoomRequest(
                        Room.RoomType.DIRECT,
                        null, null,
                        List.of(userId1, userId2),
                        null)));
    }

    public Room muteRoom(String roomId, String userId, MuteDuration duration) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        Instant mutedUntil = switch (duration) {
            case HOURS_8 -> Instant.now().plusSeconds(8 * 3600);
            case WEEK_1 -> Instant.now().plusSeconds(7 * 24 * 3600);
            case INDEFINITE -> null;
        };

        List<Member> updated = room.getMembers().stream()
                .map(m -> {
                    if (m.getUserId().equals(userId)) {
                        m.setMuted(true);
                        m.setMutedUntil(mutedUntil);
                    }
                    return m;
                })
                .toList();

        room.setMembers(updated);
        return roomRepository.save(room);
    }

    public Room unmuteRoom(String roomId, String userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        List<Member> updated = room.getMembers().stream()
                .map(m -> {
                    if (m.getUserId().equals(userId)) {
                        m.setMuted(false);
                        m.setMutedUntil(null);
                    }
                    return m;
                })
                .toList();

        room.setMembers(updated);
        return roomRepository.save(room);
    }
}
