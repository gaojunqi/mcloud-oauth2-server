package me.javaroad.oauth.service;

import java.util.Objects;
import java.util.Set;
import me.javaroad.common.exception.DataNotFoundException;
import me.javaroad.common.exception.InvalidParameterException;
import me.javaroad.oauth.dto.request.ClientRequest;
import me.javaroad.oauth.dto.response.ClientResponse;
import me.javaroad.oauth.entity.Approval;
import me.javaroad.oauth.entity.Authority;
import me.javaroad.oauth.entity.Client;
import me.javaroad.oauth.entity.Resource;
import me.javaroad.oauth.entity.Scope;
import me.javaroad.oauth.mapper.ClientMapper;
import me.javaroad.oauth.repository.ClientRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * @author heyx
 */
@Service
@Transactional(readOnly = true)
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final ResourceService resourceService;
    private final ApprovalService approvalService;
    private final AuthorityService authorityService;
    private final ScopeService scopeService;

    @Autowired
    public ClientService(ClientRepository clientRepository, ClientMapper mapper,
        PasswordEncoder passwordEncoder, ResourceService resourceService, ApprovalService approvalService,
        AuthorityService authorityService, ScopeService scopeService) {
        this.clientRepository = clientRepository;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.resourceService = resourceService;
        this.approvalService = approvalService;
        this.authorityService = authorityService;
        this.scopeService = scopeService;
    }

    public Client getClient(Long clientId) {
        return clientRepository.findOne(clientId);
    }

    @Transactional
    public ClientResponse createClient(ClientRequest clientRequest) {
        Client client = mapper.mapRequestToEntity(clientRequest);
        client.setClientSecret(passwordEncoder.encode(clientRequest.getClientSecret()));
        setResourceToClient(client, clientRequest.getResourceIds());
        setAutoApproveToClient(client, clientRequest.getAutoApproveIds());
        setAuthorityToClient(client, clientRequest.getAuthorityIds());
        setScopeToClient(client, clientRequest.getScopeIds());
        client = clientRepository.save(client);
        return mapper.mapEntityToResponse(client);
    }

    private void setScopeToClient(Client client, Set<Long> scopeIds) {
        Set<Scope> scopes = scopeService.getScopeByIds(scopeIds);
        if (CollectionUtils.isEmpty(scopes)) {
            throw new InvalidParameterException("invalid scopeIds");
        }
        client.setScope(scopes);
    }

    private void setAuthorityToClient(Client client, Set<Long> authorityIds) {
        Set<Authority> authorities = authorityService.getAuthorityByIds(authorityIds);
        if (CollectionUtils.isEmpty(authorities)) {
            throw new InvalidParameterException("invalid authorityIds");
        }
        client.setAuthorities(authorities);
    }

    private void setAutoApproveToClient(Client client, Set<Long> autoApproveIds) {
        if (CollectionUtils.isEmpty(autoApproveIds)) {
            return;
        }
        Set<Approval> approvals = approvalService.getApprovalByIds(autoApproveIds);
        if (CollectionUtils.isEmpty(approvals)) {
            throw new InvalidParameterException("invalid autoApproveIds");
        }
        client.setAutoApprove(approvals);
    }

    private void setResourceToClient(Client client, Set<Long> clientIds) {
        Set<Resource> resources = resourceService.getResourceByIds(clientIds);
        if (CollectionUtils.isEmpty(resources)) {
            throw new InvalidParameterException("invalid resourceIds");
        }
        client.setResources(resources);
    }

    @Transactional
    public Client modifyClient(ClientRequest clientRequest) {
        Client client = mapper.mapRequestToEntity(clientRequest);
        if (StringUtils.isNotBlank(clientRequest.getClientSecret())) {
            client.setClientSecret(passwordEncoder.encode(clientRequest.getClientSecret()));
        }
        return clientRepository.save(client);
    }

    @Transactional
    public void deleteClient(Long clientId) {
        Client client = getClient(clientId);
        if(Objects.isNull(client)) {
            throw new DataNotFoundException("client[id=%s] not found", clientId);
        }
        clientRepository.delete(client);
    }

    public ClientResponse getClientByClientId(String clientId) {
        Client client = clientRepository.findByClientId(clientId);
        return mapper.mapEntityToResponse(client);
    }
}