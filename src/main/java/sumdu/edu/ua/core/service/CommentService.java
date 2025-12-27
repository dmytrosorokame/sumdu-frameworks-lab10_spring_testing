package sumdu.edu.ua.core.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sumdu.edu.ua.core.domain.Comment;
import sumdu.edu.ua.core.port.CommentRepositoryPort;
import sumdu.edu.ua.persistence.entity.CommentEntity;
import sumdu.edu.ua.persistence.repository.CommentRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {
    private final CommentRepositoryPort repo;
    private final CommentRepository commentRepository;

    @Autowired
    public CommentService(CommentRepositoryPort repo, CommentRepository commentRepository) {
        this.repo = repo;
        this.commentRepository = commentRepository;
    }

    /**
     * Validates comment fields according to business rules.
     *
     * @param bookId the book ID
     * @param author the comment author
     * @param text the comment text
     * @throws IllegalArgumentException if validation fails
     */
    public void validateCommentFields(long bookId, String author, String text) {
        if (bookId <= 0) {
            throw new IllegalArgumentException("bookId must be greater than 0");
        }
        if (author == null || author.isBlank()) {
            throw new IllegalArgumentException("author is required");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text is required");
        }
    }

    /**
     * Adds a new comment after validation.
     *
     * @param bookId the book ID
     * @param author the comment author
     * @param text the comment text
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional
    public void addComment(long bookId, String author, String text) {
        validateCommentFields(bookId, author, text);
        repo.add(bookId, author.trim(), text.trim());
    }

    /**
     * Deletes a comment only if it was created not more than 24 hours ago.
     * Only ADMIN can delete comments.
     *
     * @param bookId the book ID
     * @param commentId the comment ID
     * @throws IllegalStateException if the comment was not found or is older than 24 hours
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(long bookId, long commentId) {
        Comment comment = repo.findById(bookId, commentId);
        if (comment == null) {
            throw new IllegalStateException("The comment was not found");
        }

        Instant createdAt = comment.getCreatedAt();
        if (createdAt == null) {
            throw new IllegalStateException("The time of creation of the comment is unknown");
        }

        if (Duration.between(createdAt, Instant.now()).toHours() > 24) {
            throw new IllegalStateException("You can't delete a comment that is older than 24 hours");
        }

        repo.delete(bookId, commentId);
    }

    @Transactional(readOnly = true)
    public List<Comment> getCommentsByUser(Long userId) {
        List<CommentEntity> entities = commentRepository.findByUserId(userId);
        return entities.stream()
            .map(e -> new Comment(
                e.getId(),
                e.getBook().getId(),
                e.getUser().getId(),
                e.getBook().getTitle(),
                e.getUser().getEmail(),
                e.getText(),
                e.getCreatedAt()
            ))
            .collect(Collectors.toList());
    }
}
