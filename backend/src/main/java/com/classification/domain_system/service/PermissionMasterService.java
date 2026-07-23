package com.classification.domain_system.service;

import com.classification.domain_system.entity.PermissionGroup;
import com.classification.domain_system.entity.PermissionItem;
import com.classification.domain_system.repository.PermissionGroupRepository;
import com.classification.domain_system.repository.PermissionItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionMasterService {

    private final PermissionGroupRepository groupRepository;
    private final PermissionItemRepository itemRepository;

    @Transactional(readOnly = true)
    public List<PermissionGroup> getAllPermissionGroups() {
        return groupRepository.findAllByOrderBySortOrderAsc();
    }

    @Transactional
    public PermissionGroup createGroup(PermissionGroup group) {
        if (group.getId() == null || group.getId().isBlank()) {
            group.setId(group.getCode().toLowerCase().trim());
        }
        return groupRepository.save(group);
    }

    @Transactional
    public PermissionGroup updateGroup(String groupId, PermissionGroup updated) {
        PermissionGroup existing = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));
        
        existing.setTitleKo(updated.getTitleKo());
        existing.setTitleEn(updated.getTitleEn());
        if (updated.getIcon() != null) existing.setIcon(updated.getIcon());
        if (updated.getColor() != null) existing.setColor(updated.getColor());
        if (updated.getChipClass() != null) existing.setChipClass(updated.getChipClass());
        return groupRepository.save(existing);
    }

    @Transactional
    public void deleteGroup(String groupId) {
        groupRepository.deleteById(groupId);
    }

    @Transactional
    public PermissionItem addItemToGroup(String groupId, PermissionItem item) {
        PermissionGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));
        item.setGroup(group);
        return itemRepository.save(item);
    }

    @Transactional
    public void deleteItem(UUID itemId) {
        itemRepository.deleteById(itemId);
    }
}
