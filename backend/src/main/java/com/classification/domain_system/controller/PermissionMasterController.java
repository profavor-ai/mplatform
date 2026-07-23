package com.classification.domain_system.controller;

import com.classification.domain_system.entity.PermissionGroup;
import com.classification.domain_system.entity.PermissionItem;
import com.classification.domain_system.service.PermissionMasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/permissions/groups")
@RequiredArgsConstructor
public class PermissionMasterController {

    private final PermissionMasterService permissionMasterService;

    @GetMapping
    public ResponseEntity<List<PermissionGroup>> getAllGroups() {
        return ResponseEntity.ok(permissionMasterService.getAllPermissionGroups());
    }

    @PostMapping
    public ResponseEntity<PermissionGroup> createGroup(@RequestBody PermissionGroup group) {
        return ResponseEntity.ok(permissionMasterService.createGroup(group));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PermissionGroup> updateGroup(@PathVariable String id, @RequestBody PermissionGroup group) {
        return ResponseEntity.ok(permissionMasterService.updateGroup(id, group));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable String id) {
        permissionMasterService.deleteGroup(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{groupId}/items")
    public ResponseEntity<PermissionItem> addItemToGroup(@PathVariable String groupId, @RequestBody PermissionItem item) {
        return ResponseEntity.ok(permissionMasterService.addItemToGroup(groupId, item));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable UUID itemId) {
        permissionMasterService.deleteItem(itemId);
        return ResponseEntity.ok().build();
    }
}
