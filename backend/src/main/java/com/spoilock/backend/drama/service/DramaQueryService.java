package com.spoilock.backend.drama.service;

import com.spoilock.backend.drama.dto.DramaOptionResponse;
import com.spoilock.backend.drama.repository.DramaQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DramaQueryService {

    private final DramaQueryRepository dramaQueryRepository;

    public List<DramaOptionResponse> getDramaOptions() {
        return dramaQueryRepository.findOptions();
    }
}