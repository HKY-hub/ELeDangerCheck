package org.example.eledangercheck.service;

import org.example.eledangercheck.entity.Disclosure;
import org.example.eledangercheck.exception.BusinessException;
import org.example.eledangercheck.mapper.DisclosureMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DisclosureService {

    private final DisclosureMapper disclosureMapper;

    public DisclosureService(DisclosureMapper disclosureMapper) {
        this.disclosureMapper = disclosureMapper;
    }

    public Disclosure createDisclosure(Disclosure disclosure) {
        disclosure.setCreateTime(LocalDateTime.now());
        if (disclosure.getDisclosureStatus() == null) {
            disclosure.setDisclosureStatus("draft");
        }
        disclosureMapper.insert(disclosure);
        return disclosure;
    }

    public Disclosure getDisclosureById(Long id) {
        Disclosure disclosure = disclosureMapper.selectById(id);
        if (disclosure == null) {
            throw new BusinessException("交底记录不存在");
        }
        return disclosure;
    }

    public Disclosure getDisclosureByTaskId(Long taskId) {
        Disclosure disclosure = disclosureMapper.selectByTaskId(taskId);
        if (disclosure == null) {
            throw new BusinessException("未找到该任务的交底记录");
        }
        return disclosure;
    }

    public List<Disclosure> getDisclosuresByStatus(String status) {
        return disclosureMapper.selectByDisclosureStatus(status);
    }

    public List<Disclosure> getAllDisclosures() {
        return disclosureMapper.selectList(null);
    }

    public Disclosure updateDisclosure(Long id, Disclosure disclosureDetails) {
        Disclosure disclosure = getDisclosureById(id);
        if (disclosureDetails.getDisclosureNo() != null) {
            disclosure.setDisclosureNo(disclosureDetails.getDisclosureNo());
        }
        if (disclosureDetails.getTitle() != null) {
            disclosure.setTitle(disclosureDetails.getTitle());
        }
        if (disclosureDetails.getDisclosureType() != null) {
            disclosure.setDisclosureType(disclosureDetails.getDisclosureType());
        }
        if (disclosureDetails.getContent() != null) {
            disclosure.setContent(disclosureDetails.getContent());
        }
        if (disclosureDetails.getPdfPath() != null) {
            disclosure.setPdfPath(disclosureDetails.getPdfPath());
        }
        if (disclosureDetails.getWordPath() != null) {
            disclosure.setWordPath(disclosureDetails.getWordPath());
        }
        if (disclosureDetails.getEmergencyContact() != null) {
            disclosure.setEmergencyContact(disclosureDetails.getEmergencyContact());
        }
        if (disclosureDetails.getEmergencyRoute() != null) {
            disclosure.setEmergencyRoute(disclosureDetails.getEmergencyRoute());
        }
        if (disclosureDetails.getDisclosureStatus() != null) {
            disclosure.setDisclosureStatus(disclosureDetails.getDisclosureStatus());
        }
        if (disclosureDetails.getIssuerSignature() != null) {
            disclosure.setIssuerSignature(disclosureDetails.getIssuerSignature());
        }
        if (disclosureDetails.getReceiverSignature() != null) {
            disclosure.setReceiverSignature(disclosureDetails.getReceiverSignature());
        }
        if (disclosureDetails.getDisclosureTime() != null) {
            disclosure.setDisclosureTime(disclosureDetails.getDisclosureTime());
        }
        if (disclosureDetails.getConfirmTime() != null) {
            disclosure.setConfirmTime(disclosureDetails.getConfirmTime());
        }
        disclosure.setUpdateTime(LocalDateTime.now());
        disclosureMapper.updateById(disclosure);
        return disclosure;
    }

    public void deleteDisclosure(Long id) {
        if (disclosureMapper.selectById(id) == null) {
            throw new BusinessException("交底记录不存在");
        }
        disclosureMapper.deleteById(id);
    }
}