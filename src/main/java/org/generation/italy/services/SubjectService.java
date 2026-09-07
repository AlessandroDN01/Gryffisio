package org.generation.italy.services;

import org.generation.italy.model.dto.SubjectDto;
import org.generation.italy.model.dto.SubjectRequest;
import org.generation.italy.model.dto.SubjectSaveResult;
import org.generation.italy.model.entities.Project;
import org.generation.italy.model.entities.Subject;
import org.generation.italy.model.entities.SubjectType;
import org.generation.italy.model.exceptions.NotFoundException;
import org.generation.italy.model.repositories.ProjectRepository;
import org.generation.italy.model.repositories.SubjectRepository;
import org.generation.italy.model.repositories.SubjectTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubjectService {
    private final SubjectRepository subjectRepository;
    private final ProjectRepository projectRepository;
    private final SubjectTypeRepository subjectTypeRepository;

    public SubjectService(SubjectRepository subjectRepository, ProjectRepository projectRepository, SubjectTypeRepository subjectTypeRepository) {
        this.subjectRepository = subjectRepository;
        this.projectRepository = projectRepository;
        this.subjectTypeRepository = subjectTypeRepository;
    }

    private SubjectDto toDto(Subject subject) {
        return new SubjectDto(
                subject.getId(),
                subject.getProject().getId(),
                subject.getProject().getName(),
                subject.getCode(),
                subject.getSubjectType().getId(),
                subject.getSubjectType().getType()
        );
    }

    @Transactional(readOnly = true)
    public List<SubjectDto> findAll() {
        return subjectRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubjectDto findById(Long id) throws NotFoundException {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Subject_not_found", "Subject not found: " + id));
        return toDto(subject);
    }

    @Transactional(readOnly = true)
    public List<SubjectDto> findByProjectId(Integer projectId) {
        return subjectRepository.findByProject_Id(projectId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public SubjectSaveResult createSubject(SubjectRequest request) throws NotFoundException {
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new NotFoundException("Project_not_found", "Project not found: " + request.projectId()));

        SubjectType subjectType = subjectTypeRepository.findById(request.subjectTypeId())
                .orElseThrow(() -> new NotFoundException("Subject_type_not_found", "Subject type not found: " + request.subjectTypeId()));

        boolean alreadyExists = subjectRepository.existsByProject_IdAndCodeIgnoreCase(request.projectId(), request.code());

        if(alreadyExists) {
            Subject existing = subjectRepository.findByProject_IdAndCodeIgnoreCase(request.projectId(), request.code())
                    .orElseThrow(() -> new NotFoundException("Subject_not_found", "Subject not found"));
            return new SubjectSaveResult(toDto(existing), true);
        }
        Subject subject = new Subject();
        subject.setProject(project);
        subject.setCode(request.code());
        subject.setSubjectType(subjectType);
        Subject saved = subjectRepository.save(subject);

        return new SubjectSaveResult(toDto(saved), false);
    }

    @Transactional
    public SubjectSaveResult updateSubject(Long id, SubjectRequest request) throws NotFoundException {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Subject_not_found", "Subject not found: " + id));

        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new NotFoundException("Project_not_found", "Project not found: " + request.projectId()));

        SubjectType subjectType = subjectTypeRepository.findById(request.subjectTypeId())
                .orElseThrow(() -> new NotFoundException("Subject_type_not_found", "Subject type not found: " + request.subjectTypeId()));

        boolean alreadyExists = subjectRepository.existsByProject_IdAndCodeIgnoreCaseAndIdNot(request.projectId(), request.code(), id);

        if(alreadyExists) {
            Subject existing = subjectRepository.findByProject_IdAndCodeIgnoreCase(request.projectId(), request.code())
                    .orElseThrow(() -> new NotFoundException("Subject_not_found", "Subject not found"));
            return new SubjectSaveResult(toDto(existing), true);
        }
        subject.setProject(project);
        subject.setCode(request.code());
        subject.setSubjectType(subjectType);
        Subject saved = subjectRepository.save(subject);

        return new SubjectSaveResult(toDto(saved), false);
    }

    @Transactional
    public void deleteSubject(Long id) throws NotFoundException {
        if (!subjectRepository.existsById(id)) {
            throw new NotFoundException("Subject_not_found", "Subject not found: " + id);
        }
        subjectRepository.deleteById(id);
    }
}