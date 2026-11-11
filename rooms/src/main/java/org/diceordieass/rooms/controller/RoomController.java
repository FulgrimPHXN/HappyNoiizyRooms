package org.diceordieass.rooms.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.diceordieass.rooms.dto.RoomDto;
import org.diceordieass.rooms.dto.RoomRequestDto;
import org.diceordieass.rooms.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
@Validated
public class RoomController {

    private final RoomService roomService;


    @PostMapping
    public ResponseEntity<RoomDto> createRoom(@AuthenticationPrincipal UUID ownerId, @Valid @RequestBody  RoomRequestDto dto) {
        return ResponseEntity.ok(RoomDto.from(roomService.createRoom(ownerId, dto)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomDto> getRoom(@PathVariable("id") UUID roomId) {
        return ResponseEntity.ok(RoomDto.from(roomService.getRoom(roomId)));
    }

    @GetMapping
    public ResponseEntity<List<RoomDto>> getRooms(@AuthenticationPrincipal UUID ownerId) {
        return ResponseEntity.ok(roomService.getOwnerRooms(ownerId)
                .stream().map(RoomDto::from).toList());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RoomDto> updateRoom(@PathVariable("id") UUID roomId, @AuthenticationPrincipal UUID callerId, @Valid @RequestBody RoomRequestDto dto) {
        return ResponseEntity.ok(RoomDto.from(roomService.updateRoom(roomId, callerId, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable("id") UUID roomId, @AuthenticationPrincipal UUID callerId) {
        roomService.deleteRoom(roomId, callerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<RoomDto> joinRoom(@PathVariable("id") UUID roomId, @AuthenticationPrincipal UUID callerId, @RequestParam @NotBlank String inviteCode) {
        return ResponseEntity.ok(RoomDto.from(roomService.joinRoom(roomId, callerId, inviteCode)));
    }

    @DeleteMapping("/{id}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable("id") UUID roomId, @AuthenticationPrincipal UUID callerId) {
        roomService.leaveRoom(roomId, callerId);
        return ResponseEntity.noContent().build();
    }
}
