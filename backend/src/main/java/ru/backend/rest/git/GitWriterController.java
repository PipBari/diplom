package ru.backend.rest.git;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.backend.rest.git.dto.FileNodeDto;
import ru.backend.rest.git.dto.GitConnectionRequestDto;
import ru.backend.service.git.GitService;
import ru.backend.service.git.GitWriterService;

import java.util.List;

@RestController
@RequestMapping("/git/writer")
@RequiredArgsConstructor
public class GitWriterController {

    private final GitWriterService gitWriterService;
    private final GitService gitService;

    @GetMapping("/{name}/branch/{branch}/exists")
    public ResponseEntity<Boolean> branchExists(@PathVariable String name, @PathVariable String branch) {
        GitConnectionRequestDto repo = gitService.getByName(name);
        return ResponseEntity.ok(gitWriterService.branchExists(repo, branch));
    }

    @GetMapping("/{name}/branch/{branch}/path/exists")
    public ResponseEntity<Boolean> pathExists(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestParam String path
    ) {
        GitConnectionRequestDto repo = gitService.getByName(name);
        return ResponseEntity.ok(gitWriterService.pathExists(repo, branch, path));
    }

    @GetMapping("/{name}/branch/{branch}/folders")
    public ResponseEntity<List<String>> listFolders(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestParam(defaultValue = "") String basePath
    ) {
        GitConnectionRequestDto repo = gitService.getByName(name);
        return ResponseEntity.ok(gitWriterService.listFolders(repo, branch, basePath));
    }

    @GetMapping("/{name}/branch/{branch}/tree")
    public ResponseEntity<List<FileNodeDto>> listFiles(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestParam(defaultValue = "") String path
    ) {
        GitConnectionRequestDto repo = gitService.getByName(name);
        return ResponseEntity.ok(gitWriterService.listFiles(repo, branch, path));
    }
}
