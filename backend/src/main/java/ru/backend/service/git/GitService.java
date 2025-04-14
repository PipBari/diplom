package ru.backend.service.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class GitService {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public void cloneOrUpdateRepository(String repoUrl, String localPath, String branch, String username, String token) throws GitAPIException, IOException {
        File repoDir = new File(localPath);

        if (repoDir.exists() && new File(repoDir, ".git").exists()) {
            try (Git git = Git.open(repoDir)) {
                git.checkout().setName(branch).call();
                git.pull()
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, token))
                        .call();
                System.out.println("Репозиторий обновлен (git pull) на ветке: " + branch);
            }
        } else {
            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(repoDir)
                    .setBranch(branch)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, token))
                    .call();
            System.out.println("Репозиторий клонирован на ветке: " + branch);
        }
    }

    public List<String> getCommitHistory(String localPath, int limit) throws GitAPIException, IOException {
        File repoDir = new File(localPath);
        if (!repoDir.exists() || !new File(repoDir, ".git").exists()) {
            throw new IOException("Репозиторий не найден по пути: " + localPath);
        }

        List<String> commits = new ArrayList<>();

        try (Git git = Git.open(repoDir)) {
            Iterable<RevCommit> log = git.log().setMaxCount(limit).call();
            for (RevCommit commit : log) {
                String message = commit.getShortMessage();
                String author = commit.getAuthorIdent().getName();
                String timestamp = DATE_FORMAT.format(new Date(commit.getCommitTime() * 1000L));
                commits.add(message + " (" + author + ", " + timestamp + ")");
            }
        }
        return commits;
    }

    public String getLastCommit(String localPath) throws GitAPIException, IOException {
        List<String> commits = getCommitHistory(localPath, 1);
        return commits.isEmpty() ? "Нет коммитов" : commits.get(0);
    }
}
