package com.classification.domain_system.controller;

import com.classification.domain_system.entity.Organization;
import com.classification.domain_system.entity.Department;
import com.classification.domain_system.entity.Team;
import com.classification.domain_system.repository.OrganizationRepository;
import com.classification.domain_system.repository.DepartmentRepository;
import com.classification.domain_system.repository.TeamRepository;
import com.classification.domain_system.service.RoleInitializer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationRepository organizationRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final RoleInitializer roleInitializer;

    public OrganizationController(OrganizationRepository organizationRepository,
                                  DepartmentRepository departmentRepository,
                                  TeamRepository teamRepository,
                                  RoleInitializer roleInitializer) {
        this.organizationRepository = organizationRepository;
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
        this.roleInitializer = roleInitializer;
    }

    @GetMapping
    public ResponseEntity<List<Organization>> getAllOrganizations() {
        return ResponseEntity.ok(organizationRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Organization> getOrganization(@PathVariable UUID id) {
        return organizationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Organization> createOrganization(@RequestBody Organization org) {
        Organization saved = organizationRepository.save(org);
        roleInitializer.createDefaultRolesForOrg(saved.getId());
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{orgId}/departments")
    public ResponseEntity<List<Department>> getDepartments(@PathVariable UUID orgId) {
        return ResponseEntity.ok(departmentRepository.findByOrganizationId(orgId));
    }

    @PostMapping("/{orgId}/departments")
    public ResponseEntity<Department> createDepartment(@PathVariable UUID orgId, @RequestBody Department dept) {
        dept.setOrganizationId(orgId);
        return ResponseEntity.ok(departmentRepository.save(dept));
    }

    @PutMapping("/{orgId}/departments/{deptId}")
    public ResponseEntity<Department> updateDepartment(@PathVariable UUID orgId, @PathVariable UUID deptId, @RequestBody Department deptReq) {
        return departmentRepository.findById(deptId)
                .map(existing -> {
                    existing.setName(deptReq.getName());
                    existing.setDescription(deptReq.getDescription());
                    existing.setParentDepartmentId(deptReq.getParentDepartmentId());
                    return ResponseEntity.ok(departmentRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{orgId}/departments/{deptId}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable UUID orgId, @PathVariable UUID deptId) {
        return departmentRepository.findById(deptId)
                .map(dept -> {
                    departmentRepository.delete(dept);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{orgId}/teams")
    public ResponseEntity<List<Team>> getTeams(@PathVariable UUID orgId) {
        return ResponseEntity.ok(teamRepository.findByOrganizationId(orgId));
    }

    @PostMapping("/{orgId}/teams")
    public ResponseEntity<Team> createTeam(@PathVariable UUID orgId, @RequestBody Team team) {
        team.setOrganizationId(orgId);
        return ResponseEntity.ok(teamRepository.save(team));
    }
}
