package com.security.shiftmanager.service;

import com.security.shiftmanager.dto.ServiceRequest;
import com.security.shiftmanager.dto.ServiceResponse;
import com.security.shiftmanager.model.Service;
import com.security.shiftmanager.repository.ServiceRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Business logic for security services (the places guards are assigned to). */
@org.springframework.stereotype.Service
@Transactional
public class ServiceService {

    private final ServiceRepository serviceRepository;

    public ServiceService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> search(String search, Boolean active) {
        return serviceRepository.search(SearchTerms.toLikePattern(search), active).stream()
                .map(ServiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceResponse findById(Long id) {
        return ServiceResponse.from(getService(id));
    }

    public ServiceResponse create(ServiceRequest request) {
        Service service = new Service();
        apply(service, request);
        return ServiceResponse.from(serviceRepository.save(service));
    }

    public ServiceResponse update(Long id, ServiceRequest request) {
        Service service = getService(id);
        apply(service, request);
        return ServiceResponse.from(service);
    }

    public ServiceResponse setActive(Long id, boolean active) {
        Service service = getService(id);
        service.setActive(active);
        return ServiceResponse.from(service);
    }

    Service getService(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Service not found with id " + id));
    }

    private void apply(Service service, ServiceRequest request) {
        service.setName(request.name().trim());
        service.setLocation(request.location().trim());
        service.setDescription(request.description());
        service.setActive(request.active() == null || request.active());
    }
}
