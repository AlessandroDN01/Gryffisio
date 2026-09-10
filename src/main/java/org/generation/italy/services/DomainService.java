package org.generation.italy.services;

import org.generation.italy.model.dto.DomainDto;
import org.generation.italy.model.dto.DomainRequest;
import org.generation.italy.model.entities.Activity;
import org.generation.italy.model.entities.Domain;
import org.generation.italy.model.exceptions.ConflictException;
import org.generation.italy.model.exceptions.NotFoundException;
import org.generation.italy.model.repositories.ActivityRepository;
import org.generation.italy.model.repositories.DomainRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DomainService {
    private final DomainRepository domainRepository;
    private final ActivityRepository activityRepository;

    public DomainService(DomainRepository domainRepository, ActivityRepository activityRepository) {
        this.domainRepository = domainRepository;
        this.activityRepository = activityRepository;
    }

    private DomainDto toDto(Domain domain) {
        return new DomainDto(
                domain.getId(),
                domain.getName(),
                domain.getActivities().stream().map(Activity::getId).toList(),
                domain.getActivities().stream().map(Activity::getName).toList()
        );
    }

    @Transactional(readOnly = true)
    public List<DomainDto> findAll() {
        return domainRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DomainDto findById(Integer id) throws NotFoundException {
        Domain domain = domainRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Domain_not_found", "Domain not found: " + id));
        return toDto(domain);
    }

    @Transactional(readOnly = true)
    public DomainDto findByName(String name) throws NotFoundException {
        Domain domain = domainRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new NotFoundException("Domain_not_found", "Domain not found: " + name));
        return toDto(domain);
    }

    @Transactional
    public DomainDto createDomain(DomainRequest request) throws NotFoundException {
        if (domainRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("Domain_name_already_exists", "Domain name already exists: " + request.name());
        }

        Domain domain = new Domain();
        domain.setName(request.name());
        domain.setActivities(resolveActivities(request.activityIds()));
        Domain saved = domainRepository.save(domain);
        return toDto(saved);
    }

    @Transactional
    public DomainDto updateDomain(Integer id, DomainRequest request) throws NotFoundException {
        Domain domain = domainRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Domain_not_found", "Domain not found: " + id));

        if (domainRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new ConflictException("Domain_name_already_exists", "Domain name already exists: " + request.name());
        }

        domain.setName(request.name());
        domain.setActivities(resolveActivities(request.activityIds()));
        return toDto(domainRepository.save(domain));
    }

    private Set<Activity> resolveActivities(List<Integer> activityIds) throws NotFoundException {
        if (activityIds == null || activityIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<Activity> activities = new HashSet<>(activityRepository.findAllById(activityIds));
        if (activities.size() != Set.copyOf(activityIds).size()) {
            throw new NotFoundException("Activity_not_found", "One or more activity ids do not exist");
        }
        return activities;
    }

    @Transactional
    public void deleteDomain(Integer id) throws NotFoundException {
        if (!domainRepository.existsById(id)) {
            throw new NotFoundException("Domain_not_found", "Domain not found: " + id);
        }
        domainRepository.deleteById(id);
    }
}