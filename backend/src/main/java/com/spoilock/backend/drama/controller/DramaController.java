package com.spoilock.backend.drama.controller;

import com.spoilock.backend.drama.domain.dto.DramaDetailResponse;
import com.spoilock.backend.drama.domain.dto.DramaResponse;
import com.spoilock.backend.drama.domain.dto.EpisodeResponse;
import com.spoilock.backend.drama.service.DramaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dramas")
@RequiredArgsConstructor
public class DramaController {

    private final DramaService dramaService;

    @GetMapping
    public List<DramaResponse> getDramas()
    {
        return dramaService.getDramas();
    }

    @GetMapping("/{dramaId}")
    public DramaDetailResponse getDrama(
            @PathVariable Long dramaId
    ) {
        return dramaService.getDrama(dramaId);
    }

    @GetMapping("/{dramaId}/episodes")
    public List<EpisodeResponse> getEpisodes(
            @PathVariable Long dramaId
    )
    {
        return dramaService.getEpisodes(dramaId);
    }

}
