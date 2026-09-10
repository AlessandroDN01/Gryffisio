package org.generation.italy.services;

import org.generation.italy.model.dto.CreateUserRequest;
import org.generation.italy.model.dto.OperatorDto;
import org.generation.italy.model.dto.OperatorRequest;
import org.generation.italy.model.entities.Operator;
import org.generation.italy.model.exceptions.BadRequestException;
import org.generation.italy.model.exceptions.ConflictException;
import org.generation.italy.model.exceptions.NotFoundException;
import org.generation.italy.model.repositories.OperatorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OperatorService {
    private final OperatorRepository operatorRepository;
    private final PasswordEncoder passwordEncoder;

    public OperatorService(OperatorRepository operatorRepository, PasswordEncoder passwordEncoder) {
        this.operatorRepository = operatorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private OperatorDto toDto(Operator operator) {
        return new OperatorDto(
                operator.getId(),
                operator.getFirstName(),
                operator.getLastName(),
                operator.getEmail(),
                operator.getRole().name()
        );
    }

    private Operator.Role parseRole(String role) {
        try {
            return Operator.Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid_role", "Role must be OPERATOR or ADMIN, got: " + role);
        }
    }

    @Transactional(readOnly = true)
    public List<OperatorDto> findAll() {
        return operatorRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public OperatorDto findById(Integer id) throws NotFoundException {
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Operator_not_found", "Operator not found: " + id));
        return toDto(operator);
    }

    @Transactional(readOnly = true)
    public List<OperatorDto> findByFirstNameOrLastName(String firstName, String lastName) {
       return operatorRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(firstName, lastName).stream()
               .map(this::toDto)
               .toList();
    }

    @Transactional
    public OperatorDto createOperator(CreateUserRequest request) {
        if (operatorRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Operator_email_already_exists", "Operator email already exists: " + request.email());
        }

        Operator operator = new Operator();
        operator.setFirstName(request.firstName());
        operator.setLastName(request.lastName());
        operator.setEmail(request.email());
        operator.setRole(parseRole(request.role()));
        operator.setPasswordHash(passwordEncoder.encode(request.password()));
        Operator saved = operatorRepository.save(operator);
        return toDto(saved);
    }

    @Transactional
    public OperatorDto resetPassword(Integer id, String newPassword) throws NotFoundException {
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Operator_not_found", "Operator not found: " + id));

        operator.setPasswordHash(passwordEncoder.encode(newPassword));
        return toDto(operatorRepository.save(operator));
    }

    @Transactional
    public OperatorDto updateOperator(Integer id, OperatorRequest request) throws NotFoundException {
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Operator_not_found", "Operator not found: " + id));

        if (operatorRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new ConflictException("Operator_email_already_exists", "Operator email already exists: " + request.email());
        }

        operator.setFirstName(request.firstName());
        operator.setLastName(request.lastName());
        operator.setEmail(request.email());
        operator.setRole(parseRole(request.role()));
        return toDto(operatorRepository.save(operator));
    }

    @Transactional
    public void deleteOperator(Integer id) throws NotFoundException {
        if (!operatorRepository.existsById(id)) {
            throw new NotFoundException("Operator_not_found", "Operator not found: " + id);
        }
        operatorRepository.deleteById(id);
    }
}