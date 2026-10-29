package com.spoilock.backend.drama.controller;

import com.spoilock.backend.drama.domain.dto.DramaOptionResponse;
import com.spoilock.backend.drama.service.DramaQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dramas")
public class DramaQueryController {

    private final DramaQueryService dramaQueryService;

    @GetMapping("/options")
    public List<DramaOptionResponse> getDramaOptions() {
        return dramaQueryService.getDramaOptions();
    }
}