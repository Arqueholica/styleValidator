package com.docstyler.profile.controller;

import com.docstyler.profile.model.ApprovedStyle;
import com.docstyler.profile.model.StyleProfile;
import com.docstyler.profile.service.ProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public List<StyleProfile> list() {
        return profileService.listAll();
    }

    @GetMapping("/default")
    public StyleProfile getDefault() {
        return profileService.getDefault();
    }

    @GetMapping("/{id}")
    public StyleProfile get(@PathVariable String id) {
        return profileService.findOrThrow(id);
    }

    @GetMapping("/{id}/styles")
    public List<ApprovedStyle> getStyles(@PathVariable String id) {
        return profileService.getApprovedStyles(id);
    }

    @GetMapping("/{id}/names")
    public Set<String> getNames(@PathVariable String id) {
        return profileService.getApprovedNames(id);
    }

    @PostMapping("/upload")
    public ResponseEntity<StyleProfile> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", defaultValue = "Custom Profile") String name) throws IOException {
        String xml = new String(file.getBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.status(HttpStatus.CREATED).body(profileService.create(name, xml));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        profileService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
