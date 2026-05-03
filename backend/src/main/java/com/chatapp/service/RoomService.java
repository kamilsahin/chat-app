package com.chatapp.service;

import com.chatapp.domain.model.Room;
import com.chatapp.domain.model.Room.Member;
import com.chatapp.domain.model.Room.MemberRole;
import com.chatapp.domain.model.Room.RoomType;
import com.chatapp.domain.repository.MessageRepository;
import com.chatapp.domain.repository.RoomRepository;
import com.chatapp.api.dto.MuteRequest.MuteDuration;
import com.chatapp.internal.dto.CreateRoomRequest;
import com.chatapp.internal.dto.RoomSummaryDto;
import com.chatapp.internal.dto.UpdateRoomRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;

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
                .build();

        return roomRepository.save(room);
    }

    public void deleteRoom(String roomId) {
        roomRepository.deleteById(roomId);
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
        if (type != null) {
            return roomRepository.findAllByMemberUserIdAndType(userId, type);
        }
        return roomRepository.findAllByMemberUserId(userId);
    }

    public Slice<Room> getRoomsForUser(String userId, RoomType type, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        if (type != null) {
            return roomRepository.findByMemberUserIdAndType(userId, type, pageable);
        }
        return roomRepository.findByMemberUserId(userId, pageable);
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
