package com.chatapp.service;

import com.chatapp.domain.model.Room;
import com.chatapp.domain.model.Room.Member;
import com.chatapp.domain.model.Room.MemberRole;
import com.chatapp.domain.model.Room.RoomType;
import com.chatapp.domain.repository.RoomRepository;
import com.chatapp.internal.dto.CreateRoomRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

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

    public List<Room> getRoomsForUser(String userId) {
        return roomRepository.findAllByMemberUserId(userId);
    }

    public Room getRoom(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
    }
}
